package com.example.practice_pleos

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
import android.Manifest
import android.util.Log
import java.io.File
class MainActivity : Activity() {

    // Pleos GPS 위치 접근 권한
    private val locationPermission =
        "pleos.car.permission.FUSED_LOCATION"

    private val permissionRequestCode = 1001

    // GPS 데이터 관리 클래스
    private lateinit var locationManager: GetLocation

    // 화면에 표시할 TextView
    private lateinit var latitudeText: TextView
    private lateinit var longitudeText: TextView
    private lateinit var statusText: TextView

    private lateinit var talkingAbout: GetTalkingAbout


    private val speechMicPermissionRequest = 710
    private lateinit var pcmRecorder: PcmRecorder

    @Volatile
    private var closing = false

    private data class GpsFix(val latitude: Double, val longitude: Double)

    @Volatile
    private var latestGpsFix: GpsFix? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 전체 화면 레이아웃
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.rgb(18, 24, 36))
        }

        // 2. 앱 제목
        val titleText = TextView(this).apply {
            text = "Re - 차량 GPS 위치"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        // 3. 위도 표시
        latitudeText = TextView(this).apply {
            text = "위도: 수신 대기"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 10)
        }

        // 4. 경도 표시
        longitudeText = TextView(this).apply {
            text = "경도: 수신 대기"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        // 5. GPS 상태 표시
        statusText = TextView(this).apply {
            text = "GPS 수신을 시작해 주세요."
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 30)
        }

        // 6. GPS 관리 클래스 연결
        locationManager = GetLocation(
            applicationContext,

            onLocation = { location ->
                latestGpsFix = GpsFix(location.latitude, location.longitude)
                runOnUiThread {

                    latitudeText.text = String.format(
                        Locale.US,
                        "위도: %.6f",
                        location.latitude
                    )

                    longitudeText.text = String.format(
                        Locale.US,
                        "경도: %.6f",
                        location.longitude
                    )

                    statusText.text = "GPS 위치 수신 성공"
                }
            },
            onStatus = { message ->
                runOnUiThread {
                    statusText.text = message
                }
            }

        )

        // 7. GPS 수신 시작 버튼
        val startButton = Button(this).apply {
            text = "GPS 수신 시작"

            setOnClickListener {

                if (
                    checkSelfPermission(locationPermission) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    locationManager.startLocationUpdates()
                } else {
                    requestPermissions(
                        arrayOf(locationPermission),
                        permissionRequestCode
                    )
                }
            }
        }

        // 8. GPS 수신 중지 버튼
        val stopButton = Button(this).apply {
            text = "GPS 수신 중지"

            setOnClickListener {
                locationManager.stopLocationUpdates()
            }
        }

        // 9. 화면 구성 요소 추가
        layout.addView(titleText)
        layout.addView(latitudeText)
        layout.addView(longitudeText)
        layout.addView(statusText)
        layout.addView(startButton)
        layout.addView(stopButton)

        val startSpeechButton = Button(this).apply {
            text = "음성 녹음 시작"
            isEnabled = false
        }
        val stopSpeechButton = Button(this).apply {
            text = "음성 녹음 종료"
            isEnabled = false
        }

        startSpeechButton.setOnClickListener {
            val initialFix = latestGpsFix
            if (initialFix == null) {
                if (checkSelfPermission(locationPermission) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.startLocationUpdates()
                } else {
                    requestPermissions(arrayOf(locationPermission), permissionRequestCode)
                }
                android.widget.Toast.makeText(
                    this,
                    "GPS 위치 수신 성공 후 녹음 시작을 다시 눌러 주세요.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), speechMicPermissionRequest)
                return@setOnClickListener
            }

            startSpeechButton.isEnabled = false
            stopSpeechButton.isEnabled = true
            val sessionId = java.util.UUID.randomUUID().toString()
            val sessionDir = File(filesDir, "recordings/$sessionId")
            pcmRecorder = PcmRecorder(sessionDir)

            startSpeechButton.isEnabled = false
            stopSpeechButton.isEnabled = true

            pcmRecorder.start(
                onSegmentReady = { file, startedAtMs ->
                    Log.i("PleosSTT", "구간 저장: ${file.absolutePath} (${file.length()}바이트)")
                    val fix = latestGpsFix ?: initialFix
                    talkingAbout.enqueue(file, startedAtMs, fix.latitude, fix.longitude)
                },
                onFinished = { result ->
                    runOnUiThread {
                        pcmRecorder.resetAfterFinish()
                        stopSpeechButton.isEnabled = false
                        startSpeechButton.isEnabled = true
                    }

                    result.onSuccess { folder ->
                        Log.i("PleosSTT", "녹음 종료: ${folder.absolutePath}")
                    }.onFailure { error ->
                        Log.e("PleosSTT", "녹음 실패", error)
                    }
                }
            )
        }

        stopSpeechButton.setOnClickListener {
            stopSpeechButton.isEnabled = false
            pcmRecorder.stop()
        }

        layout.addView(startSpeechButton)
        layout.addView(stopSpeechButton)
        setContentView(layout)

        talkingAbout = GetTalkingAbout(this)
        talkingAbout.initialize(
            onReady = {
                Log.i("PleosSTT", "SDK 초기화 성공")
                runOnUiThread {
                    if (!closing) startSpeechButton.isEnabled = true
                }
            },
            onError = { error ->
                Log.e("PleosSTT", "SDK 초기화 실패: $error")
            }
        )

    }

    // 11. GPS 접근 권한 요청 결과 처리
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

        if (requestCode == permissionRequestCode) {

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                locationManager.startLocationUpdates()
            } else {
                statusText.text =
                    "GPS 위치 권한이 거부되었습니다."
            }
        }
    }

    // 12. 앱 종료 시 GPS 리소스 해제
    override fun onDestroy() {
        locationManager.stopLocationUpdates()
        closing = true
        if (::pcmRecorder.isInitialized) pcmRecorder.stop()
        talkingAbout.release()
        super.onDestroy()
    }
}
