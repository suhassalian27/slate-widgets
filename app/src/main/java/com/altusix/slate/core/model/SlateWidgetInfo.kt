package com.altusix.slate.core.model

data class SlateWidgetInfo(
    val name: String,
    val sizeText: String,
    val category: String,
    val receiverClass: Class<*>,
    val hasModeOption: Boolean = false,
    val defaultOpacity: Float = 1.0f
)