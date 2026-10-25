package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AttachmentType
import com.example.data.model.Message
import com.example.data.model.MessageStatus
import com.example.data.model.PollData
import com.example.ui.theme.CheckmarkBlue
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkTextMuted
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SecurityShieldGold
import com.example.util.ImageCutoutProcessor
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    message: Message,
    isOutgoing: Boolean,
    showSenderName: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onVotePoll: ((optionId: String) -> Unit)? = null,
    onOpenViewOnce: (() -> Unit)? = null,
    onOpenInviteLink: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (message.isSystemAnnouncement) {
        SystemAnnouncementBubble(text = message.text)
        return
    }

    val context = LocalContext.current
    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    val bubbleColor = if (isOutgoing) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
        ) {
            if (message.attachmentType == AttachmentType.STICKER) {
                // Real Cutout Image Sticker
                val stickerRef = message.attachmentUrl.ifBlank { message.text }
                val stickerBitmap = remember(stickerRef) {
                    if (stickerRef.startsWith("/") || stickerRef.startsWith("content://") || stickerRef.startsWith("file://")) {
                        ImageCutoutProcessor.loadBitmapFromReference(context, stickerRef, 320)
                    } else null
                }
                Column(
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = onLongPress
                    ),
                    horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
                ) {
                    if (stickerBitmap != null) {
                        Image(
                            bitmap = stickerBitmap.asImageBitmap(),
                            contentDescription = "ملصق",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(136.dp)
                                .padding(4.dp)
                        )
                    } else if (stickerRef.startsWith("/") || stickerRef.startsWith("content://") || stickerRef.startsWith("file://")) {
                        AsyncImage(
                            model = if (stickerRef.startsWith("/")) File(stickerRef) else Uri.parse(stickerRef),
                            contentDescription = "ملصق",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(136.dp)
                                .padding(4.dp)
                        )
                    } else {
                        val fallbackBmp = remember(stickerRef) {
                            val path = ImageCutoutProcessor.ensureGraphicalStickerFile(
                                context = context,
                                key = "chat_sticker_${stickerRef.hashCode()}",
                                title = stickerRef.ifBlank { "ملصق" },
                                primaryColor = android.graphics.Color.rgb(0, 119, 255),
                                accentColor = android.graphics.Color.rgb(10, 57, 119)
                            )
                            ImageCutoutProcessor.loadBitmapFromReference(context, path, 260)
                        }
                        if (fallbackBmp != null) {
                            Image(
                                bitmap = fallbackBmp.asImageBitmap(),
                                contentDescription = "ملصق",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(128.dp)
                                    .padding(4.dp)
                            )
                        }
                    }
                    TimestampAndStatus(
                        timestamp = message.timestamp,
                        status = message.status,
                        isOutgoing = isOutgoing,
                        isEdited = message.isEdited,
                        isStarred = message.isStarred
                    )
                }
            } else if (message.attachmentType == AttachmentType.VIDEO_NOTE) {
                // Circular Video Note (WhatsApp Video Note)
                CircularVideoNoteView(
                    message = message,
                    isOutgoing = isOutgoing,
                    onLongPress = onLongPress
                )
            } else {
                // Standard Chat Bubble
                Column(
                    modifier = Modifier
                        .widthIn(min = 90.dp, max = 320.dp)
                        .clip(bubbleShape)
                        .background(bubbleColor)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = onLongPress
                        )
                        .padding(8.dp)
                ) {
                    // Reply quote header
                    if (!message.replyToText.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.18f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = message.replyToText,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Sender Name in group
                    if (!isOutgoing && showSenderName) {
                        Text(
                            text = message.senderName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    // Attachment types
                    when (message.attachmentType) {
                        AttachmentType.VIDEO -> {
                            if (message.isViewOnce) {
                                ViewOnceItem(
                                    isViewed = message.isViewedOnce,
                                    label = "فيديو (للعرض مرة واحدة)",
                                    onClick = onOpenViewOnce
                                )
                            } else {
                                VideoAttachmentView(message = message)
                            }
                        }
                        AttachmentType.IMAGE -> {
                            if (message.isViewOnce) {
                                ViewOnceItem(
                                    isViewed = message.isViewedOnce,
                                    label = "صورة (للعرض مرة واحدة)",
                                    onClick = onOpenViewOnce
                                )
                            } else {
                                ImageAttachmentView(message = message)
                            }
                        }
                        AttachmentType.DOCUMENT -> DocumentAttachmentView(message = message)
                        AttachmentType.VOICE -> VoiceNoteAdvancedView(message = message)
                        AttachmentType.POLL -> {
                            if (message.pollData != null) {
                                PollBubbleView(
                                    poll = message.pollData,
                                    onVote = onVotePoll
                                )
                            }
                        }
                        else -> Unit
                    }

                    // Text Content & WhatsApp Group Invite Link Card Detection
                    if (message.text.isNotBlank() &&
                        message.attachmentType != AttachmentType.DOCUMENT &&
                        message.attachmentType != AttachmentType.VIDEO &&
                        message.attachmentType != AttachmentType.POLL
                    ) {
                        val inviteRegex = remember {
                            Regex("""https?://(?:chat\.whatsapp\.com|kalbarq\.app/join|safir\.app/join)/[A-Za-z0-9_-]+""")
                        }
                        val matchedInviteUrl = remember(message.text) {
                            inviteRegex.find(message.text)?.value
                        }

                        if (matchedInviteUrl != null) {
                            // WhatsApp-style Group Invite Link Preview Card inside Chat Bubble
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.28f))
                                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .clickable { onOpenInviteLink?.invoke(matchedInviteUrl) }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AvatarView(
                                            preset = 2,
                                            name = "مجموعة",
                                            size = 38.dp,
                                            isGroup = true
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "دعوة للانضمام إلى مجموعة كلبرق / واتساب",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldLight
                                            )
                                            Text(
                                                text = matchedInviteUrl,
                                                fontSize = 11.sp,
                                                color = DarkTextSecondary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(EmeraldPrimary)
                                            .clickable { onOpenInviteLink?.invoke(matchedInviteUrl) }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "عرض المجموعة والانضمام (Join Group)",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = message.text,
                            fontSize = 15.sp,
                            color = if (message.isDeletedForEveryone) DarkTextSecondary else MaterialTheme.colorScheme.onSurface,
                            lineHeight = 21.sp,
                            modifier = Modifier
                                .padding(top = 2.dp, bottom = 2.dp)
                                .let { mod ->
                                    if (matchedInviteUrl != null) {
                                        mod.clickable { onOpenInviteLink?.invoke(matchedInviteUrl) }
                                    } else mod
                                }
                        )
                    }

                    // WhatsApp Gold Anti-Delete Protection Banner
                    if (message.isAntiDeleteProtected) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(SecurityShieldGold.copy(alpha = 0.16f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🛡️ منع الحذف (كلبرق الذهبي): حاول المرسل حذف هذه الرسالة",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecurityShieldGold
                            )
                        }
                    }

                    // Original Quality 100% / Resolution Badge
                    if (message.resolutionBadge.isNotBlank() && message.attachmentType != AttachmentType.NONE) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "✨ ${message.resolutionBadge}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldLight
                        )
                    }

                    // Bottom info (edited badge, timestamp, ticks, star)
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimestampAndStatus(
                            timestamp = message.timestamp,
                            status = message.status,
                            isOutgoing = isOutgoing,
                            isEdited = message.isEdited,
                            isStarred = message.isStarred
                        )
                    }
                }
            }

            // Reactions floating row
            if (message.reactions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(top = 2.dp, start = 4.dp, end = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    message.reactions.forEach { (emoji, count) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = emoji, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = count.toString(), fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ViewOnceItem(
    isViewed: Boolean,
    label: String,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.2f))
            .clickable(enabled = !isViewed) { onClick?.invoke() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isViewed) DarkTextMuted.copy(alpha = 0.3f) else EmeraldPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isViewed) Icons.Default.Check else Icons.Default.Visibility,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = if (isViewed) "تم فتحها (Viewed)" else label,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isViewed) DarkTextMuted else Color.White
            )
            Text(
                text = if (isViewed) "انتهت صلاحية الوسائط بعد المشاهدة" else "انقر للمشاهدة لمرة واحدة فقط 🔒",
                fontSize = 11.sp,
                color = DarkTextSecondary
            )
        }
    }
}

@Composable
fun CircularVideoNoteView(
    message: Message,
    isOutgoing: Boolean,
    onLongPress: (() -> Unit)?
) {
    var isPlaying by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0077FF), Color(0xFF0A3977))
                    )
                )
                .clickable { isPlaying = !isPlaying },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "تشغيل رسالة الفيديو الدائرية",
                    tint = EmeraldLight,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isPlaying) "جاري التشغيل 0:14" else "رسالة فيديو (0:30)",
                fontSize = 11.sp,
                color = DarkTextSecondary
            )
            Spacer(modifier = Modifier.width(6.dp))
            TimestampAndStatus(
                timestamp = message.timestamp,
                status = message.status,
                isOutgoing = isOutgoing,
                isEdited = false,
                isStarred = message.isStarred
            )
        }
    }
}

@Composable
fun VoiceNoteAdvancedView(message: Message) {
    var isPlaying by remember { mutableStateOf(false) }
    var speedIndex by remember { mutableIntStateOf(0) }
    val speeds = listOf("1x", "1.5x", "2x")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(EmeraldPrimary)
                .clickable { isPlaying = !isPlaying },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "تشغيل الصوت",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Waveform bars
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val heights = listOf(8, 14, 22, 12, 18, 26, 16, 24, 10, 16, 22, 14, 8, 18, 12, 20)
            heights.forEachIndexed { index, h ->
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(h.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (index < 7 && isPlaying) EmeraldLight else DarkTextMuted)
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Playback speed pill button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.25f))
                .clickable { speedIndex = (speedIndex + 1) % speeds.size }
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = speeds[speedIndex],
                fontSize = 10.5.sp,
                color = EmeraldLight,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = if (isPlaying) "0:09" else "0:18",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PollBubbleView(
    poll: PollData,
    onVote: ((optionId: String) -> Unit)?
) {
    val totalVotes = poll.options.sumOf { it.voteCount }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Poll, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = poll.question,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Text(
            text = if (poll.allowMultiple) "حدد إجابة واحدة أو أكثر" else "حدد إجابة واحدة فقط",
            fontSize = 11.sp,
            color = DarkTextSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
        )

        poll.options.forEach { option ->
            val percentage = if (totalVotes > 0) option.voteCount.toFloat() / totalVotes.toFloat() else 0f
            val isSelected = poll.userVotedOptionIds.contains(option.id)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onVote?.invoke(option.id) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, if (isSelected) EmeraldPrimary else DarkBorder, CircleShape)
                            .background(if (isSelected) EmeraldPrimary else Color.Transparent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = option.text,
                        fontSize = 13.5.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${option.voteCount} (${(percentage * 100).toInt()}%)",
                        fontSize = 11.5.sp,
                        color = DarkTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { percentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isSelected) EmeraldPrimary else EmeraldLight.copy(alpha = 0.7f),
                    trackColor = Color.Black.copy(alpha = 0.25f)
                )
            }
        }

        Text(
            text = "إجمالي الأصوات: $totalVotes",
            fontSize = 11.sp,
            color = DarkTextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun VideoAttachmentView(message: Message) {
    var isPlaying by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0D2B52), Color(0xFF07162C))
                    )
                )
                .clickable { isPlaying = !isPlaying },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "تشغيل الفيديو",
                    tint = EmeraldLight,
                    modifier = Modifier.size(32.dp)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isPlaying) "01:24 / 02:40" else "02:40",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = message.attachmentSize.ifBlank { "18.4 MB" },
                        color = DarkTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (message.text.isNotBlank()) {
            Text(
                text = message.text,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

@Composable
fun DocumentAttachmentView(message: Message) {
    var isDownloaded by remember { mutableStateOf(true) }
    var isDownloading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.16f))
            .clickable {
                if (!isDownloaded && !isDownloading) {
                    isDownloading = true
                }
            }
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE53935).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "ملف مستند",
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.attachmentName.ifBlank { "ملخص_المشروع.pdf" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "${message.attachmentSize.ifBlank { "4.2 MB" }} • مشفر بقاعدة كلبرق (حتى 2GB)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            if (isDownloading || message.transferProgressPercent < 100) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = EmeraldPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "تحميل",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (message.transferProgressPercent < 100) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (message.transferProgressPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = EmeraldPrimary,
                trackColor = Color.Black.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "جاري النقل المجزأ فائق السرعة: ${message.transferProgressPercent}%",
                fontSize = 10.sp,
                color = EmeraldLight,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ImageAttachmentView(message: Message) {
    val context = LocalContext.current
    val bmp = remember(message.attachmentUrl) {
        if (message.attachmentUrl.isNotBlank()) {
            ImageCutoutProcessor.loadBitmapFromReference(context, message.attachmentUrl, 480)
        } else null
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0055D4), Color(0xFF071E3D))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "صورة مرفقة",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "صورة",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun SystemAnnouncementBubble(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF182229))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "أمان",
                tint = Color(0xFFFFD279),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = Color(0xFFFFD279),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun TimestampAndStatus(
    timestamp: Long,
    status: MessageStatus,
    isOutgoing: Boolean,
    isEdited: Boolean,
    isStarred: Boolean
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale("ar")) }
    val formattedTime = remember(timestamp) { timeFormat.format(Date(timestamp)) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        if (isStarred) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "مميزة بنجمة",
                tint = SecurityShieldGold,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
        }

        if (isEdited) {
            Text(
                text = "تم تعديلها",
                fontSize = 10.sp,
                color = DarkTextMuted,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        Text(
            text = formattedTime,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )

        if (isOutgoing) {
            Spacer(modifier = Modifier.width(3.dp))
            when (status) {
                MessageStatus.SENDING -> {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "جاري الإرسال",
                        tint = DarkTextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                }
                MessageStatus.SENT -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "تم الإرسال",
                        tint = DarkTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
                MessageStatus.DELIVERED -> {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "تم الاستلام",
                        tint = DarkTextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }
                MessageStatus.READ -> {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "تمت القراءة",
                        tint = CheckmarkBlue,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
