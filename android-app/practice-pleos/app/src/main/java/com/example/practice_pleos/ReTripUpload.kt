package com.example.practice_pleos

import android.os.SystemClock
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.Closeable
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors

class ReTripUpload(private val baseUrl: String) : Closeable {
    private val startedAt = Instant.now().toString()
    private val tripStart = SystemClock.elapsedRealtime()
    private val clientTripId = UUID.randomUUID().toString()
    private val worker = Executors.newSingleThreadExecutor()
    private val points = ArrayDeque<JSONObject>()
    private val memos = ArrayDeque<JSONObject>()
    private var tripId: String? = null
    private var nextSeq = 0
    private var firstSavedMs: Long? = null
    val hasSavedLocation: Boolean get() = lastSavedMs >= 0

    @Volatile
    private var lastSavedMs = -1L
    private var lastQueuedMs = -1L

    fun location(latitude: Double, longitude: Double) {
        val elapsed = SystemClock.elapsedRealtime() - tripStart
        submit {
            if (elapsed > lastQueuedMs) {
                points.addLast(JSONObject().put("seq", nextSeq++).put("elapsed_ms", elapsed)
                    .put("lat", latitude).put("lon", longitude))
                lastQueuedMs = elapsed
            }
        }
    }

    fun memo(id: String, recordingStart: Long, offsetMs: Long, durationMs: Long, text: String) {
        val start = recordingStart - tripStart + offsetMs
        if (start < 0 || durationMs !in 1L..30_000L || text.isBlank()) return
        submit {
            memos.addLast(JSONObject().put("client_memo_id", id).put("consent", true)
                .put("start_elapsed_ms", start).put("end_elapsed_ms", start + durationMs)
                .put("processing_source", "manual").put("transcript", text))
        }
    }

    @Synchronized
    private fun submit(add: () -> Unit) {
        if (worker.isShutdown) return
        worker.execute {
            add()
            try {
                flush()
            } catch (error: Exception) {
                Log.w("ReUpload", "전송 대기: ${error.message}")
            }
        }
    }

    private fun flush() {
        val id = tripId ?: post("/v1/trips", JSONObject()
            .put("client_trip_id", clientTripId).put("source", "real_gps")
            .put("started_at", startedAt)).getString("trip_id").also { tripId = it }
        while (points.isNotEmpty()) {
            val point = points.first()
            post("/v1/trips/$id/points", JSONObject().put("points", JSONArray().put(point)))
            lastSavedMs = point.getLong("elapsed_ms")
            if (firstSavedMs == null) {
                firstSavedMs = lastSavedMs
                Log.i("ReUpload", "첫 GPS 전송 성공: trip_id=$id")
            }
            points.removeFirst()
        }

        val first = firstSavedMs ?: return
        while (memos.isNotEmpty()) {
            val memo = memos.first()
            if (memo.getLong("start_elapsed_ms") < first) {
                Log.w("ReUpload", "구간 시작을 덮는 GPS가 없습니다. 로컬 JSON을 확인하세요.")
                memos.removeFirst()
                continue
            }
            if (memo.getLong("end_elapsed_ms") > lastSavedMs) return
            try {
                post("/v1/trips/$id/memos", memo)
            } catch (error: HttpError) {
                if (error.status != 409 && error.status != 422) throw error
                Log.e("ReUpload", "메모 거절 HTTP ${error.status}: ${memo.getString("client_memo_id")}")
                memos.removeFirst()
                continue
            }
            Log.i("ReUpload", "메모 전송 성공: ${memo.getString("client_memo_id")}")
            memos.removeFirst()
        }
    }

    private class HttpError(val status: Int) : IOException("HTTP $status")

    private fun post(path: String, json: JSONObject): JSONObject {
        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 5000
            connection.readTimeout = 15000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.outputStream.use { it.write(json.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            if (status !in 200..299) throw HttpError(status)
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (body.isBlank()) JSONObject() else JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    @Synchronized
    override fun close() {
        worker.shutdownNow()
    }
}
