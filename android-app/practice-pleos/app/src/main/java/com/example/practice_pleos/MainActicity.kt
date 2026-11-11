package com.example.practice_pleos

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var recordingController: RecordingController

    private lateinit var recordButton: Button
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // XML 화면 연결
        setContentView(R.layout.activity_re_road)

        // XML에 만든 버튼과 텍스트 연결
        recordButton = findViewById(R.id.record_button)
        statusText = findViewById(R.id.status_text)

        // 녹음 기능 담당 클래스 생성
        recordingController = RecordingController(
            activity = this,
            onStatusChanged = { message ->
                statusText.text = message
            },
            onRecordingChanged = { recording ->
                recordButton.text = if (recording) "녹음 종료" else "녹음 시작"
            },
            onReadyChanged = { ready ->
                recordButton.isEnabled = ready
            }
        )

        // 버튼 클릭 동작
        recordButton.isEnabled = false

        recordButton.setOnClickListener {
            recordingController.toggleRecording()
        }

        // GPS 및 음성 SDK 초기화
        val serverUrl = "http://127.0.0.1:8000"

        AlertDialog.Builder(this)
            .setTitle("주행 위치와 음성 전사 전송")
            .setMessage("동의하면 주행 위치와 인식한 문장을 $serverUrl 서버에 전송합니다.")
            .setNegativeButton("로컬 녹음만") { _, _ ->
                recordingController.initialize()
            }
            .setPositiveButton("동의하고 전송") { _, _ ->
                recordingController.initialize(serverUrl)
            }
            .setOnCancelListener {
                recordingController.initialize()
            }
            .show()
    }

    // 권한 요청 결과를 녹음 담당 클래스에 전달
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        recordingController.onPermissionResult(
            requestCode,
            grantResults
        )
    }

    // 앱 종료 시 자원 정리
    override fun onDestroy() {
        recordingController.release()
        super.onDestroy()
    }
}