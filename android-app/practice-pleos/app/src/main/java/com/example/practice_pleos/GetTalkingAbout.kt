package com.example.practice_pleos

import android.content.Context
import android.location.Geocoder
import android.util.Log
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.json.JSONObject

class GetTalkingAbout(context: Context) {
    private val app = context.applicationContext
    private val worker = Executors.newSingleThreadExecutor()
    private var session: RecognitionSession? = null

    private class Pending {
        private val complete = CountDownLatch(1)
        private val endedOrComplete = CountDownLatch(1)
        var text: String? = null
            private set
        var error: String? = null
            private set

        @Synchronized
        fun finish(text: String?, error: String?) {
            if (complete.count == 0L) return
            this.text = text
            this.error = error
            complete.countDown()
            endedOrComplete.countDown()
        }

        fun ended() = endedOrComplete.countDown()

        fun awaitResult() {
            if (!endedOrComplete.await(15, TimeUnit.SECONDS)) {
                finish(null, "callback_timeout")
            } else if (!complete.await(1, TimeUnit.SECONDS)) {
                finish(null, "ended_without_final_result")
            }
        }
    }

    private class RecognitionSession(context: Context) : AutoCloseable {
        private val sdk = SpeechToText(context, Mode.ON_DEVICE)
        @Volatile private var pending: Pending? = null

        private val listener = object : ResultListener {
            override fun onUpdated(stt: String, completed: Boolean) {
                if (completed) pending?.finish(stt, null)
            }

            override fun onError(errorType: ErrorType) {
                pending?.finish(null, errorType.toString())
            }

            override fun onEndedRecognition() {
                pending?.ended()
            }

            override fun onReady() = Unit
            override fun onStartedRecognition() = Unit
            override fun onUpdatedEpdData(on: Long, off: Long) = Unit
            override fun onUpdatedRms(rms: Double) = Unit
        }

        fun initialize(): ErrorType? {
            val initialized = CountDownLatch(1)
            var failure: ErrorType? = null
            sdk.initialize(object : OnInitializeListener {
                override fun onSuccess() = initialized.countDown()

                override fun onFailure(errorType: ErrorType) {
                    failure = errorType
                    initialized.countDown()
                }
            })
            if (!initialized.await(10, TimeUnit.SECONDS)) return ErrorType.TIMEOUT
            failure?.let { return it }

            sdk.addListener(listener)
            sdk.setLocale(Locale.KOREA)
            return null
        }

        fun transcribe(audio: ByteArray): Pending {
            val result = Pending()
            pending = result
            try {
                sdk.sendAudio(audio)
                sdk.completeAudioSend()
                result.awaitResult()
            } catch (error: Exception) {
                Log.e("PleosSTT", "PCM 전사 요청 실패", error)
                result.finish(null, "recognizer_failed")
            } finally {
                pending = null
            }
            return result
        }

        override fun close() {
            try {
                sdk.removeListener(listener)
            } finally {
                sdk.release()
            }
        }
    }

    @Synchronized
    fun initialize(onReady: () -> Unit, onError: (ErrorType) -> Unit) {
        if (worker.isShutdown) return
        worker.execute {
            val failure = connect()
            if (failure == null) onReady() else onError(failure)
        }
    }

    private fun connect(): ErrorType? {
        if (session != null) return null
        val recognizer = RecognitionSession(app)
        val failure = try {
            recognizer.initialize()
        } catch (error: Exception) {
            Log.e("PleosSTT", "전사 엔진 초기화 실패", error)
            ErrorType.ON_DEVICE_ENGINE_ERROR
        }
        if (failure == null) {
            session = recognizer
            Log.i("PleosSTT", "한글 전사 엔진 준비 완료")
        } else {
            closeSession(recognizer)
        }
        return failure
    }

    @Synchronized
    fun enqueue(file: File, startedAtMs: Long, latitude: Double?, longitude: Double?, onSaved: (JSONObject) -> Unit) {
        if (worker.isShutdown) return
        worker.execute {
            try {
                val result = transcribe(file)
                onSaved(save(file, startedAtMs, latitude, longitude, result))
            } catch (error: Exception) {
                Log.e("PleosSTT", "구간 처리 실패: " + file.absolutePath, error)
            }
        }
    }

    private fun transcribe(file: File): Pending {
        val audio = file.readBytes()
        for (attempt in 1..2) {
            val failure = connect()
            val result = if (failure == null) {
                checkNotNull(session).transcribe(audio)
            } else {
                Pending().apply { finish(null, failure.toString()) }
            }
            val error = result.error ?: return result
            Log.w("PleosSTT", "전사 실패: " + file.name + " (시도 $attempt/2, $error)")
            resetSession()
            if (attempt == 2) return result
        }
        error("Unreachable recognition attempt")
    }

    private fun resetSession() {
        val previous = session
        session = null
        if (previous != null) closeSession(previous)
    }

    private fun closeSession(recognizer: RecognitionSession) {
        runCatching { recognizer.close() }
            .onFailure { Log.w("PleosSTT", "전사 엔진 종료 실패", it) }
    }


    @Suppress("DEPRECATION")
    private fun address(latitude: Double, longitude: Double): String? =
        runCatching {
            if (!Geocoder.isPresent()) return null
            Geocoder(app, Locale.KOREA).getFromLocation(latitude, longitude, 1)
                ?.firstOrNull()?.getAddressLine(0)
        }.getOrNull()

    private fun save(
        file: File, startedAtMs: Long, latitude: Double?, longitude: Double?, result: Pending
    ): JSONObject {
        val sessionId = checkNotNull(file.parentFile).name
        val segment = file.nameWithoutExtension.toInt()
        val durationMs = file.length() * 1000L / 32_000L
        val startMs = startedAtMs + (segment - 1) * 10_000L
            val zone = ZoneId.of("Asia/Seoul")
        val location = if (latitude != null && longitude != null) {
            JSONObject().put("latitude", latitude).put("longitude", longitude)
                .put("address", address(latitude, longitude) ?: JSONObject.NULL)
        } else JSONObject.NULL
        val json = JSONObject()
            .put("sessionId", sessionId)
            .put("segment", segment)
            .put("recordedFrom", Instant.ofEpochMilli(startMs).atZone(zone).toOffsetDateTime().toString())
            .put("recordedTo", Instant.ofEpochMilli(startMs + durationMs).atZone(zone).toOffsetDateTime().toString())
            .put("durationMs", durationMs)
            .put("location", location)
            .put("text", result.text.orEmpty())
            .put("status", when {
                result.error != null -> "error"
                result.text.isNullOrBlank() -> "empty"
                else -> "ok"
            })
            .put("error", result.error ?: JSONObject.NULL)

        val dir = File(app.filesDir, "transcripts/$sessionId")
        check(dir.isDirectory || dir.mkdirs())
        val target = File(dir, "${file.nameWithoutExtension}.json")
        val temporary = File(dir, "${target.name}.tmp")
        temporary.writeText(json.toString(2), Charsets.UTF_8)
        check(temporary.renameTo(target)) { "JSON 이름 변경 실패: $target" }
        if (json.getString("status") == "ok" &&
            (app.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        ) {
            json.getString("text").chunked(850).forEachIndexed { index, part ->
                Log.i("PleosTranscript", "$sessionId/$segment part=" + (index + 1) + " $part")
            }
        }
        Log.i("PleosSTT", "구간 JSON 저장 완료: ${target.absolutePath}")
        return json
    }

    @Synchronized
    fun release(onReleased: () -> Unit = {}) {
        if (worker.isShutdown) return
        worker.execute {
            try {
                resetSession()
            } finally {
                onReleased()
            }
        }
        worker.shutdown()
    }
}
