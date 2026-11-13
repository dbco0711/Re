package com.example.practice_pleos

import android.os.SystemClock
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.util.Log
import ai.pleos.playground.navi.data.DestinationInfo

class ReNavigation(
    private val naviHelper: NaviHelper,
    private val readRoadName: (CurrentLocationInfo) -> String?,
    private val onRoadChanged: (String?) -> Unit,
    private val onLocationChanged: (CurrentLocationInfo) -> Unit = {}
) {
    private data class RoadReading(
        val name: String,
        val receivedAtMs: Long
    )

    @Volatile
    private var latestRoad: RoadReading? = null

    private val listener = object : NaviHelperEventListener {
        override fun onCurrentLocationInfo(info: CurrentLocationInfo) {
            // ① SDK에서 확인한 현재 도로명만 사용합니다.
            onLocationChanged(info)
            val name = readRoadName(info)?.trim()?.takeIf { it.isNotEmpty() }
            latestRoad = name?.let { RoadReading(it, SystemClock.elapsedRealtime()) }
            onRoadChanged(name)
        }

        override fun onDestinationInfo(destinationInfo: DestinationInfo) {
            Log.d("ReNavi", "목적지 응답: $destinationInfo")
        }
    }

    fun requestDestination() {
        naviHelper.getDestinationInfo()
    }
    // ② NaviHelper 초기화 성공 후 호출합니다.
    fun attach() {
        naviHelper.addListener(listener)
    }

    fun requestGuidance(destination: RouteInfo) {
        naviHelper.requestRoute(destination)
    }

    fun requestCurrentRoad() {
        naviHelper.getCurrentLocationInfo()
    }

    fun currentRoadName(): String? {
        val reading = latestRoad ?: return null

        // ③ 오래된 도로명이 이후 GPS 기록에 계속 붙지 않도록 합니다.
        val ageMs = SystemClock.elapsedRealtime() - reading.receivedAtMs
        return reading.name.takeIf { ageMs <= 5_000L }
    }

    fun detach() {
        naviHelper.removeListener(listener)
        latestRoad = null
    }
}