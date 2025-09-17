package co.daily.android.starterkit.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import co.daily.android.starterkit.DemoState
import co.daily.android.starterkit.ParticipantDetails
import co.daily.android.starterkit.Utils
import co.daily.android.starterkit.services.DemoCallService
import co.daily.android.starterkit.R
import co.daily.android.starterkit.databinding.InCallButtonsOverlayBinding
import co.daily.model.CallState
import co.daily.model.RequestListener

class InCallButtonsOverlay(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : FrameLayout(
    context, attrs, defStyleAttr, defStyleRes
) {
    private val layoutBinding: InCallButtonsOverlayBinding

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    init {
        layoutBinding = DataBindingUtil.inflate<InCallButtonsOverlayBinding?>(
            LayoutInflater.from(context),
            R.layout.in_call_buttons_overlay,
            this,
            true).apply {
        }
    }

    fun setData(participant: ParticipantDetails) {
        layoutBinding.camButton.isChecked = participant.camEnabled
        layoutBinding.micButton.isChecked = participant.micEnabled
    }

    fun onCallStateUpdated(state: DemoState) {
        val leavingCall = state.status != CallState.joined
        layoutBinding.leaveCallButton.isEnabled = !leavingCall
    }

    fun setServiceBinder(binder: () -> DemoCallService.Binder?) {

        fun setButtonListenerWithDisable(button: View, action: (RequestListener) -> Unit) {
            button.setOnClickListener {
                button.isEnabled = false
                action {
                    button.isEnabled = true
                }
            }
        }

        layoutBinding.apply {

            setButtonListenerWithDisable(micButton) {
                binder()?.toggleMicInput(micButton.isChecked, it)
            }

            setButtonListenerWithDisable(camButton) {
                binder()?.toggleCamInput(camButton.isChecked, it)
            }

            setButtonListenerWithDisable(leaveCallButton) {
                binder()?.leave(it)
            }

            setButtonListenerWithDisable(flipCameraButton) {
                binder()?.flipCameraDirection(it)
            }

            moreOptionsButton.setOnClickListener {
                Utils.showPopupMenu(context, moreOptionsButton, R.menu.in_call_more_options) {
                    context?.let { context ->
                        when (it) {
                            R.id.in_call_more_options_people -> {
                                ParticipantsListView.open(context)
                            }

                            R.id.in_call_more_options_invite -> {
                                binder()?.let { callService ->
                                    Utils.copyMeetingLinkToClipboard(
                                        context,
                                        callService.mostRecentJoinUrl!!
                                    )
                                }
                            }
                        }
                    }
                }
            }

            audioButton.setOnClickListener {
                context?.let { context ->
                    binder()?.let { binder ->
                        val builder = AlertDialog.Builder(context)

                        val state = binder.state
                        val devices = state.availableDevices.audio

                        builder.setSingleChoiceItems(
                            devices.map { it.label }.toTypedArray(),
                            devices.indexOfFirst { it.deviceId == state.activeAudioDeviceId }
                                .takeUnless { it == -1 } ?: 0
                        ) { _, pos ->
                            binder.setAudioDevice(devices[pos])
                        }

                        builder.setNegativeButton(R.string.button_close) { _, _ -> }
                        builder.show()
                    }
                }
            }
        }
    }

    fun toggleVisibility() {
        layoutBinding.buttonOverlayView.apply {
            visibility = if (visibility == GONE) {
                VISIBLE
            } else {
                GONE
            }
        }
    }
}
