package li.gravio.common

import android.annotation.SuppressLint
import android.content.Intent
import android.location.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.preference.PreferenceManager
import java.lang.reflect.InvocationTargetException
import java.util.logging.Level

class LocalLocationService : LocationService(), LocationListener {

    private var mNmeaListener: GpsStatus.NmeaListener? = null
    private var onNmeaMessageListener: OnNmeaMessageListener? = null

    private var mTcpServer : TcpServer? = null

    override fun onLocationChanged(location: Location) {} // Ignored
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {} // Ignored

    override fun onProviderEnabled(provider: String) {
        mStatus.log("Location provider enabled: $provider")
    }

    override fun onProviderDisabled(provider: String) {
        mStatus.log("Location provider disabled: $provider")
    }

    override fun getProviderName(): String {
        return "GPS"
    }

    private fun onNmeaMessage(msg : String?) {
        if (msg != null) {
            val msg2 = msg.trimEnd()
            mStatus.messages++
            if (!powerSaving)
                mNmeaParser.parse(msg2)
            mTcpServer!!.broadcast(msg2)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStart(intent: Intent?, startId: Int) {
        try {
            try {
                mLocationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
            } catch (e: IllegalArgumentException) {
                mStatus.log(Level.SEVERE, "No GPS available")
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                onNmeaMessageListener = OnNmeaMessageListener { message: String?, timestamp: Long -> onNmeaMessage(message) }
                mLocationManager!!.addNmeaListener(onNmeaMessageListener!!)
            } else {
                mNmeaListener = GpsStatus.NmeaListener { timestamp: Long, message: String? -> onNmeaMessage(message) }
                // Workaround SDK 29 bug: https://issuetracker.google.com/issues/141019880
                try {
                    val addNmeaListener = LocationManager::class.java.getMethod("addNmeaListener", GpsStatus.NmeaListener::class.java)
                    addNmeaListener.invoke(mLocationManager, mNmeaListener)
                } catch (e: NoSuchMethodException) {
                    throw RuntimeException("Failed to call addNmeaListener through reflection: $e")
                } catch (e: IllegalAccessException) {
                    throw RuntimeException("Failed to call addNmeaListener through reflection: $e")
                } catch (e: InvocationTargetException) {
                    throw RuntimeException("Failed to call addNmeaListener through reflection: $e")
                }
            }
        } catch (e: SecurityException) {
            mStatus.log(Level.SEVERE, "No permission to access GPS")
            return
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val port = prefs.getString("serverPort", "3939")!!.toInt()

        mTcpServer = TcpServer(port, mStatus)
        mTcpServer!!.begin()
    }

    @SuppressLint("MissingPermission")
    override fun end() {
        mTcpServer?.end()
        mTcpServer = null
        mLocationManager?.removeUpdates(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (onNmeaMessageListener != null)
                mLocationManager?.removeNmeaListener(onNmeaMessageListener!!)
        } else {
            // Workaround SDK 29 bug: https://issuetracker.google.com/issues/141019880
            try {
                val removeNmeaListener = LocationManager::class.java.getMethod("removeNmeaListener", GpsStatus.NmeaListener::class.java)
                removeNmeaListener.invoke(mLocationManager, mNmeaListener)
            } catch (e: NoSuchMethodException) {
                mStatus.log("Failed to call removeNmeaListener through reflection: $e")
            } catch (e: IllegalAccessException) {
                mStatus.log("Failed to call removeNmeaListener through reflection: $e")
            } catch (e: InvocationTargetException) {
                mStatus.log("Failed to call removeNmeaListener through reflection: $e")
            }
        }

        mStatus.log("Shutdown complete.")
        super.end()
    }

}