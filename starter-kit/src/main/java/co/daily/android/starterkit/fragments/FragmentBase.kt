package co.daily.android.starterkit.fragments

import android.content.Context
import androidx.fragment.app.Fragment
import co.daily.android.starterkit.CallServiceConnection
import co.daily.android.starterkit.DemoStateListener
import co.daily.android.starterkit.services.DemoCallService

abstract class FragmentBase : Fragment(), DemoStateListener {

    private var serviceConnection: CallServiceConnection? = null

    protected val callService: DemoCallService.Binder?
        get() = serviceConnection?.service

    override fun onAttach(context: Context) {
        super.onAttach(context)
        serviceConnection = CallServiceConnection(context, this, this::onServiceConnected)
    }

    open fun onServiceConnected(binder: DemoCallService.Binder) {}

    override fun onDetach() {
        super.onDetach()
        serviceConnection!!.close()
        serviceConnection = null
    }
}