package ru.alenavir.tasks.ui.main

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import ru.alenavir.tasks.R

class AnimatedLogoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    init {
        setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(1.5f).scaleY(1.5f).setDuration(200).start()
                    setColorFilter(ContextCompat.getColor(context, R.color.colorAccent))
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                    setColorFilter(ContextCompat.getColor(context, R.color.colorTextPrimary))
                }
            }
            false
        }
    }

    fun animateOnStart() {
        scaleX = 1f
        scaleY = 1f
        setColorFilter(ContextCompat.getColor(context, R.color.colorTextPrimary))
        animate().scaleX(1.5f).scaleY(1.5f).setDuration(500)
            .withStartAction { setColorFilter(ContextCompat.getColor(context, R.color.colorAccent)) }
            .withEndAction {
                animate().scaleX(1f).scaleY(1f).setDuration(500)
                    .withEndAction { setColorFilter(ContextCompat.getColor(context, R.color.colorTextPrimary)) }
                    .start()
            }.start()
    }
}
