package com.argh.hang

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity

/**
 * Full-screen wizard shown when silent recovery is not possible.
 * Presents a deep-link button to the relevant system settings screen.
 * Requires an active authorization session before showing the deep link.
 */
class RecoveryWizardActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ISSUE = "extra_issue"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        val issue = intent.getStringExtra(EXTRA_ISSUE)
            ?.let { runCatching { RecoveryIssue.valueOf(it) }.getOrNull() }
            ?: RecoveryIssue.OVERLAY_DETACHED

        val d = { v: Int -> HangDesign.dp(this, v) }
        val card = HangDesign.card(this)

        card.addView(HangDesign.largeTitle(this, getString(R.string.recovery_title)))
        card.addView(
            HangDesign.footnote(this, getString(issueDescriptionRes(issue))).apply {
                setPadding(0, d(8), 0, d(16))
            },
        )

        val deepLinkIntent = RecoveryEngine.deepLinkFor(this, issue)
        if (deepLinkIntent != null) {
            card.addView(
                HangDesign.pillButton(this, getString(issueActionRes(issue))).apply {
                    setOnClickListener {
                        if (AuthorizationSession.isActive()) {
                            startActivity(deepLinkIntent)
                            finish()
                        } else {
                            startActivity(
                                Intent(this@RecoveryWizardActivity, VerificationActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    putExtra(VerificationActivity.EXTRA_PACKAGE, packageName)
                                    putExtra(VerificationActivity.EXTRA_ACTION, "RECOVERY")
                                },
                            )
                        }
                    }
                },
            )
        }

        card.addView(
            HangDesign.pillButton(this, getString(R.string.dismiss_button)).apply {
                setBackgroundTintList(null)
                setTextColor(HangDesign.COLOR_SECONDARY)
                setOnClickListener { finish() }
            },
        )

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

    private fun issueDescriptionRes(issue: RecoveryIssue) = when (issue) {
        RecoveryIssue.ACCESSIBILITY_REMOVED -> R.string.recovery_accessibility_desc
        RecoveryIssue.OVERLAY_PERMISSION_REMOVED -> R.string.recovery_overlay_desc
        RecoveryIssue.BATTERY_RESTRICTED -> R.string.recovery_battery_desc
        RecoveryIssue.NOTIFICATION_DISABLED -> R.string.recovery_notification_desc
        RecoveryIssue.OVERLAY_DETACHED -> R.string.recovery_overlay_desc
    }

    private fun issueActionRes(issue: RecoveryIssue) = when (issue) {
        RecoveryIssue.ACCESSIBILITY_REMOVED -> R.string.recovery_open_accessibility
        RecoveryIssue.OVERLAY_PERMISSION_REMOVED -> R.string.recovery_open_overlay
        RecoveryIssue.BATTERY_RESTRICTED -> R.string.recovery_open_battery
        RecoveryIssue.NOTIFICATION_DISABLED -> R.string.recovery_open_notifications
        RecoveryIssue.OVERLAY_DETACHED -> R.string.recovery_open_overlay
    }
}
