package com.rainan.quickcts

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class SettingsActivity : Activity() {

    private lateinit var tvDelayValue: TextView
    private lateinit var seekBarDelay: SeekBar
    private lateinit var switchHaptic: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        bindViews()
        setupDelayControls()
        setupHapticControl()
    }

    private fun bindViews() {
        val btnBack = findViewById<ImageView>(R.id.btn_back)
        btnBack.setOnClickListener {
            finish()
        }

        tvDelayValue = findViewById(R.id.tv_delay_value)
        seekBarDelay = findViewById(R.id.seek_bar_delay)
        switchHaptic = findViewById(R.id.switch_haptic)
    }

    private fun setupDelayControls() {
        val currentDelay = PreferencesManager.getDelayMs(this).toInt()
        updateDelayDisplay(currentDelay)
        seekBarDelay.progress = currentDelay

        seekBarDelay.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val rounded = (progress / 25) * 25
                    updateDelayDisplay(rounded)
                    PreferencesManager.setDelayMs(this@SettingsActivity, rounded.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    val rounded = (it.progress / 25) * 25
                    it.progress = rounded
                    PreferencesManager.setDelayMs(this@SettingsActivity, rounded.toLong())
                }
            }
        })

        // Nút preset chọn nhanh
        findViewById<Button>(R.id.btn_preset_0).setOnClickListener { setDelayPreset(0) }
        findViewById<Button>(R.id.btn_preset_150).setOnClickListener { setDelayPreset(150) }
        findViewById<Button>(R.id.btn_preset_300).setOnClickListener { setDelayPreset(300) }
        findViewById<Button>(R.id.btn_preset_500).setOnClickListener { setDelayPreset(500) }
    }

    private fun setDelayPreset(ms: Int) {
        seekBarDelay.progress = ms
        updateDelayDisplay(ms)
        PreferencesManager.setDelayMs(this, ms.toLong())
    }

    private fun updateDelayDisplay(delayMs: Int) {
        tvDelayValue.text = getString(R.string.delay_ms_format, delayMs)
    }

    private fun setupHapticControl() {
        switchHaptic.isChecked = PreferencesManager.isHapticEnabled(this)
        switchHaptic.setOnCheckedChangeListener { _, isChecked ->
            PreferencesManager.setHapticEnabled(this, isChecked)
        }
    }
}
