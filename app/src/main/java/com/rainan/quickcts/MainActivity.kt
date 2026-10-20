package com.rainan.quickcts

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var lastTriggerTime: Long = 0L

    companion object {
        private const val DEBOUNCE_THRESHOLD_MS = 600L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleTrigger()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        scheduleTrigger()
    }

    private fun scheduleTrigger() {
        // Hủy callback đang chờ trước đó để tránh dồn nhiều tác vụ khi nhấn liên tục
        handler.removeCallbacksAndMessages(null)

        val now = SystemClock.elapsedRealtime()
        if (now - lastTriggerTime < DEBOUNCE_THRESHOLD_MS) {
            finishWithoutAnimation()
            return
        }

        val delayMs = PreferencesManager.getDelayMs(this)
        val hapticEnabled = PreferencesManager.isHapticEnabled(this)

        handler.postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed

            val currentTime = SystemClock.elapsedRealtime()
            if (currentTime - lastTriggerTime < DEBOUNCE_THRESHOLD_MS) {
                finishWithoutAnimation()
                return@postDelayed
            }

            lastTriggerTime = currentTime

            val success = CtsTriggerHelper.triggerCircleToSearch(this, vibrate = hapticEnabled)
            if (!success) {
                Toast.makeText(this, getString(R.string.trigger_failed), Toast.LENGTH_SHORT).show()
            }

            finishWithoutAnimation()
        }, delayMs)
    }

    private fun finishWithoutAnimation() {
        if (!isFinishing) {
            finish()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}