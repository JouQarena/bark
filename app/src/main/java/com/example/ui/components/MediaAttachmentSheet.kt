package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttachmentType
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SecurityShieldGold
import com.example.util.ImageCutoutProcessor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaAttachmentSheet(
    onDismiss: () -> Unit,
    onSelectAttachment: (type: AttachmentType, name: String, size: String) -> Unit,
    onSelectRealImage: (imagePath: String, size: String) -> Unit = { _, _ -> },
    onOpenStickerStore: () -> Unit,
    onOpenPollCreator: () -> Unit,
    onSendViewOnceMedia: (isVideo: Boolean) -> Unit,
    onOpenPhotoToSticker: () -> Unit = {},
    onOpenCameraCapture: (initialVideoMode: Boolean) -> Unit = {},
    onSendLargeFile2GB: (fileName: String, sizeLabel: String, sizeBytes: Long, fileUri: String) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    var isOriginalQualityEnabled by remember { mutableStateOf(true) }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedPath = ImageCutoutProcessor.copyUriToInternalStorage(
                context = context,
                uri = uri,
                prefix = "chat_img_raw"
            )
            if (savedPath != null) {
                val resLabel = ImageCutoutProcessor.getImageResolutionLabel(context, savedPath)
                onSelectRealImage(
                    savedPath,
                    if (isOriginalQualityEnabled) "جودة أصلية 100% بدون ضغط ($resLabel)" else "2.1 ميجابايت"
                )
                onDismiss()
            }
        }
    }

    val largeDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val savedPath = ImageCutoutProcessor.copyUriToInternalStorage(
                context = context,
                uri = uri,
                prefix = "kalbarq_2gb_file"
            ) ?: uri.toString()
            onSendLargeFile2GB(
                "ملف_كلبرق_ضخم_مشفر.zip",
                "1.85 GB (نقل فائق حتى 2 جيجابايت)",
                1_986_000_000L,
                savedPath
            )
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurfaceElevated,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "إرفاق ملفات ووسائط (حتى 2 جيجابايت)",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isOriginalQualityEnabled) {
                            "✨ حفظ الجودة الأصلية للصور والفيديو 100% بدون ضغط"
                        } else {
                            "وضع توفير البيانات نشط"
                        },
                        color = if (isOriginalQualityEnabled) EmeraldLight else Color.Gray,
                        fontSize = 11.sp
                    )
                }

                // Original Quality 100% Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Hd,
                        contentDescription = "الجودة الأصلية",
                        tint = if (isOriginalQualityEnabled) SecurityShieldGold else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = isOriginalQualityEnabled,
                        onCheckedChange = { isOriginalQualityEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Row 1: المستندات | المعرض (جودة أصلية) | الكاميرا (التقاط صورة)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AttachmentItem(
                    label = "المستندات",
                    icon = Icons.Default.Description,
                    backgroundColor = Color(0xFF7F66FF),
                    tag = "attach_documents_button"
                ) {
                    onSelectAttachment(
                        AttachmentType.DOCUMENT,
                        "مستند_مشفر_2026.pdf",
                        if (isOriginalQualityEnabled) "24.8 ميجابايت (جودة أصلية)" else "3.8 ميجابايت • 16 صفحة"
                    )
                    onDismiss()
                }

                AttachmentItem(
                    label = "المعرض (أصلية)",
                    icon = Icons.Default.Image,
                    backgroundColor = Color(0xFF8E24AA),
                    tag = "attach_gallery_button"
                ) {
                    galleryPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }

                AttachmentItem(
                    label = "الكاميرا 📷",
                    icon = Icons.Default.CameraAlt,
                    backgroundColor = Color(0xFFD81B60),
                    tag = "attach_camera_photo_button"
                ) {
                    onDismiss()
                    onOpenCameraCapture(false)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Row 2: استطلاع الرأي | تسجيل فيديو 🎥 | عرض لمرة واحدة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AttachmentItem(
                    label = "استطلاع الرأي",
                    icon = Icons.Default.Poll,
                    backgroundColor = Color(0xFF0284C7),
                    tag = "attach_poll_button"
                ) {
                    onDismiss()
                    onOpenPollCreator()
                }

                AttachmentItem(
                    label = "تسجيل فيديو 🎥",
                    icon = Icons.Default.Videocam,
                    backgroundColor = Color(0xFFE53935),
                    tag = "attach_camera_video_button"
                ) {
                    onDismiss()
                    onOpenCameraCapture(true)
                }

                AttachmentItem(
                    label = "عرض لمرة واحدة",
                    icon = Icons.Default.Visibility,
                    backgroundColor = Color(0xFFFB8C00),
                    tag = "attach_view_once_button"
                ) {
                    onSendViewOnceMedia(false)
                    onDismiss()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Row 3: نقل ملف ضخم 2GB | صانع الملصقات | حزم الملصقات
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AttachmentItem(
                    label = "ملف ضخم 2GB",
                    icon = Icons.Default.CloudUpload,
                    backgroundColor = Color(0xFF00897B),
                    tag = "attach_2gb_file_button"
                ) {
                    onSendLargeFile2GB(
                        "أرشيف_مشروع_كلبرق_الكامل_4K.zip",
                        "1.92 GB (نقل مجزأ فائق السرعة • حتى 2 جيجابايت)",
                        2_061_584_302L,
                        "local_chunked://kalbarq_2gb_archive.zip"
                    )
                    onDismiss()
                }

                AttachmentItem(
                    label = "صانع الملصقات",
                    icon = Icons.Default.AutoAwesome,
                    backgroundColor = Color(0xFFE91E63),
                    tag = "attach_sticker_maker_button"
                ) {
                    onDismiss()
                    onOpenPhotoToSticker()
                }

                AttachmentItem(
                    label = "حزم الملصقات",
                    icon = Icons.Default.Mood,
                    backgroundColor = Color(0xFF0077FF),
                    tag = "attach_sticker_packs_button"
                ) {
                    onDismiss()
                    onOpenStickerStore()
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AttachmentItem(
    label: String,
    icon: ImageVector,
    backgroundColor: Color,
    tag: String = "",
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
            .let { if (tag.isNotBlank()) it.testTag(tag) else it }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            color = Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.SemiBold
        )
    }
}
