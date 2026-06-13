package com.argh.hang

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Lightweight Apple-inspired design system for Hang's programmatic UI:
 * refined typography scale, rounded grouped cards with subtle depth, pill
 * accent buttons, generous spacing and smooth fade-in-up motion. All
 * components are plain Android views so accessibility (TalkBack, font
 * scaling, 44dp+ touch targets) works out of the box.
 */
object HangDesign {

    // iOS-like system palette
    const val COLOR_BACKGROUND = 0xFFF2F2F7.toInt()
    const val COLOR_CARD = 0xFFFFFFFF.toInt()
    const val COLOR_INK = 0xFF1C1C1E.toInt()
    const val COLOR_SECONDARY = 0xFF6E6E73.toInt()
    const val COLOR_ACCENT = 0xFF0A84FF.toInt()
    const val COLOR_FIELD = 0xFFEFEFF4.toInt()
    const val COLOR_DESTRUCTIVE = 0xFFFF3B30.toInt()

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    fun largeTitle(context: Context, text: CharSequence): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 28f
            setTextColor(COLOR_INK)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = -0.02f
        }

    fun sectionHeader(context: Context, text: CharSequence): TextView =
        TextView(context).apply {
            this.text = text.toString().uppercase()
            textSize = 13f
            setTextColor(COLOR_SECONDARY)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.04f
            setPadding(dp(context, 4), dp(context, 24), 0, dp(context, 8))
        }

    fun body(context: Context, text: CharSequence): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 16f
            setTextColor(COLOR_INK)
            setLineSpacing(0f, 1.15f)
        }

    fun footnote(context: Context, text: CharSequence): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 14f
            setTextColor(COLOR_SECONDARY)
            setLineSpacing(0f, 1.2f)
        }

    /** Rounded grouped card with subtle depth, in the iOS settings style. */
    fun card(context: Context): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(context, 20).toFloat()
                setColor(COLOR_CARD)
            }
            elevation = dp(context, 2).toFloat()
            val pad = dp(context, 20)
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = dp(context, 4) }
        }

    /** Filled accent pill button with ripple feedback. */
    fun pillButton(context: Context, text: CharSequence): Button =
        Button(context).apply {
            this.text = text
            isAllCaps = false
            textSize = 17f
            setTextColor(COLOR_CARD)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            stateListAnimator = null
            minHeight = dp(context, 50)
            val shape = GradientDrawable().apply {
                cornerRadius = dp(context, 25).toFloat()
                setColor(COLOR_ACCENT)
            }
            background = RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), shape, null)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(context, 16) }
        }

    /** Comfortable 44dp+ check row with accent tint. */
    fun checkRow(context: Context, label: CharSequence): CheckBox =
        CheckBox(context).apply {
            text = label
            textSize = 16f
            setTextColor(COLOR_INK)
            buttonTintList = ColorStateList.valueOf(COLOR_ACCENT)
            minHeight = dp(context, 44)
        }

    /** Rounded quiet input field. */
    fun styleField(field: EditText) {
        val context = field.context
        field.background = GradientDrawable().apply {
            cornerRadius = dp(context, 12).toFloat()
            setColor(COLOR_FIELD)
        }
        field.setTextColor(COLOR_INK)
        field.setHintTextColor(COLOR_SECONDARY)
        field.textSize = 16f
        val pad = dp(context, 14)
        field.setPadding(pad, pad, pad, pad)
    }

    /** Smooth staggered entry motion. */
    fun fadeInUp(view: View, index: Int) {
        view.alpha = 0f
        view.translationY = dp(view.context, 16).toFloat()
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(60L * index)
            .setDuration(350)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    /** Success / failure haptic feedback where the platform supports it. */
    fun haptic(view: View, success: Boolean) {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (success) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(constant)
    }
}
