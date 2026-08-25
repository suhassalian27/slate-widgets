package com.altusix.slate.ui.config

import android.Manifest
import android.R
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.widgets.contacts.ContactActionType
import com.altusix.slate.widgets.contacts.ContactWidgetConfig
import com.altusix.slate.widgets.contacts.ContactsWidgetPreferences
import com.altusix.slate.widgets.contacts.getContactsWidgetsCatalog
import com.altusix.slate.widgets.contacts.loadSlotConfig
import com.altusix.slate.widgets.contacts.saveSlotConfig
import com.altusix.slate.widgets.contacts.updateAllContactsWidgets
import java.io.File
import java.io.FileOutputStream

class ContactsWidgetConfigActivity : ComponentActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetClassName: String = ""
    private var slotIndex = 0
    private var currentConfig by mutableStateOf(ContactWidgetConfig())

    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri -> queryContactDetails(uri) }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) performLaunchPicker()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        window.setBackgroundDrawableResource(R.color.transparent)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        slotIndex = intent?.extras?.getInt("extra_slot_index", 0) ?: 0
        currentConfig = loadSlotConfig(this, widgetId, slotIndex)

        val appWidgetInfo = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)
        widgetClassName = appWidgetInfo?.provider?.className ?: ""

        val isMultiActionWidget = widgetClassName.contains("EditorialBento") ||
                widgetClassName.contains("StackedBento") ||
                widgetClassName.contains("3Action")

        setContent {
            SlateConfigTheme {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

                val catalogItem = remember(widgetClassName) {
                    getContactsWidgetsCatalog().find { it.receiverClass.name == widgetClassName }
                }
                val widgetName = catalogItem?.name ?: ""
                val hasModeOption = catalogItem?.hasModeOption ?: true

                var selectedBgHex by remember { mutableLongStateOf(0xFF161618L) }
                var selectedAccentHex by remember { mutableLongStateOf(0xFFFFFFFFL) }
                var opacity by remember { mutableFloatStateOf(1.0f) }
                var isResponsive by remember { mutableStateOf(true) }
                var activePickerTarget by remember { mutableStateOf<ColorPickerTarget?>(null) }
                var selectedMainTab by remember { mutableIntStateOf(0) } // 0: Contact, 1: Style

                LaunchedEffect(widgetId) {
                    val prefs = getSharedPreferences("slate_widget_prefs", MODE_PRIVATE)
                    opacity = prefs.getFloat("widget_${widgetId}_opacity", 1.0f)
                    isResponsive = prefs.getBoolean("widget_${widgetId}_is_responsive", true)
                    selectedBgHex = prefs.getLong("widget_${widgetId}_bg_color", 0xFF161618L)
                    selectedAccentHex = prefs.getLong("widget_${widgetId}_accent_color", 0xFFFFFFFFL)
                }

                val currentSlateConfig = remember(selectedBgHex, selectedAccentHex, opacity) {
                    val themeMode = if (calculateLuminance(selectedBgHex) > 0.5f) "LIGHT" else "DARK"
                    SlateWidgetConfig(
                        themeMode = themeMode,
                        backgroundColorHex = selectedBgHex,
                        opacity = opacity,
                        accentColorHex = selectedAccentHex
                    )
                }

                ModalBottomSheet(
                    onDismissRequest = { finish() },
                    sheetState = sheetState,
                    containerColor = Color(0xFF0A0A0C),
                    contentColor = Color.White,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF2C2C30)) },
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ) {
                    ContactsConfigSheetContent(
                        currentConfig = currentConfig,
                        slateConfig = currentSlateConfig,
                        slotIndex = slotIndex,
                        widgetName = widgetName,
                        hasModeOption = hasModeOption,
                        isMultiActionWidget = isMultiActionWidget,
                        selectedBgHex = selectedBgHex,
                        selectedAccentHex = selectedAccentHex,
                        opacity = opacity,
                        isResponsive = isResponsive,
                        selectedMainTab = selectedMainTab,
                        onMainTabSelected = { selectedMainTab = it },
                        onConfigChanged = { currentConfig = it },
                        onBgHexChanged = { selectedBgHex = it },
                        onAccentHexChanged = { selectedAccentHex = it },
                        onOpacityChanged = { opacity = it },
                        onResponsiveChanged = { isResponsive = it },
                        onPickerTargetRequested = { activePickerTarget = it },
                        onChangeContactRequested = { checkPermissionAndLaunchPicker() },
                        onDismiss = { finish() },
                        onSaveClicked = {
                            saveSlotConfig(this@ContactsWidgetConfigActivity, widgetId, slotIndex, currentConfig)
                            if (slotIndex == 0) {
                                ContactsWidgetPreferences.saveConfig(this@ContactsWidgetConfigActivity, widgetId, currentConfig)
                            }
                            saveSlateWidgetConfig(this@ContactsWidgetConfigActivity, widgetId, currentSlateConfig, isResponsive)
                            updateAllContactsWidgets(this@ContactsWidgetConfigActivity)
                            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                            finish()
                        }
                    )
                }

                activePickerTarget?.let { target ->
                    val initialColor = if (target == ColorPickerTarget.BACKGROUND) Color(selectedBgHex) else Color(selectedAccentHex)
                    CustomColorPickerDialog(
                        initialColor = initialColor,
                        title = if (target == ColorPickerTarget.BACKGROUND) "Custom Background" else "Custom Accent",
                        onDismiss = { activePickerTarget = null },
                        onColorSelected = { color ->
                            val hex = (color.toArgb().toLong() and 0xFFFFFFFFL)
                            if (target == ColorPickerTarget.BACKGROUND) selectedBgHex = hex else selectedAccentHex = hex
                            activePickerTarget = null
                        }
                    )
                }
            }
        }
    }

    private fun calculateLuminance(hex: Long): Float {
        val r = ((hex shr 16) and 0xFFL) / 255f
        val g = ((hex shr 8) and 0xFFL) / 255f
        val b = (hex and 0xFFL) / 255f
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    private fun saveSlateWidgetConfig(context: Context, widgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean) {
        val prefs = context.getSharedPreferences("slate_widget_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putString("widget_${widgetId}_theme_mode", config.themeMode)
            putLong("widget_${widgetId}_bg_color", config.backgroundColorHex)
            putFloat("widget_${widgetId}_opacity", config.opacity)
            putLong("widget_${widgetId}_accent_color", config.accentColorHex)
            putBoolean("widget_${widgetId}_is_responsive", isResponsive)
            putString("widget_${widgetId}_mode", if (isResponsive) "RESPONSIVE" else "FIXED")
            apply()
        }
    }

    private fun checkPermissionAndLaunchPicker() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            performLaunchPicker()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun performLaunchPicker() {
        val pickIntent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        contactPickerLauncher.launch(pickIntent)
    }

    private fun queryContactDetails(contactUri: Uri) {
        var name = ""
        var number = ""
        var savedPhotoPath: String? = null

        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY
            )

            contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val photoUriIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                    val photoThumbIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                    val contactIdIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val lookupKeyIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)

                    if (nameIndex != -1) name = cursor.getString(nameIndex) ?: ""
                    if (numIndex != -1) number = cursor.getString(numIndex) ?: ""

                    var bitmap: Bitmap? = null

                    try {
                        ContactsContract.Contacts.openContactPhotoInputStream(contentResolver, contactUri, true)?.use { stream ->
                            bitmap = BitmapFactory.decodeStream(stream)
                        }
                    } catch (_: Exception) {}

                    if (bitmap == null) {
                        val photoUriStr = if (photoUriIndex != -1) cursor.getString(photoUriIndex) else null
                        val photoThumbStr = if (photoThumbIndex != -1) cursor.getString(photoThumbIndex) else null
                        val targetUriStr = photoUriStr ?: photoThumbStr

                        if (!targetUriStr.isNullOrEmpty()) {
                            try {
                                contentResolver.openInputStream(Uri.parse(targetUriStr))?.use { stream ->
                                    bitmap = BitmapFactory.decodeStream(stream)
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    if (bitmap == null && contactIdIndex != -1) {
                        try {
                            val contactId = cursor.getLong(contactIdIndex)
                            val lookupKey = if (lookupKeyIndex != -1) cursor.getString(lookupKeyIndex) else null

                            val contactLookupUri = if (!lookupKey.isNullOrEmpty()) {
                                ContactsContract.Contacts.getLookupUri(contactId, lookupKey)
                            } else {
                                ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
                            }

                            ContactsContract.Contacts.openContactPhotoInputStream(contentResolver, contactLookupUri, true)?.use { stream ->
                                bitmap = BitmapFactory.decodeStream(stream)
                            }
                        } catch (_: Exception) {}
                    }

                    if (bitmap != null) {
                        val photoFile = File(filesDir, "contact_photo_${widgetId}_slot_${slotIndex}.jpg")
                        FileOutputStream(photoFile).use { out ->
                            bitmap?.compress(Bitmap.CompressFormat.JPEG, 92, out)
                        }
                        savedPhotoPath = Uri.fromFile(photoFile).toString()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        currentConfig = currentConfig.copy(
            contactName = name,
            phoneNumber = number,
            photoUri = savedPhotoPath,
            initials = ContactsWidgetPreferences.getInitials(name),
            isConfigured = name.isNotEmpty()
        )
    }
}

@Composable
private fun ContactsConfigSheetContent(
    currentConfig: ContactWidgetConfig,
    slateConfig: SlateWidgetConfig,
    slotIndex: Int,
    widgetName: String,
    hasModeOption: Boolean,
    isMultiActionWidget: Boolean,
    selectedBgHex: Long,
    selectedAccentHex: Long,
    opacity: Float,
    isResponsive: Boolean,
    selectedMainTab: Int,
    onMainTabSelected: (Int) -> Unit,
    onConfigChanged: (ContactWidgetConfig) -> Unit,
    onBgHexChanged: (Long) -> Unit,
    onAccentHexChanged: (Long) -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onResponsiveChanged: (Boolean) -> Unit,
    onPickerTargetRequested: (ColorPickerTarget) -> Unit,
    onChangeContactRequested: () -> Unit,
    onDismiss: () -> Unit,
    onSaveClicked: () -> Unit
) {
    val isLightBg = slateConfig.themeMode == "LIGHT"
    val textColor = if (isLightBg) Color.Black else Color.White
    val secondaryTextColor = if (isLightBg) Color.DarkGray else Color.Gray

    val bgPresets = listOf(0xFF161618L to "Dark", 0xFF000000L to "AMOLED", 0xFFFFFFFFL to "Light")
    val accentPresets = if (isLightBg) {
        listOf(0xFF000000L, 0xFF00D166L, 0xFF2B80FFL, 0xFFFF3B30L, 0xFFFF9500L, 0xFFAF52DEL)
    } else {
        listOf(0xFFFFFFFFL, 0xFF00D166L, 0xFF2B80FFL, 0xFFFF3B30L, 0xFFFF9500L, 0xFFAF52DEL)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .imePadding()
            .padding(horizontal = 22.dp)
            .padding(bottom = 16.dp)
    ) {
        // --- Header Bar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cancel",
                color = Color.Gray,
                fontSize = 15.sp,
                modifier = Modifier.clickable { onDismiss() }.padding(vertical = 8.dp)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (slotIndex > 0) "Slot ${slotIndex + 1} Contact" else "Customize Contact",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (widgetName.isNotEmpty()) {
                    Text(text = widgetName, fontSize = 11.sp, color = Color(0xFF8E8E93), fontWeight = FontWeight.Normal)
                }
            }
            Text(
                text = "Apply",
                color = if (currentConfig.isConfigured) Color.White else Color.Gray,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier
                    .clickable(enabled = currentConfig.isConfigured) { onSaveClicked() }
                    .padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Live Preview Hero Container ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(selectedBgHex).copy(alpha = opacity))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ContactAvatarPreview(
                    photoUri = currentConfig.photoUri,
                    initials = currentConfig.initials
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = currentConfig.contactName.ifEmpty { "No Contact Selected" },
                    color = textColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                if (currentConfig.phoneNumber.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentConfig.phoneNumber,
                        color = secondaryTextColor,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Main Tab Selector ("Contact" / "Style") ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141416))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Contact", "Style").forEachIndexed { index, label ->
                val isSelected = selectedMainTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFF2C2C30) else Color.Transparent)
                        .clickable { onMainTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color(0xFF8E8E93),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedMainTab == 0) {
            // ==================== TAB 0: CONTACT & ACTION ====================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF141416))
                        .border(1.dp, Color(0xFF242428), RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SELECTED CONTACT",
                            color = Color(0xFF8E8E93),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = currentConfig.contactName.ifEmpty { "Select from phonebook" },
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = onChangeContactRequested,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(Color(0xFF3A3A3C)))
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (currentConfig.isConfigured) "Change Contact" else "Pick Contact",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                if (!isMultiActionWidget) {
                    Spacer(modifier = Modifier.height(20.dp))

                    SectionTitle(title = "Tap Action")
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val actionItems = listOf(
                            Triple(ContactActionType.CALL, Icons.Default.Call, ContactActionType.CALL.description),
                            Triple(ContactActionType.SMS, Icons.Default.Email, ContactActionType.SMS.description),
                            Triple(ContactActionType.WHATSAPP, Icons.Default.Send, ContactActionType.WHATSAPP.description),
                            Triple(ContactActionType.TELEGRAM, Icons.Default.Send, ContactActionType.TELEGRAM.description)
                        )

                        actionItems.forEach { (action, icon, desc) ->
                            val isSelected = currentConfig.actionType == action
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Color(0xFF2C2C30) else Color(0xFF141416))
                                    .border(
                                        1.dp,
                                        if (isSelected) Color.White else Color(0xFF242428),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { onConfigChanged(currentConfig.copy(actionType = action)) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF222226)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(action.label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text(desc, color = Color.Gray, fontSize = 11.sp)
                                }

                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

        } else {
            // ==================== TAB 1: STYLE CONFIGURATION ====================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column {
                    SectionTitle(title = "Background")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bgPresets.forEach { (hex, label) ->
                            SelectableChip(
                                label = label,
                                isSelected = selectedBgHex == hex,
                                colorPreview = Color(hex),
                                modifier = Modifier.weight(1f)
                            ) {
                                onBgHexChanged(hex)
                                if (hex == 0xFFFFFFFFL && selectedAccentHex == 0xFFFFFFFFL) onAccentHexChanged(0xFF000000L)
                                else if (hex != 0xFFFFFFFFL && selectedAccentHex == 0xFF000000L) onAccentHexChanged(0xFFFFFFFFL)
                            }
                        }
                        val isCustomBg = bgPresets.none { it.first == selectedBgHex }
                        RainbowPickerChip(
                            isSelected = isCustomBg,
                            activeColor = if (isCustomBg) Color(selectedBgHex) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            onPickerTargetRequested(ColorPickerTarget.BACKGROUND)
                        }
                    }
                }

                Column {
                    SectionTitle(title = "Accent Color")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        accentPresets.forEach { hex ->
                            ProfessionalSwatchCircle(
                                color = Color(hex),
                                isSelected = selectedAccentHex == hex,
                                onClick = { onAccentHexChanged(hex) }
                            )
                        }
                        val isCustomAccent = accentPresets.none { it == selectedAccentHex }
                        RainbowCustomCircle(
                            isSelected = isCustomAccent,
                            activeColor = if (isCustomAccent) Color(selectedAccentHex) else null,
                            onClick = { onPickerTargetRequested(ColorPickerTarget.ACCENT) }
                        )
                    }
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionTitle(title = "Opacity")
                        Text(text = "${(opacity * 100).toInt()}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    ModernOpacitySlider(value = opacity, onValueChange = onOpacityChanged)
                }

                if (hasModeOption) {
                    Column {
                        SectionTitle(title = "Sizing Mode")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF141416))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(true to "Responsive", false to "Fixed Aspect").forEach { (responsiveVal, label) ->
                                val isSelected = isResponsive == responsiveVal
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFF2C2C30) else Color.Transparent)
                                        .clickable { onResponsiveChanged(responsiveVal) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else Color(0xFF8E8E93),
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactAvatarPreview(photoUri: String?, initials: String) {
    val bitmap = remember(photoUri) {
        if (photoUri.isNullOrEmpty()) null
        else {
            try {
                val uri = Uri.parse(photoUri)
                if (uri.scheme == "file") {
                    BitmapFactory.decodeFile(uri.path)
                } else null
            } catch (_: Exception) { null }
        }
    }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color(0xFF222226)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initials.ifEmpty { "?" },
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}