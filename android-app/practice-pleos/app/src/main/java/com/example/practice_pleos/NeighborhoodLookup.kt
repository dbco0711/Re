package com.example.practice_pleos

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.io.Closeable
import java.util.Locale
import java.util.concurrent.Executors

internal class NeighborhoodLookup(context: Context) : Closeable {
    private val geocoder = Geocoder(context.applicationContext, Locale.KOREA)
    private val boundaries by lazy { KoreaNeighborhoods(context.applicationContext.assets) }
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val neighborhood = Regex("""(?<![가-힣0-9])([가-힣][가-힣0-9·-]*(?:동|읍|면|리))(?![가-힣0-9])""")
    private var latestLocation: Location? = null
    private var requestedLocation: Location? = null
    private var lastAttempt = 0L
    private var lastResult: String? = null
    private var pending = false
    private var closed = false

    fun update(latitude: Double, longitude: Double, show: (String?) -> Unit) {
        if (closed) return
        val location = Location("gps").apply {
            this.latitude = latitude
            this.longitude = longitude
        }
        latestLocation = location
        if (pending) return

        val now = SystemClock.elapsedRealtime()
        if (requestedLocation?.distanceTo(location)?.let { it < 50f } == true &&
            (lastResult != null || now - lastAttempt < 15_000)
        ) return

        pending = true
        requestedLocation = location
        lastAttempt = now
        lastResult = null
        show(null)
        worker.execute {
            val result = findNeighborhood(location)
            main.post {
                if (closed) return@post
                pending = false
                val latest = latestLocation ?: return@post
                if (latest.distanceTo(location) >= 50f) {
                    update(latest.latitude, latest.longitude, show)
                } else {
                    lastResult = result
                    show(result)
                }
            }
        }
    }

    fun findNeighborhood(latitude: Double, longitude: Double): String? =
        findNeighborhood(Location("recording").apply {
            this.latitude = latitude
            this.longitude = longitude
        })

    @Suppress("DEPRECATION")
    private fun findNeighborhood(location: Location): String? {
        val resolved = if (Geocoder.isPresent()) {
            runCatching {
                val address = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    ?.firstOrNull()
                sequenceOf(address?.subLocality, address?.locality, address?.getAddressLine(0))
                    .filterNotNull()
                    .firstNotNullOfOrNull { neighborhood.find(it)?.groupValues?.get(1) }
            }.onFailure { Log.w("ReLocation", "Android address lookup failed", it) }.getOrNull()
        } else {
            null
        }
        return resolved ?: runCatching {
            boundaries.find(location.latitude, location.longitude)
        }.onFailure { Log.w("ReLocation", "Offline neighborhood lookup failed", it) }.getOrNull()
    }


    override fun close() {
        closed = true
        worker.shutdownNow()
    }
}
