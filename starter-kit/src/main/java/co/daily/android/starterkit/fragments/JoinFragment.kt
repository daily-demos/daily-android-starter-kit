package co.daily.android.starterkit.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import co.daily.android.starterkit.DemoState
import co.daily.android.starterkit.DemoStateListener
import co.daily.android.starterkit.Preferences
import co.daily.android.starterkit.Utils
import co.daily.android.starterkit.R
import co.daily.android.starterkit.databinding.FragmentJoinBinding
import co.daily.model.CallState
import co.daily.model.RequestListener
import co.daily.view.VideoView
import java.net.URL

class JoinFragment : FragmentBase(), DemoStateListener {

    private lateinit var layoutBinding: FragmentJoinBinding
    private lateinit var videoView: VideoView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_join, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        layoutBinding = DataBindingUtil.bind(view)!!
        
        val prefs = Preferences(requireContext())

        fun setButtonListenerWithDisable(
            button: View,
            action: (RequestListener) -> Unit
        ) {
            button.setOnClickListener {
                button.isEnabled = false
                action {
                    button.isEnabled = true
                }
            }
        }
        
        layoutBinding.apply {
            
            segmentLocalPreview.apply {
                videoView = VideoView(requireContext())
                videoContainer.addView(videoView)

                setButtonListenerWithDisable(micButton) {
                    callService?.toggleMicInput(micButton.isChecked, it)
                }

                setButtonListenerWithDisable(camButton) {
                    callService?.toggleCamInput(camButton.isChecked, it)
                }
            }
            
            segmentTextInput.apply {
                joinInputUrl.setText(prefs.lastUrl)
                joinInputUsername.setText(prefs.lastUsername)

                Utils.listenForTextChange(joinInputUrl) {
                    prefs.lastUrl = it
                }

                Utils.listenForTextChange(joinInputUsername) {
                    prefs.lastUsername = it
                }
            }

            segmentJoinButton.joinButton.setOnClickListener {
                callService?.apply {

                    setUsername(segmentTextInput.joinInputUsername.text?.toString()?.ifEmpty { null } ?: "Guest")

                    val url = segmentTextInput.joinInputUrl.text?.toString()

                    val isValid = try {
                        URL(url)
                        true
                    } catch(_: Exception) {
                        false
                    }

                    if (url == null || !isValid) {
                        Toast(requireContext()).apply {
                            setText(R.string.join_error_invalid_url)
                            show()
                        }
                    } else {
                        segmentJoinButton.joinButton.isEnabled = false
                        join(url = url, token = null)
                    }
                }
            }
            
        }
    }

    override fun onStateChanged(newState: DemoState) {

        if (newState.localParticipant.camEnabled) {
            videoView.visibility = View.VISIBLE
            layoutBinding.segmentLocalPreview.joinVideoOffPlaceholder.visibility = View.GONE
        } else {
            videoView.visibility = View.GONE
            layoutBinding.segmentLocalPreview.joinVideoOffPlaceholder.visibility = View.VISIBLE
        }

        videoView.track = newState.localParticipant.videoTrack

        layoutBinding.apply {
            segmentLocalPreview.micButton.isChecked = newState.localParticipant.micEnabled
            segmentLocalPreview.camButton.isChecked = newState.localParticipant.camEnabled

            if (newState.status == CallState.joining) {
                segmentJoinButton.joinButton.isEnabled = false
                segmentTextInput.joinInputUrl.isEnabled = false
                segmentTextInput.joinInputUsername.isEnabled = false

            } else {
                segmentJoinButton.joinButton.isEnabled = true
                segmentTextInput.joinInputUrl.isEnabled = true
                segmentTextInput.joinInputUsername.isEnabled = true
            }
        }
    }
}