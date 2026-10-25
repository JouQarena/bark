package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttachmentType
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.StatusStory
import com.example.data.repository.SafirRepository
import com.example.ui.components.AvatarView
import com.example.ui.components.ChatBubble
import com.example.ui.components.GroupInfoSheet
import com.example.ui.components.GroupInviteSheet
import com.example.ui.components.GroupJoinPreviewSheet
import com.example.ui.components.MediaAttachmentSheet
import com.example.ui.components.PhotoToStickerSheet
import com.example.ui.components.PollCreatorDialog
import com.example.ui.components.StickerSheet
import com.example.ui.components.VoiceRecordingBar
import com.example.ui.components.WhatsAppCameraCaptureDialog
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkTextMuted
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SecurityShieldGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversationId: String,
    repository: SafirRepository,
    onBack: () -> Unit,
    onOpenStickerStore: () -> Unit,
    onOpenChat: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var conversation by remember { mutableStateOf<Conversation?>(null) }
    val messages by repository.getMessages(conversationId).collectAsState(initial = emptyList())
    val stickerPacks by repository.stickerPacks.collectAsState(initial = emptyList())
    val currentUser by repository.currentUser.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var isVideoNoteMode by remember { mutableStateOf(false) } // toggle between mic and circular video note

    // WhatsApp Voice Recording State
    var isRecordingVoice by remember { mutableStateOf(false) }
    var isVoiceLocked by remember { mutableStateOf(false) }

    val stories by repository.stories.collectAsState(initial = emptyList())
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showStickerSheet by remember { mutableStateOf(false) }
    var showPhotoToStickerSheet by remember { mutableStateOf(false) }
    var showGroupInfoSheet by remember { mutableStateOf(false) }
    var showContactInfoDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showInviteSheet by remember { mutableStateOf(false) }
    var showPollDialog by remember { mutableStateOf(false) }
    var showCameraCaptureDialog by remember { mutableStateOf(false) }
    var cameraVideoInitialMode by remember { mutableStateOf(false) }
    var showHeaderMenu by remember { mutableStateOf(false) }
    var storyToViewFromAvatar by remember { mutableStateOf<StatusStory?>(null) }
    var activeCallType by remember { mutableStateOf<String?>(null) }

    // Message selection on long press
    var selectedMessageForActions by remember { mutableStateOf<Message?>(null) }
    var messageToEdit by remember { mutableStateOf<Message?>(null) }
    var editedText by remember { mutableStateOf("") }
    var messageToDelete by remember { mutableStateOf<Message?>(null) }
    var activeViewOnceMessage by remember { mutableStateOf<Message?>(null) }

    // Group Join Preview Sheet (if link tapped)
    var inviteTokenToPreview by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    val allConversations by repository.conversations.collectAsState(initial = emptyList())
    LaunchedEffect(conversationId, allConversations) {
        conversation = repository.getConversation(conversationId)
        repository.markConversationRead(conversationId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val currentConv = conversation
    val pinnedMessages = remember(messages) { messages.filter { it.isPinned } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Chat Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (currentConv != null) {
                    AvatarView(
                        preset = currentConv.groupIconPreset,
                        name = currentConv.title,
                        size = 42.dp,
                        isOnline = currentConv.isOnline,
                        isGroup = currentConv.isGroup,
                        hasActiveStory = currentConv.hasActiveStatus,
                        imageUri = currentConv.groupIconUri,
                        onClick = {
                            if (!currentConv.isGroup && currentConv.hasActiveStatus) {
                                val matchingStory = stories.firstOrNull {
                                    it.id == currentConv.activeStoryId || it.userName == currentConv.title
                                }
                                if (matchingStory != null) {
                                    repository.markStoryViewed(matchingStory.id)
                                    storyToViewFromAvatar = matchingStory
                                } else {
                                    showContactInfoDialog = true
                                }
                            } else if (currentConv.isGroup) {
                                showGroupInfoSheet = true
                            } else {
                                showContactInfoDialog = true
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (currentConv.isGroup) showGroupInfoSheet = true
                                else showContactInfoDialog = true
                            }
                    ) {
                        Text(
                            text = currentConv.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (currentConv.isGroup) {
                                "${currentConv.participantNames.size} أعضاء • إعدادات المشرف والروابط"
                            } else {
                                if (currentConv.isOnline) "متصل الآن 🟢" else "آخر ظهور قبل قليل"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = if (currentConv.isOnline) FontWeight.Bold else FontWeight.Normal,
                            color = if (currentConv.isOnline) EmeraldLight else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Group Invite Link button for groups
                    if (currentConv.isGroup) {
                        IconButton(onClick = { showInviteSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "رابط الدعوة و QR",
                                tint = EmeraldPrimary
                            )
                        }
                    }

                    IconButton(onClick = { activeCallType = "مكالمة صوتية مشفرة (WebRTC)" }) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "اتصال صوتي",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = { activeCallType = "مكالمة فيديو مشفرة (WebRTC)" }) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "اتصال فيديو",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showHeaderMenu = true },
                            modifier = Modifier.testTag("chat_header_more_menu")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "المزيد من الخيارات",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = showHeaderMenu,
                            onDismissRequest = { showHeaderMenu = false },
                            modifier = Modifier.background(DarkSurfaceElevated)
                        ) {
                            DropdownMenuItem(
                                text = { Text("📷 التقاط صورة أو فيديو بالكاميرا", color = Color.White) },
                                onClick = {
                                    showHeaderMenu = false
                                    cameraVideoInitialMode = false
                                    showCameraCaptureDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🛡️ تجربة منع حذف الرسائل (Anti-Delete)", color = SecurityShieldGold, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    showHeaderMenu = false
                                    repository.simulateAntiDeleteIncomingMessage(conversationId)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📦 إرسال ملف ضخم (حتى 2 جيجابايت)", color = EmeraldLight, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    showHeaderMenu = false
                                    repository.sendLargeFileUpTo2GB(
                                        conversationId = conversationId,
                                        fileName = "حزمة_بيانات_كلبرق_العملاقة_2GB.zip",
                                        fileSizeMb = 1960f,
                                        mediaUri = "local_chunked://kalbarq_2gb_pack.zip"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🔒 التحقق من التشفير (Safety Number)", color = Color.White) },
                                onClick = {
                                    showHeaderMenu = false
                                    showSecurityDialog = true
                                }
                            )
                        }
                    }
                }
            }

            // Pinned Messages Banner (Up to 3 messages)
            if (pinnedMessages.isNotEmpty()) {
                val latestPinned = pinnedMessages.last()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF182229))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "مثبتة",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "رسالة مثبتة: ${latestPinned.senderName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = latestPinned.text,
                            fontSize = 12.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { repository.togglePinMessage(conversationId, latestPinned.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إلغاء التثبيت", tint = DarkTextMuted, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Telegram Group Feature Notice
            if (currentConv?.isGroup == true && currentConv.historyVisibleToNewMembers) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EmeraldPrimary.copy(alpha = 0.10f))
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "نظام تيليجرام: يتمكن جميع الأعضاء الجدد من رؤية سجل الرسائل والملخصات السابقة بالكامل.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Messages Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    val isOutgoing = message.senderId == (currentUser?.id ?: "current_user_id")
                    ChatBubble(
                        message = message,
                        isOutgoing = isOutgoing,
                        showSenderName = currentConv?.isGroup == true && !isOutgoing,
                        onLongPress = { selectedMessageForActions = message },
                        onVotePoll = { optionId ->
                            repository.votePoll(
                                messageId = message.id,
                                optionId = optionId,
                                userName = currentUser?.displayName ?: "أنا"
                            )
                        },
                        onOpenViewOnce = {
                            activeViewOnceMessage = message
                            repository.markViewOnceOpened(message.id)
                        },
                        onOpenInviteLink = { url ->
                            inviteTokenToPreview = url
                        }
                    )
                }
            }

            // Bottom Input Bar & Voice Recording Bar
            if (isRecordingVoice) {
                VoiceRecordingBar(
                    isRecording = true,
                    isLocked = isVoiceLocked,
                    onLockRecording = { isVoiceLocked = true },
                    onCancelRecording = {
                        isRecordingVoice = false
                        isVoiceLocked = false
                    },
                    onSendRecording = { durationSeconds ->
                        repository.sendMessage(
                            conversationId = conversationId,
                            text = "رسالة صوتية مشفرة (${durationSeconds} ثانية)",
                            attachmentType = AttachmentType.VOICE,
                            attachmentSize = "${durationSeconds} ثانية"
                        )
                        isRecordingVoice = false
                        isVoiceLocked = false
                    },
                    onStartRecording = {
                        isRecordingVoice = true
                        isVoiceLocked = false
                    }
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sticker / Emoji Button
                    IconButton(onClick = {
                        showStickerSheet = !showStickerSheet
                        if (showStickerSheet) showAttachmentSheet = false
                    }) {
                        Icon(
                            imageVector = Icons.Default.SentimentSatisfiedAlt,
                            contentDescription = "ملصقات ورموز",
                            tint = if (showStickerSheet) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Attachment Button
                    IconButton(
                        onClick = {
                            showAttachmentSheet = true
                            showStickerSheet = false
                        },
                        modifier = Modifier.testTag("chat_attach_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "إرفاق ملفات",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Direct WhatsApp-style Camera Button in Input Bar
                    IconButton(
                        onClick = {
                            cameraVideoInitialMode = false
                            showCameraCaptureDialog = true
                        },
                        modifier = Modifier.testTag("chat_input_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "التقاط صورة أو تسجيل فيديو بالكاميرا",
                            tint = EmeraldPrimary
                        )
                    }

                    // Text Input Field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("اكتب رسالة مشفرة...", color = DarkTextSecondary) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Send Button or Voice Note / Video Note Switcher
                    if (inputText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary)
                                .clickable {
                                    repository.sendMessage(
                                        conversationId = conversationId,
                                        text = inputText.trim()
                                    )
                                    inputText = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "إرسال",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        // Tap or hold to record WhatsApp style voice note!
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (isVideoNoteMode) Color(0xFFE53935) else EmeraldPrimary)
                                .clickable {
                                    if (isVideoNoteMode) {
                                        repository.sendMessage(
                                            conversationId = conversationId,
                                            text = "رسالة فيديو دائرية مشفرة",
                                            attachmentType = AttachmentType.VIDEO_NOTE
                                        )
                                        isVideoNoteMode = false
                                    } else {
                                        // Start WhatsApp voice recording flow
                                        isRecordingVoice = true
                                        isVoiceLocked = false
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isVideoNoteMode) Icons.Default.Videocam else Icons.Default.Mic,
                                contentDescription = "تسجيل صوتي",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Embedded Sticker Sheet
            AnimatedVisibility(
                visible = showStickerSheet,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                StickerSheet(
                    packs = stickerPacks,
                    onSelectSticker = { sticker ->
                        val path = sticker.imageUri.ifBlank { sticker.symbol }
                        repository.sendMessage(
                            conversationId = conversationId,
                            text = path,
                            attachmentType = AttachmentType.STICKER,
                            attachmentName = sticker.label,
                            attachmentUrl = path
                        )
                        showStickerSheet = false
                    },
                    onOpenStore = {
                        showStickerSheet = false
                        onOpenStickerStore()
                    },
                    onOpenPhotoToSticker = {
                        showStickerSheet = false
                        showPhotoToStickerSheet = true
                    }
                )
            }
        }

        // Media Attachment Sheet
        if (showAttachmentSheet) {
            MediaAttachmentSheet(
                onDismiss = { showAttachmentSheet = false },
                onSelectAttachment = { type, name, size ->
                    repository.sendMessage(
                        conversationId = conversationId,
                        text = if (type == AttachmentType.DOCUMENT) "مستند مشفر" else "محتوى مرئي",
                        attachmentType = type,
                        attachmentName = name,
                        attachmentSize = size
                    )
                },
                onSelectRealImage = { imagePath, size ->
                    repository.sendMessage(
                        conversationId = conversationId,
                        text = "صورة من المعرض",
                        attachmentType = AttachmentType.IMAGE,
                        attachmentName = "صورة_المعرض.jpg",
                        attachmentSize = size,
                        attachmentUrl = imagePath
                    )
                },
                onOpenStickerStore = {
                    showAttachmentSheet = false
                    onOpenStickerStore()
                },
                onOpenPollCreator = {
                    showPollDialog = true
                },
                onSendViewOnceMedia = { isVideo ->
                    repository.sendMessage(
                        conversationId = conversationId,
                        text = if (isVideo) "مقطع فيديو للعرض مرة واحدة" else "صورة للعرض مرة واحدة",
                        attachmentType = if (isVideo) AttachmentType.VIDEO else AttachmentType.IMAGE,
                        isViewOnce = true
                    )
                },
                onOpenPhotoToSticker = {
                    showAttachmentSheet = false
                    showPhotoToStickerSheet = true
                },
                onOpenCameraCapture = { isVideo ->
                    showAttachmentSheet = false
                    cameraVideoInitialMode = isVideo
                    showCameraCaptureDialog = true
                },
                onSendLargeFile2GB = { fileName, _, sizeBytes, fileUri ->
                    repository.sendLargeFileUpTo2GB(
                        conversationId = conversationId,
                        fileName = fileName,
                        fileSizeMb = (sizeBytes / (1024f * 1024f)).coerceIn(100f, 2048f),
                        mediaUri = fileUri
                    )
                }
            )
        }

        // WhatsApp Camera & Video Capture Studio Dialog (with Runtime Permissions)
        if (showCameraCaptureDialog) {
            WhatsAppCameraCaptureDialog(
                initialVideoMode = cameraVideoInitialMode,
                preserveOriginalQualityDefault = true,
                onDismiss = { showCameraCaptureDialog = false },
                onMediaCaptured = { filePath, isVideo, caption, sizeBadge, durationSeconds ->
                    showCameraCaptureDialog = false
                    if (isVideo) {
                        repository.sendMessage(
                            conversationId = conversationId,
                            text = caption.ifBlank { "مقطع فيديو بالكاميرا (${durationSeconds} ثانية) 🎥" },
                            attachmentType = AttachmentType.VIDEO,
                            attachmentName = "فيديو_كاميرا_كلبرق.mp4",
                            attachmentSize = "$sizeBadge • ${durationSeconds} ثانية",
                            attachmentUrl = filePath
                        )
                    } else {
                        repository.sendMessage(
                            conversationId = conversationId,
                            text = caption.ifBlank { "صورة ملتقطة بالكاميرا بجودة أصلية 📷" },
                            attachmentType = AttachmentType.IMAGE,
                            attachmentName = "صورة_كاميرا_كلبرق.png",
                            attachmentSize = sizeBadge,
                            attachmentUrl = filePath
                        )
                    }
                }
            )
        }

        // Fullscreen Story Viewer when tapping Contact's Status Ring in Chat Header
        val activeHeaderStory = storyToViewFromAvatar
        if (activeHeaderStory != null) {
            StoryViewerModal(
                story = activeHeaderStory,
                onClose = { storyToViewFromAvatar = null }
            )
        }

        // Photo to Sticker Converter Sheet
        if (showPhotoToStickerSheet) {
            PhotoToStickerSheet(
                repository = repository,
                onDismiss = { showPhotoToStickerSheet = false },
                onStickerCreated = { stickerUri ->
                    repository.sendMessage(
                        conversationId = conversationId,
                        text = stickerUri,
                        attachmentType = AttachmentType.STICKER,
                        attachmentName = "ملصق من المعرض",
                        attachmentUrl = stickerUri
                    )
                    showPhotoToStickerSheet = false
                }
            )
        }

        // Group Info & Description/Bio Sheet
        if (showGroupInfoSheet && currentConv != null) {
            GroupInfoSheet(
                conversation = currentConv,
                repository = repository,
                onDismiss = { showGroupInfoSheet = false },
                onOpenInviteLinkSheet = {
                    showGroupInfoSheet = false
                    showInviteSheet = true
                }
            )
        }

        // Contact Info Dialog (Personal Bio & Contact Details)
        if (showContactInfoDialog && currentConv != null) {
            val contact = repository.contacts.collectAsState().value.find { it.name == currentConv.title }
            AlertDialog(
                onDismissRequest = { showContactInfoDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarView(
                            preset = currentConv.groupIconPreset,
                            name = currentConv.title,
                            size = 46.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(currentConv.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(contact?.phoneNumber ?: "+201000000000", fontSize = 12.sp, color = DarkTextSecondary)
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "السيرة الذاتية / الأخبار:",
                            fontSize = 12.sp,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = contact?.bio ?: "مشترك في كلبرق • مشفر تماماً ⚡",
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(onClick = {
                                activeCallType = "اتصال صوتي مشفر مع ${currentConv.title}"
                                showContactInfoDialog = false
                            }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Call, contentDescription = "اتصال", tint = EmeraldPrimary)
                                    Text("صوتي", fontSize = 11.sp, color = DarkTextSecondary)
                                }
                            }

                            IconButton(onClick = {
                                activeCallType = "مكالمة فيديو مشفرة مع ${currentConv.title}"
                                showContactInfoDialog = false
                            }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Videocam, contentDescription = "فيديو", tint = EmeraldPrimary)
                                    Text("فيديو", fontSize = 11.sp, color = DarkTextSecondary)
                                }
                            }

                            IconButton(onClick = {
                                showSecurityDialog = true
                                showContactInfoDialog = false
                            }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Shield, contentDescription = "تشفير", tint = SecurityShieldGold)
                                    Text("تشفير", fontSize = 11.sp, color = DarkTextSecondary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showContactInfoDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("إغلاق", color = Color.White)
                    }
                },
                containerColor = DarkSurfaceElevated
            )
        }

        // Poll Creator Dialog
        if (showPollDialog) {
            PollCreatorDialog(
                onDismiss = { showPollDialog = false },
                onCreatePoll = { poll ->
                    repository.sendMessage(
                        conversationId = conversationId,
                        text = poll.question,
                        attachmentType = AttachmentType.POLL,
                        pollData = poll
                    )
                    showPollDialog = false
                }
            )
        }

        // Group Invite & Admin Controls Sheet
        if (showInviteSheet && currentConv != null) {
            GroupInviteSheet(
                conversation = currentConv,
                repository = repository,
                onDismiss = { showInviteSheet = false },
                onShareLinkInChat = { url ->
                    repository.sendMessage(
                        conversationId = conversationId,
                        text = "🔗 رابط الدعوة المشفر لمجموعة ${currentConv.title}:\n$url"
                    )
                }
            )
        }

        // Group Join Preview Sheet (Edge Cases & Guest Experience)
        if (inviteTokenToPreview != null) {
            val rawLink = inviteTokenToPreview!!
            val token = repository.extractInviteToken(rawLink)
            val matchedConv = allConversations.find {
                it.inviteToken.equals(token, ignoreCase = true)
            }
            GroupJoinPreviewSheet(
                conversation = matchedConv,
                isRevoked = matchedConv?.isInviteLinkRevoked == true,
                currentUserName = currentUser?.displayName ?: "أنا",
                repository = repository,
                onDismiss = { inviteTokenToPreview = null },
                onNavigateToChat = { convId ->
                    inviteTokenToPreview = null
                    if (convId != conversationId) {
                        onOpenChat(convId)
                    }
                },
                inviteLinkOrToken = rawLink
            )
        }

        // View Once Media Fullscreen Viewer Dialog
        val viewOnceMsg = activeViewOnceMessage
        if (viewOnceMsg != null) {
            AlertDialog(
                onDismissRequest = { activeViewOnceMessage = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldLight)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("وسائط للعرض مرة واحدة 🔒", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (viewOnceMsg.attachmentType == AttachmentType.VIDEO) Icons.Default.Videocam else Icons.Default.Lock,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "🛡️ محمي ضد لقطات الشاشة وتسجيل الشاشة البرمجي. ستختفي هذه الوسائط نهائياً بمجرد إغلاق هذه النافذة.",
                            fontSize = 11.5.sp,
                            color = DarkTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { activeViewOnceMessage = null },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("تمت المشاهدة", color = Color.White)
                    }
                },
                containerColor = DarkSurfaceElevated
            )
        }

        // Long-Press Message Actions & Emoji Reaction Sheet
        val msgForAction = selectedMessageForActions
        if (msgForAction != null) {
            val isMyMessage = msgForAction.senderId == (currentUser?.id ?: "current_user_id")

            AlertDialog(
                onDismissRequest = { selectedMessageForActions = null },
                title = {
                    // Floating Emoji Reaction Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("👍", "❤️", "😂", "😮", "😢", "🙏", "🔥").forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .clickable {
                                        repository.addReaction(msgForAction.id, emoji)
                                        selectedMessageForActions = null
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Star action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repository.toggleStarMessage(msgForAction.id)
                                    selectedMessageForActions = null
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = SecurityShieldGold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (msgForAction.isStarred) "إلغاء التمييز بنجمة" else "تمييز بنجمة (Starred)",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        // Pin action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repository.togglePinMessage(conversationId, msgForAction.id)
                                    selectedMessageForActions = null
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PushPin, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (msgForAction.isPinned) "إلغاء تثبيت الرسالة" else "تثبيت في أعلى المحادثة (Pin)",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        // Copy action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Message Text", msgForAction.text))
                                    Toast.makeText(context, "تم نسخ الرسالة", Toast.LENGTH_SHORT).show()
                                    selectedMessageForActions = null
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("نسخ النص", color = Color.White, fontSize = 14.sp)
                        }

                        // Edit action (if my message)
                        if (isMyMessage && !msgForAction.isDeletedForEveryone) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        messageToEdit = msgForAction
                                        editedText = msgForAction.text
                                        selectedMessageForActions = null
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = EmeraldLight)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("تعديل الرسالة (Edit خلال 15 دقيقة)", color = EmeraldLight, fontSize = 14.sp)
                            }
                        }

                        // Delete action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    messageToDelete = msgForAction
                                    selectedMessageForActions = null
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF5350))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("حذف الرسالة (Delete)", color = Color(0xFFEF5350), fontSize = 14.sp)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { selectedMessageForActions = null }) {
                        Text("إلغاء", color = DarkTextSecondary)
                    }
                },
                containerColor = DarkSurfaceElevated
            )
        }

        // Edit Message Dialog
        val toEdit = messageToEdit
        if (toEdit != null) {
            AlertDialog(
                onDismissRequest = { messageToEdit = null },
                title = { Text("تعديل الرسالة", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = EmeraldPrimary
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editedText.isNotBlank()) {
                                repository.editMessage(conversationId, toEdit.id, editedText.trim())
                            }
                            messageToEdit = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("حفظ التعديل", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { messageToEdit = null }) {
                        Text("إلغاء", color = DarkTextSecondary)
                    }
                },
                containerColor = DarkSurfaceElevated
            )
        }

        // Delete Options Dialog ("حذف لدي فقط" أو "حذف لدى الجميع")
        val toDelete = messageToDelete
        if (toDelete != null) {
            AlertDialog(
                onDismissRequest = { messageToDelete = null },
                title = { Text("حذف الرسالة؟", fontWeight = FontWeight.Bold) },
                text = {
                    Text("اختر طريقة الحذف المناسبة لك:", color = DarkTextSecondary)
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                repository.deleteMessage(conversationId, toDelete.id, forEveryone = true)
                                messageToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                        ) {
                            Text("الحذف لدى الجميع", color = Color.White, fontSize = 11.5.sp)
                        }
                        Button(
                            onClick = {
                                repository.deleteMessage(conversationId, toDelete.id, forEveryone = false)
                                messageToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBorder)
                        ) {
                            Text("الحذف لدي فقط", color = Color.White, fontSize = 11.5.sp)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { messageToDelete = null }) {
                        Text("إلغاء", color = DarkTextSecondary)
                    }
                },
                containerColor = DarkSurfaceElevated
            )
        }

        // Encryption Safety Numbers Dialog
        if (showSecurityDialog && currentConv != null) {
            AlertDialog(
                onDismissRequest = { showSecurityDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = SecurityShieldGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("التحقق من أرقام الأمان المشفرة", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "للتأكد من التشفير التام (End-to-End) بينك وبين ${currentConv.title}، يمكنك مقارنة رمز QR أو أرقام الأمان:",
                            fontSize = 12.sp,
                            color = DarkTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = "QR Code",
                                tint = Color.Black,
                                modifier = Modifier.size(114.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = currentConv.safetyNumber,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = EmeraldLight,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showSecurityDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("تم التحقق بنجاح", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSecurityDialog = false }) {
                        Text("إغلاق", color = DarkTextSecondary)
                    }
                },
                containerColor = DarkSurfaceElevated
            )
        }

        // Active Call Dialog
        if (activeCallType != null) {
            AlertDialog(
                onDismissRequest = { activeCallType = null },
                title = { Text(text = activeCallType ?: "", fontWeight = FontWeight.Bold) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AvatarView(
                            preset = currentConv?.groupIconPreset ?: 1,
                            name = currentConv?.title ?: "",
                            size = 64.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "جاري الاتصال بـ ${currentConv?.title}...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkTextPrimary
                        )
                        Text(
                            text = "🔒 المكالمة مشفرة بتقنية WebRTC التكيفية حتى 32 مشاركاً.",
                            fontSize = 12.sp,
                            color = EmeraldLight,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { activeCallType = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Text("إنهاء الاتصال", color = Color.White)
                    }
                },
                containerColor = DarkSurfaceElevated
            )
        }
    }
}
