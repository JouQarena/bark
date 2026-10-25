package com.example.ui.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.StatusStory
import com.example.data.model.StoryType
import com.example.data.repository.SafirRepository
import com.example.ui.components.AvatarView
import com.example.ui.components.WhatsAppCameraCaptureDialog
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SecurityShieldGold
import com.example.util.ImageCutoutProcessor
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatusScreen(
    repository: SafirRepository,
    onOpenStatusViewer: (String) -> Unit,
    onOpenChannels: () -> Unit = {},
    externalCreateTrigger: Int = 0
) {
    val stories by repository.stories.collectAsState(initial = emptyList())
    val channels by repository.channels.collectAsState(initial = emptyList())
    val currentUser by repository.currentUser.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var initialCreatorTab by remember { mutableIntStateOf(0) } // 0: صورة من المعرض, 1: فيديو (حتى 5 دقائق), 2: نصية
    var selectedStoryForViewer by remember { mutableStateOf<StatusStory?>(null) }
    var showCameraForStatus by remember { mutableStateOf(false) }
    var showViewersDialog by remember { mutableStateOf(false) }

    val myLatestStory = remember(stories, currentUser) {
        stories.firstOrNull { it.userId == (currentUser?.id ?: "current_user_id") } ?: stories.firstOrNull()
    }

    LaunchedEffect(externalCreateTrigger) {
        if (externalCreateTrigger > 0) {
            initialCreatorTab = 0
            showCreateDialog = true
        }
    }

    val recentStories = remember(stories) { stories.filter { !it.isViewed } }
    val viewedStories = remember(stories) { stories.filter { it.isViewed } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Section Header: الحالة
        item {
            Text(
                text = "الحالة",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }

        // My Status Item
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        initialCreatorTab = 0
                        showCreateDialog = true
                    }
                    .padding(vertical = 10.dp)
                    .testTag("my_status_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    AvatarView(
                        preset = currentUser?.avatarPreset ?: 1,
                        name = currentUser?.displayName ?: "أنا",
                        size = 56.dp,
                        imageUri = currentUser?.avatarUri ?: ""
                    )
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "إضافة حالة",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "حالتي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "انقر لإضافة صورة أو فيديو (حتى 5 دقائق) أو نص إلى حالتك",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quick action buttons: Camera Capture vs Media/Gallery vs Text
                IconButton(
                    onClick = { showCameraForStatus = true },
                    modifier = Modifier.testTag("capture_camera_status_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "التقاط صورة أو فيديو بالكاميرا للحالة",
                        tint = EmeraldPrimary
                    )
                }

                IconButton(
                    onClick = {
                        initialCreatorTab = 0
                        showCreateDialog = true
                    },
                    modifier = Modifier.testTag("add_media_status_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "إضافة صورة أو فيديو للحالة من المعرض",
                        tint = EmeraldPrimary
                    )
                }

                IconButton(
                    onClick = {
                        initialCreatorTab = 2
                        showCreateDialog = true
                    },
                    modifier = Modifier.testTag("add_text_status_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "كتابة حالة نصية",
                        tint = EmeraldPrimary
                    )
                }
            }

            // Status Viewer Notification Card ("إشعار باسم الشخص الذي شاهد الحالة")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(EmeraldPrimary.copy(alpha = 0.12f))
                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .clickable { showViewersDialog = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("status_viewers_notification_card"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.RemoveRedEye,
                        contentDescription = "مشاهدو الحالة",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "إشعار فوري باسم من شاهد حالتك 👁️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val viewersPreview = myLatestStory?.viewersList?.joinToString("، ") ?: "سارة أحمد، م. طارق المنصور"
                        Text(
                            text = "شاهدها: $viewersPreview",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Button(
                    onClick = {
                        val targetStoryId = myLatestStory?.id ?: stories.firstOrNull()?.id ?: "story_1"
                        val sampleViewers = listOf(
                            "سارة أحمد (مديرة التصميم)",
                            "م. طارق المنصور",
                            "د. ليلى الهاشمي",
                            "عمر الفاروق"
                        )
                        val viewer = sampleViewers.random()
                        repository.notifyStatusViewedByPerson(viewerName = viewer, storyId = targetStoryId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("trigger_status_viewer_notification_btn")
                ) {
                    Text("تجربة الإشعار 🔔", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Recent Stories Section
        if (recentStories.isNotEmpty()) {
            item {
                Text(
                    text = "التحديثات الحديثة",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            items(recentStories, key = { it.id }) { story ->
                StoryListItem(
                    story = story,
                    onClick = {
                        repository.markStoryViewed(story.id)
                        selectedStoryForViewer = story
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }
        }

        // Viewed Stories Section
        if (viewedStories.isNotEmpty()) {
            item {
                Text(
                    text = "التحديثات التي تمت مشاهدتها",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            items(viewedStories, key = { it.id }) { story ->
                StoryListItem(
                    story = story,
                    onClick = { selectedStoryForViewer = story }
                )
            }
        }

        // WhatsApp Updates Tab Channels Section ("القنوات")
        item {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "القنوات",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تابع أهم المواضيع التي تهمك في كلبرق",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onOpenChannels) {
                    Text("استكشاف", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        items(channels, key = { it.id }) { channel ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenChannels() }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarView(
                    preset = channel.iconPreset,
                    name = channel.name,
                    size = 48.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = channel.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (channel.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "موثقة",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = channel.posts.firstOrNull()?.text ?: channel.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onOpenChannels,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        text = "متابعة",
                        color = EmeraldPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    // Story Creator Dialog (Image, Video up to 5 minutes, or Text)
    if (showCreateDialog) {
        CreateStoryDialog(
            initialTab = initialCreatorTab,
            onDismiss = { showCreateDialog = false },
            onPost = { text, colorHex, storyType, mediaUri, videoDuration, trimStart, trimEnd ->
                repository.postStory(
                    textContent = text,
                    backgroundColorHex = colorHex,
                    storyType = storyType,
                    mediaUri = mediaUri,
                    videoDurationSeconds = videoDuration,
                    trimStartSeconds = trimStart,
                    trimEndSeconds = trimEnd
                )
                showCreateDialog = false
            }
        )
    }

    // Fullscreen Story Viewer Modal
    val activeStory = selectedStoryForViewer
    if (activeStory != null) {
        StoryViewerModal(
            story = activeStory,
            onClose = { selectedStoryForViewer = null }
        )
    }

    // Direct Camera Capture for Status (Photo or up to 5-min Video)
    if (showCameraForStatus) {
        WhatsAppCameraCaptureDialog(
            initialVideoMode = false,
            preserveOriginalQualityDefault = true,
            onDismiss = { showCameraForStatus = false },
            onMediaCaptured = { filePath, isVideo, caption, _, durationSeconds ->
                showCameraForStatus = false
                repository.postStory(
                    textContent = caption.ifBlank {
                        if (isVideo) "فيديو حالة بالكاميرا (${formatDurationMmSs(durationSeconds)}) 🎥"
                        else "صورة حالة ملتقطة بالكاميرا بجودة أصلية 📷"
                    },
                    backgroundColorHex = "#0055D4",
                    storyType = if (isVideo) StoryType.VIDEO else StoryType.IMAGE,
                    mediaUri = filePath,
                    videoDurationSeconds = durationSeconds,
                    trimStartSeconds = 0,
                    trimEndSeconds = durationSeconds
                )
            }
        )
    }

    // Status Viewers List & Notification Dialog
    if (showViewersDialog) {
        val viewers = myLatestStory?.viewersList ?: listOf("سارة أحمد (مديرة التصميم)", "م. طارق المنصور", "د. ليلى الهاشمي")
        AlertDialog(
            onDismissRequest = { showViewersDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RemoveRedEye, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "الأشخاص الذين شاهدوا حالتك (${viewers.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "🔔 ميزة كلبرق الذهبي مفعّلة: يصلك إشعار فوري باسم الشخص لحظة مشاهدته لحالتك.",
                        fontSize = 12.sp,
                        color = EmeraldLight,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    viewers.forEach { viewerName ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AvatarView(preset = 1, name = viewerName, size = 38.dp, isOnline = true)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(viewerName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("شاهد الحالة اليوم • متصل الآن 🟢", color = DarkTextSecondary, fontSize = 11.sp)
                                }
                            }
                            TextButton(
                                onClick = {
                                    val targetId = myLatestStory?.id ?: "story_1"
                                    repository.notifyStatusViewedByPerson(viewerName = viewerName, storyId = targetId)
                                }
                            ) {
                                Text("إشعار 🔔", color = EmeraldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showViewersDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("إغلاق", color = Color.White)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}

@Composable
fun StoryListItem(
    story: StatusStory,
    onClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale("ar")) }
    val formattedTime = remember(story.timestamp) { timeFormat.format(Date(story.timestamp)) }

    val typeBadge = when (story.storyType) {
        StoryType.IMAGE -> "📷 صورة"
        StoryType.VIDEO -> {
            val activeDuration = (story.trimEndSeconds - story.trimStartSeconds)
                .takeIf { it > 0 } ?: story.videoDurationSeconds
            "🎬 فيديو (${formatDurationMmSs(activeDuration)})"
        }
        StoryType.VOICE -> "🎤 صوتية"
        StoryType.TEXT -> "✍️ نص"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarView(
            preset = story.userAvatarPreset,
            name = story.userName,
            size = 52.dp,
            hasActiveStory = !story.isViewed,
            imageUri = story.userAvatarUri
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = story.userName,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$typeBadge • اليوم في $formattedTime",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryDialog(
    initialTab: Int = 0,
    onDismiss: () -> Unit,
    onPost: (
        text: String,
        colorHex: String,
        storyType: StoryType,
        mediaUri: String,
        videoDurationSeconds: Int,
        trimStartSeconds: Int,
        trimEndSeconds: Int
    ) -> Unit
) {
    val context = LocalContext.current
    val samplePhotos = remember { ImageCutoutProcessor.ensureSampleGalleryPhotos(context) }

    var selectedTab by remember { mutableIntStateOf(initialTab) } // 0: صورة, 1: فيديو (حتى 5 دقائق), 2: نص
    var storyText by remember { mutableStateOf("") }
    val colors = listOf("#0055D4", "#0A3977", "#1F4068", "#0284C7", "#4A148C", "#C2185B")
    var selectedColor by remember { mutableStateOf(colors[0]) }

    // Image state
    var selectedImagePath by remember { mutableStateOf(samplePhotos.firstOrNull()?.filePath ?: "") }

    // Video state (up to 5 minutes = 300 seconds)
    var selectedVideoUri by remember { mutableStateOf("sample_video_4m30s.mp4") }
    var rawVideoDurationSeconds by remember { mutableIntStateOf(270) } // 4 min 30 sec default sample
    var showVideoTrimmer by remember { mutableStateOf(false) }
    var trimRange by remember { mutableStateOf(0f..270f) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val saved = ImageCutoutProcessor.copyUriToInternalStorage(context, uri, "status_img")
            if (saved != null) {
                selectedImagePath = saved
                selectedTab = 0
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val durationSec = ImageCutoutProcessor.getVideoDurationSeconds(context, uri.toString())
                .takeIf { it > 0 } ?: 180
            val saved = ImageCutoutProcessor.copyUriToInternalStorage(context, uri, "status_vid") ?: uri.toString()
            selectedVideoUri = saved
            rawVideoDurationSeconds = durationSec
            val clampedEnd = durationSec.coerceAtMost(ImageCutoutProcessor.MAX_STATUS_VIDEO_SECONDS)
            trimRange = 0f..clampedEnd.toFloat()
            if (durationSec > ImageCutoutProcessor.MAX_STATUS_VIDEO_SECONDS) {
                showVideoTrimmer = true
                Toast.makeText(
                    context,
                    "مدة الفيديو أطول من 5 دقائق؛ تم ضبط أداة القص على أول 5 دقائق (300 ثانية)",
                    Toast.LENGTH_LONG
                ).show()
            }
            selectedTab = 1
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إضافة تحديث إلى حالتي",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Mode selector chips: صورة من المعرض | فيديو (حتى 5 دقائق) | نص
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("صورة", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.25f),
                            selectedLabelColor = EmeraldLight
                        ),
                        modifier = Modifier.testTag("status_tab_image")
                    )

                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("فيديو (5 د)", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.25f),
                            selectedLabelColor = EmeraldLight
                        ),
                        modifier = Modifier.testTag("status_tab_video")
                    )

                    FilterChip(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        label = { Text("نصية", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.25f),
                            selectedLabelColor = EmeraldLight
                        ),
                        modifier = Modifier.testTag("status_tab_text")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> {
                        // IMAGE STATUS TAB
                        Button(
                            onClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pick_status_image_gallery"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اختيار صورة من المعرض", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sample gallery images row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            samplePhotos.forEach { sample ->
                                val path = sample.filePath
                                val label = sample.title
                                val bmp = remember(path) { BitmapFactory.decodeFile(path) }
                                val isSelected = selectedImagePath == path
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) EmeraldPrimary else DarkBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedImagePath = path }
                                ) {
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = label,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Preview of selected image
                        val previewBmp = remember(selectedImagePath) {
                            if (selectedImagePath.isNotBlank() && File(selectedImagePath).exists()) {
                                BitmapFactory.decodeFile(selectedImagePath)
                            } else null
                        }
                        if (previewBmp != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(165.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = previewBmp.asImageBitmap(),
                                    contentDescription = "معاينة صورة الحالة",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    1 -> {
                        // VIDEO STATUS TAB (UP TO 5 MINUTES = 300 SECONDS)
                        Button(
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pick_status_video_gallery"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اختيار فيديو من المعرض (حتى 5 دقائق)", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Preset video durations for testing up to 5 minutes (300s)
                        val sampleVideos = listOf(
                            Triple("video_1m20s.mp4", "فيديو 01:20", 80),
                            Triple("video_3m45s.mp4", "فيديو 03:45", 225),
                            Triple("video_5m00s.mp4", "فيديو كامل 05:00", 300)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sampleVideos.forEach { (uri, title, durSec) ->
                                FilterChip(
                                    selected = rawVideoDurationSeconds == durSec,
                                    onClick = {
                                        selectedVideoUri = uri
                                        rawVideoDurationSeconds = durSec
                                        trimRange = 0f..durSec.toFloat()
                                    },
                                    label = { Text(title, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val activeDurationSec = (trimRange.endInclusive.toInt() - trimRange.start.toInt()).coerceIn(1, 300)

                        // Video preview box with duration & optional trimmer button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(145.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                                    )
                                )
                                .padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "تشغيل الفيديو كاملاً مباشرة (${formatDurationMmSs(activeDurationSec)} / 05:00 كحد أقصى)",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .clickable { showVideoTrimmer = !showVideoTrimmer }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ContentCut, contentDescription = "قص الفيديو", tint = EmeraldLight, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (showVideoTrimmer) "إخفاء القص" else "أداة قص اختيارية",
                                    color = EmeraldLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (showVideoTrimmer) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.25f))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "البداية: ${formatDurationMmSs(trimRange.start.toInt())}",
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "المدة: ${formatDurationMmSs(activeDurationSec)}",
                                        color = EmeraldLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "النهاية: ${formatDurationMmSs(trimRange.endInclusive.toInt())}",
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                }
                                RangeSlider(
                                    value = trimRange,
                                    onValueChange = { range ->
                                        val maxSpan = ImageCutoutProcessor.MAX_STATUS_VIDEO_SECONDS.toFloat()
                                        val end = if (range.endInclusive - range.start > maxSpan) {
                                            range.start + maxSpan
                                        } else {
                                            range.endInclusive
                                        }
                                        trimRange = range.start..end
                                    },
                                    valueRange = 0f..rawVideoDurationSeconds.coerceAtLeast(10).toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = EmeraldPrimary,
                                        activeTrackColor = EmeraldPrimary
                                    )
                                )
                            }
                        }
                    }

                    2 -> {
                        // TEXT STATUS TAB
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            colors.forEach { hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (selectedColor == hex) 2.dp else 0.dp,
                                            color = Color.White,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = hex }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val bg = Color(android.graphics.Color.parseColor(selectedColor))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = storyText.ifBlank { "اكتب حالة نصية..." },
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = storyText,
                    onValueChange = { storyText = it },
                    placeholder = {
                        Text(
                            text = if (selectedTab == 2) "اكتب نص الحالة..." else "أضف شرحاً أو تعليقاً...",
                            color = DarkTextSecondary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("status_caption_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (selectedTab) {
                        0 -> {
                            onPost(
                                storyText.ifBlank { "صورة جديدة في حالتي 📷" },
                                selectedColor,
                                StoryType.IMAGE,
                                selectedImagePath,
                                0,
                                0,
                                0
                            )
                        }
                        1 -> {
                            val startSec = trimRange.start.toInt()
                            val endSec = trimRange.endInclusive.toInt()
                            val totalSec = (endSec - startSec).coerceIn(1, ImageCutoutProcessor.MAX_STATUS_VIDEO_SECONDS)
                            onPost(
                                storyText.ifBlank { "مقطع فيديو في حالتي (${formatDurationMmSs(totalSec)}) 🎬" },
                                selectedColor,
                                StoryType.VIDEO,
                                selectedVideoUri,
                                totalSec,
                                startSec,
                                endSec
                            )
                        }
                        else -> {
                            if (storyText.isNotBlank()) {
                                onPost(
                                    storyText,
                                    selectedColor,
                                    StoryType.TEXT,
                                    "",
                                    0,
                                    0,
                                    0
                                )
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("post_status_confirm_button")
            ) {
                Text("نشر الحالة", color = Color.White, fontWeight = FontWeight.Bold)
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

@Composable
fun StoryViewerModal(
    story: StatusStory,
    onClose: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    var replyText by remember { mutableStateOf("") }
    var isVideoPaused by remember { mutableStateOf(false) }

    val totalVideoSeconds = remember(story) {
        if (story.storyType == StoryType.VIDEO) {
            ((story.trimEndSeconds - story.trimStartSeconds).takeIf { it > 0 }
                ?: story.videoDurationSeconds.takeIf { it > 0 }
                ?: 60).coerceAtMost(ImageCutoutProcessor.MAX_STATUS_VIDEO_SECONDS)
        } else 6
    }

    var elapsedVideoSeconds by remember(story.id) { mutableIntStateOf(0) }

    LaunchedEffect(story.id, isVideoPaused) {
        if (story.storyType == StoryType.VIDEO) {
            while (elapsedVideoSeconds < totalVideoSeconds && !isVideoPaused) {
                delay(1000L)
                elapsedVideoSeconds++
                progress.snapTo(elapsedVideoSeconds.toFloat() / totalVideoSeconds.toFloat())
            }
            if (elapsedVideoSeconds >= totalVideoSeconds) {
                onClose()
            }
        } else {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 5500, easing = LinearEasing)
            )
            onClose()
        }
    }

    val parsedColor = runCatching {
        Color(android.graphics.Color.parseColor(story.backgroundColorHex))
    }.getOrDefault(Color(0xFF0A3977))

    val imageBitmap = remember(story.mediaUri) {
        if (story.storyType == StoryType.IMAGE && story.mediaUri.isNotBlank() && File(story.mediaUri).exists()) {
            runCatching { BitmapFactory.decodeFile(story.mediaUri) }.getOrNull()
        } else null
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (story.storyType == StoryType.TEXT) parsedColor else Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Progress Bar
                LinearProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.35f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Header with User Avatar, Name, and Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarView(
                        preset = story.userAvatarPreset,
                        name = story.userName,
                        size = 42.dp,
                        imageUri = story.userAvatarUri
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = story.userName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (story.storyType == StoryType.VIDEO) {
                                "فيديو حالة • ${formatDurationMmSs(elapsedVideoSeconds)} / ${formatDurationMmSs(totalVideoSeconds)}"
                            } else {
                                "حالة مشفرة بين الطرفين 🔒"
                            },
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White
                        )
                    }
                }

                // Story Content Area (Image, 5-min Video Player, or Text)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (story.storyType) {
                        StoryType.IMAGE -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (imageBitmap != null) {
                                    Image(
                                        bitmap = imageBitmap.asImageBitmap(),
                                        contentDescription = story.textContent,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                    )
                                }
                                if (story.textContent.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = story.textContent,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    )
                                }
                            }
                        }

                        StoryType.VIDEO -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color(0xFF141E30), Color(0xFF243B55))
                                        )
                                    )
                                    .padding(24.dp)
                            ) {
                                IconButton(
                                    onClick = { isVideoPaused = !isVideoPaused },
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldPrimary)
                                ) {
                                    Icon(
                                        imageVector = if (isVideoPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = if (isVideoPaused) "تشغيل" else "إيقاف مؤقت",
                                        tint = Color.White,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "${formatDurationMmSs(elapsedVideoSeconds)} / ${formatDurationMmSs(totalVideoSeconds)}",
                                    color = EmeraldLight,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = story.textContent,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        StoryType.TEXT, StoryType.VOICE -> {
                            Text(
                                text = story.textContent,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 34.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                // Bottom Views Count & Reply Bar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RemoveRedEye,
                            contentDescription = "المشاهدات",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${story.viewCount} مشاهدة",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("رد...", color = Color.White.copy(alpha = 0.6f)) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White.copy(alpha = 0.8f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedContainerColor = Color.Black.copy(alpha = 0.25f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary)
                                .clickable {
                                    if (replyText.isNotBlank()) {
                                        onClose()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "إرسال رد",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDurationMmSs(totalSeconds: Int): String {
    val clamped = totalSeconds.coerceAtLeast(0)
    val mins = clamped / 60
    val secs = clamped % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
