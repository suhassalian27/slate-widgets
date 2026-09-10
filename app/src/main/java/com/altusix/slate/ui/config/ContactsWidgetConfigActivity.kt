package com.altusix.slate.ui.config

import android.Manifest
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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altusix.slate.core.theme.ThemePreferences
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.ui.components.*
import com.altusix.slate.widgets.contacts.*
import java.io.File
import java.io.FileOutputStream

class ContactsWidgetConfigActivity : ComponentActivity() {

    private enum class ContactsColorTarget { BACKGROUND, ACCENT }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

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

        val catalogItem = getContactsWidgetsCatalog().find { it.receiverClass.name == widgetClassName }
        val widgetName = catalogItem?.name ?: ""
        val hasModeOption = catalogItem?.hasModeOption ?: true
        val defaultTheme = ThemePreferences(this).getThemeSettings()

        val isMultiActionWidget = widgetClassName.contains("EditorialBento") ||
                widgetClassName.contains("StackedBento") ||
                widgetClassName.contains("3Action")

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0A0A0C), surface = Color(0xFF16161B))) {
                var selectedBgHex by remember { mutableLongStateOf(0xFF161618L) }
                var selectedAccentHex by remember { mutableLongStateOf(defaultTheme.accentHex) }
                var opacity by remember { mutableFloatStateOf(1.0f) }
                var isResponsive by remember { mutableStateOf(true) }
                var activePickerTarget by remember { mutableStateOf<ContactsColorTarget?>(null) }
                var selectedTabKey by remember { mutableStateOf("CONTACT") }

                val tabs = remember {
                    listOf(
                        ConfigTabItem("CONTACT", "Contact"),
                        ConfigTabItem("STYLE", "Widget Theme")
                    )
                }

                LaunchedEffect(widgetId) {
                    val prefs = getSharedPreferences("slate_widget_prefs", MODE_PRIVATE)
                    opacity = prefs.getFloat("widget_${widgetId}_opacity", 1.0f)
                    isResponsive = prefs.getBoolean("widget_${widgetId}_is_responsive", true)
                    selectedBgHex = prefs.getLong("widget_${widgetId}_bg_color", 0xFF161618L)
                    selectedAccentHex = prefs.getLong("widget_${widgetId}_accent_color", defaultTheme.accentHex)
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

                fun saveAndFinish() {
                    saveSlotConfig(this@ContactsWidgetConfigActivity, widgetId, slotIndex, currentConfig)
                    if (slotIndex == 0) {
                        ContactsWidgetPreferences.saveConfig(this@ContactsWidgetConfigActivity, widgetId, currentConfig)
                    }
                    saveSlateWidgetConfig(this@ContactsWidgetConfigActivity, widgetId, currentSlateConfig, isResponsive)
                    updateAllContactsWidgets(this@ContactsWidgetConfigActivity)
                    setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                    finish()
                }

                SlateConfigScaffold(
                    title = if (slotIndex > 0) "Slot ${slotIndex + 1} Contact" else "Contact Setup",
                    subtitle = widgetName.ifEmpty { null },
                    accentColor = Color(selectedAccentHex),
                    tabs = tabs,
                    selectedTabKey = selectedTabKey,
                    onTabSelected = { selectedTabKey = it },
                    onBackClick = { finish() },
                    onSaveClick = { saveAndFinish() },
                    scrollable = true,
                    previewHeight = 180.dp,
                    previewContent = {
                        val isLightBg = calculateLuminance(selectedBgHex) > 0.5f
                        Box(
                            modifier = Modifier
                                .widthIn(min = 200.dp, max = 260.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(selectedBgHex).copy(alpha = opacity))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ContactAvatarPreview(
                                    photoUri = currentConfig.photoUri,
                                    initials = currentConfig.initials,
                                    size = 64.dp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = currentConfig.contactName.ifEmpty { "No Contact Selected" },
                                    color = if (isLightBg) Color.Black else Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (currentConfig.phoneNumber.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = currentConfig.phoneNumber,
                                        color = if (isLightBg) Color(0xFF3C3C43) else Color(0xFF8E8E93),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                ) {
                    if (selectedTabKey == "CONTACT") {
                        SectionTitle(title = "Selected Contact")

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF141418))
                                .border(1.dp, Color(0xFF24242C), RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    ContactAvatarPreview(
                                        photoUri = currentConfig.photoUri,
                                        initials = currentConfig.initials,
                                        size = 44.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = currentConfig.contactName.ifEmpty { "No Contact Selected" },
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = currentConfig.phoneNumber.ifEmpty { "Tap to choose from phonebook" },
                                            color = Color(0xFF8E8E93),
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Button(
                                    onClick = { checkPermissionAndLaunchPicker() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(selectedAccentHex),
                                        contentColor = if (calculateLuminance(selectedAccentHex) > 0.5f) Color.Black else Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(
                                        text = if (currentConfig.isConfigured) "Change" else "Pick",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (!isMultiActionWidget) {
                            Spacer(modifier = Modifier.height(4.dp))
                            SectionTitle(title = "Tap Action")

                            val actionItems = listOf(
                                Triple(ContactActionType.CALL, Icons.Default.Call, ContactActionType.CALL.description),
                                Triple(ContactActionType.SMS, Icons.Default.Email, ContactActionType.SMS.description),
                                Triple(ContactActionType.WHATSAPP, Icons.Default.Send, ContactActionType.WHATSAPP.description),
                                Triple(ContactActionType.TELEGRAM, Icons.Default.Send, ContactActionType.TELEGRAM.description)
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                actionItems.forEach { (action, icon, desc) ->
                                    val isSelected = currentConfig.actionType == action
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isSelected) Color(0xFF22222A) else Color(0xFF141418))
                                            .border(
                                                1.dp,
                                                if (isSelected) Color(selectedAccentHex) else Color(0xFF24242C),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .clickable { currentConfig = currentConfig.copy(actionType = action) }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color(selectedAccentHex).copy(alpha = 0.2f) else Color(0xFF1C1C22)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isSelected) Color(selectedAccentHex) else Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = action.label,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = desc,
                                                color = Color(0xFF8E8E93),
                                                fontSize = 11.sp
                                            )
                                        }

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color(selectedAccentHex),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val isLightBg = calculateLuminance(selectedBgHex) > 0.5f

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle(title = "Background")
                            val bgPresets = listOf(
                                0xFF161618L to "Matte",
                                0xFF000000L to "AMOLED",
                                0xFFFFFFFFL to "Light"
                            )

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
                                        selectedBgHex = hex
                                        if (hex == 0xFFFFFFFFL && selectedAccentHex == 0xFFFFFFFFL) selectedAccentHex = 0xFF000000L
                                        else if (hex != 0xFFFFFFFFL && selectedAccentHex == 0xFF000000L) selectedAccentHex = 0xFFFFFFFFL
                                    }
                                }

                                val isCustomBg = bgPresets.none { it.first == selectedBgHex }
                                RainbowPickerChip(
                                    isSelected = isCustomBg,
                                    activeColor = if (isCustomBg) Color(selectedBgHex) else null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    activePickerTarget = ContactsColorTarget.BACKGROUND
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionTitle(title = "Accent Color")
                            val accentPresets = if (isLightBg) {
                                listOf(0xFF000000L, 0xFF00D166L, 0xFF2B80FFL, 0xFFFF3B30L, 0xFFFF9500L, 0xFFAF52DEL)
                            } else {
                                listOf(0xFFFFFFFFL, 0xFF00D166L, 0xFF2B80FFL, 0xFFFF3B30L, 0xFFFF9500L, 0xFFAF52DEL)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                accentPresets.forEach { hex ->
                                    ProfessionalSwatchCircle(
                                        color = Color(hex),
                                        isSelected = selectedAccentHex == hex,
                                        onClick = { selectedAccentHex = hex }
                                    )
                                }

                                val isCustomAccent = accentPresets.none { it == selectedAccentHex }
                                RainbowCustomCircle(
                                    isSelected = isCustomAccent,
                                    activeColor = if (isCustomAccent) Color(selectedAccentHex) else null,
                                    onClick = { activePickerTarget = ContactsColorTarget.ACCENT }
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionTitle(title = "Surface Translucency")
                                Text(
                                    text = "${(opacity * 100).toInt()}%",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            ModernOpacitySlider(value = opacity, onValueChange = { opacity = it })
                        }

                        if (hasModeOption) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SectionTitle(title = "Sizing Mode")
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
                                                .clickable { isResponsive = responsiveVal },
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

                activePickerTarget?.let { target ->
                    val initialColor = if (target == ContactsColorTarget.BACKGROUND) Color(selectedBgHex) else Color(selectedAccentHex)
                    CustomColorPickerDialog(
                        initialColor = initialColor,
                        title = if (target == ContactsColorTarget.BACKGROUND) "Custom Background" else "Custom Accent",
                        onDismiss = { activePickerTarget = null },
                        onColorSelected = { color ->
                            val hex = (color.toArgb().toLong() and 0xFFFFFFFFL)
                            if (target == ContactsColorTarget.BACKGROUND) selectedBgHex = hex else selectedAccentHex = hex
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
private fun ContactAvatarPreview(
    photoUri: String?,
    initials: String,
    size: Dp = 72.dp
) {
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
            .size(size)
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
                fontSize = (size.value * 0.36f).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}