package com.argh.hang

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Full-screen interruption shown when a protected action is detected.
 * The user must manually type the configured passage exactly before the
 * action is temporarily unlocked. There is no shortcut button.
 *
 * The activity signals its lifecycle to [UnlockManager] so the
 * accessibility service never stacks duplicate prompts, and logs every
 * verification outcome for bypass diagnostics.
 */
class VerificationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_ACTION = "extra_action"
    }

    private lateinit var repo: ConfigRepository
    private var targetPackage: String = ""
    private var actionName: String? = null
    private var verified = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The passage must be typed, never screenshotted or screen-recorded.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
        repo = ConfigRepository(this)
        val config = repo.snapshot()

        targetPackage = intent.getStringExtra(EXTRA_PACKAGE) ?: ""
        actionName = intent.getStringExtra(EXTRA_ACTION)
        val actionLabel = actionName
            ?.let { ProtectedAction.fromName(it)?.label }
            ?: getString(R.string.generic_action)
        val appLabel = try {
            val info = packageManager.getApplicationInfo(targetPackage, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            targetPackage
        }

        UnlockManager.onPromptShown()

        val d = { v: Int -> HangDesign.dp(this, v) }
        val card = HangDesign.card(this)
        card.addView(HangDesign.largeTitle(this, getString(R.string.verification_title)))
        card.addView(
            HangDesign.footnote(this, getString(R.string.verification_intro)).apply {
                setPadding(0, d(8), 0, d(12))
            },
        )
        card.addView(
            HangDesign.body(
                this,
                getString(R.string.blocked_action_format, actionLabel, appLabel),
            ).apply { setPadding(0, 0, 0, d(12)) },
        )
        card.addView(
            HangDesign.body(this, "\u201C${config.passage}\u201D").apply {
                setTypeface(typeface, Typeface.BOLD)
                setTextIsSelectable(false) // passage cannot be copied
                setPadding(0, 0, 0, d(16))
            },
        )
        val input = SecureEditText(this).apply {
            hint = getString(R.string.passage_hint)
            minLines = 3
            gravity = Gravity.TOP or Gravity.START
        }
        HangDesign.styleField(input)
        card.addView(input)
        val status = HangDesign.footnote(this, "").apply {
            setTextColor(HangDesign.COLOR_DESTRUCTIVE)
            setPadding(0, d(8), 0, 0)
        }
        card.addView(
            HangDesign.pillButton(this, getString(R.string.verify_button)).apply {
                setOnClickListener { button ->
                    val typed = input.text?.toString() ?: ""
                    val matches = if (config.caseSensitive) {
                        typed == config.passage
                    } else {
                        typed.equals(config.passage, ignoreCase = true)
                    }
                    if (matches) {
                        verified = true
                        UnlockManager.grantUnlock(targetPackage)
                        DiagnosticLog.verification(targetPackage, actionName, "success")
                        HangDesign.haptic(button, success = true)
                        Toast.makeText(
                            this@VerificationActivity,
                            getString(R.string.match_message),
                            Toast.LENGTH_LONG,
                        ).show()
                        finish()
                    } else {
                        DiagnosticLog.verification(targetPackage, actionName, "mismatch")
                        HangDesign.haptic(button, success = false)
                        status.text = getString(R.string.mismatch_message)
                        input.setText("")
                    }
                }
            },
        )
        card.addView(status)

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(d(20), d(40), d(20), d(40))
            addView(card)
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(HangDesign.COLOR_BACKGROUND)
            isFillViewport = true
            addView(column)
        })
        HangDesign.fadeInUp(card, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!verified) DiagnosticLog.verification(targetPackage, actionName, "dismissed")
        UnlockManager.onPromptDismissed(targetPackage, actionName)
    }
}
