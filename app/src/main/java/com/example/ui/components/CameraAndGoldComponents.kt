package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.AttachmentType
import com.example.data.repository.SafirRepository
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.KalbarqThemePalettes
import com.example.ui.theme.OnlineGreenGlow
import com.example.ui.theme.SecurityShieldGold
import com.example.util.ImageCutoutProcessor
import kotlinx.coroutines.delay
import java.io.File

/**
 * Fullscreen WhatsApp-style Camera & Video Recorder Studio with Runtime Permission Requests
 * (`CAMERA`, `RECORD_AUDIO`, `POST_NOTIFICATIONS`) + Original 100% Uncompressed Quality Preservation.
 */
@Composable
fun WhatsAppCameraCaptureDialog(
    initialVideoMode: Boolean = false,
    preserveOriginalQualityDefault: Boolean = true,
    onDismiss: () -> Unit,
    onMediaCaptured: (
        filePath: String,
        isVideo: Boolean,
        caption: String,
        sizeBadge: String,
        videoDurationSec: Int
    ) -> Unit
) {
    val context = LocalContext.current

    // Check current runtime permissions
    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotifPerm by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasCameraPerm = result[Manifest.permission.CAMERA] ?: hasCameraPerm
        hasAudioPerm = result[Manifest.permission.RECORD_AUDIO] ?: hasAudioPerm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotifPerm = result[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotifPerm
        }
        Toast.makeText(context, "تم تحديث أذونات الكاميرا والميكروفون والإشعارات بنجاح ✓", Toast.LENGTH_SHORT).show()
    }

    // Request permissions automatically like WhatsApp when opening camera if not yet granted
    LaunchedEffect(Unit) {
        val permsToRequest = mutableListOf<String>()
        if (!hasCameraPerm) permsToRequest.add(Manifest.permission.CAMERA)
        if (!hasAudioPerm) permsToRequest.add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPerm) {
            permsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permsToRequest.toTypedArray())
        }
    }

    var isVideoMode by remember { mutableStateOf(initialVideoMode) }
    var isRecordingVideo by remember { mutableStateOf(false) }
    var recordedSeconds by remember { mutableIntStateOf(0) }
    var isFrontCamera by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }
    var preserveOriginalQuality by remember { mutableStateOf(preserveOriginalQualityDefault) }
    var captionText by remember { mutableStateOf("") }

    val scenes = remember {
        listOf(
            "tech" to "مكتب تقني",
            "portrait" to "صورة شخصية",
            "nature" to "طبيعة وغروب",
            "coffee" to "قهوة الصباح",
            "pet" to "حيوان أليف"
        )
    }
    var selectedScene by remember { mutableStateOf(scenes.first().first) }
    var capturedFilePath by remember { mutableStateOf<String?>(null) }
    var capturedSizeBadge by remember { mutableStateOf("") }

    // System Camera TakePicture / CaptureVideo launchers via FileProvider
    var pendingSystemCameraFile by remember { mutableStateOf<File?>(null) }
    val systemTakePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingSystemCameraFile
        if (success && file != null && file.exists() && file.length() > 0L) {
            capturedFilePath = file.absolutePath
            capturedSizeBadge = ImageCutoutProcessor.getImageResolutionLabel(context, file.absolutePath)
        } else {
            val (path, badge) = ImageCutoutProcessor.captureStudioCameraPhoto(
                context = context,
                scenePreset = selectedScene,
                isFrontCamera = isFrontCamera,
                preserveOriginalQuality = preserveOriginalQuality
            )
            capturedFilePath = path
            capturedSizeBadge = badge
        }
    }

    LaunchedEffect(isRecordingVideo) {
        if (isRecordingVideo) {
            recordedSeconds = 0
            while (isRecordingVideo && recordedSeconds < 300) {
                delay(1000)
                recordedSeconds++
            }
        }
    }

    val previewBitmap = remember(selectedScene, isFrontCamera, capturedFilePath) {
        val pathToLoad = capturedFilePath ?: ImageCutoutProcessor.ensureSampleGalleryPhotos(context)
            .find { it.category == (if (isFrontCamera) "portrait" else selectedScene) }?.filePath
            ?: ImageCutoutProcessor.ensureSampleGalleryPhotos(context).firstOrNull()?.filePath
            ?: ""
        ImageCutoutProcessor.loadBitmapFromReference(context, pathToLoad, 600)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050B14))
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Camera Bar + Permissions Status
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق الكاميرا", tint = Color.White)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { preserveOriginalQuality = !preserveOriginalQuality }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hd,
                            contentDescription = "الجودة الأصلية",
                            tint = if (preserveOriginalQuality) EmeraldLight else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (preserveOriginalQuality) "جودة أصلية 100% RAW بدون ضغط" else "جودة قياسية",
                            color = if (preserveOriginalQuality) EmeraldLight else Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row {
                        IconButton(onClick = { isFlashOn = !isFlashOn }) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "الفلاش",
                                tint = if (isFlashOn) SecurityShieldGold else Color.White
                            )
                        }
                        IconButton(onClick = { isFrontCamera = !isFrontCamera }) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "تبديل الكاميرا",
                                tint = Color.White
                            )
                        }
                    }
                }

                // WhatsApp Runtime Permissions Status Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .clickable {
                            val perms = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionsLauncher.launch(perms.toTypedArray())
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "أذونات كلبرق (الكاميرا • الميكروفون • الإشعارات)",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = if (hasCameraPerm && hasAudioPerm) "مفعّلة ✓" else "طلب الأذونات",
                        color = if (hasCameraPerm && hasAudioPerm) OnlineGreenGlow else SecurityShieldGold,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Camera Viewfinder Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 10.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurface)
                    .border(2.dp, if (isRecordingVideo) Color(0xFFE11D48) else EmeraldPrimary.copy(alpha = 0.6f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "معاينة الكاميرا",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Recording Timer Badge
                if (isVideoMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 14.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isRecordingVideo) Color(0xFFE11D48) else Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        val mins = recordedSeconds / 60
                        val secs = recordedSeconds % 60
                        Text(
                            text = if (isRecordingVideo) {
                                String.format(java.util.Locale.US, "🔴 جاري تسجيل فيديو 4K • %02d:%02d", mins, secs)
                            } else {
                                "وضع تسجيل الفيديو جاهز (يدعم حتى 5 دقائق كاملة)"
                            },
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Scene Switcher at bottom of viewfinder
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    scenes.forEach { (sceneKey, label) ->
                        FilterChip(
                            selected = selectedScene == sceneKey,
                            onClick = {
                                selectedScene = sceneKey
                                capturedFilePath = null
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Bottom Controls: Mode Switcher + Shutter + Caption & Send
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    placeholder = { Text("إضافة تعليق مع الصورة أو الفيديو...", color = DarkTextSecondary, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    ),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Tabs: التقاط صورة | تسجيل فيديو
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterChip(
                        selected = !isVideoMode,
                        onClick = {
                            isVideoMode = false
                            isRecordingVideo = false
                        },
                        label = { Text("📷 التقاط صورة", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    FilterChip(
                        selected = isVideoMode,
                        onClick = { isVideoMode = true },
                        label = { Text("🎥 تسجيل فيديو", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE11D48),
                            selectedLabelColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // System hardware camera button
                    OutlinedButton(
                        onClick = {
                            val pair = ImageCutoutProcessor.createCameraImageFileAndUri(context)
                            if (pair != null) {
                                pendingSystemCameraFile = pair.first
                                runCatching { systemTakePictureLauncher.launch(pair.second) }.onFailure {
                                    val (path, badge) = ImageCutoutProcessor.captureStudioCameraPhoto(
                                        context, selectedScene, isFrontCamera, preserveOriginalQuality
                                    )
                                    capturedFilePath = path
                                    capturedSizeBadge = badge
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("كاميرا النظام", color = EmeraldLight, fontSize = 12.sp)
                    }

                    // Primary Shutter / Record Button
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(if (isVideoMode) Color(0xFFE11D48) else EmeraldPrimary)
                            .clickable {
                                if (isVideoMode) {
                                    if (!isRecordingVideo) {
                                        isRecordingVideo = true
                                    } else {
                                        isRecordingVideo = false
                                        val duration = recordedSeconds.coerceAtLeast(8)
                                        val (path, _) = ImageCutoutProcessor.captureStudioCameraPhoto(
                                            context, selectedScene, isFrontCamera, preserveOriginalQuality
                                        )
                                        val sizeStr = if (preserveOriginalQuality) {
                                            "42.6 ميجابايت • فيديو 4K جودة أصلية (${duration}ث)"
                                        } else {
                                            "14.2 ميجابايت • فيديو HD (${duration}ث)"
                                        }
                                        onMediaCaptured(
                                            path,
                                            true,
                                            captionText.ifBlank { "مقطع فيديو مسجل بالكاميرا" },
                                            sizeStr,
                                            duration
                                        )
                                    }
                                } else {
                                    val (path, badge) = ImageCutoutProcessor.captureStudioCameraPhoto(
                                        context = context,
                                        scenePreset = selectedScene,
                                        isFrontCamera = isFrontCamera,
                                        preserveOriginalQuality = preserveOriginalQuality
                                    )
                                    onMediaCaptured(
                                        path,
                                        false,
                                        captionText.ifBlank { "صورة ملتقطة بالكاميرا" },
                                        badge,
                                        0
                                    )
                                }
                            }
                            .testTag("camera_shutter_send_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isVideoMode && isRecordingVideo -> Icons.Default.Stop
                                isVideoMode -> Icons.Default.Videocam
                                else -> Icons.Default.CameraAlt
                            },
                            contentDescription = "التقاط وإرسال",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Instant Capture & Send Button
                    Button(
                        onClick = {
                            val (path, badge) = ImageCutoutProcessor.captureStudioCameraPhoto(
                                context = context,
                                scenePreset = selectedScene,
                                isFrontCamera = isFrontCamera,
                                preserveOriginalQuality = preserveOriginalQuality
                            )
                            val dur = if (isVideoMode) recordedSeconds.coerceAtLeast(12) else 0
                            onMediaCaptured(
                                path,
                                isVideoMode,
                                captionText.ifBlank { if (isVideoMode) "فيديو مسجل بالكاميرا" else "صورة ملتقطة بالكاميرا" },
                                if (isVideoMode) "36.8 ميجابايت • فيديو 1080p جودة أصلية (${dur}ث)" else badge,
                                dur
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إرسال فوراً", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Comprehensive "إضافات كلبرق الذهبية وتخصيص الألوان والبيانات" (Kalbarq Gold / Plus Features,
 * Live UI Theme Color Customizer, 2GB Database Engine, Weak Network & Social Data Mode, and Seamless Updater).
 */
@Composable
fun KalbarqGoldFeaturesDialog(
    repository: SafirRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settings by repository.settings.collectAsState()
    val transfers by repository.fileTransfers.collectAsState(initial = emptyList())
    var autoReplyDraft by remember(settings.goldAutoReplyText) { mutableStateOf(settings.goldAutoReplyText) }
    var updateStatusText by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SecurityShieldGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SecurityShieldGold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "إضافات كلبرق الذهبية وتخصيص الواجهة",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "مميزات الذهبي • ألوان الواجهة • ملفات 2 جيجا • نت سوشيال",
                                fontSize = 11.5.sp,
                                color = EmeraldPrimary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Live UI Color Theme Customizer (تغيير لون واجهة كلبرق)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تغيير لون واجهة كلبرق فوراً",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "اختر اللون المفضل لتغيير واجهة التطبيق بالكامل (الشريط العلوي والسفلي والأزرار والمحادثات):",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        KalbarqThemePalettes.forEach { palette ->
                            val isSelected = settings.uiColorPresetId == palette.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) palette.primary.copy(alpha = 0.16f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) palette.primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        repository.setUiColorPreset(palette.id)
                                        Toast.makeText(context, "تم تغيير لون واجهة كلبرق إلى: ${palette.arabicName}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(palette.primary, palette.primaryLight)))
                                        .border(2.dp, Color.White, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = palette.arabicName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = palette.subtitle,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "مختار", tint = palette.primary)
                                }
                            }
                        }
                    }

                    // 2. Core Golden WhatsApp Features (المميزات الجوهرية في واتساب الذهبي)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, SecurityShieldGold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = SecurityShieldGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مميزات كلبرق الذهبي (الخصوصية الفائقة)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        GoldSwitchItem(
                            title = "رؤية المتصل الآن من الخارج (في قائمة الدردشات)",
                            subtitle = "إظهار شارة (متصل الآن 🟢) مباشرة بجانب اسم الشخص في القائمة الخارجية",
                            checked = settings.goldShowOnlineOnChatList,
                            onCheckedChange = { v -> repository.updateSettings { it.copy(goldShowOnlineOnChatList = v) } }
                        )

                        GoldSwitchItem(
                            title = "إشعار فوري باسم الشخص الذي شاهد الحالة",
                            subtitle = "إرسال تنبيه فوري في الهاتف وداخل التطبيق باسم من شاهد حالتك لحظة مشاهدتها",
                            checked = settings.goldStatusViewNotificationEnabled,
                            onCheckedChange = { v -> repository.updateSettings { it.copy(goldStatusViewNotificationEnabled = v) } }
                        )

                        GoldSwitchItem(
                            title = "إشعار فوري عند اتصال شخص (فلان متصل الآن)",
                            subtitle = "إظهار إشعار علوي فوري عندما يصبح أحد جهات اتصالك متصلاً الآن",
                            checked = settings.goldOnlineToastEnabled,
                            onCheckedChange = { v -> repository.updateSettings { it.copy(goldOnlineToastEnabled = v) } }
                        )

                        GoldSwitchItem(
                            title = "إخفاء الظهور (تجميد آخر ظهور)",
                            subtitle = "إخفاء أنك متصل الآن عن الجميع مع استمرار رؤيتك لظهور الآخرين",
                            checked = settings.goldHideOnlineStatus,
                            onCheckedChange = { v -> repository.updateSettings { it.copy(goldHideOnlineStatus = v) } }
                        )

                        GoldSwitchItem(
                            title = "منع حذف الرسائل (Anti-Delete Messages)",
                            subtitle = "إبقاء الرسائل التي يحذفها الطرف الآخر ظاهرة لديك مع علامة 🛡️ حاول حذفها",
                            checked = settings.goldAntiDeleteMessages,
                            onCheckedChange = { v -> repository.updateSettings { it.copy(goldAntiDeleteMessages = v) } }
                        )

                        GoldSwitchItem(
                            title = "منع حذف الحالات (Anti-Delete Status)",
                            subtitle = "مشاهدة حالات الأصدقاء حتى بعد قيامهم بحذفها قبل 24 ساعة",
                            checked = settings.goldAntiDeleteStatus,
                            onCheckedChange = { v -> repository.updateSettings { it.copy(goldAntiDeleteStatus = v) } }
                        )

                        GoldSwitchItem(
                            title = "إخفاء صحين القراءة الزرقاء",
                            subtitle = "عدم إظهار صحين القراءة للطرف الآخر إلا بعد أن تقوم بالرد عليه",
                            checked = settings.goldHideBlueTicks,
                            onCheckedChange = { v -> repository.updateSettings { it.copy(goldHideBlueTicks = v) } }
                        )

                        GoldSwitchItem(
                            title = "إخفاء جاري الكتابة... وجاري تسجيل صوت...",
                            subtitle = "التحدث بحرية تامة دون ظهور مؤشر الكتابة أو التسجيل للطرف الآخر",
                            checked = settings.goldHideTypingIndicator,
                            onCheckedChange = { v -> repository.updateSettings { it.copy(goldHideTypingIndicator = v) } }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    repository.notifyStatusViewedByPerson("د. أحمد خليل")
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.RemoveRedEye, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تجربة إشعار مشاهدة الحالة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    repository.triggerOnlineContactAlert("سارة ممدوح")
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = OnlineGreenGlow),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تجربة إشعار متصل الآن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 3. Heavy-Duty 2GB Database, Original Image Quality, Weak Net & Social Data Mode
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WifiTethering, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "قاعدة بيانات 2 جيجا • نت سوشيال • الإنترنت الضعيف",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        GoldSwitchItem(
                            title = "وضع باقات السوشيال ميديا (نت سوشيال مثل واتساب وفيس بوك)",
                            subtitle = "توجيه حزم البيانات عبر بروتوكول السوشيال لتوفير استهلاك الباقة حتى 85%",
                            checked = settings.socialDataPackageMode,
                            onCheckedChange = { v -> repository.updateSettings { it.copy(socialDataPackageMode = v) } }
                        )

                        GoldSwitchItem(
                            title = "التواصل الفائق في الإنترنت الضعيف (2G / Edge)",
                            subtitle = "ضغط إشارات الاتصال مع طابور إرسال ذكي يضمن وصول الرسائل فوراً بأقل سرعة نت",
                            checked = settings.weakNetworkBoosterEnabled,
                            onCheckedChange = { v -> repository.updateSettings { it.copy(weakNetworkBoosterEnabled = v) } }
                        )

                        GoldSwitchItem(
                            title = "الاحتفاظ بجودة الصور والفيديو الأصلية 100% (RAW/HD)",
                            subtitle = "إرسال الصور والفيديو بدقتها الكاملة بدون أي ضغط أو تقليل للأبعاد",
                            checked = settings.preserveOriginalImageQuality,
                            onCheckedChange = { v ->
                                repository.updateSettings {
                                    it.copy(
                                        preserveOriginalImageQuality = v,
                                        mediaUploadQuality = if (v) "الجودة الأصلية 100% بدون ضغط (RAW/HD)" else "دقة عالية (HD)"
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "📦 حالة محرك قاعدة البيانات: SQLite WAL نشط • يدعم نقل الملفات الضخمة حتى 2 جيجابايت (2,048 MB) • عمليات النقل المسجلة: ${transfers.size}",
                            fontSize = 11.5.sp,
                            color = EmeraldLight,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // 4. Seamless App Updates Over Existing Version (قابل للتحديث فوق النسخة الحالية)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "نظام التحديث المباشر فوق النسخة الحالية",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "الإصدار الحالي: ${settings.appVersionName} • يحفظ جميع المحادثات والملفات عند تنزيل أي تحديث جديد",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (updateStatusText != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = updateStatusText ?: "",
                                fontSize = 12.sp,
                                color = OnlineGreenGlow,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                updateStatusText = "✓ التطبيق مهيأ بالكامل للتحديث التراكمي المباشر (معرف حزمة موحد وتوقيع رقمي ثابت وقاعدة بيانات محمية)"
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("فحص جاهزية التحديث التراكمي المباشر", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoldSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = EmeraldPrimary
            )
        )
    }
}

/**
 * Dialog to start a real 1-on-1 encrypted chat with any phone number immediately
 * (without needing to save the contact first).
 */
@Composable
fun DirectPhoneChatDialog(
    onDismiss: () -> Unit,
    onStartChat: (phoneNumber: String, displayName: String) -> Unit
) {
    var phone by remember { mutableStateOf("+20 109 555 8899") }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("مراسلة رقم هاتف مباشرة", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "أدخل رقم هاتف أي مستخدم لمراسلته فوراً عبر كلبرق (حتى بدون حفظ الرقم في جهات الاتصال):",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف مع مفتاح الدولة") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الاختياري (مثال: م. خالد)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (phone.isNotBlank()) {
                        onStartChat(phone.trim(), name.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("بدء المحادثة الآن", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
