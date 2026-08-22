package com.altusix.slate.widgets.contacts

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileOutputStream

class ContactsWidgetConfigActivity : ComponentActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var currentConfig by mutableStateOf(ContactWidgetConfig())

    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
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
        window.setBackgroundDrawableResource(android.R.color.transparent)

        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        currentConfig = ContactsWidgetPreferences.loadConfig(this, widgetId)

        // Check if current widget is the 3-Section Bento Widget
        val appWidgetInfo = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)
        val is3SectionWidget = appWidgetInfo?.provider?.className?.contains("EditorialBento") == true

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color.Transparent, surface = Color(0xFF0A0A0C))) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

                ModalBottomSheet(
                    onDismissRequest = { finish() },
                    sheetState = sheetState,
                    containerColor = Color(0xFF0A0A0C),
                    contentColor = Color.White,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF2C2C30)) },
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color.Gray,
                                fontSize = 15.sp,
                                modifier = Modifier.clickable { finish() }.padding(vertical = 8.dp)
                            )
                            Text(
                                text = "Select Contact",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Save",
                                color = if (currentConfig.isConfigured) Color.White else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier
                                    .clickable(enabled = currentConfig.isConfigured) { saveAndFinish() }
                                    .padding(vertical = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Selected Contact Preview Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF141416))
                                .border(1.dp, Color(0xFF242428), RoundedCornerShape(24.dp))
                                .padding(vertical = 24.dp, horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ContactAvatarPreview(
                                    photoUri = currentConfig.photoUri,
                                    initials = currentConfig.initials
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "CONTACT",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = currentConfig.contactName.ifEmpty { "No Contact Selected" },
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (currentConfig.phoneNumber.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = currentConfig.phoneNumber,
                                        color = Color.Gray,
                                        fontSize = 13.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                OutlinedButton(
                                    onClick = { checkPermissionAndLaunchPicker() },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF3A3A3C)))
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Change Contact", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        }

                        // Show Tap Action selector ONLY for Single Action Widgets
                        if (!is3SectionWidget) {
                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Tap Action",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.align(Alignment.Start)
                            )

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
                                            .background(if (isSelected) Color(0xFF1C1C1E) else Color(0xFF141416))
                                            .border(
                                                1.dp,
                                                if (isSelected) Color(0xFF3A3A3C) else Color.Transparent,
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable { currentConfig = currentConfig.copy(actionType = action) }
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF2C2C30)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(action.label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                            Text(desc, color = Color.Gray, fontSize = 12.sp)
                                        }

                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    if (bitmap == null) {
                        val photoUriStr = if (photoUriIndex != -1) cursor.getString(photoUriIndex) else null
                        val photoThumbStr = if (photoThumbIndex != -1) cursor.getString(photoThumbIndex) else null
                        val targetUriStr = photoUriStr ?: photoThumbStr

                        if (!targetUriStr.isNullOrEmpty()) {
                            try {
                                contentResolver.openInputStream(Uri.parse(targetUriStr))?.use { stream ->
                                    bitmap = BitmapFactory.decodeStream(stream)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
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
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    if (bitmap != null) {
                        val photoFile = File(filesDir, "contact_photo_$widgetId.jpg")
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

    private fun saveAndFinish() {
        if (!currentConfig.isConfigured) return
        ContactsWidgetPreferences.saveConfig(this, widgetId, currentConfig)
        updateAllContactsWidgets(this)
        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
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
            .size(80.dp)
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
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
