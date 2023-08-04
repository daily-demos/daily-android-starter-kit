package co.daily.android.starterkit.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.util.Log
import co.daily.CallClient
import co.daily.CallClientListener
import co.daily.android.starterkit.DemoState
import co.daily.android.starterkit.DemoStateAllParticipantsListener
import co.daily.android.starterkit.DemoStateListener
import co.daily.android.starterkit.ParticipantDetails
import co.daily.android.starterkit.ParticipantStateTracker
import co.daily.model.AvailableDevices
import co.daily.model.CallState
import co.daily.model.MediaDeviceInfo
import co.daily.model.MeetingToken
import co.daily.model.NetworkStats
import co.daily.model.Participant
import co.daily.model.ParticipantCounts
import co.daily.model.ParticipantId
import co.daily.model.ParticipantLeftReason
import co.daily.model.RequestListener
import co.daily.model.livestream.LiveStreamStatus
import co.daily.model.recording.RecordingStatus
import co.daily.model.streaming.StreamId
import co.daily.settings.BitRate
import co.daily.settings.CameraInputSettingsUpdate
import co.daily.settings.CameraPublishingSettingsUpdate
import co.daily.settings.ClientSettingsUpdate
import co.daily.settings.FacingModeUpdate
import co.daily.settings.FrameRate
import co.daily.settings.InputSettings
import co.daily.settings.InputSettingsUpdate
import co.daily.settings.PublishingSettings
import co.daily.settings.PublishingSettingsUpdate
import co.daily.settings.Scale
import co.daily.settings.VideoEncodingSettingsUpdate
import co.daily.settings.VideoEncodingsSettingsUpdate
import co.daily.settings.VideoMaxQualityUpdate
import co.daily.settings.VideoMediaTrackSettingsUpdate
import co.daily.settings.VideoSendSettingsUpdate
import co.daily.settings.subscription.Subscribed
import co.daily.settings.subscription.SubscriptionProfile
import co.daily.settings.subscription.SubscriptionProfileSettings
import co.daily.settings.subscription.SubscriptionProfileSettingsUpdate
import co.daily.settings.subscription.SubscriptionSettings
import co.daily.settings.subscription.SubscriptionSettingsUpdate
import co.daily.settings.subscription.Unsubscribed
import co.daily.settings.subscription.VideoReceiveSettingsUpdate
import co.daily.settings.subscription.VideoSubscriptionSettingsUpdate
import co.daily.settings.subscription.base
import java.util.concurrent.CopyOnWriteArrayList

private const val TAG = "CallService"

private const val ACTION_LEAVE = "action_leave"

class DemoCallService : Service() {

    private val profileActiveCamera = SubscriptionProfile("activeCamera")
    private val profileActiveScreenShare = SubscriptionProfile("activeScreenShare")

    private val listeners = CopyOnWriteArrayList<DemoStateListener>()
    private val allParticipantsListeners = CopyOnWriteArrayList<DemoStateAllParticipantsListener>()

    private var state: DemoState = DemoState.default()

    private var cameraDirection = FacingModeUpdate.user

    private var callClient: CallClient? = null

    private val participantStateTrackerListener =
        ParticipantStateTracker.Listener { participants ->
            updateServiceState { it.copy(remoteParticipantsToShow = participants) }
            updateRemoteVideoSubscriptions()
        }

    private var participantStateTracker = ParticipantStateTracker(
        participantStateTrackerListener
    )

    private var mostRecentJoinUrl: String? = null

    private fun resetParticipants() {
        participantStateTracker = ParticipantStateTracker(
            participantStateTrackerListener
        )

        // We don't reset participantCounts here because the SDK doesn't
        // give us another callback if the value in a new call is the same
        // as the old call.
        updateServiceState { it.copy(
            remoteParticipantsToShow = emptyMap(),
        ) }
    }

    inner class Binder : android.os.Binder() {

        val mostRecentJoinUrl: String?
            get() = this@DemoCallService.mostRecentJoinUrl

        val state: DemoState
            get() = this@DemoCallService.state

        fun addListener(listener: DemoStateListener) {
            listeners.add(listener)
            listener.onStateChanged(state)
        }

        fun removeListener(listener: DemoStateListener) {
            listeners.remove(listener)
        }

        fun addAllParticipantsListener(listener: DemoStateAllParticipantsListener) {
            allParticipantsListeners.add(listener)
            listener.onRegistered(callClient!!.participants().all.values.map {
                ParticipantDetails.from(it).run {
                    id to this
                }
            }.toMap())
        }

        fun removeAllParticipantsListener(listener: DemoStateAllParticipantsListener) {
            allParticipantsListeners.remove(listener)
        }

        fun join(url: String, token: MeetingToken?) {

            updateServiceState { it.copy(status = CallState.joining) }
            this@DemoCallService.mostRecentJoinUrl = url

            callClient?.join(url, token, createClientSettings()) {
                it.error?.apply {
                    Log.e(TAG, "Got error while joining call: $msg")
                    listeners.forEach { it.onError("Failed to join call: $msg") }
                }
                it.success?.apply {
                    Log.i(TAG, "Successfully joined call")
                }
            }
        }

        fun leave(listener: RequestListener) {
            updateServiceState { it.copy(status = CallState.leaving) }
            callClient?.leave(listener)
        }

        fun setUsername(username: String) {
            callClient?.setUserName(username)

            // Update this immediately, before we get the callback
            updateServiceState { it.copy(localParticipant = it.localParticipant.copy(username = username)) }
        }

        fun flipCameraDirection(listener: RequestListener) {

            cameraDirection = when (cameraDirection) {
                FacingModeUpdate.user -> FacingModeUpdate.environment
                FacingModeUpdate.environment -> FacingModeUpdate.user
            }

            callClient?.updateInputs(
                InputSettingsUpdate(
                    camera = CameraInputSettingsUpdate(
                        settings = VideoMediaTrackSettingsUpdate(
                            facingMode = cameraDirection
                        )
                    )
                ),
                listener
            )
        }

        fun toggleMicInput(enabled: Boolean, listener: RequestListener) {
            Log.d(TAG, "toggleMicInput $enabled")
            callClient?.setInputsEnabled(microphone = enabled, listener = listener)
        }

        fun toggleCamInput(enabled: Boolean, listener: RequestListener) {
            Log.d(TAG, "toggleCamInput $enabled")
            callClient?.setInputsEnabled(camera = enabled, listener = listener)
        }

        fun toggleMicPublishing(enabled: Boolean, listener: RequestListener) {
            Log.d(TAG, "toggleMicPublishing $enabled")
            callClient?.setIsPublishing(microphone = enabled, listener = listener)
        }

        fun toggleCamPublishing(enabled: Boolean, listener: RequestListener) {
            Log.d(TAG, "toggleCamPublishing $enabled")
            callClient?.setIsPublishing(camera = enabled, listener = listener)
        }

        fun setAudioDevice(device: MediaDeviceInfo) {
            Log.i(TAG, "Setting audio device to $device")
            if (device.deviceId != state.activeAudioDeviceId) {
                callClient?.setAudioDevice(device.deviceId)
                updateServiceState { it.copy(activeAudioDeviceId = device.deviceId) }
            }
        }

        fun setParticipantsShown(count: Int) {
            participantStateTracker.setParticipantsShown(count)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand(${intent?.action})")
        if (intent?.action == ACTION_LEAVE) {
            callClient?.leave()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): Binder {
        Log.i(TAG, "onBind")
        return Binder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "onUnbind")
        stopSelf()
        return false
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")

        try {
            callClient = CallClient(appContext = applicationContext).apply {
                addListener(callClientListener)
                setupParticipantSubscriptionProfiles(this)

                setInputsEnabled(camera = true, microphone = true)

                updateVideoForLocalParticipant(participants().local)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Got exception while creating CallClient", e)
            listeners.forEach { it.onError("Failed to initialize call client") }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")
        callClient?.release()
        callClient = null
    }

    companion object {
        fun leaveIntent(context: Context): Intent =
            Intent(context, DemoCallService::class.java).apply {
                action = ACTION_LEAVE
            }
    }

    private val callClientListener = object : CallClientListener {
        override fun onCallStateUpdated(state: CallState) {
            Log.i(TAG, "onCallStateUpdated($state)")

            updateServiceState { it.copy(status = state) }

            if (state == CallState.left) {
                resetParticipants()

                // Workaround for SDK misreporting input state after leaving
                // (internal reference CSDK-1449)
                this@DemoCallService.state.localParticipant.apply {
                    callClient?.setInputsEnabled(
                        camera = camEnabled,
                        microphone = micEnabled
                    )
                }
            }
        }

        override fun onInputsUpdated(inputSettings: InputSettings) {
            Log.i(TAG, "onInputsUpdated($inputSettings)")
            updateServiceState {
                it.copy(
                    localParticipant = it.localParticipant.copy(
                        camEnabled = inputSettings.camera.isEnabled,
                        micEnabled = inputSettings.microphone.isEnabled
                    )
                )
            }
        }

        override fun onPublishingUpdated(publishingSettings: PublishingSettings) {
            Log.i(TAG, "onPublishingUpdated($publishingSettings)")
        }

        override fun onParticipantLeft(
            participant: Participant,
            reason: ParticipantLeftReason
        ) {
            Log.i(TAG, "onParticipantLeft($participant, $reason)")

            val details = ParticipantDetails.from(participant)

            allParticipantsListeners.forEach { it.onLeft(details) }

            updateVideoForLocalParticipant(participant)
            participantStateTracker.onParticipantLeft(details)
        }

        override fun onParticipantJoined(participant: Participant) {
            Log.i(TAG, "onParticipantJoined($participant)")

            val details = ParticipantDetails.from(participant)

            allParticipantsListeners.forEach { it.onJoined(details) }

            updateVideoForLocalParticipant(participant)
            participantStateTracker.onParticipantJoined(details)
        }

        override fun onParticipantUpdated(participant: Participant) {
            Log.i(TAG, "onParticipantUpdated($participant)")

            val details = ParticipantDetails.from(participant)

            allParticipantsListeners.forEach { it.onUpdated(details) }

            updateVideoForLocalParticipant(participant)
            participantStateTracker.onParticipantUpdated(details)

            if (participant.info.isLocal && state.localParticipant.username != participant.info.userName) {
                updateServiceState { it.copy(localParticipant = it.localParticipant.copy(
                    username = participant.info.userName ?: ParticipantDetails.defaultUsername
                )) }
            }
        }

        override fun onActiveSpeakerChanged(activeSpeaker: Participant?) {
            Log.i(TAG, "onActiveSpeakerChanged($activeSpeaker)")
            val details = activeSpeaker?.run { ParticipantDetails.from(this) }
            updateServiceState { it.copy(activeSpeaker = details?.id) }
            details?.apply { participantStateTracker.onActiveSpeakerChanged(this) }
        }

        override fun onError(message: String) {
            Log.i(TAG, "onError($message)")
            listeners.forEach { it.onError(message) }
        }

        override fun onSubscriptionsUpdated(
            subscriptions: Map<ParticipantId, SubscriptionSettings>
        ) {
            Log.i(TAG, "onSubscriptionsUpdated($subscriptions)")
        }

        override fun onSubscriptionProfilesUpdated(
            subscriptionProfiles: Map<SubscriptionProfile, SubscriptionProfileSettings>
        ) {
            Log.i(TAG, "onSubscriptionProfilesUpdated($subscriptionProfiles)")
        }

        override fun onAvailableDevicesUpdated(availableDevices: AvailableDevices) {
            Log.i(TAG, "onAvailableDevicesUpdated($availableDevices)")
            updateServiceState {
                it.copy(
                    availableDevices = availableDevices,
                    activeAudioDeviceId = callClient?.audioDevice()
                )
            }
        }

        override fun onAppMessage(message: String, from: ParticipantId) {
            Log.i(TAG, "onAppMessage($message, $from)")
        }

        override fun onParticipantCountsUpdated(newParticipantCounts: ParticipantCounts) {
            Log.i(TAG, "onParticipantCountsUpdated($newParticipantCounts)")
            updateServiceState { it.copy(participantCount = newParticipantCounts.present) }
        }

        override fun onNetworkStatsUpdated(newNetworkStatistics: NetworkStats) {
            Log.i(TAG, "onNetworkStatsUpdated($newNetworkStatistics)")
        }

        override fun onRecordingStarted(status: RecordingStatus) {
            Log.i(TAG, "onRecordingStarted($status)")
        }

        override fun onRecordingStopped(streamId: StreamId) {
            Log.i(TAG, "onRecordingStopped($streamId)")
        }

        override fun onRecordingError(streamId: StreamId, message: String) {
            Log.i(TAG, "onRecordingError($streamId, $message)")
            listeners.forEach { it.onError("Recording error: $message") }
        }

        override fun onLiveStreamStarted(status: LiveStreamStatus) {
            Log.i(TAG, "onLiveStreamStarted($status)")
        }

        override fun onLiveStreamStopped(streamId: StreamId) {
            Log.i(TAG, "onLiveStreamStopped($streamId)")
        }

        override fun onLiveStreamError(streamId: StreamId, message: String) {
            Log.i(TAG, "onLiveStreamError($streamId, $message)")
            listeners.forEach { it.onError("Live stream error: $message") }
        }

        override fun onLiveStreamWarning(streamId: StreamId, message: String) {
            Log.i(TAG, "onLiveStreamWarning($streamId, $message)")
            listeners.forEach { it.onError("Live stream warning: $message") }
        }
    }

    private fun updateVideoForLocalParticipant(participant: Participant) {
        if (participant.info.isLocal) {
            updateServiceState { it.copy(localParticipant = it.localParticipant.copy(
                videoTrack = participant.media?.camera?.track
            )) }
        }
    }

    private fun updateServiceState(stateUpdate: (DemoState) -> DemoState) {
        val newState = stateUpdate(state)
        state = newState
        listeners.forEach { it.onStateChanged(newState) }
    }

    private fun setupParticipantSubscriptionProfiles(callClient: CallClient) {
        callClient.updateSubscriptionProfiles(
            mapOf(
                profileActiveCamera to
                    SubscriptionProfileSettingsUpdate(
                        camera = VideoSubscriptionSettingsUpdate(
                            subscriptionState = Subscribed(),
                            receiveSettings = VideoReceiveSettingsUpdate(
                                maxQuality = VideoMaxQualityUpdate.high
                            )
                        ),
                        screenVideo = VideoSubscriptionSettingsUpdate(
                            subscriptionState = Unsubscribed()
                        )
                    ),
                profileActiveScreenShare to
                    SubscriptionProfileSettingsUpdate(
                        camera = VideoSubscriptionSettingsUpdate(
                            subscriptionState = Unsubscribed()
                        ),
                        screenVideo = VideoSubscriptionSettingsUpdate(
                            subscriptionState = Subscribed(),
                            receiveSettings = VideoReceiveSettingsUpdate(
                                maxQuality = VideoMaxQualityUpdate.high
                            )
                        )
                    ),
                SubscriptionProfile.base to
                    SubscriptionProfileSettingsUpdate(
                        camera = VideoSubscriptionSettingsUpdate(
                            subscriptionState = Unsubscribed()
                        ),
                        screenVideo = VideoSubscriptionSettingsUpdate(
                            subscriptionState = Unsubscribed()
                        )
                    )
            )
        )
    }

    private fun updateRemoteVideoSubscriptions() {
        callClient?.apply {
            updateSubscriptions(
                // Subscribe to the currently displayed participant
                forParticipants = state.remoteParticipantsToShow.keys.map { it.id }.associateWith {
                    SubscriptionSettingsUpdate(profile = profileActiveCamera)
                },
                // Unsubscribe from remote participants not currently displayed
                forParticipantsWithProfiles = mapOf(
                    profileActiveCamera to SubscriptionSettingsUpdate(
                        profile = SubscriptionProfile.base
                    ),
                    profileActiveScreenShare to SubscriptionSettingsUpdate(
                        profile = SubscriptionProfile.base
                    )
                )
            )
        }
    }

    private fun createClientSettings(): ClientSettingsUpdate {
        return ClientSettingsUpdate(
            publishingSettings = PublishingSettingsUpdate(
                camera = CameraPublishingSettingsUpdate(
                    sendSettings = VideoSendSettingsUpdate(
                        encodings = VideoEncodingsSettingsUpdate(
                            settings = mapOf(
                                VideoMaxQualityUpdate.low to
                                    VideoEncodingSettingsUpdate(
                                        maxBitrate = BitRate(80000),
                                        maxFramerate = FrameRate(10),
                                        scaleResolutionDownBy = Scale(4F)
                                    ),
                                VideoMaxQualityUpdate.medium to
                                    VideoEncodingSettingsUpdate(
                                        maxBitrate = BitRate(680000),
                                        maxFramerate = FrameRate(30),
                                        scaleResolutionDownBy = Scale(1F)
                                    )
                            )
                        )
                    )
                )
            )
        )
    }
}
