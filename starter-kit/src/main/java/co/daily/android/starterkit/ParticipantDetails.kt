package co.daily.android.starterkit

import co.daily.model.MediaState
import co.daily.model.MediaStreamTrack
import co.daily.model.Participant
import co.daily.model.ParticipantId

@Suppress("DataClassPrivateConstructor")
data class ParticipantDetails private constructor(
    val id: Id,
    val username: String,
    val videoTrack: MediaStreamTrack?,
    val camEnabled: Boolean,
    val micEnabled: Boolean
) {
    sealed class Id : Comparable<Id> {
        object Local : Id()
        data class Remote(val id: ParticipantId) : Id()

        override fun compareTo(other: Id) = when (this) {
            Local -> when (other) {
                Local -> 0
                is Remote -> -1
            }
            is Remote -> when (other) {
                Local -> 1
                is Remote -> id.compareTo(other.id)
            }
        }
    }

    companion object {
        const val defaultUsername = "Guest"

        fun from(participant: Participant) = ParticipantDetails(
            id = when (participant.info.isLocal) {
                true -> Id.Local
                false -> Id.Remote(participant.id)
            },
            username = participant.info.userName ?: defaultUsername,
            videoTrack = when (isMediaEnabled(participant.media?.camera?.state)) {
                false -> null
                true -> participant.media?.camera?.track
            },
            camEnabled = isMediaEnabled(participant.media?.camera?.state),
            micEnabled = isMediaEnabled(participant.media?.microphone?.state),
        )

        val defaultLocal = ParticipantDetails(
            id = Id.Local,
            username = defaultUsername,
            videoTrack = null,
            camEnabled = true,
            micEnabled = true
        )

        private fun isMediaEnabled(state: MediaState?) = when (state) {
            MediaState.blocked, MediaState.off, MediaState.interrupted, null -> false
            MediaState.receivable, MediaState.loading, MediaState.playable -> true
        }
    }
}