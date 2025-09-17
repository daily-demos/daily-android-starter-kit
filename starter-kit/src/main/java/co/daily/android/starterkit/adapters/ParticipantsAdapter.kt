package co.daily.android.starterkit.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import co.daily.android.starterkit.DemoState
import co.daily.android.starterkit.DemoStateAllParticipantsListener
import co.daily.android.starterkit.DemoStateListener
import co.daily.android.starterkit.ParticipantDetails
import co.daily.android.starterkit.Utils
import co.daily.android.starterkit.services.DemoCallService
import co.daily.android.starterkit.R
import co.daily.android.starterkit.databinding.DialogUsernameBinding
import co.daily.android.starterkit.databinding.ParticipantListEntryBinding
import co.daily.model.CallState
import java.util.TreeSet

/**
 * The logic in this class is very carefully constructed to perform ALMOST all
 * operations in O(log(n)) time.
 *
 * Participants are tracked in two collections:
 *
 *   * `participants`: this is a map of known participant details, with the ID as the key
 *   * `participantsSorted`: this is a sorted set of participants, by username (and then by
 *      id in the case of conflicts)
 *
 *  We need `participants` to be able to remove items from `participantsSorted` by ID in O(log(n))
 *  time.
 *
 *  Unfortunately, getting an element from a TreeSet by index is O(n), however this could be
 *  optimised in future by using an indexed treeset.
 */
class ParticipantsAdapter(
    private val helper: Helper
) : RecyclerView.Adapter<ParticipantsAdapter.ParticipantVH>(), DemoStateListener {

    interface Helper {
        var localUsername: String?
    }

    inner class ParticipantVH(private val layout: ParticipantListEntryBinding) : RecyclerView.ViewHolder(
        layout.root
    ) {
        fun bind(participant: ParticipantDetails) {
            layout.apply {
                username.text = participant.username

                if (participant.micEnabled) {
                    participantMicOff.visibility = View.GONE
                    participantMicOn.visibility = View.VISIBLE
                } else {
                    participantMicOff.visibility = View.VISIBLE
                    participantMicOn.visibility = View.GONE
                }

                if (participant.id == ParticipantDetails.Id.Local) {
                    participantMoreOptions.visibility = View.VISIBLE
                    participantMoreOptions.setOnClickListener {
                        Utils.showPopupMenu(
                            itemView.context,
                            participantMoreOptions,
                            R.menu.participant_more_options
                        ) {
                            when (it) {
                                R.id.participant_more_options_set_username -> {
                                    onSetUsernameSelected()
                                }
                            }
                        }
                    }

                } else {
                    participantMoreOptions.visibility = View.GONE
                }
            }
        }

        private fun onSetUsernameSelected() {
            AlertDialog.Builder(itemView.context).apply {
                DialogUsernameBinding.inflate(LayoutInflater.from(itemView.context)).apply {
                    setView(root)
                    inputUsername.setText(helper.localUsername)

                    setPositiveButton(R.string.button_set) { _, _ ->
                        helper.localUsername = inputUsername.text.toString()
                    }

                    setNegativeButton(R.string.button_cancel) { _, _ -> }

                    show()
                }
            }
        }
    }

    private var participantsListener: DemoStateAllParticipantsListener? = null

    private var binder: DemoCallService.Binder? = null

    private val participantsSorted = TreeSet<ParticipantDetails> { a, b ->
        a.username.compareTo(b.username).takeUnless { it == 0 }
            ?: a.id.compareTo(b.id)
    }

    private val participants = HashMap<ParticipantDetails.Id, ParticipantDetails>()

    private fun positionOf(participant: ParticipantDetails) = participantsSorted.headSet(participant).size

    fun bindService(binder: DemoCallService.Binder) {

        if (this.binder != null) {
            throw RuntimeException("Already bound!")
        }

        this.binder = binder

        val participantsListener = object : DemoStateAllParticipantsListener {

            @SuppressLint("NotifyDataSetChanged")
            override fun onRegistered(
                allParticipants: Map<ParticipantDetails.Id, ParticipantDetails>
            ) = ifNotObsolete {

                participantsSorted.clear()
                participants.clear()

                participants.putAll(allParticipants)
                participantsSorted.addAll(allParticipants.values)

                notifyDataSetChanged()
            }

            override fun onJoined(participant: ParticipantDetails) = ifNotObsolete {

                if (participants.contains(participant.id)) {
                    throw RuntimeException("Got participantJoined for existing participant!")
                }

                participants[participant.id] = participant
                participantsSorted.add(participant)

                notifyItemInserted(positionOf(participant))
            }

            override fun onUpdated(participant: ParticipantDetails) = ifNotObsolete {

                if (!participants.contains(participant.id)) {
                    throw RuntimeException("Got participantUpdated for unknown participant!")
                }

                val oldParticipant = participants.remove(participant.id)!!

                val oldPosition = positionOf(oldParticipant)

                participantsSorted.remove(oldParticipant)

                participants[participant.id] = participant
                participantsSorted.add(participant)

                val newPosition = positionOf(participant)

                if (newPosition != oldPosition) {
                    notifyItemMoved(oldPosition, newPosition)
                }

                notifyItemChanged(newPosition)
            }

            override fun onLeft(participant: ParticipantDetails) = ifNotObsolete {

                val oldParticipant = participants.remove(participant.id)!!

                val oldPosition = positionOf(oldParticipant)

                participantsSorted.remove(oldParticipant)

                notifyItemRemoved(oldPosition)
            }

            private inline fun ifNotObsolete(action: () -> Unit) {
                if (participantsListener == this) {
                    action()
                }
            }
        }

        this.participantsListener = participantsListener

        binder.addAllParticipantsListener(participantsListener)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun unbindService() {

        binder!!.removeAllParticipantsListener(participantsListener!!)
        binder = null

        participantsListener = null

        participantsSorted.clear()
        participants.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ParticipantVH(ParticipantListEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ))

    override fun onBindViewHolder(holder: ParticipantVH, position: Int) {
        // Unfortunately this is an O(n) operation, however for very large calls, this could
        // be optimised by using an indexed treeset.
        holder.bind(participantsSorted.elementAt(position))
    }

    override fun getItemCount() = participantsSorted.size

    override fun onStateChanged(newState: DemoState) {
        if (newState.status == CallState.left) {

            participants.keys.iterator().apply {
                while (hasNext()) {
                    if (next() != ParticipantDetails.Id.Local) remove()
                }
            }

            participantsSorted.iterator().apply {
                while (hasNext()) {
                    if (next().id != ParticipantDetails.Id.Local) remove()
                }
            }
        }
    }
}