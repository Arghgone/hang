package com.argh.hang

import android.content.Context
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import coil.load
import java.io.File

/**
 * Manages the overlay ImageView window.
 *
 * Responsibilities:
 *  - Import a user image from a URI into private app storage.
 *  - Attach / detach the overlay window via WindowManager.
 *  - Apply config: position (9-point grid + offset), size, opacity, edge snap.
 *  - Retry logic on WindowManager failures (XOS workaround).
 *  - Show / hide based on current orientation.
 */
class OverlayEngine {

    private var imageView: ImageView? = null
    private var retryCount = 0
    private val handler = Handler(Looper.getMainLooper())

    /** Copy image from [uri] to private storage and return the stored path. */
    fun importImage(context: Context, uri: Uri): String? {
        return try {
            val dest = File(context.filesDir, "overlay_image.png")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            DiagnosticLog.overlay("import-ok", dest.absolutePath)
            dest.absolutePath
        } catch (e: Exception) {
            DiagnosticLog.overlay("import-failed", e.message ?: "")
            null
        }
    }

    /** @return true if the view is currently attached to the window. */
    fun isAttached(): Boolean = imageView?.isAttachedToWindow == true

    /**
     * Create and attach the overlay window. Should only be called on the main thread.
     * Returns true on success. On failure, schedules retries up to [MAX_RETRIES] times.
     */
    fun attach(context: Context, windowManager: WindowManager, config: ProtectionConfig) {
        if (isAttached()) return
        val imagePath = config.overlayImagePath ?: return
        val imageFile = File(imagePath)
        if (!imageFile.exists()) {
            DiagnosticLog.overlay("attach-skipped", "image file missing: $imagePath")
            return
        }

        val dm = context.resources.displayMetrics
        val widthPx = (config.overlayWidthDp * dm.density).toInt()

        val params = WindowManager.LayoutParams(
            widthPx,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )

        // XOS workaround: set explicit gravity + absolute x/y instead of relying on defaults.
        params.gravity = Gravity.TOP or Gravity.START
        params.x = computeX(dm.widthPixels, widthPx, config, dm.density)
        params.y = computeY(dm.heightPixels, config, dm.density)

        val view = ImageView(context).apply {
            isClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            alpha = config.overlayOpacity
            scaleType = ImageView.ScaleType.FIT_CENTER
            load(imageFile)
        }

        try {
            windowManager.addView(view, params)
            imageView = view
            retryCount = 0
            DiagnosticLog.overlay("attached", "x=${params.x} y=${params.y} w=$widthPx")
        } catch (e: WindowManager.BadTokenException) {
            DiagnosticLog.overlay("attach-BadTokenException", e.message ?: "")
            scheduleRetry(context, windowManager, config)
        } catch (e: SecurityException) {
            DiagnosticLog.overlay("attach-SecurityException", e.message ?: "")
            scheduleRetry(context, windowManager, config)
        } catch (e: Exception) {
            DiagnosticLog.overlay("attach-error", e.message ?: "")
            scheduleRetry(context, windowManager, config)
        }
    }

    /** Remove the overlay window. */
    fun detach(windowManager: WindowManager) {
        val view = imageView ?: return
        try {
            if (view.isAttachedToWindow) windowManager.removeView(view)
            DiagnosticLog.overlay("detached", "")
        } catch (e: Exception) {
            DiagnosticLog.overlay("detach-error", e.message ?: "")
        } finally {
            imageView = null
        }
    }

    /** Show or hide based on current orientation. */
    fun updateVisibility(isPortrait: Boolean, config: ProtectionConfig) {
        val visible = if (isPortrait) config.overlayVisiblePortrait else config.overlayVisibleLandscape
        imageView?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun scheduleRetry(context: Context, windowManager: WindowManager, config: ProtectionConfig) {
        if (retryCount >= MAX_RETRIES) {
            DiagnosticLog.overlay("attach-max-retries-reached", "invoking recovery")
            RecoveryEngine.requestWizard(context, RecoveryIssue.OVERLAY_DETACHED)
            return
        }
        retryCount++
        DiagnosticLog.overlay("attach-retry", "attempt $retryCount in ${RETRY_DELAY_MS}ms")
        handler.postDelayed({ attach(context, windowManager, config) }, RETRY_DELAY_MS)
    }

    private fun computeX(screenW: Int, stickerW: Int, config: ProtectionConfig, density: Float): Int {
        val baseX = when (config.overlayGridCol) {
            0 -> 0                          // left
            1 -> (screenW - stickerW) / 2   // center
            2 -> screenW - stickerW         // right
            else -> 0
        }
        val snapped = when (config.overlayEdgeSnap) {
            EdgeSnap.LEFT -> 0
            EdgeSnap.RIGHT -> screenW - stickerW
            else -> baseX
        }
        return snapped + (config.overlayOffsetXDp * density).toInt()
    }

    private fun computeY(screenH: Int, config: ProtectionConfig, density: Float): Int {
        val baseY = when (config.overlayGridRow) {
            0 -> 0                  // top
            1 -> screenH / 2        // middle
            2 -> (screenH * 2) / 3  // bottom
            else -> 0
        }
        val snapped = when (config.overlayEdgeSnap) {
            EdgeSnap.TOP -> 0
            EdgeSnap.BOTTOM -> screenH - (120 * density).toInt() // approx sticker height
            else -> baseY
        }
        return snapped + (config.overlayOffsetYDp * density).toInt()
    }

    companion object {
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 3_000L
    }
}
