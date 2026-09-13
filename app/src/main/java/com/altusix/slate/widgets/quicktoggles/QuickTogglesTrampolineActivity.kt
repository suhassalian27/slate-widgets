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

    private val dynamicSystemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.net.wifi.WIFI_AP_STATE_CHANGED") {
                val state = intent.getIntExtra("wifi_state", 0)
                getSharedPreferences("slate_toggles_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("hotspot_broadcast_state", state == 13).apply()
            }
            updateAllQuickTogglesWidgets(applicationContext)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerSystemReceivers()
        handleLaunch(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunch(intent)
    }

    private fun handleLaunch(intent: Intent?) {
        val targetIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_TARGET_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_TARGET_INTENT)
        }

        if (targetIntent != null) {
            try {
                hasLaunched = true
                startActivity(targetIntent)
            } catch (_: Exception) {
                finish()
            }
        } else {
            finish()
        }
    }

    private fun registerSystemReceivers() {
        val filter = IntentFilter().apply {
            addAction("android.net.wifi.WIFI_STATE_CHANGED")
            addAction("android.net.wifi.STATE_CHANGE")
            addAction("android.net.wifi.WIFI_AP_STATE_CHANGED")
            addAction("android.net.conn.CONNECTIVITY_CHANGE")
            addAction("android.bluetooth.adapter.action.STATE_CHANGED")
            addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED")
            addAction("android.location.PROVIDERS_CHANGED")
            addAction("android.location.MODE_CHANGED")
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction("android.os.action.POWER_SAVE_MODE_CHANGED")
            addAction("android.app.action.INTERRUPTION_FILTER_CHANGED")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dynamicSystemReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(dynamicSystemReceiver, filter)
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasLaunched) {
            val appContext = applicationContext
            // Stage 1: Immediate refresh
            updateAllQuickTogglesWidgets(appContext)

            // Multi-stage background refreshes for hardware state stabilization
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({ updateAllQuickTogglesWidgets(appContext) }, 400)
            handler.postDelayed({ updateAllQuickTogglesWidgets(appContext) }, 1200)
            handler.postDelayed({ updateAllQuickTogglesWidgets(appContext) }, 2200)

            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(dynamicSystemReceiver)
        } catch (_: Exception) {}
        updateAllQuickTogglesWidgets(applicationContext)
    }
}