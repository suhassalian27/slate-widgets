package com.altusix.slate.widgets.quicktoggles

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class QuickTogglesTrampolineActivity : Activity() {

    companion object {
        const val EXTRA_TARGET_INTENT = "extra_target_intent"
    }

    private var hasLaunched = false
    private val handler = Handler(Looper.getMainLooper())

    private val dynamicSystemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.net.wifi.WIFI_AP_STATE_CHANGED") {
                val state = intent.getIntExtra("wifi_state", 0)
                getSharedPreferences("slate_toggles_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("hotspot_broadcast_state", state == 13).apply()
            }
            updateAllQuickTogglesWidgets(this@QuickTogglesTrampolineActivity)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_TARGET_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_TARGET_INTENT)
        }

        val filter = IntentFilter().apply {
            addAction("android.net.wifi.WIFI_STATE_CHANGED")
            addAction("android.net.wifi.STATE_CHANGE")
            addAction("android.net.wifi.WIFI_AP_STATE_CHANGED")
            addAction("android.bluetooth.adapter.action.STATE_CHANGED")
            addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED")
            addAction("android.location.PROVIDERS_CHANGED")
            addAction("android.location.MODE_CHANGED")
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dynamicSystemReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(dynamicSystemReceiver, filter)
        }

        if (targetIntent != null) {
            try {
                startActivity(targetIntent)
                hasLaunched = true
            } catch (_: Exception) {
                finish()
            }
        } else {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasLaunched) {
            // Stage 1: Immediate check upon closing settings
            updateAllQuickTogglesWidgets(this)

            // Stage 2: 400ms for fast switches
            handler.postDelayed({ updateAllQuickTogglesWidgets(applicationContext) }, 400)

            // Stage 3: 1200ms for Wi-Fi / Bluetooth chip power cycling
            handler.postDelayed({ updateAllQuickTogglesWidgets(applicationContext) }, 1200)

            // Stage 4: 2200ms for slow Hotspot SoftAP interface bindings
            handler.postDelayed({
                updateAllQuickTogglesWidgets(applicationContext)
                finish()
            }, 2200)
        }
    }

    override fun onStop() {
        super.onStop()
        updateAllQuickTogglesWidgets(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        try {
            unregisterReceiver(dynamicSystemReceiver)
        } catch (_: Exception) {}
        updateAllQuickTogglesWidgets(applicationContext)
    }
}
