package co.daily.android.starterkit

interface DemoStateListener {
    fun onStateChanged(newState: DemoState) {}
    fun onError(msg: String) {}
}
