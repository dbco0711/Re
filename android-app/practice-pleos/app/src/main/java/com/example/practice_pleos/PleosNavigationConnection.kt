package com.example.practice_pleos

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.helper.NaviHelper

class PleosNavigationConnection(
    context: Context,
    private val readRoadName: (CurrentLocationInfo) -> String?,
    private val onRoadChanged: (String?) -> Unit,
    private val onLocationChanged: (CurrentLocationInfo) -> Unit = {}
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var naviHelper: NaviHelper? = null
    private var navigation: ReNavigation? = null

    // Activity의 onCreate()에서 호출합니다.
    fun connect() {
        if (naviHelper != null) return

        val helper = NaviHelper(appContext)
        naviHelper = helper

        helper.initialize(
            onConnected = {
                mainHandler.post {
                    // ① 홈이 이미 종료됐다면 늦게 도착한 콜백을 건너뜁니다.
                    if (naviHelper !== helper || navigation != null) return@post

                    val connection = ReNavigation(
                        onLocationChanged = { info ->
                            mainHandler.post {
                                if (naviHelper === helper) {
                                    onLocationChanged(info)
                                }
                            }
                        },
                        naviHelper = helper,
                        readRoadName = readRoadName,
                        onRoadChanged = { roadName ->
                            mainHandler.post {
                                if (naviHelper === helper) {
                                    onRoadChanged(roadName)
                                }
                            }
                        }
                    )

                    connection.attach()
                    navigation = connection

                    Log.i("ReNavi", "Pleos 내비 서비스 연결 완료")
                    connection.requestCurrentRoad()
                    connection.requestDestination()
                }
            },
            onFailed = { error ->
                mainHandler.post {
                    if (naviHelper !== helper) return@post

                    Log.e("ReNavi", "Pleos 내비 서비스 연결 실패", error)
                    close()
                }
            }
        )
    }

    // Activity의 onDestroy()에서 호출합니다.
    fun close() {
        val helper = naviHelper
        naviHelper = null

        val connection = navigation
        navigation = null

        try {
            connection?.detach()
        } finally {
            helper?.release()
        }
    }
}