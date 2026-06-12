package com.argh.hang

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Setup screen: choose protected apps and actions, define the verification
 * passage and matching rules. Changing an existing passage requires typing
 * the current passage first (secured update workflow).
 *
 * Hang itself can be added to the protected apps so that disabling,
 * uninstalling, force stopping, hiding, or revoking access from Hang also
 * requires verification.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var repo: ConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = ConfigRepository(this)
        val config = repo.snapshot()
        val d = { v: Int -> HangDesign.dp(this, v) }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(d(20), d(24), d(20), d(32))
        }

        column.addView(HangDesign.largeTitle(this, getString(R.string.app_name)))
        column.addView(HangDesign.footnote(this, getString(R.string.setup_subtitle)))

        // -- Step 1: detection service ---------------------------------------
        column.addView(HangDesign.sectionHeader(this, getString(R.string.section_service)))
        val serviceCard = HangDesign.card(this).apply {
            addView(HangDesign.footnote(this@MainActivity, getString(R.string.service_hint)))
            addView(
                HangDesign.pillButton(this@MainActivity, getString(R.string.open_accessibility)).apply {
                    setOnClickListener {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                },
            )
        }
        column.addView(serviceCard)

        // -- Step 2: protected actions -----------------------------------------
        column.addView(HangDesign.sectionHeader(this, getString(R.string.section_actions)))
        val actionsCard = HangDesign.card(this)
        val actionBoxes = ProtectedAction.entries.map { action ->
            HangDesign.checkRow(this, action.label).apply {
                isChecked = action in config.protectedActions
                tag = action
            }.also { actionsCard.addView(it) }
        }
        column.addView(actionsCard)

        // -- Step 3: protected apps ----------------------------------------------
        column.addView(HangDesign.sectionHeader(this, getString(R.string.section_apps)))
        val appsCard = HangDesign.card(this)
        // Hang itself first, so weakening Hang can also be protected.
        val selfBox = HangDesign.checkRow(this, getString(R.string.protect_self)).apply {
            isChecked = packageName in config.protectedPackages
            tag = packageName
        }
        appsCard.addView(selfBox)
        val launchableApps = packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName != packageName }
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { packageManager.getApplicationLabel(it).toString().lowercase() }
        val appBoxes = listOf(selfBox) + launchableApps.map { appInfo ->
            HangDesign.checkRow(this, packageManager.getApplicationLabel(appInfo)).apply {
                isChecked = appInfo.packageName in config.protectedPackages
                tag = appInfo.packageName
            }.also { appsCard.addView(it) }
        }
        column.addView(appsCard)

        // -- Step 4: passage & matching --------------------------------------------
        column.addView(HangDesign.sectionHeader(this, getString(R.string.section_passage)))
        val passageCard = HangDesign.card(this)
        val passageInput = EditText(this).apply {
            setText(config.passage)
            minLines = 2
        }
        HangDesign.styleField(passageInput)
        passageCard.addView(passageInput)
        val caseBox = HangDesign.checkRow(this, getString(R.string.passage_strict)).apply {
            isChecked = config.caseSensitive
        }
        passageCard.addView(caseBox)
        column.addView(passageCard)

        // -- Save ----------------------------------------------------------------------
        val saveButton = HangDesign.pillButton(this, getString(R.string.save_config)).apply {
            setOnClickListener { button ->
                val newPassage = passageInput.text.toString().trim()
                if (newPassage.length < 20) {
                    HangDesign.haptic(button, success = false)
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.passage_too_short),
                        Toast.LENGTH_LONG,
                    ).show()
                    return@setOnClickListener
                }
                val save = {
                    lifecycleScope.launch {
                        repo.setProtectedActions(
                            actionBoxes.filter { it.isChecked }
                                .map { it.tag as ProtectedAction }.toSet(),
                        )
                        repo.setProtectedPackages(
                            appBoxes.filter { it.isChecked }
                                .map { it.tag as String }.toSet(),
                        )
                        repo.setPassage(newPassage)
                        repo.setCaseSensitive(caseBox.isChecked)
                        HangDesign.haptic(button, success = true)
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.saved),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
                if (newPassage != config.passage) {
                    requireCurrentPassage(config.passage, config.caseSensitive) { save() }
                } else {
                    save()
                }
            }
        }
        column.addView(saveButton)

        setContentView(ScrollView(this).apply {
            setBackgroundColor(HangDesign.COLOR_BACKGROUND)
            isFillViewport = true
            addView(column)
        })

        listOf(serviceCard, actionsCard, appsCard, passageCard, saveButton)
            .forEachIndexed { i, v -> HangDesign.fadeInUp(v, i) }
    }

    /**
     * Secured passage-update workflow: the user must manually type the current
     * passage (in a hardened field) before a new one can be saved.
     */
    private fun requireCurrentPassage(
        currentPassage: String,
        caseSensitive: Boolean,
        onVerified: () -> Unit,
    ) {
        val d = { v: Int -> HangDesign.dp(this, v) }
        val input = SecureEditText(this).apply {
            hint = getString(R.string.confirm_passage_hint)
            minLines = 2
        }
        HangDesign.styleField(input)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(d(16), d(16), d(16), 0)
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_passage_title))
            .setMessage(getString(R.string.confirm_passage_message))
            .setView(container)
            .setPositiveButton(getString(R.string.confirm_button)) { _, _ ->
                val typed = input.text?.toString() ?: ""
                val matches = if (caseSensitive) {
                    typed == currentPassage
                } else {
                    typed.equals(currentPassage, ignoreCase = true)
                }
                if (matches) {
                    onVerified()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.passage_mismatch_current),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel_button), null)
            .show()
    }
}
