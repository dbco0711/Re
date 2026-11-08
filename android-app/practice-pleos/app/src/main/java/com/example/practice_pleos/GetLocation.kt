package com.example.practice_pleos

import android.content.Context
import android.util.Log

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GetLocation(
    private val context: Context,
    private val onLocation: (LocationData) -> Unit,
    private val onStatus: (String) -> Unit
) {

    companion object {
        private const val TAG = "PLEOS_GPS"
    }

    private var fusedLocation: FusedLocation? = null
    private var listener: FusedLocationListener? = null

    fun startLocationUpdates() {

        if (fusedLocation != null) {
            Log.d(TAG, "GPS가 이미 실행 중입니다.")
            return
        }

        Log.d(TAG, "========== GPS START ==========")

        try {
            val sdk = FusedLocation(context) //FusedLocatino 객체 생성

            if (!sdk.isSupported()) {
                Log.e(TAG, "FusedLocation SDK 미지원")
                onStatus("GPS SDK 미지원")
                return
            }

            Log.d(TAG, "FusedLocation SDK 지원 확인")

            sdk.initialize()
            fusedLocation = sdk

            Log.d(TAG, "FusedLocation 초기화s완료")

            val locationListener = object : FusedLocationListener {

                override fun onChanged(location: LocationData?) {

                    if (location == null) {
                        Log.w(TAG, "GPS 데이터 없음 (null)")
                        onStatus("위치 데이터 없음")
                        return
                    }

                    val latitude = location.latitude
                    val longitude = location.longitude

                    val timestamp = SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss.SSS",
                        Locale.US
                    ).format(Date())

                    Log.d(TAG, "------------------------------")
                    Log.d(TAG, "GPS 수신 시각: $timestamp")
                    Log.d(TAG, "Latitude : $latitude")
                    Log.d(TAG, "Longitude: $longitude")
                    Log.d(TAG, "------------------------------")

                    onLocation(location)
                }

                override fun onError(exception: Exception) {
                    Log.e(
                        TAG,
                        "GPS 수신 오류: ${exception.message}",
                        exception
                    )

                    onStatus(
                        "GPS 오류: ${exception.message}"
                    )
                }
            }

            listener = locationListener

            sdk.registerFusedLocationCallback(locationListener)

            Log.d(TAG, "GPS 위치 업데이트 등록 완료")
            onStatus("GPS 수신 대기 중")

        } catch (e: Exception) {

            Log.e(TAG, "GPS 초기화 오류", e)
            onStatus("초기화 실패: ${e.message}")

            stopLocationUpdates()
        }
    }

    fun stopLocationUpdates() {

        val sdk = fusedLocation ?: return

        try {
            listener?.let {
                sdk.unregisterFusedLocationCallback(it)
            }

            Log.d(TAG, "GPS 콜백 해제 완료") //

        } catch (e: Exception) {
            Log.e(TAG, "GPS 해제 오류", e)
        } finally {

            listener = null
            sdk.release()
            fusedLocation = null

            Log.d(TAG, "========== GPS STOP ==========")
            onStatus("GPS 수신 중지")
        }
    }
}