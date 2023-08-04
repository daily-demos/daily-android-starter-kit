package co.daily.android.starterkit.layouts

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import kotlin.math.min


class DailySquareAspectLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val parentW = MeasureSpec.getSize(widthMeasureSpec)
        val parentH = MeasureSpec.getSize(heightMeasureSpec)

        val itemSize = min(parentW, parentH)

        val newSpec = MeasureSpec.makeMeasureSpec(itemSize, MeasureSpec.EXACTLY)

        setMeasuredDimension(newSpec, newSpec)

        measureChildren(newSpec, newSpec)
    }
}