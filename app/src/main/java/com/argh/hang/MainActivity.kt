package com.argh.hang

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Settings screen for Hang.
 *
 * Sections:
 *  1. Overlay       — image, position, size, opacity, portrait/landscape
 *  2. Passage       — set/change passage, auth duration
 *  3. Service       — accessibility, battery optimisation, notifications
 *  4. System Health — live status indicators
 *  5. Diagnostics   — recent log entries, export
 */
class MainActivity : AppCompatActivity() {

    private lateinit var repo: ConfigRepository

    private val pickImageLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) onImagePicked(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = ConfigRepository(this)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        // XOS mitigation: re-prompt if overlay permission was silently revoked
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, getString(R.string.overlay_permission_lost_toast), Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
    }

    // -------------------------------------------------------------------------
    // UI construction
    // -------------------------------------------------------------------------

    private fun buildUi() {
        val config = repo.snapshot()
        val d = { v: Int -> HangDesign.dp(this, v) }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(d(20), d(24), d(20), d(40))
        }

        column.addView(HangDesign.largeTitle(this, getString(R.string.app_name)))
        column.addView(HangDesign.footnote(this, getString(R.string.setup_subtitle)))

        // 1 ─ Overlay ─────────────────────────────────────────────────────────
        column.addView(HangDesign.sectionHeader(this, getString(R.string.section_overlay)))
        val overlayCard = buildOverlayCard(config)
        column.addView(overlayCard)

        // 2 ─ Passage ──────────────────────────────────────────────────────────
        column.addView(HangDesign.sectionHeader(this, getString(R.string.section_passage)))
        val passageCard = buildPassageCard(config)
        column.addView(passageCard)

        // 3 ─ Service ──────────────────────────────────────────────────────────
        column.addView(HangDesign.sectionHeader(this, getString(R.string.section_service)))
        val serviceCard = buildServiceCard()
        column.addView(serviceCard)

        // 4 ─ System Health ────────────────────────────────────────────────────
        column.addView(HangDesign.sectionHeader(this, getString(R.string.section_health)))
        val healthCard = buildHealthCard()
        column.addView(healthCard)

        // 5 ─ Diagnostics ──────────────────────────────────────────────────────
        column.addView(HangDesign.sectionHeader(this, getString(R.string.section_diagnostics)))
        val diagCard = buildDiagnosticsCard()
        column.addView(diagCard)

        setContentView(ScrollView(this).apply {
            setBackgroundColor(HangDesign.COLOR_BACKGROUND)
            isFillViewport = true
            addView(column)
        })

        listOf(overlayCard, passageCard, serviceCard, healthCard, diagCard)
            .forEachIndexed { i, v -> HangDesign.fadeInUp(v, i) }
    }

    // ─── Section 1: Overlay ──────────────────────────────────────────────────

    private fun buildOverlayCard(config: ProtectionConfig): LinearLayout {
        val d = { v: Int -> HangDesign.dp(this, v) }
        val card = HangDesign.card(this)

        // Status badge
        val statusBadge = HangDesign.footnote(
            this,
            if (config.overlayEnabled) getString(R.string.overlay_active)
            else getString(R.string.overlay_inactive),
        ).apply {
            setTextColor(
                if (config.overlayEnabled) 0xFF34C759.toInt() else HangDesign.COLOR_SECONDARY
            )
        }
        card.addView(statusBadge)

        // Import image button
        card.addView(
            HangDesign.pillButton(this, getString(R.string.import_image)).apply {
                setOnClickListener { pickImageLauncher.launch("image/png") }
            },
        )

        // Preview path
        if (config.overlayImagePath != null) {
            card.addView(
                HangDesign.footnote(this, getString(R.string.image_loaded)).apply {
                    setPadding(0, d(4), 0, 0)
                },
            )
        }

        // Grid position (row × col labels)
        card.addView(HangDesign.footnote(this, getString(R.string.position_label)).apply {
            setPadding(0, d(12), 0, d(4))
        })
        val rowLabels = arrayOf(
            getString(R.string.grid_top), getString(R.string.grid_mid), getString(R.string.grid_bot)
        )
        val colLabels = arrayOf(
            getString(R.string.grid_left), getString(R.string.grid_center), getString(R.string.grid_right)
        )
        var selectedRow = config.overlayGridRow
        var selectedCol = config.overlayGridCol

        val gridContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (r in 0..2) {
            val rowLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (c in 0..2) {
                val label = "${rowLabels[r]}-${colLabels[c]}"
                val btn = HangDesign.pillButton(this, label).apply {
                    layoutParams = LinearLayout.LayoutParams(0, d(40), 1f).apply {
                        setMargins(d(2), d(2), d(2), d(2))
                    }
                    alpha = if (r == selectedRow && c == selectedCol) 1f else 0.35f
                    setOnClickListener {
                        selectedRow = r; selectedCol = c
                        lifecycleScope.launch { repo.setOverlayGridPosition(r, c) }
                        notifySettingsSaved()
                        rebuildUiWithAuth { buildUi() }
                    }
                }
                rowLayout.addView(btn)
            }
            gridContainer.addView(rowLayout)
        }
        card.addView(gridContainer)

        // Size slider
        card.addView(HangDesign.footnote(this, getString(R.string.size_label, config.overlayWidthDp)).apply {
            setPadding(0, d(12), 0, d(4))
            tag = "size_label"
        })
        card.addView(SeekBar(this).apply {
            max = 296  // 320 - 24
            progress = (config.overlayWidthDp - 24).coerceIn(0, 296)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    val dp = progress + 24
                    (card.findViewWithTag<TextView>("size_label"))
                        ?.text = getString(R.string.size_label, dp)
                    if (fromUser) lifecycleScope.launch { repo.setOverlayWidthDp(dp) }
                }
                override fun onStartTrackingTouch(sb: SeekBar) = Unit
                override fun onStopTrackingTouch(sb: SeekBar) = notifySettingsSaved()
            })
        })

        // Opacity slider
        card.addView(HangDesign.footnote(this, getString(R.string.opacity_label, (config.overlayOpacity * 100).toInt())).apply {
            setPadding(0, d(8), 0, d(4))
            tag = "opacity_label"
        })
        card.addView(SeekBar(this).apply {
            max = 90  // 10%…100% in 1% steps
            progress = ((config.overlayOpacity * 100).toInt() - 10).coerceIn(0, 90)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    val pct = progress + 10
                    (card.findViewWithTag<TextView>("opacity_label"))
                        ?.text = getString(R.string.opacity_label, pct)
                    if (fromUser) lifecycleScope.launch { repo.setOverlayOpacity(pct / 100f) }
                }
                override fun onStartTrackingTouch(sb: SeekBar) = Unit
                override fun onStopTrackingTouch(sb: SeekBar) = notifySettingsSaved()
            })
        })

        // Portrait / Landscape visibility
        card.addView(
            HangDesign.checkRow(this, getString(R.string.visible_portrait)).apply {
                isChecked = config.overlayVisiblePortrait
                setOnCheckedChangeListener { _, checked ->
                    lifecycleScope.launch { repo.setOverlayVisibility(checked, config.overlayVisibleLandscape) }
                }
            },
        )
        card.addView(
            HangDesign.checkRow(this, getString(R.string.visible_landscape)).apply {
                isChecked = config.overlayVisibleLandscape
                setOnCheckedChangeListener { _, checked ->
                    lifecycleScope.launch { repo.setOverlayVisibility(config.overlayVisiblePortrait, checked) }
                }
            },
        )

        // Enable/disable toggle (destructive → needs auth)
        val enableLabel = if (config.overlayEnabled)
            getString(R.string.disable_overlay) else getString(R.string.enable_overlay)
        card.addView(
            HangDesign.pillButton(this, enableLabel).apply {
                if (config.overlayEnabled) {
                    backgroundTintList = android.content.res.ColorStateList.valueOf(HangDesign.COLOR_DESTRUCTIVE)
                }
                setOnClickListener { button ->
                    if (config.overlayEnabled) {
                        requireAuth {
                            lifecycleScope.launch {
                                repo.setOverlayEnabled(false)
                                stopService(Intent(this@MainActivity, OverlayService::class.java))
                                buildUi()
                            }
                        }
                    } else {
                        lifecycleScope.launch {
                            repo.setOverlayEnabled(true)
                            startOverlayService()
                            buildUi()
                        }
                    }
                }
            },
        )

        return card
    }

    // ─── Section 2: Passage ──────────────────────────────────────────────────

    private fun buildPassageCard(config: ProtectionConfig): LinearLayout {
        val d = { v: Int -> HangDesign.dp(this, v) }
        val card = HangDesign.card(this)

        val passageInput = EditText(this).apply { setText(config.passage); minLines = 2 }
        HangDesign.styleField(passageInput)
        card.addView(passageInput)

        val caseBox = HangDesign.checkRow(this, getString(R.string.passage_strict)).apply {
            isChecked = config.caseSensitive
        }
        card.addView(caseBox)

        // Auth duration slider (1–5 min)
        val durationMin = (config.authDurationMs / 60_000L).toInt().coerceIn(1, 5)
        val durationLabel = HangDesign.footnote(
            this, getString(R.string.auth_duration_label, durationMin)
        ).apply { tag = "duration_label" }
        card.addView(durationLabel)
        card.addView(SeekBar(this).apply {
            max = 4
            progress = durationMin - 1
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    val min = progress + 1
                    (card.findViewWithTag<TextView>("duration_label"))
                        ?.text = getString(R.string.auth_duration_label, min)
                    if (fromUser) lifecycleScope.launch {
                        repo.setAuthDurationMs(min * 60_000L)
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar) = Unit
                override fun onStopTrackingTouch(sb: SeekBar) = notifySettingsSaved()
            })
        })

        card.addView(
            HangDesign.pillButton(this, getString(R.string.save_passage)).apply {
                setOnClickListener { button ->
                    val newPassage = passageInput.text.toString().trim()
                    if (newPassage.length < 20) {
                        HangDesign.haptic(button, success = false)
                        Toast.makeText(this@MainActivity, getString(R.string.passage_too_short), Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                    val save = {
                        lifecycleScope.launch {
                            repo.setPassage(newPassage)
                            repo.setCaseSensitive(caseBox.isChecked)
                            HangDesign.haptic(button, success = true)
                            Toast.makeText(this@MainActivity, getString(R.string.saved), Toast.LENGTH_SHORT).show()
                        }
                    }
                    if (newPassage != config.passage) {
                        requireCurrentPassage(config.passage, config.caseSensitive) { save() }
                    } else {
                        save()
                    }
                }
            },
        )
        return card
    }

    // ─── Section 3: Service ───────────────────────────────────────────────────

    private fun buildServiceCard(): LinearLayout {
        val card = HangDesign.card(this)
        card.addView(HangDesign.footnote(this, getString(R.string.service_hint)))

        card.addView(
            HangDesign.pillButton(this, getString(R.string.open_accessibility)).apply {
                setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            },
        )
        card.addView(
            HangDesign.pillButton(this, getString(R.string.open_battery_opt)).apply {
                setOnClickListener {
                    startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:$packageName"),
                        )
                    )
                }
            },
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            card.addView(
                HangDesign.pillButton(this, getString(R.string.request_notifications)).apply {
                    setOnClickListener {
                        requestPermissions(
                            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100
                        )
                    }
                },
            )
        }
        return card
    }

    // ─── Section 4: System Health ─────────────────────────────────────────────

    private fun buildHealthCard(): LinearLayout {
        val card = HangDesign.card(this)
        card.addView(healthRow(getString(R.string.health_overlay), overlayEngine = true))
        card.addView(healthRow(getString(R.string.health_service), OverlayService.isRunning.get()))
        card.addView(healthRow(getString(R.string.health_accessibility), isAccessibilityEnabled()))
        card.addView(healthRow(getString(R.string.health_overlay_perm), Settings.canDrawOverlays(this)))
        card.addView(healthRow(getString(R.string.health_battery), isBatteryUnrestricted()))
        return card
    }

    private fun healthRow(label: String, status: Boolean = false, overlayEngine: Boolean = false): LinearLayout {
        val ok = if (overlayEngine) {
            val config = repo.snapshot()
            !config.overlayEnabled || Settings.canDrawOverlays(this)
        } else status

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val d = { v: Int -> HangDesign.dp(this@MainActivity, v) }
            setPadding(0, d(6), 0, d(6))
            addView(HangDesign.body(this@MainActivity, label).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(HangDesign.footnote(this@MainActivity, if (ok) "✓" else "✗").apply {
                setTextColor(if (ok) 0xFF34C759.toInt() else HangDesign.COLOR_DESTRUCTIVE)
            })
        }
    }

    // ─── Section 5: Diagnostics ───────────────────────────────────────────────

    private fun buildDiagnosticsCard(): LinearLayout {
        val d = { v: Int -> HangDesign.dp(this, v) }
        val card = HangDesign.card(this)

        val recentEntries = DiagnosticLog.getRecentEntries().takeLast(10)
        if (recentEntries.isEmpty()) {
            card.addView(HangDesign.footnote(this, getString(R.string.diag_empty)))
        } else {
            recentEntries.forEach { entry ->
                card.addView(HangDesign.footnote(this, entry).apply {
                    setPadding(0, d(2), 0, d(2))
                    textSize = 11f
                })
            }
        }

        card.addView(
            HangDesign.pillButton(this, getString(R.string.export_logs)).apply {
                setOnClickListener {
                    requireAuth {
                        val logs = DiagnosticLog.getRecentEntries().joinToString("\n")
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, logs)
                            putExtra(Intent.EXTRA_SUBJECT, "Hang Diagnostics")
                        }
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.export_logs)))
                    }
                }
            },
        )
        return card
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val services = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return services.contains(packageName, ignoreCase = true)
    }

    private fun isBatteryUnrestricted(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun notifySettingsSaved() {
        // Notify OverlayService to re-read config on next watchdog tick (no-op; watchdog polls config)
    }

    private fun rebuildUiWithAuth(block: () -> Unit) = block()

    /** Require active session before executing [block]. Launches verification if needed. */
    private fun requireAuth(block: () -> Unit) {
        if (AuthorizationSession.isActive()) {
            block()
        } else {
            startActivity(
                Intent(this, VerificationActivity::class.java).apply {
                    putExtra(VerificationActivity.EXTRA_PACKAGE, packageName)
                    putExtra(VerificationActivity.EXTRA_ACTION, ProtectedAction.DISABLE.name)
                }
            )
        }
    }

    /** Secured passage-update: user must type current passage before saving a new one. */
    private fun requireCurrentPassage(
        currentPassage: String,
        caseSensitive: Boolean,
        onVerified: () -> Unit,
    ) {
        val d = { v: Int -> HangDesign.dp(this, v) }
        val input = SecureEditText(this).apply { hint = getString(R.string.confirm_passage_hint); minLines = 2 }
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
                val matches = if (caseSensitive) typed == currentPassage
                              else typed.equals(currentPassage, ignoreCase = true)
                if (matches) onVerified()
                else Toast.makeText(this, getString(R.string.passage_mismatch_current), Toast.LENGTH_LONG).show()
            }
            .setNegativeButton(getString(R.string.cancel_button), null)
            .show()
    }

    private fun onImagePicked(uri: Uri) {
        val engine = OverlayEngine()
        val path = engine.importImage(this, uri)
        if (path != null) {
            lifecycleScope.launch {
                repo.setOverlayImagePath(path)
                Toast.makeText(this@MainActivity, getString(R.string.image_imported), Toast.LENGTH_SHORT).show()
                buildUi()
            }
        } else {
            Toast.makeText(this, getString(R.string.image_import_failed), Toast.LENGTH_LONG).show()
        }
    }
}
