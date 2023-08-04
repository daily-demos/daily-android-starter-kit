package co.daily.android.starterkit

class ParticipantStateTracker(private val listener: Listener) {

    fun interface Listener {
        fun onShownParticipantsUpdated(participants: Map<ParticipantDetails.Id.Remote, ParticipantDetails>)
    }

    private var remoteParticipantsShownMax = 6
    private val remoteParticipantsShown = LinkedHashSet<ParticipantDetails.Id.Remote>()
    private val remoteParticipantsNotShown = LinkedHashSet<ParticipantDetails.Id.Remote>()
    private val remoteParticipantsData: MutableMap<ParticipantDetails.Id.Remote, ParticipantDetails> =
        mutableMapOf()

    private fun notifyShownUpdate() {
        listener.onShownParticipantsUpdated(remoteParticipantsShown.associateWith { remoteParticipantsData[it]!! })
    }

    fun setParticipantsShown(max: Int) {

        if (max == remoteParticipantsShownMax) {
            return
        }

        remoteParticipantsShownMax = max

        if (max == remoteParticipantsShown.size) {
            return

        } else if (max >= remoteParticipantsShown.size) {
            remoteParticipantsNotShown.reversed().take(max - remoteParticipantsShown.size).forEach {
                remoteParticipantsNotShown.remove(it)
                remoteParticipantsShown.add(it)
            }

            notifyShownUpdate()

        } else {
            remoteParticipantsShown.take(remoteParticipantsShown.size - max).forEach {
                remoteParticipantsShown.remove(it)
                remoteParticipantsNotShown.add(it)
            }

            notifyShownUpdate()
        }
    }

    fun onParticipantJoined(participant: ParticipantDetails) = ifRemote(participant) { id ->

        remoteParticipantsData.put(id, participant)

        if (remoteParticipantsShown.size < remoteParticipantsShownMax) {
            remoteParticipantsShown.add(id)
            notifyShownUpdate()
        } else {
            remoteParticipantsNotShown.add(id)
        }
    }

    fun onParticipantUpdated(participant: ParticipantDetails) = ifRemote(participant) { id ->

        val oldData = remoteParticipantsData.put(id, participant)

        if (remoteParticipantsShown.contains(id) && oldData != participant) {
            notifyShownUpdate()
        }
    }

    fun onParticipantLeft(participant: ParticipantDetails) = ifRemote(participant) { id ->

        remoteParticipantsData.remove(id)
        remoteParticipantsNotShown.remove(id)

        if (remoteParticipantsShown.remove(id)) {

            remoteParticipantsNotShown.firstOrNull()?.apply {
                remoteParticipantsNotShown.remove(this)
                remoteParticipantsShown.add(this)
            }

            notifyShownUpdate()
        }
    }

    fun onActiveSpeakerChanged(participant: ParticipantDetails) = ifRemote(participant) { id ->

        if (remoteParticipantsNotShown.remove(participant.id)) {

            // Remove the oldest participant in the list
            remoteParticipantsShown.firstOrNull()?.apply {
                remoteParticipantsShown.remove(this)
                remoteParticipantsNotShown.add(this)
            }

            remoteParticipantsShown.add(id)

            notifyShownUpdate()

        } else if (remoteParticipantsShown.lastOrNull() != id) {

            // Move to the end of the list
            remoteParticipantsShown.remove(id)
            remoteParticipantsShown.add(id)

            // No need to notify -- the list order isn't used by the UI
        }
    }

    companion object {
        private inline fun ifRemote(
            participant: ParticipantDetails,
            action: (ParticipantDetails.Id.Remote) -> Unit
        ) {
            when (participant.id) {
                ParticipantDetails.Id.Local -> return
                is ParticipantDetails.Id.Remote -> action(participant.id)
            }
        }
    }
}