package co.daily.android.starterkit.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import co.daily.android.starterkit.CallServiceConnection
import co.daily.android.starterkit.ParticipantDetails
import co.daily.android.starterkit.Preferences
import co.daily.android.starterkit.adapters.ParticipantsAdapter
import co.daily.core.dailydemo.R
import co.daily.core.dailydemo.databinding.ParticipantsListBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class ParticipantsListView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : FrameLayout(
    context, attrs, defStyleAttr, defStyleRes
) {
    private val layoutBinding: ParticipantsListBinding
    private val adapter = ParticipantsAdapter(object : ParticipantsAdapter.Helper {
        override var localUsername: String?
            get() = service?.state?.localParticipant?.username
            set(value) {
                service?.service?.setUsername(value ?: ParticipantDetails.defaultUsername)
                Preferences(context).lastUsername = value
            }
    })

    private var service: CallServiceConnection? = null

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    init {
        layoutBinding = DataBindingUtil.inflate<ParticipantsListBinding>(
            LayoutInflater.from(context),
            R.layout.participants_list,
            this,
            true
        ).apply {
            peopleList.adapter = adapter
            peopleList.layoutManager = LinearLayoutManager(context)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        service = CallServiceConnection(context, adapter) {
            adapter.bindService(it)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        adapter.unbindService()
        service!!.close()
        service = null
    }

    companion object {
        fun open(context: Context) {

            val dialog = BottomSheetDialog(context)
            val view = ParticipantsListView(context)

            dialog.setCancelable(true)
            dialog.setContentView(view)

            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

            dialog.show()
        }
    }
}
