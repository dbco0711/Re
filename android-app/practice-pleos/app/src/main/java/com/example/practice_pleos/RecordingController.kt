package com.example.practice_pleos

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import java.io.File
import java.util.UUID

class RecordingController(
    private val activity: Activity,
    private val onStatusChanged: (String) -> Unit,
    private val onRecordingChanged: (Boolean) -> Unit,
    private val onReadyChanged: (Boolean) -> Unit
) {

    companion object {
        private const val TAG = "PleosSTT"

        // 사용할 서버 주소로 변경
        private const val SERVER_URL = "http://127.0.0.1:8000"

        const val GPS_PERMISSION_REQUEST = 1001
        const val MIC_PERMISSION_REQUEST = 710
    }

    private data class GpsFix(
        val latitude: Double,
        val longitude: Double
    )

    @Volatile
    private var latestGpsFix: GpsFix? = null

    @Volatile
    private var closing = false

    private var isRecording = false

    private lateinit var pcmRecorder: PcmRecorder

    private val locationManager = GetLocation(
        activity.applicationContext,

        onLocation = { location ->
            latestGpsFix = GpsFix(
                location.latitude,
                location.longitude
            )

            upload?.location(
                location.latitude,
                location.longitude
            )

            activity.runOnUiThread {
                onStatusChanged(
                    "GPS 위치 수신 성공"
                )
            }
        },

        onStatus = { message ->
            activity.runOnUiThread {
                onStatusChanged(message)
            }
        }
    )

    private val talkingAbout = GetTalkingAbout(activity)

    private var upload: ReTripUpload? = null

    private var sttReady = false
    // 컨트롤러 초기화
    fun initialize(serverUrl: String? = null) {
        if (closing) return

        upload = serverUrl?.let { ReTripUpload(it) }

        talkingAbout.initialize(
            onReady = {
                activity.runOnUiThread {
                    if (!closing) {
                        sttReady = true
                        onReadyChanged(true)
                        onStatusChanged("녹음 준비 완료")
                    }
                }
            },
            onError = { error ->
                Log.e(TAG, "SDK 초기화 실패: $error")
                activity.runOnUiThread {
                    if (!closing) {
                        sttReady = false
                        onReadyChanged(false)
                        onStatusChanged("음성 처리 SDK 초기화 실패")
                    }
                }
            }
        )

        startGps()
    }

    // GPS 수신 시작
    private fun startGps() {
        if (
            activity.checkSelfPermission("pleos.car.permission.FUSED_LOCATION")
            == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.startLocationUpdates()
        } else {
            activity.requestPermissions(
                arrayOf("pleos.car.permission.FUSED_LOCATION"),
                GPS_PERMISSION_REQUEST
            )
        }
    }

    // 녹음 버튼 클릭 시 호출
    fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    // 녹음 시작
    private fun startRecording() {
        val initialFix = latestGpsFix

        // GPS가 아직 수신되지 않은 경우
        if (initialFix == null) {
            startGps()

            Toast.makeText(
                activity,
                "GPS 위치 수신 후 다시 녹음 버튼을 눌러 주세요.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        // 마이크 권한 확인
        if (
            activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            activity.requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                MIC_PERMISSION_REQUEST
            )

            return
        }

        // 첫 GPS 위치가 서버에 저장되었는지 확인
        val tripUpload = upload

        if (tripUpload != null && !tripUpload.hasSavedLocation) {
            Toast.makeText(
                activity,
                "서버에 첫 GPS가 저장된 후 다시 눌러 주세요.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val sessionId = UUID.randomUUID().toString()
        val sessionDir = File(
            activity.filesDir,
            "recordings/$sessionId"
        )

        pcmRecorder = PcmRecorder(sessionDir)

        isRecording = true
        onRecordingChanged(true)
        onStatusChanged("녹음 중")

        pcmRecorder.start(

            onSegmentReady = { file, startedAtMs, startedElapsedMs ->

                Log.i(
                    TAG,
                    "구간 저장: ${file.absolutePath} (${file.length()}바이트)"
                )

                val fix = latestGpsFix ?: initialFix

                talkingAbout.enqueue(
                    file,
                    startedAtMs,
                    fix.latitude,
                    fix.longitude
                ) { json ->

                    if (json.optString("status") == "ok") {
                        tripUpload?.memo(
                            "$sessionId-${file.nameWithoutExtension}",
                            startedElapsedMs,
                            (json.getInt("segment") - 1) * 10_000L,
                            json.getLong("durationMs"),
                            json.getString("text")
                        )
                    }
                }
            },

            onFinished = { result ->
                result.onSuccess { folder ->
                    Log.i(TAG, "녹음 종료: ${folder.absolutePath}")
                }.onFailure { error ->
                    Log.e(TAG, "녹음 실패", error)
                }

                activity.runOnUiThread {
                    pcmRecorder.resetAfterFinish()
                    isRecording = false

                    if (closing) {
                        releaseProcessing()
                    } else {
                        onRecordingChanged(false)
                        onReadyChanged(sttReady)
                        onStatusChanged(if (result.isSuccess) "녹음 종료" else "녹음 실패")
                    }
                }
            }
        )
    }

    // 녹음 종료
    private fun stopRecording() {
        if (!isRecording) return

        onStatusChanged("녹음 종료 중")
        pcmRecorder.stop()
    }

    // 권한 요청 결과 전달
    fun onPermissionResult(
        requestCode: Int,
        grantResults: IntArray
    ) {
        val granted =
            grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED

        when (requestCode) {

            GPS_PERMISSION_REQUEST -> {
                if (granted) {
                    locationManager.startLocationUpdates()
                } else {
                    onStatusChanged("GPS 위치 권한이 거부되었습니다.")
                }
            }

            MIC_PERMISSION_REQUEST -> {
                if (granted) {
                    onStatusChanged("마이크 권한 승인 완료. 녹음 버튼을 다시 눌러 주세요.")
                } else {
                    onStatusChanged("마이크 권한이 거부되었습니다.")
                }
            }
        }
    }

    // Activity 종료 시 자원 정리
    fun release() {
        if (closing) return
        closing = true

        locationManager.stopLocationUpdates()

        if (isRecording) {
            pcmRecorder.stop()
            // onFinished에서 마지막 구간을 넘긴 뒤 전사 종료를 요청합니다.
        } else {
            releaseProcessing()
        }
    }

    private fun releaseProcessing() {
        val tripUpload = upload
        talkingAbout.release {
            tripUpload?.close()
        }
    }
}