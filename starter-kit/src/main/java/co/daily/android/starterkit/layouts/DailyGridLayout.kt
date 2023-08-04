package co.daily.android.starterkit.layouts

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.core.view.children
import kotlin.math.min


class DailyGridLayout @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ViewGroup(context, attrs, defStyleAttr, defStyleRes) {

    class LayoutParams : MarginLayoutParams {
        constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs)
        constructor(width: Int, height: Int) : super(width, height)
        constructor(source: ViewGroup.LayoutParams?) : super(source)
    }

    override fun generateLayoutParams(attrs: AttributeSet?) = LayoutParams(context, attrs)

    override fun generateLayoutParams(p: ViewGroup.LayoutParams) = LayoutParams(p)

    override fun generateDefaultLayoutParams() = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
    )

    private var rows = 1
    private var cols = 1

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (childCount == 0) {
            return
        }

        rows = 1
        cols = 1

        while (rows * cols < childCount) {

            val itemW = measuredWidth / cols
            val itemH = measuredHeight / rows

            if (itemW > itemH) {
                cols++
            } else {
                rows++
            }
        }

        if ((rows - 1) * cols >= childCount) {
            rows--
        }

        if ((cols - 1) * rows >= childCount) {
            cols--
        }

        val itemW = measuredWidth / cols
        val itemH = measuredHeight / rows

        val itemSize = min(itemW, itemH)

        children.forEach {
            measureChildWithMargins(
                it,
                widthMeasureSpec,
                MeasureSpec.getSize(widthMeasureSpec) - itemSize,
                heightMeasureSpec,
                MeasureSpec.getSize(heightMeasureSpec) - itemSize,
            )
        }

    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {

        val itemSize = min(measuredWidth / cols, measuredHeight / rows)

        val children = children.toList()
        val childCountOnLastRow = (childCount % cols).takeUnless { it == 0 } ?: cols

        val hOffset = (measuredWidth - itemSize * cols) / 2
        val vOffset = (measuredHeight - itemSize * rows) / 2

        children.forEachIndexed { index, child ->

            val row = index / cols
            val col = index % cols

            val lastRowOffset = if (row == rows - 1) {
                ((cols - childCountOnLastRow) * itemSize) / 2
            } else {
                0
            }

            val childLeft = l + col * itemSize + hOffset + lastRowOffset
            val childTop = t + row * itemSize + vOffset

            child.layout(
                childLeft,
                childTop,
                childLeft + child.measuredWidth,
                childTop + child.measuredHeight
            )
        }
    }
}