package com.argh.hang

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Full-screen interruption shown when a protected action is detected.
 * The user must manually type the configured passage exactly before the
 * action is temporarily unlocked. There is no shortcut button.
 */
class VerificationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_ACTION = "extra_action"
    }

    private lateinit var repo: ConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = ConfigRepository(this)
        val config = repo.snapshot()

        val targetPackage = intent.getStringExtra(EXTRA_PACKAGE) ?: ""
        val actionName = intent.getStringExtra(EXTRA_ACTION)
            ?.let { ProtectedAction.fromName(it)?.label } ?: "protected action"

        val pad = (16 * resources.displayMetrics.density).toInt()

        val intro = TextView(this).apply {
            text = getString(R.string.verification_intro)
            textSize = 16f
        }
        val actionInfo = TextView(this).apply {
            text = "Blocked action: $actionName\nApp: $targetPackage"
            setPadding(0, pad / 2, 0, pad / 2)
        }
        val passageView = TextView(this).apply {
            text = "\u201C${config.passage}\u201D"
            setTypeface(typeface, Typeface.BOLD)
            textSize = 16f
            setTextIsSelectable(false) // passage cannot be copied
            setPadding(0, pad / 2, 0, pad)
        }
        val input = SecureEditText(this).apply {
            hint = "Type the passage here"
            minLines = 3
            gravity = Gravity.TOP or Gravity.START
        }
        val status = TextView(this).apply { setPadding(0, pad / 2, 0, 0) }
        val verify = Button(this).apply {
            text = getString(R.string.verify_button)
            setOnClickListener {
                val typed = input.text?.toString() ?: ""
                val matches = if (config.caseSensitive) {
                    typed == config.passage
                } else {
                    typed.equals(config.passage, ignoreCase = true)
                }
                if (matches) {
                    UnlockManager.grantUnlock(targetPackage)
                    Toast.makeText(
                        this@VerificationActivity,
                        getString(R.string.match_message),
                        Toast.LENGTH_LONG,
                    ).show()
                    finish()
                } else {
                    status.text = getString(R.string.mismatch_message)
                    input.setText("")
                }
            }
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            addView(intro)
            addView(actionInfo)
            addView(passageView)
            addView(input)
            addView(verify)
            addView(status)
        }
        setContentView(ScrollView(this).apply { addView(column) })
    }
}
