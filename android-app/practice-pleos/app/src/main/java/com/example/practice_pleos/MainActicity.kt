package com.example.practice_pleos

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.util.Log
import ai.pleos.playground.navi.helper.NaviHelper

class MainActivity : Activity() {

    private lateinit var recordingController: RecordingController
    private lateinit var neighborhoodLookup: NeighborhoodLookup

    private lateinit var recordButton: Button
    private lateinit var statusText: TextView

    private var navigationConnection: PleosNavigationConnection? = null

    private var recallButtonController: RecallButtonController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // XML 화면 연결
        setContentView(R.layout.activity_re_road)
        val locationText = findViewById<TextView>(R.id.pleos_location_text)
        neighborhoodLookup = NeighborhoodLookup(this)

        navigationConnection = PleosNavigationConnection(
            context = this,
            readRoadName = { null }, // ② 실제 SDK 응답에서 도로명 필드 확인 후 연결
            onRoadChanged = { roadName ->
                android.util.Log.d("ReNavi", "현재 도로명: ${roadName ?: "미확인"}")
            }
        ).also { it.connect() }

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
            },
            onGpsLocationChanged = { latitude, longitude ->
                recallButtonController?.onPositionAvailable(ReMemoryClient.Position(latitude, longitude))
                neighborhoodLookup.update(latitude, longitude) { name ->
                    locationText.text = if (name == null) {
                        getString(R.string.current_neighborhood_unavailable)
                    } else {
                        getString(R.string.current_neighborhood, name)
                    }
                }
            }
        )

        val serverUrl = "http://127.0.0.1:8000"
        recallButtonController = RecallButtonController(
            this, findViewById(R.id.recall_button), serverUrl
        ) { recordingController.currentPosition() }

        findViewById<Button>(R.id.nav_home).setOnClickListener {
            startActivity(android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_HOME)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            finish()
        }

        recordButton.isEnabled = false
        var recordingConsentChosen = false
        recordButton.setOnClickListener {
            if (recordingConsentChosen) {
                recordingController.toggleRecording()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("주행 위치와 음성 전사 전송")
                    .setMessage("동의하면 주행 위치와 인식한 문장을 서버에 전송합니다.")
                    .setNegativeButton("로컬 녹음만") { _, _ ->
                        recordingConsentChosen = true
                        recordingController.toggleRecording()
                    }
                    .setPositiveButton("동의하고 전송") { _, _ ->
                        recordingController.enableUpload(serverUrl)
                        recordingConsentChosen = true
                        recordingController.toggleRecording()
                    }
                    .show()
            }
        }
        recordingController.initialize()
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
        navigationConnection?.close()
        navigationConnection = null

        neighborhoodLookup.close()
        recordingController.release()

        recallButtonController?.close()
        super.onDestroy()

    }
}