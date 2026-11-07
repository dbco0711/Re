package re.starter

import java.net.HttpURLConnection
import java.net.URL

// C 담당: UI 스레드 밖에서 호출. baseUrl은 실제 Emulator 환경에 맞게 지정.
class ReApiClient(private val baseUrl: String) {
    fun health(): String = get("/health")
    fun demoCard(): String = get("/v1/demo-card")

    private fun get(path: String): String {
        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val status = connection.responseCode
            check(status in 200..299) { "HTTP $status" }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
