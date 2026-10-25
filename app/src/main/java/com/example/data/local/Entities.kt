package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.AttachmentType
import com.example.data.model.Conversation
import com.example.data.model.LargeFileTransferRecord
import com.example.data.model.Message
import com.example.data.model.MessageStatus
import com.example.data.model.StatusStory
import com.example.data.model.StoryType

@Entity(
    tableName = "conversations",
    indices = [Index(value = ["lastMessageTime"]), Index(value = ["isPinned"])]
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val isGroup: Boolean,
    val groupIconPreset: Int,
    val groupDescription: String,
    val participantNames: List<String>,
    val lastMessageText: String,
    val lastMessageTime: Long,
    val unreadCount: Int,
    val isPinned: Boolean,
    val isOnline: Boolean,
    val historyVisibleToNewMembers: Boolean,
    val safetyNumber: String
) {
    fun toDomain(): Conversation = Conversation(
        id = id,
        title = title,
        isGroup = isGroup,
        groupIconPreset = groupIconPreset,
        groupDescription = groupDescription,
        participantNames = participantNames,
        lastMessageText = lastMessageText,
        lastMessageTime = lastMessageTime,
        unreadCount = unreadCount,
        isPinned = isPinned,
        isOnline = isOnline,
        historyVisibleToNewMembers = historyVisibleToNewMembers,
        safetyNumber = safetyNumber
    )
}

@Entity(
    tableName = "messages",
    indices = [Index(value = ["conversationId", "timestamp"])]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long,
    val status: MessageStatus,
    val attachmentType: AttachmentType,
    val attachmentUrl: String,
    val attachmentName: String,
    val attachmentSize: String,
    val isEncrypted: Boolean,
    val isSystemAnnouncement: Boolean,
    val replyToText: String?
) {
    fun toDomain(): Message = Message(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        senderName = senderName,
        text = text,
        timestamp = timestamp,
        status = status,
        attachmentType = attachmentType,
        attachmentUrl = attachmentUrl,
        attachmentName = attachmentName,
        attachmentSize = attachmentSize,
        isEncrypted = isEncrypted,
        isSystemAnnouncement = isSystemAnnouncement,
        replyToText = replyToText
    )
}

@Entity(tableName = "status_stories")
data class StatusStoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userAvatarPreset: Int,
    val storyType: StoryType,
    val textContent: String,
    val mediaPreset: Int,
    val backgroundColorHex: String,
    val timestamp: Long,
    val expiresAt: Long,
    val isViewed: Boolean,
    val viewCount: Int
) {
    fun toDomain(): StatusStory = StatusStory(
        id = id,
        userId = userId,
        userName = userName,
        userAvatarPreset = userAvatarPreset,
        storyType = storyType,
        textContent = textContent,
        mediaPreset = mediaPreset,
        backgroundColorHex = backgroundColorHex,
        timestamp = timestamp,
        expiresAt = expiresAt,
        isViewed = isViewed,
        viewCount = viewCount
    )
}

@Entity(tableName = "sticker_packs")
data class StickerPackEntity(
    @PrimaryKey val id: String,
    val name: String,
    val author: String,
    val thumbnailSymbol: String,
    val stickersJson: String,
    val isInstalled: Boolean
)

/**
 * Heavy-duty database table for tracking multi-gigabyte (up to 2 GB / 2,048 MB) chunked file transfers
 * with SHA-256 verification and resumable chunk state so transfers survive weak networks.
 */
@Entity(
    tableName = "file_transfers",
    indices = [Index(value = ["conversationId", "timestamp"])]
)
data class FileTransferEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val fileName: String,
    val fileSizeMb: Float,
    val totalChunks: Int,
    val uploadedChunks: Int,
    val sha256Checksum: String,
    val isCompleted: Boolean,
    val isOriginalQuality: Boolean,
    val timestamp: Long
) {
    fun toDomain(): LargeFileTransferRecord = LargeFileTransferRecord(
        id = id,
        conversationId = conversationId,
        fileName = fileName,
        fileSizeMb = fileSizeMb,
        totalChunks = totalChunks,
        uploadedChunks = uploadedChunks,
        sha256Checksum = sha256Checksum,
        isCompleted = isCompleted,
        isOriginalQuality = isOriginalQuality,
        timestamp = timestamp
    )
}
