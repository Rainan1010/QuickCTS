package com.rainan.quickcts

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Delay 300ms để hệ thống kịp thu gọn Quick Settings / hoàn tất hiệu ứng trước khi chụp màn hình CTS
        handler.postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed

            val success = CtsTriggerHelper.triggerCircleToSearch(this)
            if (!success) {
                Toast.makeText(this, getString(R.string.trigger_failed), Toast.LENGTH_SHORT).show()
            }

            finish()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }
        }, 300L)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}