package com.argh.hang

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Setup screen: choose protected apps and actions, define the verification
 * passage and matching rules. Changing an existing passage requires typing
 * the current passage first (secured update workflow).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var repo: ConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = ConfigRepository(this)
        val config = repo.snapshot()
        val pad = (16 * resources.displayMetrics.density).toInt()

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        // -- Accessibility service hint -------------------------------------
        column.addView(TextView(this).apply {
            text = "1. Enable the accessibility service so protected actions can be detected."
        })
        column.addView(Button(this).apply {
            text = "Open Accessibility Settings"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        })

        // -- Protected actions ----------------------------------------------
        column.addView(TextView(this).apply {
            text = "2. Protected actions"
            setPadding(0, pad, 0, 0)
        })
        val actionBoxes = ProtectedAction.entries.map { action ->
            CheckBox(this).apply {
                text = action.label
                isChecked = action in config.protectedActions
                tag = action
            }.also { column.addView(it) }
        }

        // -- Protected apps ---------------------------------------------------
        column.addView(TextView(this).apply {
            text = "3. Protected apps"
            setPadding(0, pad, 0, 0)
        })
        val launchableApps = packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { packageManager.getApplicationLabel(it).toString().lowercase() }
        val appBoxes = launchableApps.map { appInfo ->
            CheckBox(this).apply {
                text = packageManager.getApplicationLabel(appInfo)
                isChecked = appInfo.packageName in config.protectedPackages
                tag = appInfo.packageName
            }.also { column.addView(it) }
        }

        // -- Passage & matching ----------------------------------------------
        column.addView(TextView(this).apply {
            text = "4. Verification passage"
            setPadding(0, pad, 0, 0)
        })
        val passageInput = EditText(this).apply {
            setText(config.passage)
            minLines = 2
        }
        column.addView(passageInput)
        val caseBox = CheckBox(this).apply {
            text = "Strict matching (case-sensitive)"
            isChecked = config.caseSensitive
        }
        column.addView(caseBox)

        // -- Save ---------------------------------------------------------------
        column.addView(Button(this).apply {
            text = "Save configuration"
            setOnClickListener {
                val newPassage = passageInput.text.toString().trim()
                if (newPassage.length < 20) {
                    Toast.makeText(
                        this@MainActivity,
                        "Passage must be at least 20 characters.",
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
                        Toast.makeText(this@MainActivity, "Saved.", Toast.LENGTH_SHORT).show()
                    }
                }
                if (newPassage != config.passage) {
                    requireCurrentPassage(config.passage, config.caseSensitive) { save() }
                } else {
                    save()
                }
            }
        })

        setContentView(ScrollView(this).apply { addView(column) })
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
        val pad = (16 * resources.displayMetrics.density).toInt()
        val input = SecureEditText(this).apply {
            hint = "Type your current passage"
            minLines = 2
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, 0)
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle("Confirm passage change")
            .setMessage("To change the passage, first type your current passage exactly.")
            .setView(container)
            .setPositiveButton("Confirm") { _, _ ->
                val typed = input.text?.toString() ?: ""
                val matches = if (caseSensitive) {
                    typed == currentPassage
                } else {
                    typed.equals(currentPassage, ignoreCase = true)
                }
                if (matches) {
                    onVerified()
                } else {
                    Toast.makeText(this, "Current passage did not match.", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
