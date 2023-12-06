package co.daily.android.starterkit

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import co.daily.android.starterkit.fragments.InCallFragment
import co.daily.android.starterkit.fragments.JoinFragment
import co.daily.android.starterkit.fragments.JoiningFragment
import co.daily.android.starterkit.fragments.WaitingForOthersFragment
import co.daily.android.starterkit.services.DemoActiveCallService
import co.daily.core.dailydemo.R
import co.daily.model.CallState

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity(), DemoStateListener {

    private enum class FragmentType {
        Join,
        Joining,
        WaitingForOthers,
        InCall
    }

    private val requestPermissionLauncher =
        registerForActivityResult(RequestMultiplePermissions()) { result ->
            if (result.values.any { !it }) {
                checkPermissions()
            } else {
                // permission is granted, we can initialize
                initialize()
            }
        }

    private var serviceConnection: CallServiceConnection? = null

    private var currentFragment: FragmentType? = null

    private var triggeredForegroundService = false

    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)

        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        checkPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceConnection?.close()
        serviceConnection = null
    }

    private fun initialize() {
        serviceConnection = CallServiceConnection(this, this) {}
    }

    private fun checkPermissions() {
        val permissionList = applicationContext.packageManager
            .getPackageInfo(applicationContext.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions

        val notGrantedPermissions = permissionList.map {
            Pair(it, ContextCompat.checkSelfPermission(applicationContext, it))
        }.filter {
            it.second != PackageManager.PERMISSION_GRANTED
        }.map {
            it.first
        }.toTypedArray()

        if (notGrantedPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(notGrantedPermissions)
        } else {
            // permission is granted, we can initialize
            initialize()
        }
    }

    override fun onStateChanged(newState: DemoState) {
        Log.i(TAG, "onCallStateChanged: $newState")

        chooseFragment(
            when (newState.status) {
                CallState.initialized, CallState.left -> FragmentType.Join
                CallState.joining -> FragmentType.Joining
                CallState.joined, CallState.leaving -> if (newState.participantCount < 2) {
                    FragmentType.WaitingForOthers
                } else {
                    FragmentType.InCall
                }
            }
        )

        when (newState.status) {
            CallState.joined -> {
                if (!triggeredForegroundService) {

                    // Start the foreground service to keep the call alive
                    Log.i(TAG, "Starting foreground service")

                    ContextCompat.startForegroundService(
                        this,
                        Intent(this, DemoActiveCallService::class.java)
                    )

                    triggeredForegroundService = true
                }
            }
            CallState.left -> {
                triggeredForegroundService = false
            }
            else -> {}
        }
    }

    private fun chooseFragment(type: FragmentType) {

        if (type == currentFragment) {
            return
        }

        currentFragment = type

        val fragmentClass = when (type) {
            FragmentType.Join -> JoinFragment::class.java
            FragmentType.WaitingForOthers -> WaitingForOthersFragment::class.java
            FragmentType.InCall -> InCallFragment::class.java
            FragmentType.Joining -> JoiningFragment::class.java
        }

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container_view, fragmentClass, null)
        }
    }

    override fun onError(msg: String) {
        Log.e(TAG, "Got error: $msg")
        Toast.makeText(this, "Error: $msg", Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        when (currentFragment) {
            FragmentType.WaitingForOthers, FragmentType.InCall -> serviceConnection?.service?.leave {}
            else -> super.onBackPressed()
        }
    }
}
