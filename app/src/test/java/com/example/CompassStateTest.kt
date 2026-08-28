package com.example

import com.example.model.CompassState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompassStateTest {

    @Test
    fun testCardinalDirections() {
        assertEquals("N", CompassState.headingToCardinal(0f))
        assertEquals("N", CompassState.headingToCardinal(358f))
        assertEquals("N", CompassState.headingToCardinal(5f))
        assertEquals("NE", CompassState.headingToCardinal(45f))
        assertEquals("E", CompassState.headingToCardinal(90f))
        assertEquals("SE", CompassState.headingToCardinal(135f))
        assertEquals("S", CompassState.headingToCardinal(180f))
        assertEquals("SW", CompassState.headingToCardinal(225f))
        assertEquals("W", CompassState.headingToCardinal(270f))
        assertEquals("NW", CompassState.headingToCardinal(315f))
        assertEquals("NNE", CompassState.headingToCardinal(22.5f))
    }

    @Test
    fun testEffectiveHeadingTrueNorth() {
        val magneticState = CompassState(azimuth = 100f, declination = 5f, useTrueNorth = false)
        assertEquals(100f, magneticState.effectiveHeading, 0.01f)

        val trueNorthState = CompassState(azimuth = 100f, declination = 5f, useTrueNorth = true)
        assertEquals(105f, trueNorthState.effectiveHeading, 0.01f)

        val wrappedState = CompassState(azimuth = 358f, declination = 5f, useTrueNorth = true)
        assertEquals(3f, wrappedState.effectiveHeading, 0.01f)
    }

    @Test
    fun testBearingDeviation() {
        val state = CompassState(azimuth = 90f, lockedBearing = 120f)
        // Current 90, Target 120 -> Diff is -30 (need to turn right 30)
        val dev = state.bearingDeviation
        assertEquals(-30f, dev ?: 0f, 0.01f)

        val wrapState = CompassState(azimuth = 10f, lockedBearing = 350f)
        // Current 10, Target 350 -> Diff is +20 (turn left 20)
        assertEquals(20f, wrapState.bearingDeviation ?: 0f, 0.01f)
    }

    @Test
    fun testLevelIndicator() {
        val flatState = CompassState(pitch = 0.5f, roll = -0.8f)
        assertTrue(flatState.isLevel)

        val tiltedState = CompassState(pitch = 5f, roll = 0f)
        assertFalse(tiltedState.isLevel)
    }

    @Test
    fun testDmsFormatting() {
        val dmsLat = CompassState.toDms(37.7749, true)
        assertTrue(dmsLat.contains("37°"))
        assertTrue(dmsLat.contains("N"))

        val dmsLon = CompassState.toDms(-122.4194, false)
        assertTrue(dmsLon.contains("122°"))
        assertTrue(dmsLon.contains("W"))
    }
}
