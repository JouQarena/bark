package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StoryRingActive
import com.example.util.ImageCutoutProcessor
import java.io.File

@Composable
fun AvatarView(
    preset: Int,
    name: String = "",
    size: Dp = 48.dp,
    isOnline: Boolean = false,
    hasActiveStory: Boolean = false,
    isGroup: Boolean = false,
    imageUri: String = "",
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (gradientColors, iconVector) = getPresetVisuals(preset, isGroup)

    val loadedBitmap = remember(imageUri) {
        if (imageUri.isNotBlank()) {
            ImageCutoutProcessor.loadBitmapFromReference(context, imageUri, maxDimension = 320)
        } else {
            null
        }
    }

    val baseModifier = modifier
        .size(size)
        .then(
            if (hasActiveStory) {
                Modifier.border(2.5.dp, StoryRingActive, CircleShape)
            } else {
                Modifier
            }
        )
        .then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        )

    Box(
        modifier = baseModifier,
        contentAlignment = Alignment.Center
    ) {
        val innerSize = if (hasActiveStory) size - 6.dp else size
        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(CircleShape)
                .background(Brush.linearGradient(gradientColors)),
            contentAlignment = Alignment.Center
        ) {
            if (loadedBitmap != null) {
                Image(
                    bitmap = loadedBitmap.asImageBitmap(),
                    contentDescription = name.ifBlank { "الصورة الشخصية" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(innerSize)
                        .clip(CircleShape)
                )
            } else if (imageUri.isNotBlank()) {
                AsyncImage(
                    model = if (imageUri.startsWith("/")) File(imageUri) else Uri.parse(imageUri),
                    contentDescription = name.ifBlank { "الصورة الشخصية" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(innerSize)
                        .clip(CircleShape)
                )
            } else if (name.isNotBlank() && !isGroup) {
                val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "ك"
                Text(
                    text = initial,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.42f).sp
                )
            } else if (iconVector != null) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = name.ifBlank { "أيقونة" },
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.52f)
                )
            } else {
                Icon(
                    imageVector = if (isGroup) Icons.Default.Groups else Icons.Default.Person,
                    contentDescription = name.ifBlank { "مستخدم" },
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.52f)
                )
            }
        }

        // Online status dot
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.3f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF0B141A))
                    .border(1.5.dp, Color(0xFF0B141A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(size * 0.22f)
                        .clip(CircleShape)
                        .background(EmeraldLight)
                )
            }
        }
    }
}

/**
 * Reusable WhatsApp-style Photo Picker Dialog for User Profile Photo or Group Icon Photo.
 * Supports picking directly from the Android System Gallery (PickVisualMedia) OR selecting a real
 * photographic image from the local device gallery folder, plus removing the current photo.
 */
@Composable
fun RealPhotoPickerSheetDialog(
    title: String,
    currentImageUri: String,
    onDismiss: () -> Unit,
    onPhotoSelected: (String) -> Unit,
    onRemovePhoto: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val samplePhotos = remember { ImageCutoutProcessor.ensureSampleGalleryPhotos(context) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageCutoutProcessor.copyUriToInternalStorage(context, uri, "avatar")
            onPhotoSelected(savedPath ?: uri.toString())
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Primary button: Open system Android Photo Picker
                Button(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "اختيار صورة من معرض الجهاز",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "أو اختر من صور المعرض المحفوظة في الجهاز:",
                    fontSize = 12.sp,
                    color = DarkTextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(165.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(samplePhotos, key = { it.id }) { photo ->
                        val bmp = remember(photo.filePath) {
                            ImageCutoutProcessor.loadBitmapFromReference(context, photo.filePath, 200)
                        }
                        val isSelected = currentImageUri == photo.filePath
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) EmeraldPrimary else Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onPhotoSelected(photo.filePath)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = photo.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(68.dp)
                                )
                            }
                        }
                    }
                }

                if (currentImageUri.isNotBlank() && onRemovePhoto != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            onRemovePhoto()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إزالة الصورة الحالية")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = DarkTextSecondary)
            }
        },
        containerColor = DarkSurfaceElevated
    )
}

private fun getPresetVisuals(preset: Int, isGroup: Boolean): Pair<List<Color>, ImageVector?> {
    if (isGroup) {
        return when (preset) {
            1 -> listOf(Color(0xFF0077FF), Color(0xFF0A3977)) to Icons.Default.Groups
            2 -> listOf(Color(0xFF00C6FF), Color(0xFF0055D4)) to Icons.Default.Diversity3
            else -> listOf(Color(0xFF5E35B1), Color(0xFF311B92)) to Icons.Default.Groups
        }
    }

    return when (preset) {
        1 -> listOf(Color(0xFF0077FF), Color(0xFF0A3977)) to Icons.Default.Person
        2 -> listOf(Color(0xFF0288D1), Color(0xFF01579B)) to Icons.Default.LocalHospital
        3 -> listOf(Color(0xFF8E24AA), Color(0xFF4A148C)) to Icons.Default.Palette
        4 -> listOf(Color(0xFFF57C00), Color(0xFFE65100)) to Icons.Default.RocketLaunch
        5 -> listOf(Color(0xFF00B4D8), Color(0xFF03045E)) to Icons.Default.Lock
        6 -> listOf(Color(0xFFD81B60), Color(0xFF880E4F)) to Icons.Default.Favorite
        7 -> listOf(Color(0xFF2563EB), Color(0xFF1E3A8A)) to Icons.Default.Spa
        else -> listOf(Color(0xFF3949AB), Color(0xFF1A237E)) to Icons.Default.Face
    }
}
