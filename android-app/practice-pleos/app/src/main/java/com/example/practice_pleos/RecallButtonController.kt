package com.example.practice_pleos

import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import java.io.Closeable

class RecallButtonController(
    private val activity: Activity,
    private val button: Button,
    serverUrl: String,
    currentPosition: () -> ReMemoryClient.Position?
) : Closeable {
    private val client = ReMemoryClient(serverUrl)
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
        showText("현재 위치 주변의 메모를 불러오는 중입니다…")
        client.listNearbyMemos(position, offset) { result ->
            activity.runOnUiThread {
                if (closed || activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                button.isEnabled = true

                val page = result.getOrNull()
                if (page == null) {
                    showText("메모를 불러오지 못했습니다. 서버 연결을 확인해 주세요.")
                    return@runOnUiThread
                }

                val content = page.memos.joinToString("\n\n") { memo ->
                    val body = memo.transcript?.takeIf { it.isNotBlank() }
                        ?: memo.summary?.takeIf { it.isNotBlank() }
                        ?: "저장된 텍스트가 없습니다."
                    "${memo.recordedAt}\n$body"
                }
                showText(content.ifEmpty { "현재 위치 주변에 저장된 메모가 없습니다." })
                if (offset > 0) {
                    previous.visibility = View.VISIBLE
                    previous.setOnClickListener { loadPage(position, (offset - 20).coerceAtLeast(0)) }
                }
                page.nextOffset?.let { nextOffset ->
                    next.visibility = View.VISIBLE
                    next.setOnClickListener { loadPage(position, nextOffset) }
                }
            }
        }
    }

    private fun showText(message: String) {
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
    }
}
