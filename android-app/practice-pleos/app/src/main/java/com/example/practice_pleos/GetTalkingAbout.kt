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
    private val speechToText = SpeechToText(app, Mode.ON_DEVICE)
    private val worker = Executors.newSingleThreadExecutor()
    private val lock = Any()

    @Volatile private var closed = false
    private var unusable = false
    private var pending: Pending? = null

    private class Pending {
        val done = CountDownLatch(1)
        var text: String? = null
        var error: String? = null
    }

    fun initialize(onReady: () -> Unit, onError: (ErrorType) -> Unit) {
        speechToText.initialize(object : OnInitializeListener {
            override fun onSuccess() {
                synchronized(lock) {
                    if (closed) return
                    speechToText.addListener(listener)
                    speechToText.setLocale(Locale.KOREA)
                }
                onReady()
            }

            override fun onFailure(errorType: ErrorType) = onError(errorType)
        })
    }

    fun enqueue(file: File, startedAtMs: Long, latitude: Double?, longitude: Double?) {
        synchronized(lock) {
            if (closed) return
            worker.execute {
                if (closed || unusable) return@execute
                try {
                    val result = transcribe(file)
                    if (!closed) save(file, startedAtMs, latitude, longitude, result)
                } catch (error: Exception) {
                    Log.e("PleosSTT", "구간 처리 실패: ${file.absolutePath}", error)
                }
            }
        }
    }

    private fun transcribe(file: File): Pending {
        val result = Pending()
        synchronized(lock) { pending = result }
        try {
            speechToText.sendAudio(file.readBytes())
            speechToText.completeAudioSend()
            if (!result.done.await(60, TimeUnit.SECONDS)) {
                unusable = true
                result.error = "callback_timeout"
                Log.e("PleosSTT", "응답 시간 초과. 남은 PCM은 보관됩니다.")
            }
        } catch (error: Exception) {
            unusable = true
            throw error
        } finally {
            synchronized(lock) { pending = null }
        }
        return result
    }

    private val listener = object : ResultListener {
        override fun onUpdated(stt: String, completed: Boolean) {
            if (completed) finish(stt, null)
        }

        override fun onError(errorType: ErrorType) = finish(null, errorType.toString())
        override fun onReady() = Unit
        override fun onStartedRecognition() = Unit
        override fun onEndedRecognition() = Unit
        override fun onUpdatedEpdData(on: Long, off: Long) = Unit
        override fun onUpdatedRms(rms: Double) = Unit
    }

    private fun finish(text: String?, error: String?) {
        synchronized(lock) {
            val result = pending ?: return
            if (result.done.count == 0L) return
            result.text = text
            result.error = error
            result.done.countDown()
        }
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
    ) {
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
        Log.i("PleosSTT", "구간 JSON 저장 완료: ${target.absolutePath}")
    }

    fun release() {
        synchronized(lock) {
            if (closed) return
            closed = true
            finish(null, "closed")
            worker.execute {
                speechToText.removeListener(listener)
                speechToText.release()
            }
            worker.shutdown()
        }
    }
}
