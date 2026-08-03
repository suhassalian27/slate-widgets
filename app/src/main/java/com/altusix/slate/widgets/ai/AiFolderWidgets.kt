package com.altusix.slate.widgets.ai

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import com.altusix.slate.data.local.SlateDataStore
import com.altusix.slate.data.local.SlateWidgetConfig
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

abstract class BaseAiFolderWidget(
    private val contentComposers: @Composable (SlateWidgetConfig) -> Unit
) : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val config = try {
            val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
            SlateDataStore(context).getWidgetConfig(appWidgetId)
                .catch { emit(SlateWidgetConfig()) }.first()
        } catch (e: Exception) { SlateWidgetConfig() }

        provideContent {
            contentComposers(config)
        }
    }
}

// ============================================================================
// BARS (4x1)
// ============================================================================
class AiBarPrimaryWidget : BaseAiFolderWidget({ config -> AiBarPrimaryTile(config) })
class AiBarPrimaryReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = AiBarPrimaryWidget() }

class AiBarDock5Widget : BaseAiFolderWidget({ config -> AiBarDock5Tile(config) })
class AiBarDock5Receiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = AiBarDock5Widget() }

class AiBarCapsuleWidget : BaseAiFolderWidget({ config -> AiBarCapsuleTile(config) })
class AiBarCapsuleReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = AiBarCapsuleWidget() }

class AiBarSplitActionWidget : BaseAiFolderWidget({ config -> AiBarSplitActionTile(config) })
class AiBarSplitActionReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = AiBarSplitActionWidget() }

// ============================================================================
// FOLDERS (2x2 / 4x2)
// ============================================================================
class AiFolder4ClassicWidget : BaseAiFolderWidget({ config -> AiFolder4ClassicTile(config) })
class AiFolder4ClassicReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = AiFolder4ClassicWidget() }

class AiFolder6BentoHeroWidget : BaseAiFolderWidget({ config -> AiFolder6BentoHeroTile(config) })
class AiFolder6BentoHeroReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = AiFolder6BentoHeroWidget() }

class AiFolder8BentoSideWidget : BaseAiFolderWidget({ config -> AiFolder8BentoSideTile(config) })
class AiFolder8BentoSideReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = AiFolder8BentoSideWidget() }

class AiFolder9GridWidget : BaseAiFolderWidget({ config -> AiFolder9GridTile(config) })
class AiFolder9GridReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = AiFolder9GridWidget() }

class AiFolder10MegaWidget : BaseAiFolderWidget({ config -> AiFolder10MegaTile(config) })
class AiFolder10MegaReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = AiFolder10MegaWidget() }

class AiFolder7AsymmetricWidget : BaseAiFolderWidget({ config -> AiFolder7AsymmetricTile(config) })
class AiFolder7AsymmetricReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = AiFolder7AsymmetricWidget() }

class AiFolderFloatingMatrixWidget : BaseAiFolderWidget({ config -> AiFolderFloatingMatrixTile(config) })
class AiFolderFloatingMatrixReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = AiFolderFloatingMatrixWidget() }

suspend fun updateAllAiFolderWidgets(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    if (manager.getGlanceIds(AiBarPrimaryWidget::class.java).isNotEmpty()) AiBarPrimaryWidget().updateAll(context)
    if (manager.getGlanceIds(AiBarDock5Widget::class.java).isNotEmpty()) AiBarDock5Widget().updateAll(context)
    if (manager.getGlanceIds(AiBarCapsuleWidget::class.java).isNotEmpty()) AiBarCapsuleWidget().updateAll(context)
    if (manager.getGlanceIds(AiBarSplitActionWidget::class.java).isNotEmpty()) AiBarSplitActionWidget().updateAll(context)
    if (manager.getGlanceIds(AiFolder4ClassicWidget::class.java).isNotEmpty()) AiFolder4ClassicWidget().updateAll(context)
    if (manager.getGlanceIds(AiFolder6BentoHeroWidget::class.java).isNotEmpty()) AiFolder6BentoHeroWidget().updateAll(context)
    if (manager.getGlanceIds(AiFolder8BentoSideWidget::class.java).isNotEmpty()) AiFolder8BentoSideWidget().updateAll(context)
    if (manager.getGlanceIds(AiFolder9GridWidget::class.java).isNotEmpty()) AiFolder9GridWidget().updateAll(context)
    if (manager.getGlanceIds(AiFolder10MegaWidget::class.java).isNotEmpty()) AiFolder10MegaWidget().updateAll(context)
    if (manager.getGlanceIds(AiFolder7AsymmetricWidget::class.java).isNotEmpty()) AiFolder7AsymmetricWidget().updateAll(context)
    if (manager.getGlanceIds(AiFolderFloatingMatrixWidget::class.java).isNotEmpty()) AiFolderFloatingMatrixWidget().updateAll(context)
}