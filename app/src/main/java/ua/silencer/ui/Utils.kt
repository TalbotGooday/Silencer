package ua.silencer.ui

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

@ColorInt
fun Context.getColorFromAttr(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

fun View.setFadedBackgroundColor(
    @ColorInt colorNew: Int,
    @ColorInt colorOld: Int? = null,
    duration: Long = 1500L,
    delay: Long = 0
) {
    val colorOldFinal = colorOld ?: Color.TRANSPARENT
    val animator = ValueAnimator.ofFloat(0f, .8f, 0f)
    animator.duration = duration
    animator.startDelay = delay
    animator.start()

    val argbEvaluator = ArgbEvaluator()
    animator.addUpdateListener { animation ->
        val animatedValue = animation.animatedValue as Float

        val color = argbEvaluator.evaluate(animatedValue, colorOldFinal, colorNew) as Int
        setBackgroundColor(color)
    }
}

