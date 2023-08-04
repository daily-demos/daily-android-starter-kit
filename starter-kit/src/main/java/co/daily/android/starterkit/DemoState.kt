package co.daily.android.starterkit

import co.daily.model.AvailableDevices
import co.daily.model.CallState

data class DemoState(
    val status: CallState,
    val localParticipant: ParticipantDetails,
    val availableDevices: AvailableDevices,
    val activeAudioDeviceId: String?,
    val participantCount: Int,
    val remoteParticipantsToShow: Map<ParticipantDetails.Id.Remote, ParticipantDetails>,
    val activeSpeaker: ParticipantDetails.Id?
) {
    companion object {
        fun default(): DemoState = DemoState(
            status = CallState.initialized,
            localParticipant = ParticipantDetails.defaultLocal,
            availableDevices = AvailableDevices(
                camera = emptyList(),
                microphone = emptyList(),
                speaker = emptyList(),
                audio = emptyList()
            ),
            activeAudioDeviceId = null,
            participantCount = 1,
            remoteParticipantsToShow = emptyMap(),
            activeSpeaker = null
        )
    }
}
