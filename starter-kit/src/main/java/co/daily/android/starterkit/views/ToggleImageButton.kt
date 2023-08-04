package co.daily.android.starterkit.views

import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import androidx.appcompat.widget.AppCompatImageButton

class ToggleImageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageButton(
    context, attrs, defStyleAttr
), Checkable {

    override fun setChecked(checked: Boolean) {
        isSelected = checked
    }

    override fun isChecked() = isSelected

    override fun toggle() {
        isChecked = !isChecked
    }

    override fun performClick(): Boolean {
        toggle()
        return super.performClick()
    }
}