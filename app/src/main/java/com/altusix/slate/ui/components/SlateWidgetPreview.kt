package com.altusix.slate.ui.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.data.local.SlateWidgetConfig

@Composable
fun SlateWidgetPreviewImage(
    widgetInfo: SlateWidgetInfo,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val previewBitmap = remember(widgetInfo) {
        generatePreviewBitmap(context, widgetInfo)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (previewBitmap != null) {
            Image(
                bitmap = previewBitmap.asImageBitmap(),
                contentDescription = widgetInfo.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = widgetInfo.name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

private fun generatePreviewBitmap(context: Context, widgetInfo: SlateWidgetInfo): Bitmap? {
    return try {
        val clazz = widgetInfo.receiverClass
        val constructor = clazz.declaredConstructors.firstOrNull { it.parameterTypes.isEmpty() } ?: return null
        constructor.isAccessible = true
        val receiverInstance = constructor.newInstance()

        val defaultConfig = SlateWidgetConfig(
            themeMode = "DARK",
            backgroundColorHex = 0xFF161618L,
            opacity = 1.0f,
            accentColorHex = 0xFFFFFFFFL
        )

        val (wDp, hDp) = when (widgetInfo.sizeText) {
            "4x1" -> 300 to 75
            "4x2" -> 300 to 150
            "3x2" -> 220 to 150
            "2x1" -> 200 to 90
            "1x2" -> 90 to 180
            else -> 150 to 150
        }

        val methods = clazz.methods + clazz.declaredMethods
        for (method in methods) {
            if (method.returnType == Bitmap::class.java) {
                method.isAccessible = true
                val params = method.parameterTypes
                try {
                    val args = arrayOfNulls<Any>(params.size)
                    for (i in params.indices) {
                        val p = params[i]
                        when {
                            p == Context::class.java -> args[i] = context
                            p == SlateWidgetConfig::class.java -> args[i] = defaultConfig
                            p == Boolean::class.javaPrimitiveType || p == Boolean::class.javaObjectType -> args[i] = false
                            p == Int::class.javaPrimitiveType || p == Int::class.javaObjectType -> {
                                args[i] = when (i) {
                                    params.size - 3 -> wDp
                                    params.size - 2 -> hDp
                                    else -> -1
                                }
                            }
                        }
                    }
                    val result = method.invoke(receiverInstance, *args) as? Bitmap
                    if (result != null) return result
                } catch (_: Exception) {}
            }
        }
        null
    } catch (_: Exception) {
        null
    }
}