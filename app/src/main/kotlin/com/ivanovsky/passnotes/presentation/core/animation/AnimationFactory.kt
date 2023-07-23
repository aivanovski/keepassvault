package com.ivanovsky.passnotes.presentation.core.animation

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.LinearInterpolator
import java.util.concurrent.TimeUnit

object AnimationFactory {

    fun createRotationAnimation(view: View): Animator {
        return ObjectAnimator.ofFloat(
            view,
            "rotation",
            0f,
            360f
        ).apply {
            duration = TimeUnit.SECONDS.toMillis(1)
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
    }
}