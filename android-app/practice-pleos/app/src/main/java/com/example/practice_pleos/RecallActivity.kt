package com.example.practice_pleos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock

class RecallActivity : Activity() {
    private lateinit var recall: RecallButtonController
    private lateinit var location: GetLocation
    private var active = false
    private var fix: GpsFix? = null

    private data class GpsFix(val position: ReMemoryClient.Position, val receivedAt: Long)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_re_road)
        location = GetLocation(this, onLocation = { data ->
            val receivedAt = SystemClock.elapsedRealtime()
            val position = ReMemoryClient.Position(data.latitude, data.longitude)
            if (position.latitude in -90.0..90.0 && position.longitude in -180.0..180.0) {
                runOnUiThread {
                    if (active) {
                        fix = GpsFix(position, receivedAt)
                        recall.onPositionAvailable(position)
                    }
                }
            }
        }, onStatus = {})
        recall = RecallButtonController(
            this,
            findViewById(R.id.recall_button),
            requireNotNull(intent.getStringExtra(EXTRA_SERVER_URL))
        ) { currentPosition() }
    }

    override fun onResume() {
        super.onResume()
        active = true
        if (checkSelfPermission(LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            location.startLocationUpdates()
        }
    }

    override fun onPause() {
        active = false
        fix = null
        location.stopLocationUpdates()
        super.onPause()
    }

    private fun currentPosition(): ReMemoryClient.Position? {
        if (checkSelfPermission(LOCATION_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(LOCATION_PERMISSION), LOCATION_REQUEST)
            return null
        }
        return fix?.takeIf { SystemClock.elapsedRealtime() - it.receivedAt <= 30_000L }?.position
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (active && requestCode == LOCATION_REQUEST &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            location.startLocationUpdates()
        }
    }

    override fun onDestroy() {
        recall.close()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_SERVER_URL = "server_url"
        private const val LOCATION_PERMISSION = "pleos.car.permission.FUSED_LOCATION"
        private const val LOCATION_REQUEST = 1

        fun createIntent(context: Context, serverUrl: String): Intent =
            Intent(context, RecallActivity::class.java).putExtra(EXTRA_SERVER_URL, serverUrl)
    }
}
