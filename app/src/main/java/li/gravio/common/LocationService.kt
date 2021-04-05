package li.gravio.common

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.os.*
import androidx.core.app.NotificationCompat
import com.github.petr_s.nmea.GpsSatellite
import com.github.petr_s.nmea.NMEAAdapter
import com.github.petr_s.nmea.NMEAHandler
import com.github.petr_s.nmea.NMEAParser
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import li.gravio.gpsinjector.R
import java.util.*
import java.util.logging.Level
import java.util.logging.LogRecord
import kotlin.collections.ArrayList

abstract class LocationService : Service(), NMEAHandler, ConnectionCallbacks, OnConnectionFailedListener, ResultCallback<Status> {

    companion object {
        var instance: LocationService? = null
        var powerSaving = false
    }

    protected val CHANNEL_ID = "location_service"
    protected val mBinder: ServiceBinder = ServiceBinder(this)

    protected var mGoogleApiClient: GoogleApiClient? = null
    protected var mLocationRequest: LocationRequest? = null

    protected var mLocationManager: LocationManager? = null
    protected val mStatus: LocationServiceStatus = LocationServiceStatus(getProviderName())
    protected val mNmeaParser: NMEAParser = NMEAParser(this)

    protected var mEnableMock: Boolean = false
    protected var mMockProviders: ArrayList<MockLocationProvider> = ArrayList(2)

    abstract fun getProviderName(): String

    fun getStatus(): LocationServiceStatus{
        return mStatus
    }

    open fun end() {
        instance = null
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID,
                    "Location Service",
                    NotificationManager.IMPORTANCE_HIGH)
            channel.setSound(null, null)

            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Service Running")
                    .setSmallIcon(R.drawable.ic_stat_vortac)
                    .setContentText("Location Source: " + getProviderName()).build()
            startForeground(1, notification)
        }

        mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }


    @Synchronized
    protected open fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API).build()
        createLocationRequest()
    }

    protected open fun createLocationRequest() {
        mLocationRequest = LocationRequest()
        mLocationRequest!!.setInterval(1000)
        mLocationRequest!!.setFastestInterval(100)
        mLocationRequest!!.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
    }

    override fun onDestroy() {
        end()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    @SuppressLint("MissingPermission")
    override fun onLocation(loc: Location?) {
        if (loc != null) {
            mStatus.location = loc

            if (mEnableMock){
                for (mP in mMockProviders)
                    mP.pushLocation(loc)

                LocationServices.FusedLocationApi.setMockLocation(mGoogleApiClient, loc);
            }
        }
    }

    override fun onSatellites(sats: List<GpsSatellite?>?) {
        if (sats != null) {
            val numSats = sats.count()
            mStatus.satellites = numSats
        }
    }

    override fun onUnrecognized(sentence: String?) {
        mStatus.log(Level.WARNING, "Unrecognized: $sentence")
    }

    override fun onBadChecksum(expected: Int, actual: Int) {
        mStatus.log(Level.WARNING, "Bad chksum; exp $expected, got $actual")
    }

    override fun onException(e: Exception?) {
        mStatus.log(Level.WARNING, e.toString())
    }

    override fun onStart() { }
    override fun onFinish() { }

    @SuppressLint("MissingPermission")
    override fun onConnected(connectionHint: Bundle?) {
        try {
        LocationServices.FusedLocationApi.setMockMode(mGoogleApiClient, true)
        }
        catch (x: SecurityException) {
            mStatus.log(Level.SEVERE,"Select GPS Injector as Mock Location App in Developer Settings, then restart the app!")
        }
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
    }

    override fun onConnectionSuspended(cause: Int) {
        mGoogleApiClient!!.connect()
    }

    override fun onResult(result: Status) {}

    class LocationServiceStatus(providerName: String) : NMEAAdapter(), ILogger {
        var provider: String = "Undefined"
        var location: Location? = null
        var satellites: Int = 0
        var messages: Int = 0
        val logBuffer: ArrayDeque<LogRecord> = ArrayDeque(100)

        init {
            provider = providerName
        }

        @Synchronized
        override fun log(level: Level, msg: String) {
            if (logBuffer.count() >= 50)
                logBuffer.poll()
            logBuffer.add(LogRecord(level, msg))
        }

        override fun log(msg: String) {
            log(Level.INFO, msg)
        }
    }

    class ServiceBinder(locationService: LocationService) : Binder() {
        private var service: LocationService? = null

        init {
            service = locationService
        }

        fun getService(): LocationService? {
            return service
        }

        fun getStatus(): LocationServiceStatus {
            return service!!.mStatus
        }

        fun shutdown() {
            service!!.end()
        }
    }
}

interface ILogger {
    fun log(level: Level, msg: String)
    fun log(msg: String)
}