package li.gravio.common

import android.content.Intent
import android.location.LocationManager
import androidx.preference.PreferenceManager
import java.util.logging.Level


class RemoteLocationService : LocationService(), TcpClient.IMessageReceiver {
    private var mTcpClient : TcpClient? = null

    override fun getProviderName(): String {
        return "Remote"
    }

    override fun onStart(intent: Intent?, startId: Int) {

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val addressPort = prefs.getString("serverAddress" + intent?.getStringExtra("Target"), "192.168.43.1:3939")!!.split(':')

        val address = addressPort[0]
        val port = if (addressPort.count() > 1) addressPort[1] else "3939"

        mEnableMock = prefs.getBoolean("enableMock", false)

        if (mEnableMock){
            try {
                buildGoogleApiClient();
                mGoogleApiClient!!.connect();

                mMockProviders.add(
                    MockLocationProvider( LocationManager.NETWORK_PROVIDER, applicationContext))
                mMockProviders.add(
                    MockLocationProvider(LocationManager.GPS_PROVIDER, applicationContext))
            }
            catch (x: SecurityException){
                mEnableMock=false
                mStatus.log(Level.SEVERE, "Check mock application setting!")
                return
            }
        }

        mTcpClient = TcpClient(this, mStatus)
        mTcpClient!!.begin(address, port.toInt())
    }

    override fun end() {
        mEnableMock = false
        for (mP in mMockProviders)
            mP.shutdown()

        mTcpClient?.end()
        mTcpClient = null;
        mStatus.log("Shutdown complete.")
        super.end()
    }

    override fun messageReceived(message: String?) {
        if (message != null) {
            if(mNmeaParser.parse(message.trimEnd()))
                mStatus.messages++
        }
    }
}