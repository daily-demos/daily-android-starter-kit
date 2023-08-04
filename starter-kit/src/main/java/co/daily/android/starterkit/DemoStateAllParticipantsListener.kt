package co.daily.android.starterkit

interface DemoStateAllParticipantsListener {
    fun onRegistered(allParticipants: Map<ParticipantDetails.Id, ParticipantDetails>)
    fun onJoined(participant: ParticipantDetails)
    fun onUpdated(participant: ParticipantDetails)
    fun onLeft(participant: ParticipantDetails)
}
