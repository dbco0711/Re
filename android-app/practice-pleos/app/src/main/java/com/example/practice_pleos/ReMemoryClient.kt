package com.example.practice_pleos

import android.os.SystemClock
import android.util.Log
import org.json.JSONObject
import java.io.Closeable
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class ReMemoryClient(private val baseUrl: String) : Closeable {
    data class Memo(val id: String, val recordedAt: String, val summary: String?, val transcript: String?)
    data class Page(val memos: List<Memo>, val nextOffset: Int?)
    data class Position(val latitude: Double, val longitude: Double)

    private val worker = Executors.newSingleThreadExecutor()
    private var searching = false
    private var nextSearchAtMs = 0L

    @Synchronized
    fun searchRoute(tripId: String, onResult: (Memo?) -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (worker.isShutdown || searching || now < nextSearchAtMs) return
        searching = true
        nextSearchAtMs = now + 5_000L
        worker.execute {
            try {
                val cards = request(
                    "/v1/memories/search",
                    JSONObject().put("current_trip_id", tripId).put("limit", 1)
                ).getJSONArray("cards")
                val id = cards.optJSONObject(0)?.getString("memo_id")
                val memo = id?.let { parseMemo(request("/v1/memos/$it")) }
                onResult(memo)
            } catch (error: Exception) {
                Log.w("ReMemoryClient", "경로 메모 조회 실패 (${error.javaClass.simpleName})")
                onResult(null)
            } finally {
                synchronized(this) { searching = false }
            }
        }
    }

    @Synchronized
    fun listNearbyMemos(position: Position, offset: Int, onResult: (Result<Page>) -> Unit) {
        if (worker.isShutdown) return
        worker.execute {
            val result = runCatching {
                val response = request(
                    "/v1/memos/nearby?lat=${position.latitude}&lon=${position.longitude}&limit=20&offset=$offset"
                )
                val items = response.getJSONArray("items")
                val nextOffset = if (response.isNull("next_offset")) null
                    else response.getInt("next_offset")
                Page(List(items.length()) { parseMemo(items.getJSONObject(it)) }, nextOffset)
            }
            onResult(result)
        }
    }

    private fun parseMemo(json: JSONObject): Memo {
        val summary = if (json.isNull("summary")) null else json.getString("summary")
        val transcript = if (json.isNull("transcript")) null else json.getString("transcript")
        return Memo(json.getString("memo_id"), json.getString("recorded_at"), summary, transcript)
    }

    private fun request(path: String, body: JSONObject? = null): JSONObject {
        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = if (body == null) "GET" else "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            if (status !in 200..299) throw IOException("HTTP $status")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
        } finally {
            connection.disconnect()
        }
    }

    @Synchronized
    override fun close() {
        worker.shutdownNow()
    }
}
