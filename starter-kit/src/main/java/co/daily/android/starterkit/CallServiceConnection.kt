package co.daily.android.starterkit

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import co.daily.android.starterkit.services.DemoCallService

class CallServiceConnection(
    private val context: Context,
    private val listener: DemoStateListener,
    private val connectedListener: (DemoCallService.Binder) -> Unit
) {

    private companion object {
        val TAG = "CallServiceConnection"
    }

    private var callService: DemoCallService.Binder? = null
    private var mostRecentState: DemoState? = null

    private val innerListener = object : DemoStateListener {
        override fun onStateChanged(newState: DemoState) {
            mostRecentState = newState
            listener.onStateChanged(newState)
        }

        override fun onError(msg: String) {
            listener.onError(msg)
        }
    }

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder) {
            Log.i(TAG, "Connected to service")
            callService = service as DemoCallService.Binder
            callService?.addListener(innerListener)
            connectedListener(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "Disconnected from service")
            callService = null
        }
    }

    val service: DemoCallService.Binder?
        get() = callService

    val state: DemoState?
        get() = mostRecentState

    init {
        if (!context.bindService(
                Intent(context, DemoCallService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT
            )
        ) {
            throw RuntimeException("Failed to bind to call service")
        }
    }

    fun close() {
        context.unbindService(serviceConnection)
        callService?.removeListener(innerListener)
        callService = null
    }
}