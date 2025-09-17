package co.daily.android.starterkit.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import co.daily.android.starterkit.ParticipantDetails
import co.daily.android.starterkit.R
import co.daily.android.starterkit.databinding.ParticipantVideoViewBinding
import co.daily.view.VideoView

class ParticipantVideoView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : FrameLayout(
    context, attrs, defStyleAttr, defStyleRes
) {
    private val binding: ParticipantVideoViewBinding
    private val videoView: VideoView?

    private val borderColorNormal: Int
    private val borderColorActive: Int

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    init {
        binding = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            R.layout.participant_video_view,
            this,
            true)

        if (!isInEditMode) {
            videoView = VideoView(context)
            binding.participantVideoContainer.addView(videoView)
        } else {
            videoView = null
        }

        borderColorNormal = context.resources.getColor(R.color.card_border_normal, null)
        borderColorActive = context.resources.getColor(R.color.card_border_active_speaker, null)
    }

    fun setData(participant: ParticipantDetails, activeSpeaker: ParticipantDetails.Id?) {
        binding.participantUsernameText.text = participant.username

        binding.participantVideoCard.strokeColor = if (activeSpeaker == participant.id) {
             borderColorActive
        } else {
            borderColorNormal
        }

        if (participant.camEnabled) {
            videoView?.visibility = VISIBLE
            videoView?.track = participant.videoTrack
            binding.participantVideoOffPlaceholder.visibility = GONE
        } else {
            videoView?.visibility = GONE
            binding.participantVideoOffPlaceholder.visibility = VISIBLE
        }

        if (participant.micEnabled) {
            binding.participantUsernameMicOff.visibility = GONE
            binding.participantUsernameMicOn.visibility = VISIBLE
        } else {
            binding.participantUsernameMicOn.visibility = GONE
            binding.participantUsernameMicOff.visibility = VISIBLE
        }
    }
}