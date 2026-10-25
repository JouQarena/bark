package com.example.data.model

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ
}

enum class AttachmentType {
    NONE,
    IMAGE,
    VIDEO,
    DOCUMENT,
    STICKER,
    VOICE,
    VIDEO_NOTE,
    POLL
}

enum class StoryType {
    IMAGE,
    VIDEO,
    TEXT,
    VOICE
}

data class User(
    val id: String,
    val phoneNumber: String,
    val displayName: String,
    val avatarUrl: String = "",
    val avatarUri: String = "",
    val avatarPreset: Int = 1,
    val bio: String = "مرحباً! أنا أستخدم تطبيق كلبرق.",
    val isOnline: Boolean = true,
    val lastSeenText: String = "متصل الآن",
    val safetyNumber: String = "9482 1049 8592 1058 9204 8194"
)

data class PollOption(
    val id: String,
    val text: String,
    val voteCount: Int = 0,
    val voterNames: List<String> = emptyList()
)

data class PollData(
    val question: String,
    val options: List<PollOption>,
    val allowMultiple: Boolean = false,
    val userVotedOptionIds: List<String> = emptyList()
)

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long,
    val status: MessageStatus = MessageStatus.SENT,
    val attachmentType: AttachmentType = AttachmentType.NONE,
    val attachmentUrl: String = "",
    val attachmentName: String = "",
    val attachmentSize: String = "",
    val isEncrypted: Boolean = true,
    val isSystemAnnouncement: Boolean = false,
    val replyToText: String? = null,
    // Advanced WhatsApp & Kalbarq Gold features
    val reactions: Map<String, Int> = emptyMap(),
    val isEdited: Boolean = false,
    val isDeletedForEveryone: Boolean = false,
    val isAntiDeleteProtected: Boolean = false,
    val originalTextBeforeDelete: String? = null,
    val isViewOnce: Boolean = false,
    val isViewedOnce: Boolean = false,
    val isStarred: Boolean = false,
    val isPinned: Boolean = false,
    val voiceDurationSeconds: Int = 18,
    val pollData: PollData? = null,
    val isOriginalQuality: Boolean = true,
    val resolutionBadge: String = "",
    val transferProgressPercent: Int = 100
)

data class PendingJoinRequest(
    val userId: String,
    val userName: String,
    val userAvatarPreset: Int,
    val requestedAt: Long
)

data class Conversation(
    val id: String,
    val title: String,
    val isGroup: Boolean,
    val groupIconPreset: Int = 1,
    val groupIconUri: String = "",
    val groupDescription: String = "",
    val participantNames: List<String> = emptyList(),
    val lastMessageText: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isOnline: Boolean = false,
    val lastSeenLabel: String = "متصل الآن",
    val hasActiveStatus: Boolean = false,
    val hasUnviewedStatus: Boolean = false,
    val activeStoryId: String? = null,
    val historyVisibleToNewMembers: Boolean = true,
    val safetyNumber: String = "7392 4819 0284 1948 2049 1930",
    // Group Invite Links & Admin Controls
    val inviteToken: String = "kalbarq_token_grp_${System.currentTimeMillis() % 100000}",
    val isInviteLinkRevoked: Boolean = false,
    val requireAdminApproval: Boolean = false,
    val pendingRequests: List<PendingJoinRequest> = emptyList(),
    val adminNames: List<String> = emptyList(),
    val isAnnouncementOnly: Boolean = false,
    val onlyAdminsCanEditInfo: Boolean = true,
    val onlyAdminsCanAddMembers: Boolean = false
)

data class StatusViewerRecord(
    val viewerName: String,
    val viewerAvatarPreset: Int,
    val viewedAtTimestamp: Long
)

data class StatusStory(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatarPreset: Int,
    val userAvatarUri: String = "",
    val storyType: StoryType,
    val textContent: String = "",
    val mediaPreset: Int = 0,
    val mediaUri: String = "",
    val videoDurationSeconds: Int = 0,
    val trimStartSeconds: Int = 0,
    val trimEndSeconds: Int = 0,
    val backgroundColorHex: String = "#0055D4",
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 86400000L,
    val isViewed: Boolean = false,
    val viewCount: Int = 14,
    val viewersList: List<String> = listOf("د. أحمد خليل", "سارة ممدوح", "عمر الشامي", "مريم علي"),
    val isOriginalQuality: Boolean = true
)

data class KalbarqLiveAlert(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String, // "STATUS_VIEW", "ONLINE_NOW", "FILE_2GB", "WEAK_NET"
    val timestamp: Long = System.currentTimeMillis()
)

data class LargeFileTransferRecord(
    val id: String,
    val conversationId: String,
    val fileName: String,
    val fileSizeMb: Float,
    val totalChunks: Int,
    val uploadedChunks: Int,
    val sha256Checksum: String,
    val isCompleted: Boolean,
    val isOriginalQuality: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class Sticker(
    val id: String,
    val packId: String,
    val symbol: String,
    val label: String,
    val imageUri: String = ""
)

data class StickerPack(
    val id: String,
    val name: String,
    val author: String,
    val thumbnailSymbol: String,
    val thumbnailUri: String = "",
    val stickers: List<Sticker>,
    val isInstalled: Boolean = true,
    val isExternalAppPack: Boolean = false
)

data class Community(
    val id: String,
    val name: String,
    val description: String,
    val iconPreset: Int,
    val announcementGroupId: String,
    val subGroupIds: List<String>,
    val totalMembers: Int
)

data class CallLink(
    val id: String,
    val title: String,
    val url: String,
    val isVideo: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

data class LinkedDevice(
    val id: String,
    val name: String,
    val os: String,
    val location: String,
    val lastActive: String,
    val isCurrent: Boolean = false
)

data class ChannelPost(
    val id: String,
    val text: String,
    val timestamp: Long,
    val reactions: Map<String, Int> = mapOf("👍" to 124, "❤️" to 89, "🔥" to 65)
)

data class Channel(
    val id: String,
    val name: String,
    val description: String,
    val iconPreset: Int,
    val followersCount: String,
    val isVerified: Boolean = true,
    val posts: List<ChannelPost> = emptyList()
)

data class ContactItem(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val avatarPreset: Int,
    val avatarUri: String = "",
    val bio: String,
    val isOnline: Boolean = true,
    val lastSeenLabel: String = "متصل الآن"
)

data class CustomPhotoSticker(
    val id: String,
    val title: String,
    val previewEmoji: String = "",
    val imageUri: String = "",
    val borderWidthPx: Int = 10,
    val cropZoom: Float = 1.0f,
    val isCutout: Boolean = true,
    val isFavorite: Boolean = true
)

data class WhatsAppSettings(
    // الخصوصية (Privacy)
    val lastSeenPrivacy: String = "الكل", // الكل، جهات اتصالي، جهات اتصالي باستثناء...، لا أحد
    val onlinePrivacy: String = "الكل", // الكل، مثل آخر ظهور
    val profilePhotoPrivacy: String = "الكل",
    val aboutPrivacy: String = "الكل",
    val statusPrivacy: String = "جهات اتصالي",
    val readReceiptsEnabled: Boolean = true,
    val defaultDisappearingTimer: String = "إيقاف", // إيقاف، 24 ساعة، 7 أيام، 90 يوماً
    val groupsPrivacy: String = "الكل",
    val liveLocationSharing: String = "لا توجد دردشات",
    val silenceUnknownCallers: Boolean = false,
    val blockedContactsCount: Int = 2,
    val blockedContacts: List<String> = listOf("+20 109 888 7766", "+20 112 444 3322"),
    val fingerprintLockEnabled: Boolean = false,
    val fingerprintLockTimeout: String = "حالاً", // حالاً، بعد دقيقة واحدة، بعد 30 دقيقة

    // الدردشات والمظهر وتغيير لون واجهة كلبرق (Chats & Theme Color)
    val themeMode: String = "داكن", // النظام الافتراضي، فاتح، داكن
    val uiColorPresetId: String = "ROYAL_BLUE", // ROYAL_BLUE, KALBARQ_GOLD, EMERALD_GREEN, ROYAL_PURPLE, CRIMSON_RUBY, CYBER_CYAN
    val chatWallpaperHex: String = "#0B141A",
    val enterIsSend: Boolean = false,
    val mediaVisibilityInGallery: Boolean = true,
    val fontSize: String = "متوسط", // صغير، متوسط، كبير
    val keepChatsArchived: Boolean = true,
    val lastBackupTime: String = "اليوم، 04:00 ص",
    val backupIncludeVideos: Boolean = true,

    // إضافات كلبرق الذهبية (WhatsApp Gold / Kalbarq Pro Core Features)
    val goldShowOnlineOnChatList: Boolean = true,
    val goldOnlineToastEnabled: Boolean = true,
    val goldStatusViewNotificationEnabled: Boolean = true,
    val goldHideOnlineStatus: Boolean = false,
    val goldAntiDeleteMessages: Boolean = true,
    val goldAntiDeleteStatus: Boolean = true,
    val goldHideBlueTicks: Boolean = false,
    val goldHideSecondTick: Boolean = false,
    val goldHideTypingIndicator: Boolean = false,
    val goldAutoReplyEnabled: Boolean = false,
    val goldAutoReplyText: String = "مرحباً بك! أنا مشغول حالياً وسأرد عليك في أقرب وقت عبر كلبرق ⚡",

    // الإشعارات (Notifications)
    val conversationTones: Boolean = true,
    val messageNotificationTone: String = "نغمة واتساب الافتراضية",
    val messageVibrate: String = "افتراضي", // إيقاف، افتراضي، قصير، طويل
    val messageHighPriority: Boolean = true,
    val reactionNotifications: Boolean = true,
    val groupNotificationTone: String = "نغمة المجموعات الافتراضية",
    val groupVibrate: String = "افتراضي",
    val groupHighPriority: Boolean = true,
    val groupReactionNotifications: Boolean = true,
    val callRingtone: String = "رنين كلبرق الرسمي",
    val callVibrate: String = "افتراضي",

    // التخزين والبيانات والإنترنت الضعيف ونت السوشيال (Storage, 2GB DB, Weak Net & Social Data)
    val useLessDataForCalls: Boolean = true,
    val weakNetworkBoosterEnabled: Boolean = true,
    val socialDataPackageMode: Boolean = true,
    val preserveOriginalImageQuality: Boolean = true,
    val maxFileSizeLimitGb: Int = 2,
    val mediaUploadQuality: String = "الجودة الأصلية 100% بدون ضغط (RAW/HD)", // الجودة الأصلية 100% بدون ضغط (RAW/HD)، دقة عالية (HD)، الجودة القياسية
    val autoDownloadMobile: Set<String> = setOf("الصور"),
    val autoDownloadWifi: Set<String> = setOf("الصور", "الفيديو", "الملفات", "المقاطع الصوتية"),
    val autoDownloadRoaming: Set<String> = emptySet(),
    val proxyEnabled: Boolean = false,
    val proxyAddress: String = "",
    val storageUsedMb: Float = 284.5f,
    val networkSentMb: Float = 42.8f,
    val networkReceivedMb: Float = 118.4f,

    // الحساب والتحديثات (Account & Seamless In-Place Updates)
    val securityNotificationsEnabled: Boolean = true,
    val passkeysEnabled: Boolean = true,
    val emailAddress: String = "user@kalbarq.app",
    val twoStepVerificationEnabled: Boolean = false,
    val twoStepPin: String = "",
    val changeNumberNotifyContacts: Boolean = true,
    val changeNumberNotifyMode: String = "كل جهات الاتصال",
    val accountReportRequested: Boolean = false,
    val appVersionName: String = "2.5.0 Pro (قابل للتحديث المباشر)",
    val appVersionCode: Int = 25,
    val autoInstallUpdatesOverCurrent: Boolean = true,

    // لغة التطبيق (App Language)
    val appLanguage: String = "العربية (لغة الجهاز)"
)
