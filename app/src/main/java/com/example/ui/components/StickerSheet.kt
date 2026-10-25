package com.example.ui.components

import android.net.Uri
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Sticker
import com.example.data.model.StickerPack
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SecurityShieldGold
import com.example.util.ImageCutoutProcessor
import java.io.File

@Composable
fun StickerSheet(
    packs: List<StickerPack>,
    onSelectSticker: (Sticker) -> Unit,
    onOpenStore: () -> Unit,
    onOpenPhotoToSticker: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val installedPacks = remember(packs) { packs.filter { it.isInstalled } }
    var selectedPackIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(DarkSurfaceElevated)
    ) {
        // Tab Row with Favorites, Installed Packs, Photo-to-Sticker Maker & Store button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (installedPacks.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = selectedPackIndex.coerceIn(0, (installedPacks.size - 1).coerceAtLeast(0)),
                    modifier = Modifier.weight(1f),
                    containerColor = DarkSurface,
                    contentColor = EmeraldPrimary,
                    edgePadding = 4.dp
                ) {
                    installedPacks.forEachIndexed { index, pack ->
                        Tab(
                            selected = selectedPackIndex == index,
                            onClick = { selectedPackIndex = index },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (pack.id == "pack_favorites") {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = SecurityShieldGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        val thumbBmp = remember(pack.thumbnailUri) {
                                            if (pack.thumbnailUri.isNotBlank()) {
                                                ImageCutoutProcessor.loadBitmapFromReference(context, pack.thumbnailUri, 64)
                                            } else null
                                        }
                                        if (thumbBmp != null) {
                                            Image(
                                                bitmap = thumbBmp.asImageBitmap(),
                                                contentDescription = pack.name,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = pack.name,
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedPackIndex == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedPackIndex == index) EmeraldPrimary else Color.White.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        )
                    }
                }
            } else {
                Text(
                    text = "لا توجد حزم ملصقات مثبتة",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                )
            }

            // Create Sticker from Gallery Photo button
            IconButton(
                onClick = onOpenPhotoToSticker,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "إنشاء ملصق من المعرض",
                    tint = EmeraldLight,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Sticker Packs Store / External Packs button
            IconButton(
                onClick = onOpenStore,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "إضافة حزم ملصقات",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Active Pack Stickers Grid
        val activePack = installedPacks.getOrNull(selectedPackIndex)
        if (activePack != null) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // First tile in Favorites: "إنشاء ملصق" button like WhatsApp
                item {
                    Column(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(EmeraldPrimary.copy(alpha = 0.16f))
                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .clickable { onOpenPhotoToSticker() }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "إنشاء ملصق",
                            tint = EmeraldLight,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "إنشاء",
                            fontSize = 11.sp,
                            color = EmeraldLight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(activePack.stickers, key = { it.id }) { sticker ->
                    val ref = sticker.imageUri.ifBlank { sticker.symbol }
                    val bmp = remember(ref) {
                        ImageCutoutProcessor.loadBitmapFromReference(context, ref, 180)
                    }
                    Column(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurface.copy(alpha = 0.55f))
                            .clickable { onSelectSticker(sticker) }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = sticker.label,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(52.dp)
                            )
                        } else if (ref.startsWith("/") || ref.startsWith("content://") || ref.startsWith("file://")) {
                            AsyncImage(
                                model = if (ref.startsWith("/")) File(ref) else Uri.parse(ref),
                                contentDescription = sticker.label,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(52.dp)
                            )
                        } else {
                            Text(
                                text = sticker.label.take(8),
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
