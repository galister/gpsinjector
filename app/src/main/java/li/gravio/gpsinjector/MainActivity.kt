package li.gravio.gpsinjector

import android.Manifest
import android.annotation.TargetApi
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import li.gravio.common.LocalLocationService
import li.gravio.common.LocationService
import li.gravio.common.RemoteLocationService


class MainActivity : AppCompatActivity() {

    private val SDK_PERMISSION_REQUEST = 127
    private var permissions: ArrayList<String>? = null

    private var mMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        permissions = getPerms()

        LocationService.powerSaving = false
        if(LocationService.instance != null) {
            reconfigureUI(true)
        }
    }

    override fun onResume() {
        super.onResume()
        LocationService.powerSaving = false

        if (LocationService.instance != null)
            reconfigureUI(true)
    }

    override fun onPause() {
        LocationService.powerSaving = true
        super.onPause()
    }

    override fun onDestroy() {
        LocationService.powerSaving = true
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        mMenu = menu
        return true
    }

    private fun ensureStopped() {
        if (LocationService.instance != null)
        {
            LocationService.instance!!.end()
        }
    }

    fun startServer() {
        ensureStopped()

        val intent = Intent(this, LocalLocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        }
        else {
            startService(Intent(this, LocalLocationService::class.java))
        }
        reconfigureUI(true)
    }

    fun startClient(postfix: String) {
        ensureStopped()

        val intent = Intent(this, RemoteLocationService::class.java)

        intent.putExtra("Target", postfix)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        }
        else {
            startService(intent)
        }
        reconfigureUI(true)
    }

    fun reconfigureUI(running: Boolean){
        val view = supportFragmentManager.primaryNavigationFragment?.view

        view?.findViewById<TextView>(R.id.textView)?.visibility = if (running) View.VISIBLE else View.GONE

        view?.findViewById<Button>(R.id.button_server)?.visibility = if (running) View.GONE else View.VISIBLE

        view?.findViewById<Button>(R.id.button_clienta)?.visibility = if (running) View.GONE else View.VISIBLE

        view?.findViewById<Button>(R.id.button_clientb)?.visibility = if (running) View.GONE else View.VISIBLE

        mMenu?.findItem(R.id.action_stop)?.setVisible(running)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        if (item.itemId == R.id.action_exit) {
            ensureStopped()

            if (Build.VERSION.SDK_INT >= 21)
                finishAndRemoveTask()
            else
                finishActivity(0)
            return true
        }

        if (item.itemId == R.id.action_settings) {

            val intent = Intent(this, SettingsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY;
            this@MainActivity.startActivity(intent)
            return true
        }

        if (item.itemId == R.id.action_stop) {

            ensureStopped()
            reconfigureUI(false)

            return true
        }

        return super.onOptionsItemSelected(item)
    }


    @TargetApi(23)
    private fun getPerms(): ArrayList<String> {
        val perms = ArrayList<String>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.ACCESS_FINE_LOCATION)

            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)

            if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.INTERNET)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED)
                    perms.add(Manifest.permission.FOREGROUND_SERVICE)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
                        perms.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }

            if (perms.size > 0) {
                requestPermissions(perms.toTypedArray(), SDK_PERMISSION_REQUEST)
            }
        }
        return perms;
    }

    @TargetApi(23)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}