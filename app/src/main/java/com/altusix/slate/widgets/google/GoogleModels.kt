package com.altusix.slate.widgets.google

data class GoogleWidgetConfig(
    val searchHint: String = "Search",
    val showVoiceSearch: Boolean = true,
    val showLens: Boolean = true,
    val useGoogleColors: Boolean = false
)