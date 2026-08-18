package com.altusix.slate.widgets.calculator

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.altusix.slate.R
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.data.local.SlateWidgetConfig

fun getCalculatorWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Standard Calculator", "2x2", "Calculator", StandardCalc2x2Receiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Split Capsule Calc", "2x2", "Calculator", SplitCalc2x2Receiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Studio Express Calc", "4x2", "Calculator", StudioCalc4x2Receiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Circular Stage Calc", "2x2", "Calculator", CircleCalc2x2Receiver::class.java, hasModeOption = false)
    )
}

fun updateAllCalculatorWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        StandardCalc2x2Receiver::class.java,
        SplitCalc2x2Receiver::class.java,
        StudioCalc4x2Receiver::class.java,
        CircleCalc2x2Receiver::class.java
    )
    for (receiverClass in receivers) {
        val ids = manager.getAppWidgetIds(ComponentName(context, receiverClass)) ?: intArrayOf()
        if (ids.isNotEmpty()) {
            val intent = Intent(context, receiverClass).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}

abstract class BaseCalcReceiver(private val layoutResId: Int) : AppWidgetProvider() {

    companion object {
        const val ACTION_CALC_KEY = "com.altusix.slate.ACTION_CALC_KEY"
        const val EXTRA_KEY = "extra_calc_key"
    }

    abstract fun renderBitmap(context: Context, state: CalculatorState, config: SlateWidgetConfig, wDp: Int, hDp: Int): android.graphics.Bitmap
    abstract fun getKeyMap(): Map<Int, String>

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CALC_KEY) {
            val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val key = intent.getStringExtra(EXTRA_KEY) ?: ""

            if (id != AppWidgetManager.INVALID_APPWIDGET_ID && key.isNotEmpty()) {
                val current = CalculatorEngine.getWidgetState(context, id)
                val updated = CalculatorEngine.handleKeyPress(current, key)
                CalculatorEngine.saveWidgetState(context, id, updated)
                updateSingleWidget(context, AppWidgetManager.getInstance(context), id)
                return
            }
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) updateSingleWidget(context, appWidgetManager, id)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle?) {
        updateSingleWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    private fun updateSingleWidget(context: Context, manager: AppWidgetManager, id: Int) {
        try {
            val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
            val themeMode = widgetPrefs.getString("widget_${id}_theme_mode", "DARK") ?: "DARK"
            val bgColor = widgetPrefs.getLong("widget_${id}_bg_color", if (themeMode == "LIGHT") 0xFFFFFFFFL else 0xFF161618L)
            val opacity = widgetPrefs.getFloat("widget_${id}_opacity", 1.0f)
            val accentColor = widgetPrefs.getLong("widget_${id}_accent_color", if (themeMode == "LIGHT") 0xFF000000L else 0xFFFFFFFFL)

            val config = SlateWidgetConfig(themeMode, bgColor, opacity, accentColor)
            val state = CalculatorEngine.getWidgetState(context, id)

            val options = manager.getAppWidgetOptions(id)
            val wDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 160
            val hDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 160

            val bitmap = renderBitmap(context, state, config, wDp, hDp)
            val views = RemoteViews(context.packageName, layoutResId)
            views.setImageViewBitmap(R.id.widget_canvas_surface, bitmap)

            for ((viewId, keyVal) in getKeyMap()) {
                val keyIntent = Intent(context, this.javaClass).apply {
                    action = ACTION_CALC_KEY
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    putExtra(EXTRA_KEY, keyVal)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    id * 1000 + viewId,
                    keyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(viewId, pendingIntent)
            }

            manager.updateAppWidget(id, views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class StandardCalc2x2Receiver : BaseCalcReceiver(R.layout.widget_calculator_2x2_layout) {
    override fun renderBitmap(context: Context, state: CalculatorState, config: SlateWidgetConfig, wDp: Int, hDp: Int) =
        generateCalculator2x2Bitmap(context, state, config, wDp, hDp)

    override fun getKeyMap() = mapOf(
        R.id.btn_calc_ac to "AC", R.id.btn_calc_del to "DEL", R.id.btn_calc_percent to "%", R.id.btn_calc_div to "÷",
        R.id.btn_calc_7 to "7", R.id.btn_calc_8 to "8", R.id.btn_calc_9 to "9", R.id.btn_calc_mul to "×",
        R.id.btn_calc_4 to "4", R.id.btn_calc_5 to "5", R.id.btn_calc_6 to "6", R.id.btn_calc_sub to "-",
        R.id.btn_calc_1 to "1", R.id.btn_calc_2 to "2", R.id.btn_calc_3 to "3", R.id.btn_calc_add to "+",
        R.id.btn_calc_0 to "0", R.id.btn_calc_dot to ".", R.id.btn_calc_eq to "="
    )
}

class SplitCalc2x2Receiver : BaseCalcReceiver(R.layout.widget_calc_split_layout) {
    override fun renderBitmap(context: Context, state: CalculatorState, config: SlateWidgetConfig, wDp: Int, hDp: Int) =
        generateSplitCalculatorBitmap(context, state, config, wDp, hDp)

    override fun getKeyMap() = mapOf(
        R.id.btn_calc_ac to "AC", R.id.btn_calc_del to "DEL", R.id.btn_calc_percent to "%",
        R.id.btn_calc_7 to "7", R.id.btn_calc_8 to "8", R.id.btn_calc_9 to "9",
        R.id.btn_calc_4 to "4", R.id.btn_calc_5 to "5", R.id.btn_calc_6 to "6",
        R.id.btn_calc_1 to "1", R.id.btn_calc_2 to "2", R.id.btn_calc_3 to "3",
        R.id.btn_calc_0 to "0", R.id.btn_calc_dot to ".",
        R.id.btn_calc_div to "÷", R.id.btn_calc_mul to "×", R.id.btn_calc_sub to "-", R.id.btn_calc_add to "+", R.id.btn_calc_eq to "="
    )
}

class StudioCalc4x2Receiver : BaseCalcReceiver(R.layout.widget_calc_4x2_layout) {
    override fun renderBitmap(context: Context, state: CalculatorState, config: SlateWidgetConfig, wDp: Int, hDp: Int) =
        generateStudioCalculator4x2Bitmap(context, state, config, wDp, hDp)

    override fun getKeyMap() = mapOf(
        R.id.btn_calc_7 to "7", R.id.btn_calc_8 to "8", R.id.btn_calc_9 to "9", R.id.btn_calc_div to "÷",
        R.id.btn_calc_4 to "4", R.id.btn_calc_5 to "5", R.id.btn_calc_6 to "6", R.id.btn_calc_mul to "×",
        R.id.btn_calc_1 to "1", R.id.btn_calc_2 to "2", R.id.btn_calc_3 to "3", R.id.btn_calc_sub to "-",
        R.id.btn_calc_0 to "0", R.id.btn_calc_dot to ".", R.id.btn_calc_percent to "%", R.id.btn_calc_add to "+",
        R.id.btn_calc_ac to "AC", R.id.btn_calc_del to "DEL"
    )
}

class CircleCalc2x2Receiver : BaseCalcReceiver(R.layout.widget_calc_circle_layout) {
    override fun renderBitmap(context: Context, state: CalculatorState, config: SlateWidgetConfig, wDp: Int, hDp: Int) =
        generateCircleCalculatorBitmap(context, state, config, wDp, hDp)

    override fun getKeyMap() = mapOf(
        R.id.btn_calc_ac to "AC", R.id.btn_calc_del to "DEL", R.id.btn_calc_percent to "%", R.id.btn_calc_div to "÷",
        R.id.btn_calc_7 to "7", R.id.btn_calc_8 to "8", R.id.btn_calc_9 to "9", R.id.btn_calc_mul to "×",
        R.id.btn_calc_4 to "4", R.id.btn_calc_5 to "5", R.id.btn_calc_6 to "6", R.id.btn_calc_sub to "-",
        R.id.btn_calc_1 to "1", R.id.btn_calc_2 to "2", R.id.btn_calc_3 to "3", R.id.btn_calc_add to "+",
        R.id.btn_calc_0 to "0", R.id.btn_calc_dot to "."
    )
}