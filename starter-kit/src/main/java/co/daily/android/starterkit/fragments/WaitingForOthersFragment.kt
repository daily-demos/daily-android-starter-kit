package co.daily.android.starterkit.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import co.daily.android.starterkit.DemoState
import co.daily.android.starterkit.DemoStateListener
import co.daily.android.starterkit.Utils
import co.daily.android.starterkit.R
import co.daily.android.starterkit.databinding.FragmentWaitingForOthersBinding

class WaitingForOthersFragment : FragmentBase(), DemoStateListener {

    private lateinit var layoutBinding: FragmentWaitingForOthersBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_waiting_for_others, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        layoutBinding = DataBindingUtil.bind(view)!!

        layoutBinding.apply {
            buttonsOverlay.setServiceBinder { callService }

            backgroundTapIntercept.setOnClickListener {
                buttonsOverlay.toggleVisibility()
            }

            copyLinkButton.setOnClickListener {
                context?.let { context ->
                    callService?.let { callService ->
                        Utils.copyMeetingLinkToClipboard(context, callService.mostRecentJoinUrl!!)
                    }
                }
            }
        }
    }

    override fun onStateChanged(newState: DemoState) {
        layoutBinding.buttonsOverlay.onCallStateUpdated(newState)
        layoutBinding.waitingForOthersVideoCard.setData(newState.localParticipant, null)
        layoutBinding.buttonsOverlay.setData(newState.localParticipant)
    }
}