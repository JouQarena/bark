package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.SafirRepository
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SecurityShieldGold
import com.example.util.ImageCutoutProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoToStickerSheet(
    repository: SafirRepository,
    onDismiss: () -> Unit,
    onStickerCreated: (stickerImageUri: String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val gallerySamples = remember { ImageCutoutProcessor.ensureSampleGalleryPhotos(context) }
    var selectedImagePath by remember {
        mutableStateOf(gallerySamples.firstOrNull()?.filePath ?: "")
    }
    var stickerTitle by remember { mutableStateOf("ملصق من المعرض") }

    // User confirmed controls: Auto background removal + manual crop/zoom + border control
    var removeBackground by remember { mutableStateOf(true) }
    var borderWidthPx by remember { mutableFloatStateOf(10f) }
    var selectedBorderColor by remember { mutableIntStateOf(android.graphics.Color.WHITE) }
    var cropZoom by remember { mutableFloatStateOf(1.05f) }
    var cropOffsetX by remember { mutableFloatStateOf(0f) }
    var cropOffsetY by remember { mutableFloatStateOf(0f) }

    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Single photo picker from Android system gallery
    val singlePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val copied = ImageCutoutProcessor.copyUriToInternalStorage(context, uri, "sticker_src")
            if (copied != null) {
                selectedImagePath = copied
            } else {
                selectedImagePath = uri.toString()
            }
        }
    }

    // Multiple photo picker for importing an external Sticker Maker pack from gallery
    val multiPhotoPackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 8)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val savedPaths = uris.mapNotNull { uri ->
                val rawBmp = ImageCutoutProcessor.loadBitmapFromReference(context, uri.toString(), 360)
                if (rawBmp != null) {
                    val cutout = ImageCutoutProcessor.processPhotoToStickerBitmap(
                        source = rawBmp,
                        removeBackground = true,
                        cropZoom = 1.05f,
                        borderWidthPx = 10,
                        borderColor = android.graphics.Color.WHITE
                    )
                    ImageCutoutProcessor.saveStickerBitmapToInternalStorage(context, cutout)
                } else null
            }
            if (savedPaths.isNotEmpty()) {
                repository.importExternalStickerPack(
                    name = "حزمة Sticker Maker (${savedPaths.size} ملصقات)",
                    author = "Sticker Maker",
                    stickerTitlesOrUris = savedPaths
                )
                Toast.makeText(context, "تم استيراد حزمة الملصقات من المعرض بنجاح", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        }
    }

    // Recompute real Bitmap cutout whenever image or parameters change
    LaunchedEffect(selectedImagePath, removeBackground, borderWidthPx, selectedBorderColor, cropZoom, cropOffsetX, cropOffsetY) {
        if (selectedImagePath.isBlank()) return@LaunchedEffect
        isProcessing = true
        val result = withContext(Dispatchers.Default) {
            val src = ImageCutoutProcessor.loadBitmapFromReference(context, selectedImagePath, 420)
            if (src != null) {
                ImageCutoutProcessor.processPhotoToStickerBitmap(
                    source = src,
                    removeBackground = removeBackground,
                    cropZoom = cropZoom,
                    cropOffsetX = cropOffsetX,
                    cropOffsetY = cropOffsetY,
                    borderWidthPx = borderWidthPx.toInt(),
                    borderColor = selectedBorderColor
                )
            } else null
        }
        processedBitmap = result
        isProcessing = false
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
                .padding(horizontal = 18.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = SecurityShieldGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "تحويل الصور إلى ملصقات",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "عزل الخلفية تلقائياً مع التحكم بالحدود والقص اليدوي",
                        fontSize = 11.5.sp,
                        color = DarkTextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Button to pick real photo from Android System Gallery
            Button(
                onClick = {
                    singlePhotoLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("pick_gallery_photo_for_sticker"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "اختيار صورة من معرض الجهاز",
                    fontWeight = FontWeight.Bold,
                    color = EmeraldLight,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Cutout Preview Canvas with Checkerboard Transparency Grid
            Box(
                modifier = Modifier
                    .size(185.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.dp, EmeraldPrimary, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Checkerboard background to show real transparency
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cellSize = 14.dp.toPx()
                    val cols = (size.width / cellSize).toInt() + 1
                    val rows = (size.height / cellSize).toInt() + 1
                    for (r in 0 until rows) {
                        for (c in 0 until cols) {
                            val isEven = (r + c) % 2 == 0
                            drawRect(
                                color = if (isEven) Color(0xFF1B2832) else Color(0xFF121C24),
                                topLeft = Offset(c * cellSize, r * cellSize),
                                size = Size(cellSize, cellSize)
                            )
                        }
                    }
                }

                if (isProcessing) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = EmeraldPrimary, modifier = Modifier.size(34.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("جاري عزل الخلفية...", fontSize = 11.sp, color = EmeraldLight)
                    }
                } else {
                    val bmp = processedBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "معاينة الملصق المفرغ",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(156.dp)
                                .padding(6.dp)
                        )
                    }
                }

                // Status badge at bottom of preview
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = if (removeBackground) Icons.Default.ContentCut else Icons.Default.Crop,
                        contentDescription = null,
                        tint = EmeraldLight,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (removeBackground) "تم عزل الخلفية تلقائياً ✓" else "قص يدوي مع الإطار",
                        fontSize = 10.sp,
                        color = EmeraldLight,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal Strip of Real Photographic Images from Device Storage
            Text(
                text = "صور المعرض المتاحة للتحويل الفوري:",
                fontSize = 12.sp,
                color = DarkTextSecondary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(gallerySamples, key = { it.id }) { sample ->
                    val thumb = remember(sample.filePath) {
                        ImageCutoutProcessor.loadBitmapFromReference(context, sample.filePath, 140)
                    }
                    val isSelected = selectedImagePath == sample.filePath
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) EmeraldPrimary else DarkBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedImagePath = sample.filePath
                                stickerTitle = sample.title
                            }
                    ) {
                        if (thumb != null) {
                            Image(
                                bitmap = thumb.asImageBitmap(),
                                contentDescription = sample.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Control 1: Auto Background Cutout Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.22f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "عزل الخلفية تلقائياً",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "تفريغ خلفية الصورة وإبقاء العنصر الأساسي فقط",
                        fontSize = 11.sp,
                        color = DarkTextSecondary
                    )
                }
                Switch(
                    checked = removeBackground,
                    onCheckedChange = { removeBackground = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Control 2: Border Stroke Width & Color
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.22f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سُمك ولون حدود الملصق: ${borderWidthPx.toInt()}px",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val borderColors = listOf(
                            android.graphics.Color.WHITE to Color.White,
                            android.graphics.Color.rgb(255, 210, 121) to SecurityShieldGold,
                            android.graphics.Color.rgb(0, 168, 132) to EmeraldPrimary,
                            android.graphics.Color.rgb(233, 30, 99) to Color(0xFFE91E63)
                        )
                        borderColors.forEach { (intColor, composeColor) ->
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(composeColor)
                                    .border(
                                        width = if (selectedBorderColor == intColor) 2.dp else 0.dp,
                                        color = Color.Black,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedBorderColor = intColor }
                            )
                        }
                    }
                }
                Slider(
                    value = borderWidthPx,
                    onValueChange = { borderWidthPx = it },
                    valueRange = 0f..16f,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldPrimary,
                        activeTrackColor = EmeraldPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Control 3: Manual Crop & Zoom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.22f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "القص اليدوي والتقريب (Crop & Zoom)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${String.format("%.1f", cropZoom)}x",
                        fontSize = 12.sp,
                        color = EmeraldLight,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = cropZoom,
                    onValueChange = { cropZoom = it },
                    valueRange = 1.0f..2.2f,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldPrimary,
                        activeTrackColor = EmeraldPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = stickerTitle,
                onValueChange = { stickerTitle = it },
                label = { Text("تسمية الملصق في المفضلة", color = DarkTextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = EmeraldPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Save to Favorites & Send Button
            Button(
                onClick = {
                    val bmpToSave = processedBitmap
                        ?: ImageCutoutProcessor.loadBitmapFromReference(context, selectedImagePath, 360)
                    if (bmpToSave != null) {
                        val savedUri = ImageCutoutProcessor.saveStickerBitmapToInternalStorage(context, bmpToSave)
                        repository.addPhotoSticker(
                            title = stickerTitle,
                            imageUri = savedUri,
                            borderWidthPx = borderWidthPx.toInt(),
                            cropZoom = cropZoom,
                            isCutout = removeBackground
                        )
                        Toast.makeText(context, "تم حفظ الملصق المصور في المفضلة ⭐", Toast.LENGTH_SHORT).show()
                        onStickerCreated(savedUri)
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_photo_sticker_button"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("حفظ في الملصقات المفضلة وإرسال", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Import external Sticker Maker pack
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        multiPhotoPackLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldLight)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حزمة من المعرض", fontSize = 11.5.sp)
                }

                OutlinedButton(
                    onClick = {
                        val sampleUris = gallerySamples.take(6).map { it.filePath }
                        repository.importExternalStickerPack(
                            name = "حزمة Sticker Maker الاحترافية",
                            author = "Sticker Maker",
                            stickerTitlesOrUris = sampleUris
                        )
                        Toast.makeText(context, "تمت إضافة حزمة Sticker Maker إلى تطبيق كلبرق بنجاح!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("import_external_sticker_pack_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldLight)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة حزمة Sticker Maker", fontSize = 11.5.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
