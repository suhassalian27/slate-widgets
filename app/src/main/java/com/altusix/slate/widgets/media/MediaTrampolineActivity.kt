package com.altusix.slate.widgets.media

import android.app.Activity
import android.os.Bundle

class MediaTrampolineActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        SlateMediaNotificationService.openActivePlayer(this)
        finish()
        overridePendingTransition(0, 0)
    }
}