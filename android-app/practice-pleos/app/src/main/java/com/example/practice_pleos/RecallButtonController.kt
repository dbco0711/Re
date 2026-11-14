package com.example.practice_pleos

import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import java.io.Closeable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class RecallButtonController(
    private val activity: Activity,
    private val button: Button,
    serverUrl: String,
    currentPosition: () -> ReMemoryClient.Position?
) : Closeable {
    private val client = ReMemoryClient(serverUrl)
    private val lookup = NeighborhoodLookup(activity)
    private val content = activity.findViewById<LinearLayout>(R.id.recall_content)
    private val timeFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREA)
        .withZone(ZoneId.of("Asia/Seoul"))
    private val text = activity.findViewById<TextView>(R.id.recall_text)
    private val scroll = activity.findViewById<ScrollView>(R.id.recall_scroll)
    private val previous = activity.findViewById<Button>(R.id.recall_previous)
    private val next = activity.findViewById<Button>(R.id.recall_next)
    private var waitingForPosition = false
    private var closed = false
    private val locationTimeout = Runnable {
        waitingForPosition = false
        showText("현재 GPS 위치를 받지 못했습니다. 위치 수신을 확인한 뒤 회상하기를 다시 눌러 주세요.")
    }

    init {
        button.setOnClickListener {
            val position = currentPosition()
            if (position == null) {
                waitingForPosition = true
                showText("현재 GPS 위치를 확인하는 중입니다…")
                button.removeCallbacks(locationTimeout)
                button.postDelayed(locationTimeout, 15_000L)
            } else {
                loadPage(position)
            }
        }
    }

    fun onPositionAvailable(position: ReMemoryClient.Position) {
        if (!closed && waitingForPosition) loadPage(position)
    }

    private fun loadPage(position: ReMemoryClient.Position, offset: Int = 0) {
        waitingForPosition = false
        button.removeCallbacks(locationTimeout)
        button.isEnabled = false
        showText("현재 위치 주변의 녹음을 불러오는 중입니다…")
        client.listNearbyRecordings(position, offset, lookup) { result ->
            activity.runOnUiThread {
                if (closed || activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                button.isEnabled = true

                val page = result.getOrNull()
                if (page == null) {
                    showText("녹음을 불러오지 못했습니다. 서버 연결을 확인해 주세요.")
                    return@runOnUiThread
                }

                if (page.recordings.isEmpty()) {
                    showText("현재 위치 주변에 저장된 녹음이 없습니다.")
                } else {
                    text.visibility = View.GONE
                    page.recordings.forEach(::addRecording)
                }
                if (offset > 0) {
                    previous.visibility = View.VISIBLE
                    previous.setOnClickListener { loadPage(position, (offset - ReMemoryClient.RECORDINGS_PER_PAGE).coerceAtLeast(0)) }
                }
                page.nextOffset?.let { nextOffset ->
                    next.visibility = View.VISIBLE
                    next.setOnClickListener { loadPage(position, nextOffset) }
                }
            }
        }
    }

    private fun addRecording(recording: ReMemoryClient.Recording) {
        val time = runCatching { timeFormat.format(Instant.parse(recording.recordedAt)) }
            .getOrDefault(recording.recordedAt)
        val heading = "$time  ·  ${recording.neighborhood ?: "동 정보 없음"}"
        val toggle = Button(activity).apply {
            isAllCaps = false
            text = "$heading  ▾"
            ViewCompat.setStateDescription(this, "접힘")
        }
        val detail = TextView(activity).apply {
            textSize = 18f
            text = recording.transcript?.takeIf { it.isNotBlank() } ?: "저장된 대화 내용이 없습니다."
            visibility = View.GONE
        }
        toggle.setOnClickListener {
            val expanded = detail.visibility != View.VISIBLE
            detail.visibility = if (expanded) View.VISIBLE else View.GONE
            toggle.text = if (expanded) "$heading  ▴" else "$heading  ▾"
            ViewCompat.setStateDescription(toggle, if (expanded) "펼쳐짐" else "접힘")
        }
        content.addView(toggle)
        content.addView(detail)
    }

    private fun showText(message: String) {
        if (content.childCount > 1) content.removeViews(1, content.childCount - 1)
        text.visibility = View.VISIBLE
        text.text = message
        scroll.scrollTo(0, 0)
        previous.visibility = View.GONE
        next.visibility = View.GONE
    }

    override fun close() {
        closed = true
        button.removeCallbacks(locationTimeout)
        button.setOnClickListener(null)
        previous.setOnClickListener(null)
        next.setOnClickListener(null)
        client.close()
        lookup.close()
    }
}
