package com.example.ui.screens

import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.model.StickerPack
import com.example.data.repository.SafirRepository
import com.example.ui.components.PhotoToStickerSheet
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SecurityShieldGold
import com.example.util.ImageCutoutProcessor

@Composable
fun StickerStoreScreen(
    repository: SafirRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val stickerPacks by repository.stickerPacks.collectAsState(initial = emptyList())
    var showCreatePackModal by remember { mutableStateOf(false) }
    var showPhotoToStickerSheet by remember { mutableStateOf(false) }

    val externalAppsCatalog = remember {
        listOf(
            Triple("حزمة ملصقات ردود الفعل المصورة", "Sticker Maker", listOf("photo_portrait_1", "photo_cat_pet", "photo_coffee_cup", "photo_trophy_cup")),
            Triple("حزمة القهوة والصباح (Sticker.ly)", "Sticker.ly Pack", listOf("photo_coffee_cup", "photo_red_rose", "photo_sunset_nature", "photo_cat_pet")),
            Triple("حزمة العمل والمبرمجين (WStickers)", "WStickers Studio", listOf("photo_tech_setup", "photo_group_team", "photo_trophy_cup", "photo_portrait_1"))
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "الملصقات وحزم التطبيقات الخارجية",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { showPhotoToStickerSheet = true },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تحويل صورة لملصق", fontSize = 11.5.sp, color = Color.White)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top Action Cards: Create Pack from Gallery & Import from Sticker Maker
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showPhotoToStickerSheet = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("صورة من المعرض لملصق", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = { showCreatePackModal = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إنشاء حزمة من الصور", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // External Sticker Maker Import Section
                Text(
                    text = "إضافة حزم ملصقات من تطبيقات خارجية (Sticker Maker)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val samples = remember { ImageCutoutProcessor.ensureSampleGalleryPhotos(context) }

                externalAppsCatalog.forEach { (packTitle, appAuthor, photoIds) ->
                    val alreadyAdded = stickerPacks.any { it.name == packTitle }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = packTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "المصدر: $appAuthor • ملصقات مصورة مفرغة الخلفية",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                val paths = photoIds.mapNotNull { id ->
                                    samples.find { it.id == id }?.filePath
                                }
                                repository.importExternalStickerPack(
                                    name = packTitle,
                                    author = appAuthor,
                                    stickerTitlesOrUris = paths
                                )
                                Toast.makeText(context, "تمت إضافة $packTitle إلى واتساب كلبرق", Toast.LENGTH_SHORT).show()
                            },
                            enabled = !alreadyAdded,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (alreadyAdded) "مُضافة ✓" else "إضافة للكلبرق", fontSize = 11.5.sp, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "حزم الملصقات المثبتة والمفضلة",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            items(stickerPacks, key = { it.id }) { pack ->
                StickerPackRow(
                    pack = pack,
                    onToggleInstall = {
                        if (pack.id != "pack_favorites") {
                            repository.setStickerPackInstalled(pack.id, !pack.isInstalled)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showCreatePackModal) {
        CreateStickerPackFromPhotosDialog(
            onDismiss = { showCreatePackModal = false },
            onCreate = { name, author, selectedImagePaths ->
                repository.importExternalStickerPack(
                    name = name,
                    author = author,
                    stickerTitlesOrUris = selectedImagePaths
                )
                showCreatePackModal = false
            }
        )
    }

    if (showPhotoToStickerSheet) {
        PhotoToStickerSheet(
            repository = repository,
            onDismiss = { showPhotoToStickerSheet = false },
            onStickerCreated = {
                showPhotoToStickerSheet = false
            }
        )
    }
}

@Composable
fun StickerPackRow(
    pack: StickerPack,
    onToggleInstall: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                val thumbBmp = remember(pack.thumbnailUri) {
                    if (pack.thumbnailUri.isNotBlank()) {
                        ImageCutoutProcessor.loadBitmapFromReference(context, pack.thumbnailUri, 120)
                    } else null
                }
                if (thumbBmp != null) {
                    Image(
                        bitmap = thumbBmp.asImageBitmap(),
                        contentDescription = pack.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = SecurityShieldGold,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pack.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${pack.author} • ${pack.stickers.size} ملصقات مصورة",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (pack.id == "pack_favorites") {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("المفضلة ⭐", fontSize = 11.sp, color = EmeraldLight, fontWeight = FontWeight.Bold)
                }
            } else if (pack.isInstalled) {
                OutlinedButton(
                    onClick = onToggleInstall,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350))
                ) {
                    Text("إزالة", fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = onToggleInstall,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تنزيل", fontSize = 12.sp, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Real Cutout Sticker Images Preview Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(pack.stickers.take(8), key = { it.id }) { sticker ->
                val ref = sticker.imageUri.ifBlank { sticker.symbol }
                val bmp = remember(ref) {
                    ImageCutoutProcessor.loadBitmapFromReference(context, ref, 140)
                }
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = sticker.label,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateStickerPackFromPhotosDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, author: String, selectedImagePaths: List<String>) -> Unit
) {
    val context = LocalContext.current
    var packName by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("Sticker Maker") }

    val galleryPhotos = remember { ImageCutoutProcessor.ensureSampleGalleryPhotos(context) }
    val selectedPaths = remember {
        mutableStateListOf<String>().apply {
            addAll(galleryPhotos.take(4).map { it.filePath })
        }
    }

    val multiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 8)
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val copied = ImageCutoutProcessor.copyUriToInternalStorage(context, uri, "pack_img")
            if (copied != null && !selectedPaths.contains(copied)) {
                selectedPaths.add(copied)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("إنشاء حزمة ملصقات من صور المعرض", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = packName,
                    onValueChange = { packName = it },
                    placeholder = { Text("اسم حزمة الملصقات", color = DarkTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    placeholder = { Text("اسم مصمم الحزمة / التطبيق", color = DarkTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        multiLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldLight)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة صور من معرض الجهاز", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "الصور المحددة لتحويلها وعزل خلفيتها (${selectedPaths.size}):",
                    fontSize = 12.sp,
                    color = DarkTextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(galleryPhotos, key = { it.id }) { photo ->
                        val isChosen = selectedPaths.contains(photo.filePath)
                        val bmp = remember(photo.filePath) {
                            ImageCutoutProcessor.loadBitmapFromReference(context, photo.filePath, 120)
                        }
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isChosen) 2.5.dp else 1.dp,
                                    color = if (isChosen) EmeraldPrimary else DarkBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    if (isChosen) selectedPaths.remove(photo.filePath)
                                    else selectedPaths.add(photo.filePath)
                                }
                        ) {
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = photo.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = packName.ifBlank { "حزمتي المفضلة" }
                    val finalPaths = if (selectedPaths.isNotEmpty()) selectedPaths.toList() else galleryPhotos.take(4).map { it.filePath }
                    onCreate(finalName, author, finalPaths)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("تحويل وحفظ الحزمة", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = DarkTextSecondary)
            }
        },
        containerColor = DarkSurfaceElevated
    )
}
