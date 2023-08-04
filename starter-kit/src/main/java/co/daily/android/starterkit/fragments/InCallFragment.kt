package co.daily.android.starterkit.fragments

import android.animation.LayoutTransition
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.setMargins
import androidx.databinding.DataBindingUtil
import co.daily.android.starterkit.DemoState
import co.daily.android.starterkit.DemoStateListener
import co.daily.android.starterkit.ParticipantDetails
import co.daily.android.starterkit.layouts.DailyGridLayout
import co.daily.android.starterkit.views.ParticipantVideoView
import co.daily.core.dailydemo.R
import co.daily.core.dailydemo.databinding.FragmentInCallBinding
import kotlin.math.roundToInt

class InCallFragment : FragmentBase(), DemoStateListener {

    companion object {
        private const val TAG = "InCallFragment"
    }

    private class ParticipantView(
        context: Context,
        private var participant: ParticipantDetails,
        private var activeSpeaker: ParticipantDetails.Id?
    ) {
        val view = ParticipantVideoView(context).apply {
            setData(participant, activeSpeaker)
        }

        fun update(newParticipant: ParticipantDetails, newActiveSpeaker: ParticipantDetails.Id?) {
            if (participant != newParticipant || activeSpeaker != newActiveSpeaker) {
                participant = newParticipant
                activeSpeaker = newActiveSpeaker
                view.setData(participant, activeSpeaker)
            }
        }

        fun participant() = participant
    }

    private lateinit var layoutBinding: FragmentInCallBinding
    private lateinit var grid: DailyGridLayout

    private val participantViews = ArrayList<ParticipantView>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_in_call, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        layoutBinding = DataBindingUtil.bind(view)!!

        grid = DailyGridLayout(requireContext())
        grid.layoutTransition = LayoutTransition()

        layoutBinding.apply {

            videoGridContainer.addView(grid)

            buttonsOverlay.setServiceBinder { callService }

            backgroundTapIntercept.setOnClickListener {
                buttonsOverlay.toggleVisibility()
            }
        }
    }

    override fun onStateChanged(newState: DemoState) {
        layoutBinding.buttonsOverlay.onCallStateUpdated(newState)
        layoutBinding.localParticipantVideoView.setData(newState.localParticipant, newState.activeSpeaker)
        layoutBinding.buttonsOverlay.setData(newState.localParticipant)

        context?.let { context ->

            val existingViews = ArrayList(participantViews)

            val participants = HashMap(newState.remoteParticipantsToShow)

            // Handle existing participants

            existingViews.removeIf { existingView ->

                val matchingParticipant =
                    participants.remove(existingView.participant().id) ?: return@removeIf false

                Log.i(TAG, "Updating view for '${matchingParticipant.username}'")

                existingView.update(matchingParticipant, newState.activeSpeaker)
                true
            }

            // Match new participants to existing views

            while (participants.isNotEmpty() && existingViews.isNotEmpty()) {

                val participant = participants.remove(participants.iterator().next().key)!!
                val view = existingViews.removeFirst()

                Log.i(TAG, "Switching a view from '${view.participant().username}' to '${participant.username}'")

                view.update(participant, newState.activeSpeaker)
            }

            // Handle displayed participant count change

            if (existingViews.isEmpty()) {
                participants.values.forEach {

                    Log.i(TAG, "Creating a view for ${it.username}")

                    participantViews.add(ParticipantView(context, it, newState.activeSpeaker).apply {
                        this.view.layoutParams = MarginLayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        ).apply {
                            setMargins(TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                4f,
                                context.resources.displayMetrics
                            ).roundToInt())
                        }
                        grid.addView(this.view)
                    })
                }
            } else if (participants.isEmpty()) {
                existingViews.forEach {

                    Log.i(TAG, "Removing a view previously assigned to ${it.participant().username}")

                    participantViews.remove(it)
                    grid.removeView(it.view)
                }
            }
        }
    }
}