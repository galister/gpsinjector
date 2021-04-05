package li.gravio.common

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import java.lang.Exception


class MockLocationProvider(var providerName: String?,var ctx: Context) {

    init {
        val lm = ctx.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager
        try {
            lm.addTestProvider(
                providerName!!, false, false, false, false, false,
                true, true, 1, 1
            )
            lm.setTestProviderEnabled(providerName!!, true)
        } catch (e: SecurityException) {
            throw SecurityException("Not allowed to perform MOCK_LOCATION")
        }
    }

    fun pushLocation(mockLocation : Location) {
        val lm = ctx.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager
        mockLocation.provider = providerName
        mockLocation.time = System.currentTimeMillis()
        mockLocation.accuracy = 3f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mockLocation.bearingAccuracyDegrees = 0.5f
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mockLocation.verticalAccuracyMeters = 0.5f
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mockLocation.speedAccuracyMetersPerSecond = 0.1f
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mockLocation.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        lm.setTestProviderLocation(providerName!!, mockLocation)
    }

    fun shutdown() {
        val lm = ctx.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager
        try {
            lm.removeTestProvider(providerName!!)
        }
        catch (x: Exception) {}
    }
}