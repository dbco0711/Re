package com.example.practice_pleos

import android.content.res.AssetManager
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import kotlin.math.roundToInt

internal class KoreaNeighborhoods(assets: AssetManager) {
    private val boundaries = GZIPInputStream(assets.open("korea_neighborhoods.bin.gz")).use { it.readBytes() }

    fun find(latitude: Double, longitude: Double): String? {
        val x = (longitude * 100_000).roundToInt()
        val y = (latitude * 100_000).roundToInt()
        val data = ByteBuffer.wrap(boundaries)

        // Each polygon has a bounding box and a length-prefixed payload.
        while (data.hasRemaining()) {
            val west = data.int
            val south = data.int
            val east = data.int
            val north = data.int
            val size = data.int
            val next = data.position() + size

            if (x in west..east && y in south..north) {
                val name = ByteArray(data.short.toInt() and 0xffff)
                data.get(name)
                val rings = data.short.toInt() and 0xffff
                val insideOuter = containsRing(data, x, y)
                if (insideOuter && (1 until rings).none { containsRing(data, x, y) }) {
                    return name.toString(Charsets.UTF_8)
                }
            }
            data.position(next)
        }
        return null
    }

    private fun containsRing(data: ByteBuffer, x: Int, y: Int): Boolean {
        val count = data.int
        val last = data.position() + (count - 1) * 8
        var previousX = data.getInt(last)
        var previousY = data.getInt(last + 4)
        var inside = false

        repeat(count) {
            val currentX = data.int
            val currentY = data.int
            if ((previousY > y) != (currentY > y)) {
                val intersection = previousX +
                    (currentX - previousX).toDouble() * (y - previousY) / (currentY - previousY)
                if (x < intersection) inside = !inside
            }
            previousX = currentX
            previousY = currentY
        }
        return inside
    }
}
