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
            "2x1" -> 200 to 100
            "1x2" -> 100 to 200
            else -> 150 to 150
        }

        val methods = clazz.methods + clazz.declaredMethods
        for (method in methods) {
            if (method.returnType == Bitmap::class.java) {
                method.isAccessible = true
                val bitmap = invokeRenderMethod(
                    receiverInstance = receiverInstance,
                    method = method,
                    context = context,
                    defaultConfig = defaultConfig,
                    wDp = wDp,
                    hDp = hDp
                )
                if (bitmap != null) return bitmap
            }
        }
        null
    } catch (_: Exception) {
        null
    }
}

private fun invokeRenderMethod(
    receiverInstance: Any,
    method: java.lang.reflect.Method,
    context: Context,
    defaultConfig: SlateWidgetConfig,
    wDp: Int,
    hDp: Int
): Bitmap? {
    val params = method.parameterTypes
    val args = arrayOfNulls<Any>(params.size)

    val intIndices = mutableListOf<Int>()
    for (i in params.indices) {
        if (params[i] == Int::class.javaPrimitiveType || params[i] == Int::class.javaObjectType) {
            intIndices.add(i)
        }
    }

    for (i in params.indices) {
        val p = params[i]
        when {
            p == Context::class.java -> args[i] = context
            p == SlateWidgetConfig::class.java -> args[i] = defaultConfig
            p == Boolean::class.javaPrimitiveType || p == Boolean::class.javaObjectType -> args[i] = false
        }
    }

    when (method.name) {
        "renderWidgetBitmap" -> {
            // BaseCanvasWidgetProvider: (context, appWidgetId, config, wDp, hDp)
            if (intIndices.size >= 3) {
                args[intIndices[0]] = -1    // appWidgetId
                args[intIndices[1]] = wDp   // wDp
                args[intIndices[2]] = hDp   // hDp
            } else if (intIndices.size == 2) {
                args[intIndices[0]] = wDp
                args[intIndices[1]] = hDp
            }
        }
        "renderBitmapForWidget" -> {
            // BaseDeviceInfoReceiver: (context, config, isResponsive, wDp, hDp, widgetId)
            if (intIndices.size >= 3) {
                args[intIndices[0]] = wDp   // wDp
                args[intIndices[1]] = hDp   // hDp
                args[intIndices[2]] = -1    // widgetId
            } else if (intIndices.size == 2) {
                args[intIndices[0]] = wDp
                args[intIndices[1]] = hDp
            }
        }
        else -> {
            // Fallback resolution for custom single-widget render methods
            if (intIndices.size == 2) {
                args[intIndices[0]] = wDp
                args[intIndices[1]] = hDp
            } else if (intIndices.size >= 3) {
                if (params.size > 1 && params[1] == Int::class.javaPrimitiveType) {
                    args[intIndices[0]] = -1
                    args[intIndices[1]] = wDp
                    args[intIndices[2]] = hDp
                } else {
                    args[intIndices[0]] = wDp
                    args[intIndices[1]] = hDp
                    args[intIndices[2]] = -1
                }
            }
        }
    }

    return method.invoke(receiverInstance, *args) as? Bitmap
}