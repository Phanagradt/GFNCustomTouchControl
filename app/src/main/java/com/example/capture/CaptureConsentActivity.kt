package com.example.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.service.ScreenCaptureService

class CaptureConsentActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PENDING_ACTION = "extra_pending_action"
        const val EXTRA_REPLAY_SEC = "extra_replay_sec"

        @Volatile
        private var promptInFlight = false

        fun requestCapture(context: Context, pendingAction: String, replayDurationSec: Int = 30) {
            if (promptInFlight) return
            val intent = Intent(context, CaptureConsentActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_PENDING_ACTION, pendingAction)
                putExtra(EXTRA_REPLAY_SEC, replayDurationSec)
            }
            context.startActivity(intent)
        }
    }

    private var pendingAction: String = ScreenCaptureService.ACTION_TAKE_SCREENSHOT
    private var replaySec: Int = 30

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        promptInFlight = false
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            ScreenCaptureService.startWithProjection(
                context = applicationContext,
                resultCode = result.resultCode,
                data = data,
                pendingAction = pendingAction,
                replayDurationSec = replaySec
            )
        } else {
            CaptureBridge.postBanner("Screen capture permission cancelled")
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            // Do not re-prompt automatically on Activity recreation
            promptInFlight = false
            finish()
            return
        }
        pendingAction = intent?.getStringExtra(EXTRA_PENDING_ACTION)
            ?: ScreenCaptureService.ACTION_TAKE_SCREENSHOT
        replaySec = intent?.getIntExtra(EXTRA_REPLAY_SEC, 30) ?: 30
        promptInFlight = true

        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val config = if (pendingAction == ScreenCaptureService.ACTION_START_GPU_FILTER) {
                MediaProjectionConfig.createConfigForUserChoice()
            } else {
                MediaProjectionConfig.createConfigForDefaultDisplay()
            }
            mgr.createScreenCaptureIntent(config)
        } else {
            mgr.createScreenCaptureIntent()
        }
        runCatching {
            projectionLauncher.launch(captureIntent)
        }.onFailure {
            promptInFlight = false
            CaptureBridge.postBanner("Unable to request screen capture permission")
            finish()
        }
    }

    override fun onDestroy() {
        promptInFlight = false
        super.onDestroy()
    }
}

