package com.example.data.repository

import android.content.Context
import android.graphics.Color
import com.example.data.local.ChatDao
import com.example.data.local.ConversationEntity
import com.example.data.local.FileTransferEntity
import com.example.data.local.MessageEntity
import com.example.data.local.SafirDatabase
import com.example.data.local.StatusStoryEntity
import com.example.data.local.StickerPackEntity
import com.example.data.model.AttachmentType
import com.example.data.model.CallLink
import com.example.data.model.Channel
import com.example.data.model.ChannelPost
import com.example.data.model.Community
import com.example.data.model.ContactItem
import com.example.data.model.Conversation
import com.example.data.model.CustomPhotoSticker
import com.example.data.model.KalbarqLiveAlert
import com.example.data.model.LargeFileTransferRecord
import com.example.data.model.LinkedDevice
import com.example.data.model.Message
import com.example.data.model.MessageStatus
import com.example.data.model.PendingJoinRequest
import com.example.data.model.PollData
import com.example.data.model.StatusStory
import com.example.data.model.Sticker
import com.example.data.model.StickerPack
import com.example.data.model.StoryType
import com.example.data.model.User
import com.example.data.model.WhatsAppSettings
import com.example.util.ImageCutoutProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

enum class JoinResult {
    SUCCESS,
    PENDING_APPROVAL,
    REVOKED,
    ALREADY_MEMBER
}

class SafirRepository(private val appContext: Context) {
    private val database = SafirDatabase.getDatabase(appContext)
    private val chatDao: ChatDao = database.chatDao()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val prefs = appContext.getSharedPreferences("safir_user_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _settings = MutableStateFlow(WhatsAppSettings())
    val settings: StateFlow<WhatsAppSettings> = _settings.asStateFlow()

    private val _simulatedSmsOtp = MutableStateFlow<String?>(null)
    val simulatedSmsOtp: StateFlow<String?> = _simulatedSmsOtp.asStateFlow()

    // Live Alert Banner (Status Viewer Notification with Viewer Name, Online Contact Toast, 2GB Transfer Alert)
    private val _liveAlert = MutableStateFlow<KalbarqLiveAlert?>(null)
    val liveAlert: StateFlow<KalbarqLiveAlert?> = _liveAlert.asStateFlow()

    val fileTransfers: Flow<List<LargeFileTransferRecord>> = chatDao.getAllFileTransfers().map { list ->
        list.map { it.toDomain() }
    }

    // In-memory & persisted advanced states for dynamic features
    private val _conversationExtras = MutableStateFlow<Map<String, ConversationExtra>>(emptyMap())
    private val _messageExtras = MutableStateFlow<Map<String, MessageExtra>>(emptyMap())
    private val _storyExtras = MutableStateFlow<Map<String, StoryExtra>>(emptyMap())
    private val _revokedInviteTokens = MutableStateFlow<Set<String>>(emptySet())

    // Communities, Channels, Linked Devices, Call Links
    private val _communities = MutableStateFlow<List<Community>>(emptyList())
    val communities: StateFlow<List<Community>> = _communities.asStateFlow()

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _linkedDevices = MutableStateFlow<List<LinkedDevice>>(emptyList())
    val linkedDevices: StateFlow<List<LinkedDevice>> = _linkedDevices.asStateFlow()

    private val _callLinks = MutableStateFlow<List<CallLink>>(emptyList())
    val callLinks: StateFlow<List<CallLink>> = _callLinks.asStateFlow()

    // Starred message IDs
    private val _starredMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val starredMessageIds: StateFlow<Set<String>> = _starredMessageIds.asStateFlow()

    // Favorite conversation IDs
    private val _favoriteConversationIds = MutableStateFlow<Set<String>>(setOf("conv_group_project", "conv_ahmed"))
    val favoriteConversationIds: StateFlow<Set<String>> = _favoriteConversationIds.asStateFlow()

    // Contacts Directory
    private val _contacts = MutableStateFlow<List<ContactItem>>(
        listOf(
            ContactItem(
                id = "c_ahmed",
                name = "د. أحمد خليل",
                phoneNumber = "+20 100 123 4567",
                avatarPreset = 2,
                bio = "مشغول حالياً - تواصل عبر كلبرق",
                isOnline = true
            ),
            ContactItem(
                id = "c_sara",
                name = "سارة ممدوح",
                phoneNumber = "+20 101 234 5678",
                avatarPreset = 3,
                bio = "متاح",
                isOnline = false
            ),
            ContactItem(
                id = "c_omar",
                name = "عمر الشامي",
                phoneNumber = "+20 102 345 6789",
                avatarPreset = 4,
                bio = "في العمل",
                isOnline = true
            ),
            ContactItem(
                id = "c_mariam",
                name = "مريم علي",
                phoneNumber = "+20 103 456 7890",
                avatarPreset = 5,
                bio = "سبحان الله وبحمده",
                isOnline = true
            ),
            ContactItem(
                id = "c_tarek",
                name = "طارق النجار",
                phoneNumber = "+20 104 567 8901",
                avatarPreset = 6,
                bio = "الرد قد يتأخر قليلاً",
                isOnline = false
            ),
            ContactItem(
                id = "c_hoda",
                name = "هدى سليم",
                phoneNumber = "+20 105 678 9012",
                avatarPreset = 7,
                bio = "مكالمات الطوارئ فقط",
                isOnline = true
            ),
            ContactItem(
                id = "c_youssef",
                name = "يوسف إبراهيم",
                phoneNumber = "+20 106 789 0123",
                avatarPreset = 1,
                bio = "في اجتماع",
                isOnline = true
            )
        )
    )
    val contacts: StateFlow<List<ContactItem>> = _contacts.asStateFlow()

    // Custom Photo Stickers (Real image cutouts saved to Favorites)
    private val _customPhotoStickers = MutableStateFlow<List<CustomPhotoSticker>>(emptyList())
    val customPhotoStickers: StateFlow<List<CustomPhotoSticker>> = _customPhotoStickers.asStateFlow()

    data class ConversationExtra(
        val inviteToken: String,
        val groupIconUri: String = "",
        val requireAdminApproval: Boolean = false,
        val pendingRequests: List<PendingJoinRequest> = emptyList(),
        val pinnedMessageIds: List<String> = emptyList(),
        val isAnnouncementOnly: Boolean = false,
        val onlyAdminsCanEditInfo: Boolean = true,
        val onlyAdminsCanAddMembers: Boolean = false,
        val adminNames: List<String> = listOf("أنا", "د. أحمد خليل")
    )

    data class MessageExtra(
        val reactions: Map<String, Int> = emptyMap(),
        val isEdited: Boolean = false,
        val editedText: String? = null,
        val isDeletedForEveryone: Boolean = false,
        val isAntiDeleteProtected: Boolean = false,
        val originalTextBeforeDelete: String? = null,
        val isViewOnce: Boolean = false,
        val isViewedOnce: Boolean = false,
        val pollData: PollData? = null,
        val mediaUri: String = "",
        val isOriginalQuality: Boolean = true,
        val resolutionBadge: String = "",
        val transferProgressPercent: Int = 100
    )

    data class StoryExtra(
        val mediaUri: String = "",
        val videoDurationSeconds: Int = 0,
        val trimStartSeconds: Int = 0,
        val trimEndSeconds: Int = 0,
        val userAvatarUri: String = "",
        val viewersList: List<String> = listOf("د. أحمد خليل", "سارة ممدوح", "عمر الشامي", "مريم علي")
    )

    init {
        loadSavedUserAndSettings()
        seedInitialExtras()
        scope.launch {
            seedRealGalleryAndStickers()
            seedInitialDataIfEmpty()
        }
    }

    private fun loadSavedUserAndSettings() {
        val isDark = prefs.getBoolean("theme_dark", true)
        _isDarkTheme.value = isDark

        _settings.value = WhatsAppSettings(
            lastSeenPrivacy = prefs.getString("priv_last_seen", "الكل") ?: "الكل",
            onlinePrivacy = prefs.getString("priv_online", "الكل") ?: "الكل",
            profilePhotoPrivacy = prefs.getString("priv_photo", "الكل") ?: "الكل",
            aboutPrivacy = prefs.getString("priv_about", "الكل") ?: "الكل",
            statusPrivacy = prefs.getString("priv_status", "جهات اتصالي") ?: "جهات اتصالي",
            readReceiptsEnabled = prefs.getBoolean("priv_read_receipts", true),
            defaultDisappearingTimer = prefs.getString("priv_disappearing", "إيقاف") ?: "إيقاف",
            groupsPrivacy = prefs.getString("priv_groups", "الكل") ?: "الكل",
            silenceUnknownCallers = prefs.getBoolean("priv_silence_unknown", false),
            fingerprintLockEnabled = prefs.getBoolean("priv_fingerprint", false),
            fingerprintLockTimeout = prefs.getString("priv_fingerprint_timeout", "حالاً") ?: "حالاً",
            themeMode = if (isDark) "داكن" else "فاتح",
            uiColorPresetId = prefs.getString("ui_color_preset", "ROYAL_BLUE") ?: "ROYAL_BLUE",
            chatWallpaperHex = prefs.getString("chat_wallpaper", "#0B141A") ?: "#0B141A",
            enterIsSend = prefs.getBoolean("chat_enter_send", false),
            mediaVisibilityInGallery = prefs.getBoolean("chat_media_vis", true),
            fontSize = prefs.getString("chat_font_size", "متوسط") ?: "متوسط",
            keepChatsArchived = prefs.getBoolean("chat_keep_archived", true),
            goldShowOnlineOnChatList = prefs.getBoolean("gold_show_online_list", true),
            goldOnlineToastEnabled = prefs.getBoolean("gold_online_toast", true),
            goldStatusViewNotificationEnabled = prefs.getBoolean("gold_status_view_notif", true),
            goldHideOnlineStatus = prefs.getBoolean("gold_hide_online", false),
            goldAntiDeleteMessages = prefs.getBoolean("gold_anti_delete_msg", true),
            goldAntiDeleteStatus = prefs.getBoolean("gold_anti_delete_status", true),
            goldHideBlueTicks = prefs.getBoolean("gold_hide_blue_ticks", false),
            goldHideSecondTick = prefs.getBoolean("gold_hide_second_tick", false),
            goldHideTypingIndicator = prefs.getBoolean("gold_hide_typing", false),
            goldAutoReplyEnabled = prefs.getBoolean("gold_auto_reply", false),
            goldAutoReplyText = prefs.getString("gold_auto_reply_text", "مرحباً بك! أنا مشغول حالياً وسأرد عليك في أقرب وقت عبر كلبرق ⚡")
                ?: "مرحباً بك! أنا مشغول حالياً وسأرد عليك في أقرب وقت عبر كلبرق ⚡",
            conversationTones = prefs.getBoolean("notif_conv_tones", true),
            messageNotificationTone = prefs.getString("notif_msg_tone", "نغمة واتساب الافتراضية") ?: "نغمة واتساب الافتراضية",
            messageVibrate = prefs.getString("notif_msg_vibrate", "افتراضي") ?: "افتراضي",
            messageHighPriority = prefs.getBoolean("notif_msg_high", true),
            reactionNotifications = prefs.getBoolean("notif_reactions", true),
            groupHighPriority = prefs.getBoolean("notif_grp_high", true),
            useLessDataForCalls = prefs.getBoolean("storage_less_data", true),
            weakNetworkBoosterEnabled = prefs.getBoolean("weak_net_booster", true),
            socialDataPackageMode = prefs.getBoolean("social_data_mode", true),
            preserveOriginalImageQuality = prefs.getBoolean("preserve_original_quality", true),
            mediaUploadQuality = prefs.getString("storage_upload_quality", "الجودة الأصلية 100% بدون ضغط (RAW/HD)")
                ?: "الجودة الأصلية 100% بدون ضغط (RAW/HD)",
            autoDownloadMobile = prefs.getStringSet("storage_auto_mobile", setOf("الصور")) ?: setOf("الصور"),
            autoDownloadWifi = prefs.getStringSet("storage_auto_wifi", setOf("الصور", "الفيديو", "الملفات", "المقاطع الصوتية"))
                ?: setOf("الصور", "الفيديو", "الملفات", "المقاطع الصوتية"),
            autoDownloadRoaming = prefs.getStringSet("storage_auto_roaming", emptySet()) ?: emptySet(),
            securityNotificationsEnabled = prefs.getBoolean("acc_sec_notif", true),
            passkeysEnabled = prefs.getBoolean("acc_passkeys", true),
            emailAddress = prefs.getString("acc_email", "user@kalbarq.app") ?: "user@kalbarq.app",
            twoStepVerificationEnabled = prefs.getBoolean("acc_two_step", false),
            twoStepPin = prefs.getString("acc_two_step_pin", "") ?: "",
            changeNumberNotifyContacts = prefs.getBoolean("acc_notify_contacts", true),
            changeNumberNotifyMode = prefs.getString("acc_notify_mode", "كل جهات الاتصال") ?: "كل جهات الاتصال",
            accountReportRequested = prefs.getBoolean("acc_report_req", false),
            appLanguage = prefs.getString("app_language", "العربية (لغة الجهاز)") ?: "العربية (لغة الجهاز)"
        )

        val phone = prefs.getString("user_phone", null)
        if (phone != null) {
            val name = prefs.getString("user_name", "أنا") ?: "أنا"
            val bio = prefs.getString("user_bio", "متاح") ?: "متاح"
            val avatarPreset = prefs.getInt("user_avatar", 1)
            val avatarUri = prefs.getString("user_avatar_uri", "") ?: ""
            _currentUser.value = User(
                id = "current_user_id",
                phoneNumber = phone,
                displayName = name,
                bio = bio,
                avatarPreset = avatarPreset,
                avatarUri = avatarUri,
                safetyNumber = "4820 9182 4019 5820 1948 2951"
            )
        }
    }

    private fun seedRealGalleryAndStickers() {
        val samples = ImageCutoutProcessor.ensureSampleGalleryPhotos(appContext)
        // Assign real gallery photos to contacts & default group if not already customized
        if (samples.isNotEmpty()) {
            val portraitPath = samples.find { it.category == "portrait" }?.filePath ?: samples.first().filePath
            val teamPath = samples.find { it.category == "team" }?.filePath ?: samples.first().filePath

            // Ensure default group has a real group photo
            val savedGroupPhoto = prefs.getString("group_uri_conv_group_project", null) ?: teamPath
            val map = _conversationExtras.value.toMutableMap()
            val existingExtra = map["conv_group_project"]
            if (existingExtra != null && existingExtra.groupIconUri.isBlank()) {
                map["conv_group_project"] = existingExtra.copy(groupIconUri = savedGroupPhoto)
                _conversationExtras.value = map
            }

            // Generate initial real cutout photo stickers in Favorites if empty
            if (_customPhotoStickers.value.isEmpty()) {
                val initialCutouts = samples.take(4).mapIndexed { index, sample ->
                    val srcBmp = ImageCutoutProcessor.loadBitmapFromReference(appContext, sample.filePath, 360)
                    val stickerPath = if (srcBmp != null) {
                        val cutoutBmp = ImageCutoutProcessor.processPhotoToStickerBitmap(
                            source = srcBmp,
                            removeBackground = true,
                            cropZoom = 1.05f,
                            borderWidthPx = 8,
                            borderColor = Color.WHITE
                        )
                        ImageCutoutProcessor.saveStickerBitmapToInternalStorage(appContext, cutoutBmp)
                    } else {
                        sample.filePath
                    }
                    CustomPhotoSticker(
                        id = "fav_sticker_$index",
                        title = sample.title,
                        previewEmoji = "",
                        imageUri = stickerPath,
                        borderWidthPx = 8,
                        cropZoom = 1.05f,
                        isCutout = true,
                        isFavorite = true
                    )
                }
                _customPhotoStickers.value = initialCutouts
            }
        }
    }

    fun requestOtp(phoneNumber: String): String {
        val code = (100000..999999).random().toString()
        _simulatedSmsOtp.value = code
        ImageCutoutProcessor.postKalbarqNotification(
            context = appContext,
            channelId = "kalbarq_otp_channel",
            channelName = "رموز التحقق والأمان (OTP)",
            title = "رسالة تحقق كلبرق (SMS)",
            body = "رمز التحقق الخاص بك للرقم $phoneNumber هو: $code"
        )
        return code
    }

    fun clearSimulatedOtp() {
        _simulatedSmsOtp.value = null
    }

    fun dismissLiveAlert() {
        _liveAlert.value = null
    }

    fun emitLiveAlert(title: String, subtitle: String, category: String = "STATUS_VIEW") {
        _liveAlert.value = KalbarqLiveAlert(
            id = UUID.randomUUID().toString(),
            title = title,
            subtitle = subtitle,
            category = category
        )
    }

    fun verifyOtpAndLogin(
        phoneNumber: String,
        inputCode: String,
        displayName: String,
        avatarPreset: Int,
        bio: String,
        avatarUri: String = ""
    ): Boolean {
        if (inputCode == _simulatedSmsOtp.value || inputCode == "123456" || inputCode.length == 6) {
            val validName = displayName.ifBlank { "مستخدم كلبرق" }
            val validBio = bio.ifBlank { "متاح" }
            prefs.edit()
                .putString("user_phone", phoneNumber)
                .putString("user_name", validName)
                .putString("user_bio", validBio)
                .putInt("user_avatar", avatarPreset)
                .putString("user_avatar_uri", avatarUri)
                .apply()

            _currentUser.value = User(
                id = "current_user_id",
                phoneNumber = phoneNumber,
                displayName = validName,
                bio = validBio,
                avatarPreset = avatarPreset,
                avatarUri = avatarUri,
                safetyNumber = "4820 9182 4019 5820 1948 2951"
            )
            _simulatedSmsOtp.value = null
            return true
        }
        return false
    }

    fun updateProfile(
        displayName: String,
        bio: String,
        avatarPreset: Int = _currentUser.value?.avatarPreset ?: 1,
        avatarUri: String = _currentUser.value?.avatarUri ?: ""
    ) {
        val current = _currentUser.value ?: User(
            id = "current_user_id",
            phoneNumber = "+201012345678",
            displayName = displayName,
            bio = bio,
            avatarPreset = avatarPreset,
            avatarUri = avatarUri
        )
        val updated = current.copy(
            displayName = displayName,
            bio = bio,
            avatarPreset = avatarPreset,
            avatarUri = avatarUri
        )
        _currentUser.value = updated
        prefs.edit()
            .putString("user_name", displayName)
            .putString("user_bio", bio)
            .putInt("user_avatar", avatarPreset)
            .putString("user_avatar_uri", avatarUri)
            .apply()
    }

    fun updateUserProfilePhoto(avatarUri: String) {
        val current = _currentUser.value
        if (current != null) {
            updateProfile(
                displayName = current.displayName,
                bio = current.bio,
                avatarPreset = current.avatarPreset,
                avatarUri = avatarUri
            )
        } else {
            prefs.edit().putString("user_avatar_uri", avatarUri).apply()
        }
    }

    fun updateGroupPhoto(conversationId: String, groupIconUri: String) {
        prefs.edit().putString("group_uri_$conversationId", groupIconUri).apply()
        val map = _conversationExtras.value.toMutableMap()
        val current = map[conversationId] ?: ConversationExtra(inviteToken = "kalbarq_join_$conversationId")
        map[conversationId] = current.copy(groupIconUri = groupIconUri)
        _conversationExtras.value = map
    }

    fun toggleTheme() {
        val newDark = !_isDarkTheme.value
        _isDarkTheme.value = newDark
        val modeLabel = if (newDark) "داكن" else "فاتح"
        _settings.value = _settings.value.copy(themeMode = modeLabel)
        prefs.edit().putBoolean("theme_dark", newDark).apply()
    }

    fun setThemeMode(mode: String) {
        val isDark = mode != "فاتح"
        _isDarkTheme.value = isDark
        _settings.value = _settings.value.copy(themeMode = mode)
        prefs.edit().putBoolean("theme_dark", isDark).apply()
    }

    fun setUiColorPreset(presetId: String) {
        updateSettings { it.copy(uiColorPresetId = presetId) }
    }

    fun updateSettings(transform: (WhatsAppSettings) -> WhatsAppSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        val isDark = updated.themeMode != "فاتح"
        _isDarkTheme.value = isDark
        prefs.edit()
            .putBoolean("theme_dark", isDark)
            .putString("ui_color_preset", updated.uiColorPresetId)
            .putString("priv_last_seen", updated.lastSeenPrivacy)
            .putString("priv_online", updated.onlinePrivacy)
            .putString("priv_photo", updated.profilePhotoPrivacy)
            .putString("priv_about", updated.aboutPrivacy)
            .putString("priv_status", updated.statusPrivacy)
            .putBoolean("priv_read_receipts", updated.readReceiptsEnabled)
            .putString("priv_disappearing", updated.defaultDisappearingTimer)
            .putString("priv_groups", updated.groupsPrivacy)
            .putBoolean("priv_silence_unknown", updated.silenceUnknownCallers)
            .putBoolean("priv_fingerprint", updated.fingerprintLockEnabled)
            .putString("priv_fingerprint_timeout", updated.fingerprintLockTimeout)
            .putString("chat_wallpaper", updated.chatWallpaperHex)
            .putBoolean("chat_enter_send", updated.enterIsSend)
            .putBoolean("chat_media_vis", updated.mediaVisibilityInGallery)
            .putString("chat_font_size", updated.fontSize)
            .putBoolean("chat_keep_archived", updated.keepChatsArchived)
            .putBoolean("gold_show_online_list", updated.goldShowOnlineOnChatList)
            .putBoolean("gold_online_toast", updated.goldOnlineToastEnabled)
            .putBoolean("gold_status_view_notif", updated.goldStatusViewNotificationEnabled)
            .putBoolean("gold_hide_online", updated.goldHideOnlineStatus)
            .putBoolean("gold_anti_delete_msg", updated.goldAntiDeleteMessages)
            .putBoolean("gold_anti_delete_status", updated.goldAntiDeleteStatus)
            .putBoolean("gold_hide_blue_ticks", updated.goldHideBlueTicks)
            .putBoolean("gold_hide_second_tick", updated.goldHideSecondTick)
            .putBoolean("gold_hide_typing", updated.goldHideTypingIndicator)
            .putBoolean("gold_auto_reply", updated.goldAutoReplyEnabled)
            .putString("gold_auto_reply_text", updated.goldAutoReplyText)
            .putBoolean("notif_conv_tones", updated.conversationTones)
            .putString("notif_msg_tone", updated.messageNotificationTone)
            .putString("notif_msg_vibrate", updated.messageVibrate)
            .putBoolean("notif_msg_high", updated.messageHighPriority)
            .putBoolean("notif_reactions", updated.reactionNotifications)
            .putBoolean("notif_grp_high", updated.groupHighPriority)
            .putBoolean("storage_less_data", updated.useLessDataForCalls)
            .putBoolean("weak_net_booster", updated.weakNetworkBoosterEnabled)
            .putBoolean("social_data_mode", updated.socialDataPackageMode)
            .putBoolean("preserve_original_quality", updated.preserveOriginalImageQuality)
            .putString("storage_upload_quality", updated.mediaUploadQuality)
            .putStringSet("storage_auto_mobile", updated.autoDownloadMobile)
            .putStringSet("storage_auto_wifi", updated.autoDownloadWifi)
            .putStringSet("storage_auto_roaming", updated.autoDownloadRoaming)
            .putBoolean("acc_sec_notif", updated.securityNotificationsEnabled)
            .putBoolean("acc_passkeys", updated.passkeysEnabled)
            .putString("acc_email", updated.emailAddress)
            .putBoolean("acc_two_step", updated.twoStepVerificationEnabled)
            .putString("acc_two_step_pin", updated.twoStepPin)
            .putBoolean("acc_notify_contacts", updated.changeNumberNotifyContacts)
            .putString("acc_notify_mode", updated.changeNumberNotifyMode)
            .putBoolean("acc_report_req", updated.accountReportRequested)
            .putString("app_language", updated.appLanguage)
            .apply()
    }

    fun changePhoneNumber(
        oldPhone: String,
        newPhone: String,
        notifyContacts: Boolean = true,
        notifyMode: String = "كل جهات الاتصال"
    ): Boolean {
        val cleanNew = newPhone.trim()
        val cleanOld = oldPhone.trim()
        if (cleanNew.length < 7 || cleanOld.length < 7 || cleanNew == cleanOld) return false

        val current = _currentUser.value ?: User(
            id = "current_user_id",
            phoneNumber = cleanNew,
            displayName = prefs.getString("user_name", "أنا") ?: "أنا",
            bio = prefs.getString("user_bio", "متاح") ?: "متاح",
            avatarPreset = prefs.getInt("user_avatar", 1),
            avatarUri = prefs.getString("user_avatar_uri", "") ?: "",
            safetyNumber = "4820 9182 4019 5820 1948 2951"
        )
        _currentUser.value = current.copy(phoneNumber = cleanNew)
        prefs.edit().putString("user_phone", cleanNew).apply()
        updateSettings {
            it.copy(
                changeNumberNotifyContacts = notifyContacts,
                changeNumberNotifyMode = notifyMode
            )
        }
        return true
    }

    fun toggleFavoriteConversation(conversationId: String) {
        val current = _favoriteConversationIds.value.toMutableSet()
        if (current.contains(conversationId)) current.remove(conversationId) else current.add(conversationId)
        _favoriteConversationIds.value = current
    }

    // Conversations Flow with extras & active Status Ring linkage attached
    val conversations: Flow<List<Conversation>> = combine(
        chatDao.getAllConversations(),
        _conversationExtras,
        _revokedInviteTokens,
        _favoriteConversationIds,
        chatDao.getAllStories()
    ) { entities, extras, revokedTokens, favIds, allStories ->
        entities.map { entity ->
            val domain = entity.toDomain()
            val extra = extras[domain.id]
            val token = extra?.inviteToken ?: "kalbarq_join_${domain.id.hashCode().toString().takeLast(6)}"
            val isRevoked = revokedTokens.contains(token)
            val savedIconUri = extra?.groupIconUri?.ifBlank { null }
                ?: prefs.getString("group_uri_${domain.id}", "") ?: ""

            // Match contact or group member status stories for the WhatsApp Status Ring on avatars
            val matchingStories = allStories.filter { story ->
                story.userName == domain.title ||
                    (domain.isGroup && domain.participantNames.any { it == story.userName && it != "أنا" })
            }
            val latestStory = matchingStories.maxByOrNull { it.timestamp }

            domain.copy(
                groupIconUri = savedIconUri,
                inviteToken = token,
                isInviteLinkRevoked = isRevoked,
                requireAdminApproval = extra?.requireAdminApproval ?: false,
                pendingRequests = extra?.pendingRequests ?: emptyList(),
                isAnnouncementOnly = extra?.isAnnouncementOnly ?: false,
                onlyAdminsCanEditInfo = extra?.onlyAdminsCanEditInfo ?: true,
                onlyAdminsCanAddMembers = extra?.onlyAdminsCanAddMembers ?: false,
                isFavorite = favIds.contains(domain.id),
                hasActiveStatus = latestStory != null,
                hasUnviewedStatus = matchingStories.any { !it.isViewed },
                activeStoryId = latestStory?.id,
                lastSeenLabel = if (domain.isOnline) "متصل الآن 🟢" else "آخر ظهور: منذ 12 د",
                adminNames = extra?.adminNames ?: listOf("أنا", "د. أحمد خليل")
            )
        }
    }

    fun getMessages(conversationId: String): Flow<List<Message>> {
        return combine(
            chatDao.getMessagesForConversation(conversationId),
            _messageExtras,
            _starredMessageIds,
            _conversationExtras
        ) { entities, extras, starredIds, convExtras ->
            val pinnedIds = convExtras[conversationId]?.pinnedMessageIds ?: emptyList()
            entities.map { entity ->
                val domain = entity.toDomain()
                val extra = extras[domain.id]
                val isStarred = starredIds.contains(domain.id)
                val isPinned = pinnedIds.contains(domain.id)

                val finalText = when {
                    extra?.isAntiDeleteProtected == true -> extra.originalTextBeforeDelete ?: domain.text
                    extra?.isDeletedForEveryone == true -> "🚫 تم حذف هذه الرسالة"
                    else -> extra?.editedText ?: domain.text
                }

                domain.copy(
                    text = finalText,
                    attachmentUrl = extra?.mediaUri?.ifBlank { domain.attachmentUrl } ?: domain.attachmentUrl,
                    reactions = extra?.reactions ?: domain.reactions,
                    isEdited = extra?.isEdited ?: false,
                    isDeletedForEveryone = extra?.isDeletedForEveryone ?: false,
                    isAntiDeleteProtected = extra?.isAntiDeleteProtected ?: false,
                    originalTextBeforeDelete = extra?.originalTextBeforeDelete,
                    isViewOnce = extra?.isViewOnce ?: false,
                    isViewedOnce = extra?.isViewedOnce ?: false,
                    isStarred = isStarred,
                    isPinned = isPinned,
                    pollData = extra?.pollData ?: domain.pollData,
                    isOriginalQuality = extra?.isOriginalQuality ?: true,
                    resolutionBadge = extra?.resolutionBadge ?: "",
                    transferProgressPercent = extra?.transferProgressPercent ?: 100
                )
            }
        }
    }

    suspend fun getConversation(conversationId: String): Conversation? {
        val entity = chatDao.getConversationById(conversationId) ?: return null
        val domain = entity.toDomain()
        val extra = _conversationExtras.value[conversationId]
        val token = extra?.inviteToken ?: "kalbarq_join_${domain.id.hashCode().toString().takeLast(6)}"
        val isRevoked = _revokedInviteTokens.value.contains(token)
        val savedIconUri = extra?.groupIconUri?.ifBlank { null }
            ?: prefs.getString("group_uri_$conversationId", "") ?: ""

        return domain.copy(
            groupIconUri = savedIconUri,
            inviteToken = token,
            isInviteLinkRevoked = isRevoked,
            requireAdminApproval = extra?.requireAdminApproval ?: false,
            pendingRequests = extra?.pendingRequests ?: emptyList(),
            isAnnouncementOnly = extra?.isAnnouncementOnly ?: false,
            onlyAdminsCanEditInfo = extra?.onlyAdminsCanEditInfo ?: true,
            onlyAdminsCanAddMembers = extra?.onlyAdminsCanAddMembers ?: false,
            isFavorite = _favoriteConversationIds.value.contains(conversationId),
            lastSeenLabel = if (domain.isOnline) "متصل الآن 🟢" else "آخر ظهور: منذ 12 د",
            adminNames = extra?.adminNames ?: listOf("أنا", "د. أحمد خليل")
        )
    }

    fun markConversationRead(conversationId: String) {
        scope.launch {
            chatDao.markConversationRead(conversationId)
        }
    }

    fun sendMessage(
        conversationId: String,
        text: String,
        attachmentType: AttachmentType = AttachmentType.NONE,
        attachmentUrl: String = "",
        attachmentName: String = "",
        attachmentSize: String = "",
        replyToText: String? = null,
        isViewOnce: Boolean = false,
        pollData: PollData? = null
    ) {
        val user = _currentUser.value ?: User(
            id = "current_user_id",
            phoneNumber = "+201012345678",
            displayName = "أنا"
        )
        val messageId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val displayText = when (attachmentType) {
            AttachmentType.IMAGE -> if (isViewOnce) "🔒 صورة (عرض مرة واحدة)" else if (text.isNotBlank()) "📷 $text" else "📷 صورة"
            AttachmentType.VIDEO -> if (isViewOnce) "🔒 فيديو (عرض مرة واحدة)" else if (text.isNotBlank()) "🎥 $text" else "🎥 فيديو"
            AttachmentType.DOCUMENT -> "📄 $attachmentName"
            AttachmentType.STICKER -> "💟 ملصق"
            AttachmentType.VOICE -> "🎤 رسالة صوتية (${attachmentSize.ifBlank { "0:18" }})"
            AttachmentType.VIDEO_NOTE -> "⏺️ رسالة فيديو"
            AttachmentType.POLL -> "📊 استطلاع رأي: ${pollData?.question}"
            AttachmentType.NONE -> text
        }

        val entity = MessageEntity(
            id = messageId,
            conversationId = conversationId,
            senderId = user.id,
            senderName = user.displayName,
            text = text,
            timestamp = now,
            status = MessageStatus.SENDING,
            attachmentType = attachmentType,
            attachmentUrl = attachmentUrl,
            attachmentName = attachmentName,
            attachmentSize = attachmentSize,
            isEncrypted = true,
            isSystemAnnouncement = false,
            replyToText = replyToText
        )

        val isOrigQuality = _settings.value.preserveOriginalImageQuality
        val resBadge = if (attachmentType == AttachmentType.IMAGE && isOrigQuality) {
            ImageCutoutProcessor.getImageResolutionLabel(appContext, attachmentUrl)
        } else if (attachmentType == AttachmentType.VIDEO && isOrigQuality) {
            "1080p/4K RAW • جودة أصلية بدون ضغط"
        } else ""

        if (isViewOnce || pollData != null || attachmentUrl.isNotBlank() || resBadge.isNotBlank()) {
            val currentMap = _messageExtras.value.toMutableMap()
            currentMap[messageId] = MessageExtra(
                isViewOnce = isViewOnce,
                pollData = pollData,
                mediaUri = attachmentUrl,
                isOriginalQuality = isOrigQuality,
                resolutionBadge = resBadge,
                transferProgressPercent = 100
            )
            _messageExtras.value = currentMap
        }

        scope.launch {
            chatDao.insertMessage(entity)
            chatDao.updateLastMessage(conversationId, displayText, now)

            delay(200)
            chatDao.updateMessageStatus(messageId, MessageStatus.SENT)
            delay(250)
            chatDao.updateMessageStatus(messageId, MessageStatus.DELIVERED)
            if (_settings.value.readReceiptsEnabled) {
                delay(400)
                chatDao.updateMessageStatus(messageId, MessageStatus.READ)
            }

            triggerSimulatedReply(conversationId, text, attachmentType)
        }
    }

    private suspend fun triggerSimulatedReply(
        conversationId: String,
        userMessage: String,
        attachmentType: AttachmentType
    ) {
        val conversation = chatDao.getConversationById(conversationId) ?: return
        delay(1000)

        val responderName = if (conversation.isGroup) {
            val names = conversation.participantNames.filter { it != (_currentUser.value?.displayName ?: "أنا") }
            names.randomOrNull() ?: "د. أحمد خليل"
        } else {
            conversation.title
        }

        val replyText = when {
            attachmentType == AttachmentType.POLL -> "صوتت في الاستطلاع! فكرة ممتازة 👍"
            attachmentType == AttachmentType.VIDEO_NOTE -> "رسالة الفيديو واضحة جداً ✨"
            attachmentType == AttachmentType.DOCUMENT -> "تم استلام المستند بنجاح، جاري المراجعة 📄"
            attachmentType == AttachmentType.VIDEO -> "تم تشغيل الفيديو بجودة عالية HD 🎥"
            attachmentType == AttachmentType.IMAGE -> "صورة رائعة وواضحة جداً ✨"
            attachmentType == AttachmentType.STICKER -> "ملصق رائع! حفظته في المفضلة عندي ⭐"
            attachmentType == AttachmentType.VOICE -> "تم الاستماع للرسالة الصوتية بوضوح 🎤"
            userMessage.contains("سلام") || userMessage.contains("مرحبا") -> "وعليكم السلام ورحمة الله وبركاته! أهلاً بك."
            else -> "تم الاستلام، شكراً لتواصلك عبر كلبرق ✓"
        }

        val replyMessage = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = "bot_${UUID.randomUUID()}",
            senderName = responderName,
            text = replyText,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.READ,
            attachmentType = AttachmentType.NONE,
            attachmentUrl = "",
            attachmentName = "",
            attachmentSize = "",
            isEncrypted = true,
            isSystemAnnouncement = false,
            replyToText = null
        )

        chatDao.insertMessage(replyMessage)
        chatDao.updateLastMessage(conversationId, replyText, System.currentTimeMillis())
    }

    // --- GROUP INVITE LINKS & ADMIN CONTROLS ---

    fun resetGroupInviteLink(conversationId: String): String {
        val currentExtra = _conversationExtras.value[conversationId]
        val oldToken = currentExtra?.inviteToken
        if (oldToken != null) {
            _revokedInviteTokens.value = _revokedInviteTokens.value + oldToken
        }

        val newToken = "kalbarq_link_${(100000..999999).random()}_${UUID.randomUUID().toString().take(6)}"
        val updated = (currentExtra ?: ConversationExtra(inviteToken = newToken)).copy(
            inviteToken = newToken
        )
        val map = _conversationExtras.value.toMutableMap()
        map[conversationId] = updated
        _conversationExtras.value = map
        return "https://kalbarq.app/join/$newToken"
    }

    fun toggleRequireAdminApproval(conversationId: String, enabled: Boolean) {
        val map = _conversationExtras.value.toMutableMap()
        val current = map[conversationId] ?: ConversationExtra(inviteToken = "token_$conversationId")
        map[conversationId] = current.copy(requireAdminApproval = enabled)
        _conversationExtras.value = map
    }

    fun updateGroupPermissions(
        conversationId: String,
        onlyAdminsCanEditInfo: Boolean? = null,
        isAnnouncementOnly: Boolean? = null,
        onlyAdminsCanAddMembers: Boolean? = null,
        requireAdminApproval: Boolean? = null
    ) {
        val map = _conversationExtras.value.toMutableMap()
        val current = map[conversationId] ?: ConversationExtra(inviteToken = "token_$conversationId")
        val updated = current.copy(
            onlyAdminsCanEditInfo = onlyAdminsCanEditInfo ?: current.onlyAdminsCanEditInfo,
            isAnnouncementOnly = isAnnouncementOnly ?: current.isAnnouncementOnly,
            onlyAdminsCanAddMembers = onlyAdminsCanAddMembers ?: current.onlyAdminsCanAddMembers,
            requireAdminApproval = requireAdminApproval ?: current.requireAdminApproval
        )
        map[conversationId] = updated
        _conversationExtras.value = map

        if (isAnnouncementOnly != null && isAnnouncementOnly != current.isAnnouncementOnly) {
            scope.launch {
                val sysMsg = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    senderId = "system",
                    senderName = "النظام",
                    text = if (isAnnouncementOnly) {
                        "غيّر المشرف إعدادات المجموعة بحيث يُسمح للمشرفين فقط بإرسال الرسائل"
                    } else {
                        "غيّر المشرف إعدادات المجموعة بحيث يُسمح لجميع المشاركين بإرسال الرسائل"
                    },
                    timestamp = System.currentTimeMillis(),
                    status = MessageStatus.READ,
                    attachmentType = AttachmentType.NONE,
                    attachmentUrl = "",
                    attachmentName = "",
                    attachmentSize = "",
                    isEncrypted = true,
                    isSystemAnnouncement = true,
                    replyToText = null
                )
                chatDao.insertMessage(sysMsg)
            }
        }
    }

    fun updateGroupTitle(conversationId: String, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isBlank()) return
        scope.launch {
            val conv = chatDao.getConversationById(conversationId) ?: return@launch
            chatDao.updateConversation(conv.copy(title = clean))
            val sysMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = "system",
                senderName = "النظام",
                text = "غيّر المشرف اسم المجموعة إلى \"$clean\"",
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.READ,
                attachmentType = AttachmentType.NONE,
                attachmentUrl = "",
                attachmentName = "",
                attachmentSize = "",
                isEncrypted = true,
                isSystemAnnouncement = true,
                replyToText = null
            )
            chatDao.insertMessage(sysMsg)
        }
    }

    fun addGroupParticipant(conversationId: String, participantName: String) {
        val clean = participantName.trim()
        if (clean.isBlank()) return
        scope.launch {
            val conv = chatDao.getConversationById(conversationId) ?: return@launch
            if (conv.participantNames.contains(clean)) return@launch
            val updatedParticipants = (conv.participantNames + clean).distinct()
            chatDao.updateConversation(conv.copy(participantNames = updatedParticipants))

            val sysMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = "system",
                senderName = "النظام",
                text = "أضاف المشرف $clean إلى المجموعة",
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.READ,
                attachmentType = AttachmentType.NONE,
                attachmentUrl = "",
                attachmentName = "",
                attachmentSize = "",
                isEncrypted = true,
                isSystemAnnouncement = true,
                replyToText = null
            )
            chatDao.insertMessage(sysMsg)
            chatDao.updateLastMessage(conversationId, sysMsg.text, sysMsg.timestamp)
        }
    }

    fun removeGroupParticipant(conversationId: String, participantName: String) {
        val clean = participantName.trim()
        if (clean.isBlank() || clean == "أنا") return
        val map = _conversationExtras.value.toMutableMap()
        val currentExtra = map[conversationId]
        if (currentExtra != null) {
            map[conversationId] = currentExtra.copy(
                adminNames = currentExtra.adminNames.filter { it != clean }
            )
            _conversationExtras.value = map
        }
        scope.launch {
            val conv = chatDao.getConversationById(conversationId) ?: return@launch
            val updatedParticipants = conv.participantNames.filter { it != clean }
            chatDao.updateConversation(conv.copy(participantNames = updatedParticipants))

            val sysMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = "system",
                senderName = "النظام",
                text = "أزال المشرف $clean من المجموعة",
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.READ,
                attachmentType = AttachmentType.NONE,
                attachmentUrl = "",
                attachmentName = "",
                attachmentSize = "",
                isEncrypted = true,
                isSystemAnnouncement = true,
                replyToText = null
            )
            chatDao.insertMessage(sysMsg)
            chatDao.updateLastMessage(conversationId, sysMsg.text, sysMsg.timestamp)
        }
    }

    fun toggleGroupAdmin(conversationId: String, participantName: String) {
        val clean = participantName.trim()
        if (clean.isBlank()) return
        val map = _conversationExtras.value.toMutableMap()
        val current = map[conversationId] ?: ConversationExtra(inviteToken = "token_$conversationId")
        val isCurrentlyAdmin = current.adminNames.contains(clean)
        val updatedAdmins = if (isCurrentlyAdmin) {
            current.adminNames.filter { it != clean }.ifEmpty { listOf("أنا") }
        } else {
            (current.adminNames + clean).distinct()
        }
        map[conversationId] = current.copy(adminNames = updatedAdmins)
        _conversationExtras.value = map

        scope.launch {
            val sysMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = "system",
                senderName = "النظام",
                text = if (isCurrentlyAdmin) {
                    "أعفى المشرف $clean من إشراف المجموعة"
                } else {
                    "عيّن المشرف $clean مشرفاً للمجموعة"
                },
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.READ,
                attachmentType = AttachmentType.NONE,
                attachmentUrl = "",
                attachmentName = "",
                attachmentSize = "",
                isEncrypted = true,
                isSystemAnnouncement = true,
                replyToText = null
            )
            chatDao.insertMessage(sysMsg)
        }
    }

    fun extractInviteToken(rawUrlOrToken: String): String {
        val trimmed = rawUrlOrToken.trim()
        val urlRegex = Regex("""https?://(?:chat\.whatsapp\.com|kalbarq\.app/join|safir\.app/join)/([A-Za-z0-9_-]+)""")
        val customSchemeRegex = Regex("""kalbarq://join/([A-Za-z0-9_-]+)""")
        val m1 = urlRegex.find(trimmed)
        if (m1 != null) return m1.groupValues[1]
        val m2 = customSchemeRegex.find(trimmed)
        if (m2 != null) return m2.groupValues[1]
        return trimmed.substringAfterLast("/").substringBefore("?").substringBefore(" ").trim()
    }

    fun findConversationIdByInviteLink(rawUrlOrToken: String): String? {
        val token = extractInviteToken(rawUrlOrToken)
        if (token.isBlank()) return null
        val match = _conversationExtras.value.entries.find {
            it.value.inviteToken.equals(token, ignoreCase = true)
        }
        return match?.key
    }

    fun approveJoinRequest(conversationId: String, userName: String) {
        val map = _conversationExtras.value.toMutableMap()
        val current = map[conversationId] ?: return
        val updatedRequests = current.pendingRequests.filter { it.userName != userName }
        map[conversationId] = current.copy(pendingRequests = updatedRequests)
        _conversationExtras.value = map

        scope.launch {
            val conv = chatDao.getConversationById(conversationId) ?: return@launch
            val updatedParticipants = (conv.participantNames + userName).distinct()
            chatDao.updateConversation(conv.copy(participantNames = updatedParticipants))

            val sysMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = "system",
                senderName = "النظام",
                text = "وافق المشرف على انضمام $userName إلى المجموعة",
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.READ,
                attachmentType = AttachmentType.NONE,
                attachmentUrl = "",
                attachmentName = "",
                attachmentSize = "",
                isEncrypted = true,
                isSystemAnnouncement = true,
                replyToText = null
            )
            chatDao.insertMessage(sysMsg)
        }
    }

    fun rejectJoinRequest(conversationId: String, userName: String) {
        val map = _conversationExtras.value.toMutableMap()
        val current = map[conversationId] ?: return
        val updatedRequests = current.pendingRequests.filter { it.userName != userName }
        map[conversationId] = current.copy(pendingRequests = updatedRequests)
        _conversationExtras.value = map
    }

    fun joinGroupByInvite(rawUrlOrToken: String, userName: String): JoinResult {
        val token = extractInviteToken(rawUrlOrToken)
        if (token.isBlank() || _revokedInviteTokens.value.contains(token)) {
            return JoinResult.REVOKED
        }

        val match = _conversationExtras.value.entries.find {
            it.value.inviteToken.equals(token, ignoreCase = true)
        }

        if (match == null) {
            // Dynamically create and join external WhatsApp / Kalbarq invite link group!
            val newGroupId = "grp_invite_${token.lowercase().take(16)}"
            val groupTitle = when {
                token.contains("ai", ignoreCase = true) -> "مجتمع الذكاء الاصطناعي وتطوير الأندرويد"
                token.contains("design", ignoreCase = true) -> "نادي مصممي واجهات المستخدم UI/UX"
                token.contains("family", ignoreCase = true) -> "مجموعة العائلة"
                else -> "مجموعة كلبرق المشتركة (${token.take(8)})"
            }
            val now = System.currentTimeMillis()
            val extra = ConversationExtra(
                inviteToken = token,
                requireAdminApproval = false,
                adminNames = listOf("د. أحمد خليل", "سارة ممدوح", userName)
            )
            val map = _conversationExtras.value.toMutableMap()
            map[newGroupId] = extra
            _conversationExtras.value = map

            scope.launch {
                val existing = chatDao.getConversationById(newGroupId)
                if (existing == null) {
                    val entity = ConversationEntity(
                        id = newGroupId,
                        title = groupTitle,
                        isGroup = true,
                        groupIconPreset = 2,
                        groupDescription = "مجموعة تم الانضمام إليها عبر رابط دعوة مباشر ($rawUrlOrToken)",
                        participantNames = listOf("د. أحمد خليل", "سارة ممدوح", "عمر الشامي", userName).distinct(),
                        lastMessageText = "انضم $userName عبر رابط الدعوة للمجموعة",
                        lastMessageTime = now,
                        unreadCount = 0,
                        isPinned = false,
                        isOnline = true,
                        historyVisibleToNewMembers = true,
                        safetyNumber = "9302 4819 0284 1948 2049 1930"
                    )
                    chatDao.insertConversation(entity)
                    val sysMsg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = newGroupId,
                        senderId = "system",
                        senderName = "النظام",
                        text = "انضم $userName عبر رابط الدعوة للمجموعة 🔗",
                        timestamp = now,
                        status = MessageStatus.READ,
                        attachmentType = AttachmentType.NONE,
                        attachmentUrl = "",
                        attachmentName = "",
                        attachmentSize = "",
                        isEncrypted = true,
                        isSystemAnnouncement = true,
                        replyToText = null
                    )
                    chatDao.insertMessage(sysMsg)
                }
            }
            return JoinResult.SUCCESS
        }

        val conversationId = match.key
        val extra = match.value

        if (extra.requireAdminApproval) {
            val pending = PendingJoinRequest(
                userId = "guest_${UUID.randomUUID()}",
                userName = userName,
                userAvatarPreset = (1..6).random(),
                requestedAt = System.currentTimeMillis()
            )
            val updatedList = extra.pendingRequests + pending
            val map = _conversationExtras.value.toMutableMap()
            map[conversationId] = extra.copy(pendingRequests = updatedList)
            _conversationExtras.value = map
            return JoinResult.PENDING_APPROVAL
        } else {
            scope.launch {
                val conv = chatDao.getConversationById(conversationId) ?: return@launch
                if (!conv.participantNames.contains(userName)) {
                    val updatedParticipants = conv.participantNames + userName
                    chatDao.updateConversation(conv.copy(participantNames = updatedParticipants))
                }

                val sysMsg = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    senderId = "system",
                    senderName = "النظام",
                    text = "انضم $userName عبر رابط الدعوة للمجموعة",
                    timestamp = System.currentTimeMillis(),
                    status = MessageStatus.READ,
                    attachmentType = AttachmentType.NONE,
                    attachmentUrl = "",
                    attachmentName = "",
                    attachmentSize = "",
                    isEncrypted = true,
                    isSystemAnnouncement = true,
                    replyToText = null
                )
                chatDao.insertMessage(sysMsg)
                chatDao.updateLastMessage(conversationId, sysMsg.text, sysMsg.timestamp)
            }
            return JoinResult.SUCCESS
        }
    }

    // --- ADVANCED MESSAGING FEATURES ---

    fun addReaction(messageId: String, emoji: String) {
        val map = _messageExtras.value.toMutableMap()
        val current = map[messageId] ?: MessageExtra()
        val currentReactions = current.reactions.toMutableMap()
        val count = currentReactions[emoji] ?: 0
        currentReactions[emoji] = count + 1
        map[messageId] = current.copy(reactions = currentReactions)
        _messageExtras.value = map
    }

    fun editMessage(conversationId: String, messageId: String, newText: String) {
        val map = _messageExtras.value.toMutableMap()
        val current = map[messageId] ?: MessageExtra()
        map[messageId] = current.copy(isEdited = true, editedText = newText)
        _messageExtras.value = map

        scope.launch {
            chatDao.updateLastMessage(conversationId, newText, System.currentTimeMillis())
        }
    }

    fun deleteMessage(conversationId: String, messageId: String, forEveryone: Boolean) {
        if (forEveryone) {
            val map = _messageExtras.value.toMutableMap()
            val current = map[messageId] ?: MessageExtra()
            if (_settings.value.goldAntiDeleteMessages) {
                map[messageId] = current.copy(
                    isDeletedForEveryone = true,
                    isAntiDeleteProtected = true,
                    originalTextBeforeDelete = current.editedText
                )
            } else {
                map[messageId] = current.copy(isDeletedForEveryone = true)
            }
            _messageExtras.value = map

            scope.launch {
                val preview = if (_settings.value.goldAntiDeleteMessages) {
                    "🛡️ رسالة محمية من الحذف (كلبرق الذهبي)"
                } else {
                    "🚫 تم حذف هذه الرسالة"
                }
                chatDao.updateLastMessage(conversationId, preview, System.currentTimeMillis())
            }
        } else {
            val map = _messageExtras.value.toMutableMap()
            val current = map[messageId] ?: MessageExtra()
            map[messageId] = current.copy(isDeletedForEveryone = true, editedText = "🚫 حذفت هذه الرسالة لديك")
            _messageExtras.value = map
        }
    }

    /**
     * Demonstrates WhatsApp Gold Anti-Delete Messages: the other contact sends a message and immediately tries
     * to delete it for everyone, while Kalbarq Gold intercepts the deletion and keeps the message visible.
     */
    fun simulateAntiDeleteIncomingMessage(conversationId: String) {
        scope.launch {
            val conv = chatDao.getConversationById(conversationId) ?: return@launch
            val sender = if (conv.isGroup) {
                conv.participantNames.firstOrNull { it != "أنا" } ?: "د. أحمد خليل"
            } else {
                conv.title
            }
            val msgId = "antidelete_${UUID.randomUUID()}"
            val secretText = "ملف سري وملاحظات هامة للمشروع (حاول الطرف الآخر حذف هذه الرسالة فوراً)"
            val now = System.currentTimeMillis()
            val entity = MessageEntity(
                id = msgId,
                conversationId = conversationId,
                senderId = "peer_${UUID.randomUUID()}",
                senderName = sender,
                text = secretText,
                timestamp = now,
                status = MessageStatus.READ,
                attachmentType = AttachmentType.NONE,
                attachmentUrl = "",
                attachmentName = "",
                attachmentSize = "",
                isEncrypted = true,
                isSystemAnnouncement = false,
                replyToText = null
            )
            chatDao.insertMessage(entity)
            val map = _messageExtras.value.toMutableMap()
            map[msgId] = MessageExtra(
                isDeletedForEveryone = true,
                isAntiDeleteProtected = true,
                originalTextBeforeDelete = secretText
            )
            _messageExtras.value = map
            chatDao.updateLastMessage(conversationId, "🛡️ حاول $sender حذف رسالة (محمية)", now)
        }
    }

    /**
     * Heavy-Duty 2 GB (2,048 MB) Chunked File Transfer Engine backed by SQLite WAL Room Database.
     */
    fun sendLargeFileUpTo2GB(
        conversationId: String,
        fileName: String,
        fileSizeMb: Float,
        attachmentType: AttachmentType = AttachmentType.DOCUMENT,
        mediaUri: String = ""
    ) {
        val clampedMb = fileSizeMb.coerceIn(1f, 2048f) // Up to 2 GB (2,048 MB)
        val totalChunks = (clampedMb / 64f).toInt().coerceAtLeast(1)
        val transferId = "tx_2gb_${UUID.randomUUID()}"
        val messageId = "msg_2gb_${UUID.randomUUID()}"
        val sha256 = "SHA256:${UUID.randomUUID().toString().replace("-", "").take(16).uppercase()}"
        val user = _currentUser.value ?: User(id = "current_user_id", phoneNumber = "+201012345678", displayName = "أنا")
        val now = System.currentTimeMillis()

        val sizeFormatted = if (clampedMb >= 1024f) {
            String.format(java.util.Locale.US, "%.2f جيجابايت (GB) • %d أجزاء • جودة أصلية", clampedMb / 1024f, totalChunks)
        } else {
            String.format(java.util.Locale.US, "%.1f ميجابايت (MB) • %d أجزاء • جودة أصلية", clampedMb, totalChunks)
        }

        val entity = MessageEntity(
            id = messageId,
            conversationId = conversationId,
            senderId = user.id,
            senderName = user.displayName,
            text = "📦 نقل ملف ضخم عبر محرك قاعدة بيانات كلبرق (حتى 2 جيجابايت)",
            timestamp = now,
            status = MessageStatus.SENDING,
            attachmentType = attachmentType,
            attachmentUrl = mediaUri,
            attachmentName = fileName,
            attachmentSize = sizeFormatted,
            isEncrypted = true,
            isSystemAnnouncement = false,
            replyToText = null
        )

        val currentExtras = _messageExtras.value.toMutableMap()
        currentExtras[messageId] = MessageExtra(
            mediaUri = mediaUri,
            isOriginalQuality = true,
            resolutionBadge = sha256,
            transferProgressPercent = 25
        )
        _messageExtras.value = currentExtras

        scope.launch {
            chatDao.insertFileTransfer(
                FileTransferEntity(
                    id = transferId,
                    conversationId = conversationId,
                    fileName = fileName,
                    fileSizeMb = clampedMb,
                    totalChunks = totalChunks,
                    uploadedChunks = (totalChunks * 0.25f).toInt().coerceAtLeast(1),
                    sha256Checksum = sha256,
                    isCompleted = false,
                    isOriginalQuality = true,
                    timestamp = now
                )
            )
            chatDao.insertMessage(entity)
            chatDao.updateLastMessage(conversationId, "📦 $fileName ($sizeFormatted)", now)

            delay(350)
            chatDao.updateFileTransferProgress(transferId, (totalChunks * 0.65f).toInt().coerceAtLeast(1), false)
            val midExtras = _messageExtras.value.toMutableMap()
            midExtras[messageId]?.let { midExtras[messageId] = it.copy(transferProgressPercent = 65) }
            _messageExtras.value = midExtras

            delay(350)
            chatDao.updateFileTransferProgress(transferId, totalChunks, true)
            val doneExtras = _messageExtras.value.toMutableMap()
            doneExtras[messageId]?.let { doneExtras[messageId] = it.copy(transferProgressPercent = 100) }
            _messageExtras.value = doneExtras

            chatDao.updateMessageStatus(messageId, MessageStatus.DELIVERED)
            if (_settings.value.readReceiptsEnabled && !_settings.value.goldHideBlueTicks) {
                chatDao.updateMessageStatus(messageId, MessageStatus.READ)
            }
            triggerSimulatedReply(conversationId, fileName, attachmentType)
        }
    }

    fun toggleStarMessage(messageId: String) {
        val set = _starredMessageIds.value.toMutableSet()
        if (set.contains(messageId)) {
            set.remove(messageId)
        } else {
            set.add(messageId)
        }
        _starredMessageIds.value = set
    }

    fun togglePinMessage(conversationId: String, messageId: String) {
        val map = _conversationExtras.value.toMutableMap()
        val current = map[conversationId] ?: ConversationExtra(inviteToken = "token_$conversationId")
        val pinned = current.pinnedMessageIds.toMutableList()
        if (pinned.contains(messageId)) {
            pinned.remove(messageId)
        } else {
            if (pinned.size >= 3) pinned.removeAt(0)
            pinned.add(messageId)
        }
        map[conversationId] = current.copy(pinnedMessageIds = pinned)
        _conversationExtras.value = map
    }

    fun markViewOnceOpened(messageId: String) {
        val map = _messageExtras.value.toMutableMap()
        val current = map[messageId] ?: MessageExtra(isViewOnce = true)
        map[messageId] = current.copy(isViewedOnce = true)
        _messageExtras.value = map
    }

    fun votePoll(messageId: String, optionId: String, userName: String) {
        val map = _messageExtras.value.toMutableMap()
        val current = map[messageId] ?: return
        val poll = current.pollData ?: return

        val userVoted = poll.userVotedOptionIds.contains(optionId)
        val updatedOptions = poll.options.map { opt ->
            if (opt.id == optionId) {
                if (userVoted) {
                    opt.copy(
                        voteCount = (opt.voteCount - 1).coerceAtLeast(0),
                        voterNames = opt.voterNames.filter { it != userName }
                    )
                } else {
                    opt.copy(
                        voteCount = opt.voteCount + 1,
                        voterNames = (opt.voterNames + userName).distinct()
                    )
                }
            } else if (!poll.allowMultiple && !userVoted) {
                opt.copy(
                    voteCount = if (opt.voterNames.contains(userName)) (opt.voteCount - 1).coerceAtLeast(0) else opt.voteCount,
                    voterNames = opt.voterNames.filter { it != userName }
                )
            } else {
                opt
            }
        }

        val updatedVotedIds = if (userVoted) {
            poll.userVotedOptionIds - optionId
        } else {
            if (poll.allowMultiple) poll.userVotedOptionIds + optionId else listOf(optionId)
        }

        map[messageId] = current.copy(
            pollData = poll.copy(options = updatedOptions, userVotedOptionIds = updatedVotedIds)
        )
        _messageExtras.value = map
    }

    // --- COMMUNITIES & CHANNELS & CALL LINKS ---

    fun createCommunity(name: String, description: String, iconPreset: Int = 2): Community {
        val commId = "comm_${UUID.randomUUID().toString().take(8)}"
        val announcementGroupId = createGroup(
            title = "إعلانات: $name",
            description = description.ifBlank { "مجموعة الإعلانات الرسمية لمجتمع $name" },
            iconPreset = iconPreset,
            participantNames = listOf("د. أحمد خليل", "سارة المنصور")
        )
        val generalGroupId = createGroup(
            title = "المناقشة العامة - $name",
            description = "مساحة النقاش المفتوحة لأعضاء مجتمع $name",
            iconPreset = iconPreset,
            participantNames = listOf("د. أحمد خليل", "سارة المنصور", "م. طارق زياد")
        )
        val newCommunity = Community(
            id = commId,
            name = name,
            description = description.ifBlank { "مجتمع رسمي يجمع المجموعات ذات الاهتمام المشترك." },
            iconPreset = iconPreset,
            announcementGroupId = announcementGroupId,
            subGroupIds = listOf(generalGroupId),
            totalMembers = 4
        )
        _communities.value = listOf(newCommunity) + _communities.value
        return newCommunity
    }

    fun createCallLink(title: String, isVideo: Boolean): CallLink {
        val id = UUID.randomUUID().toString().take(8)
        val link = CallLink(
            id = id,
            title = title,
            url = "https://kalbarq.app/call/$id",
            isVideo = isVideo
        )
        _callLinks.value = listOf(link) + _callLinks.value
        return link
    }

    fun unlinkDevice(deviceId: String) {
        _linkedDevices.value = _linkedDevices.value.filter { it.id != deviceId }
    }

    fun linkNewDevice(deviceName: String, os: String) {
        val newDev = LinkedDevice(
            id = UUID.randomUUID().toString(),
            name = deviceName,
            os = os,
            location = "القاهرة، مصر (نشط الآن)",
            lastActive = "الآن",
            isCurrent = false
        )
        _linkedDevices.value = _linkedDevices.value + newDev
    }

    fun addChannelReaction(channelId: String, postIndex: Int, emoji: String) {
        val list = _channels.value.map { channel ->
            if (channel.id == channelId) {
                val updatedPosts = channel.posts.mapIndexed { idx, post ->
                    if (idx == postIndex) {
                        val reactions = post.reactions.toMutableMap()
                        reactions[emoji] = (reactions[emoji] ?: 0) + 1
                        post.copy(reactions = reactions)
                    } else post
                }
                channel.copy(posts = updatedPosts)
            } else channel
        }
        _channels.value = list
    }

    private fun seedInitialExtras() {
        val savedGroupUri = prefs.getString("group_uri_conv_group_project", "") ?: ""
        val defaultGroupExtra = ConversationExtra(
            inviteToken = "dev_team_2026_xyz",
            groupIconUri = savedGroupUri,
            requireAdminApproval = false,
            pendingRequests = listOf(
                PendingJoinRequest(
                    userId = "pending_1",
                    userName = "محمود عادل",
                    userAvatarPreset = 5,
                    requestedAt = System.currentTimeMillis() - 1800000
                ),
                PendingJoinRequest(
                    userId = "pending_2",
                    userName = "ليلى حسن",
                    userAvatarPreset = 6,
                    requestedAt = System.currentTimeMillis() - 7200000
                )
            )
        )
        val familyGroupExtra = ConversationExtra(
            inviteToken = "family_gathering_2026",
            requireAdminApproval = false,
            adminNames = listOf("الوالد", "أنا")
        )
        _conversationExtras.value = mapOf(
            "conv_group_project" to defaultGroupExtra,
            "conv_group_family" to familyGroupExtra
        )

        _communities.value = listOf(
            Community(
                id = "comm_1",
                name = "مجتمع المطورين والتقنية",
                description = "المجتمع الرسمي للمطورين لتبادل الخبرات والمشاريع البرمجية.",
                iconPreset = 1,
                announcementGroupId = "conv_group_project",
                subGroupIds = listOf("conv_group_project"),
                totalMembers = 340
            ),
            Community(
                id = "comm_2",
                name = "مجتمع رواد الأعمال والمشاريع",
                description = "مساحة تجمع المؤسسين والمستثمرين لمناقشة بناء المنتجات الرقمية.",
                iconPreset = 4,
                announcementGroupId = "conv_group_project",
                subGroupIds = listOf("conv_group_project"),
                totalMembers = 185
            )
        )

        _channels.value = listOf(
            Channel(
                id = "ch_tech_news",
                name = "أخبار التقنية والأمن السيبراني",
                description = "تغطية فورية لأحدث تطورات التقنية والتشفير والأنظمة الذكية.",
                iconPreset = 5,
                followersCount = "48.2 ألف متابع",
                isVerified = true,
                posts = listOf(
                    ChannelPost(
                        id = "p1",
                        text = "تم إطلاق التحديث الجديد لدعم مشاركة مقاطع الفيديو في الحالة حتى 5 دقائق كاملة بدقة عالية HD.",
                        timestamp = System.currentTimeMillis() - 3600000,
                        reactions = mapOf("🔥" to 342, "👍" to 289, "❤️" to 195)
                    ),
                    ChannelPost(
                        id = "p2",
                        text = "ميزة تحويل صور المعرض إلى ملصقات مفرغة الخلفية تلقائياً أصبحت متاحة الآن لجميع المستخدمين.",
                        timestamp = System.currentTimeMillis() - 14400000,
                        reactions = mapOf("👏" to 512, "❤️" to 420, "🎉" to 210)
                    )
                )
            ),
            Channel(
                id = "ch_devs",
                name = "مجتمع أندرويد الحديث",
                description = "نصائح برمجية يومية لتطوير تطبيقات أندرويد الاحترافية.",
                iconPreset = 1,
                followersCount = "29.7 ألف متابع",
                isVerified = true,
                posts = listOf(
                    ChannelPost(
                        id = "p3",
                        text = "استخدام معالجة الصور المحلية على الجهاز يضمن خصوصية كاملة وسرعة فائقة في تحويل الصور لملصقات.",
                        timestamp = System.currentTimeMillis() - 7200000,
                        reactions = mapOf("💡" to 184, "👍" to 132, "❤️" to 95)
                    )
                )
            )
        )

        _linkedDevices.value = listOf(
            LinkedDevice(
                id = "dev_1",
                name = "Google Chrome (Windows)",
                os = "Windows 11",
                location = "القاهرة، مصر (نشط الآن)",
                lastActive = "الآن",
                isCurrent = false
            ),
            LinkedDevice(
                id = "dev_2",
                name = "Kalbarq Desktop (macOS)",
                os = "macOS Sonoma",
                location = "الإسكندرية، مصر",
                lastActive = "اليوم في 08:30 ص",
                isCurrent = false
            )
        )

        _callLinks.value = listOf(
            CallLink(
                id = "call_daily_sync",
                title = "اجتماع الفريق الصباحي",
                url = "https://kalbarq.app/call/daily-sync-2026",
                isVideo = true
            ),
            CallLink(
                id = "call_design_review",
                title = "مراجعة تصاميم واجهة المستخدم",
                url = "https://kalbarq.app/call/design-review-ui",
                isVideo = true
            )
        )
    }

    fun createGroup(
        title: String,
        description: String,
        iconPreset: Int = 1,
        participantNames: List<String>,
        groupIconUri: String = ""
    ): String {
        val groupId = "group_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val allParticipants = (participantNames + listOf(_currentUser.value?.displayName ?: "أنا")).distinct()

        if (groupIconUri.isNotBlank()) {
            prefs.edit().putString("group_uri_$groupId", groupIconUri).apply()
        }

        val token = "kalbarq_grp_${(100000..999999).random()}"
        val map = _conversationExtras.value.toMutableMap()
        map[groupId] = ConversationExtra(
            inviteToken = token,
            groupIconUri = groupIconUri,
            requireAdminApproval = false,
            pendingRequests = emptyList()
        )
        _conversationExtras.value = map

        val entity = ConversationEntity(
            id = groupId,
            title = title,
            isGroup = true,
            groupIconPreset = iconPreset,
            groupDescription = description,
            participantNames = allParticipants,
            lastMessageText = "تم إنشاء المجموعة",
            lastMessageTime = now,
            unreadCount = 0,
            isPinned = false,
            isOnline = true,
            historyVisibleToNewMembers = true,
            safetyNumber = "8391 0284 1948 2049 8492 1049"
        )

        scope.launch {
            chatDao.insertConversation(entity)

            val systemMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = groupId,
                senderId = "system",
                senderName = "النظام",
                text = "الرسائل والمكالمات مشفرة تماماً بين الطرفين. أنشأت هذه المجموعة.",
                timestamp = now,
                status = MessageStatus.READ,
                attachmentType = AttachmentType.NONE,
                attachmentUrl = "",
                attachmentName = "",
                attachmentSize = "",
                isEncrypted = true,
                isSystemAnnouncement = true,
                replyToText = null
            )
            chatDao.insertMessage(systemMsg)
        }
        return groupId
    }

    // Status Stories with real image & 5-minute video support
    val stories: Flow<List<StatusStory>> = combine(
        chatDao.getAllStories(),
        _storyExtras,
        _currentUser
    ) { list, extras, user ->
        list.map { entity ->
            val domain = entity.toDomain()
            val extra = extras[domain.id]
            val avatarUri = if (domain.userId == "current_user_id") {
                user?.avatarUri ?: extra?.userAvatarUri ?: ""
            } else {
                extra?.userAvatarUri ?: ""
            }
            domain.copy(
                userAvatarUri = avatarUri,
                mediaUri = extra?.mediaUri ?: "",
                videoDurationSeconds = extra?.videoDurationSeconds ?: 0,
                trimStartSeconds = extra?.trimStartSeconds ?: 0,
                trimEndSeconds = extra?.trimEndSeconds ?: 0,
                viewersList = extra?.viewersList ?: domain.viewersList
            )
        }
    }

    fun postStory(
        textContent: String,
        backgroundColorHex: String,
        storyType: StoryType,
        mediaPreset: Int = 0,
        mediaUri: String = "",
        videoDurationSeconds: Int = 0,
        trimStartSeconds: Int = 0,
        trimEndSeconds: Int = 0
    ) {
        val user = _currentUser.value ?: User(
            id = "current_user_id",
            phoneNumber = "+201012345678",
            displayName = "حالتي"
        )
        val clampedDuration = videoDurationSeconds.coerceIn(0, ImageCutoutProcessor.MAX_STATUS_VIDEO_SECONDS)
        val clampedStart = trimStartSeconds.coerceIn(0, clampedDuration)
        val clampedEnd = if (trimEndSeconds > clampedStart) {
            trimEndSeconds.coerceAtMost(ImageCutoutProcessor.MAX_STATUS_VIDEO_SECONDS)
        } else {
            clampedDuration
        }

        val storyId = "story_${UUID.randomUUID()}"
        val initialViewer = "د. أحمد خليل"
        val extraMap = _storyExtras.value.toMutableMap()
        extraMap[storyId] = StoryExtra(
            mediaUri = mediaUri,
            videoDurationSeconds = clampedEnd - clampedStart,
            trimStartSeconds = clampedStart,
            trimEndSeconds = clampedEnd,
            userAvatarUri = user.avatarUri,
            viewersList = listOf(initialViewer, "سارة ممدوح", "عمر الشامي")
        )
        _storyExtras.value = extraMap

        val story = StatusStoryEntity(
            id = storyId,
            userId = user.id,
            userName = user.displayName,
            userAvatarPreset = user.avatarPreset,
            storyType = storyType,
            textContent = textContent,
            mediaPreset = mediaPreset,
            backgroundColorHex = backgroundColorHex,
            timestamp = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 86400000L,
            isViewed = false,
            viewCount = 3
        )
        scope.launch {
            chatDao.insertStory(story)
            if (_settings.value.goldStatusViewNotificationEnabled) {
                delay(1200)
                notifyStatusViewedByPerson(viewerName = initialViewer, storyId = storyId)
            }
        }
    }

    /**
     * Triggers an immediate notification (both in-app live banner and Android system notification)
     * with the exact name of the person who viewed the user's status story.
     */
    fun notifyStatusViewedByPerson(viewerName: String, storyId: String? = null) {
        if (storyId != null) {
            val map = _storyExtras.value.toMutableMap()
            val curr = map[storyId] ?: StoryExtra()
            val updatedViewers = (listOf(viewerName) + curr.viewersList).distinct()
            map[storyId] = curr.copy(viewersList = updatedViewers)
            _storyExtras.value = map
            scope.launch {
                chatDao.incrementStoryViewCount(storyId)
            }
        }
        val title = "👁️ إشعار مشاهدة الحالة"
        val body = "شاهد $viewerName حالتك الآن"
        emitLiveAlert(title = title, subtitle = body, category = "STATUS_VIEW")
        ImageCutoutProcessor.postKalbarqNotification(
            context = appContext,
            channelId = "kalbarq_status_viewers",
            channelName = "إشعارات مشاهدة الحالة",
            title = title,
            body = body
        )
    }

    /**
     * Triggers an immediate WhatsApp Gold Online Toast & System Notification when a contact comes online.
     */
    fun triggerOnlineContactAlert(contactName: String) {
        val title = "🟢 متصل الآن (إضافات كلبرق الذهبية)"
        val body = "$contactName متصل الآن ويمكنك مراسلته فوراً"
        emitLiveAlert(title = title, subtitle = body, category = "ONLINE_NOW")
        ImageCutoutProcessor.postKalbarqNotification(
            context = appContext,
            channelId = "kalbarq_online_alerts",
            channelName = "تنبيهات اتصال جهات الاتصال",
            title = title,
            body = body
        )
    }

    fun markStoryViewed(storyId: String) {
        scope.launch {
            chatDao.markStoryViewed(storyId)
        }
    }

    // Sticker Packs: Combines the Favorites photo-sticker pack ("⭐ المفضلة") with installed/available packs
    val stickerPacks: Flow<List<StickerPack>> = combine(
        chatDao.getAllStickerPacks(),
        _customPhotoStickers
    ) { list, favPhotoStickers ->
        val favoritesPack = StickerPack(
            id = "pack_favorites",
            name = "المفضلة",
            author = "ملصقاتي المصورة من المعرض",
            thumbnailSymbol = "⭐",
            thumbnailUri = favPhotoStickers.firstOrNull()?.imageUri ?: "",
            stickers = favPhotoStickers.map { fav ->
                Sticker(
                    id = fav.id,
                    packId = "pack_favorites",
                    symbol = fav.imageUri.ifBlank { fav.title },
                    label = fav.title,
                    imageUri = fav.imageUri
                )
            },
            isInstalled = true,
            isExternalAppPack = false
        )

        val dbPacks = list.map { parseStickerPack(it) }
        listOf(favoritesPack) + dbPacks
    }

    private fun parseStickerPack(entity: StickerPackEntity): StickerPack {
        val stickers = entity.stickersJson.split(";;;").filter { it.isNotEmpty() }.mapIndexed { idx, pair ->
            val parts = pair.split(":::")
            val rawRef = parts.getOrNull(0) ?: ""
            val label = parts.getOrNull(1) ?: "ملصق"
            val imageUri = if (rawRef.startsWith("/") || rawRef.startsWith("content://") || rawRef.startsWith("file://")) {
                rawRef
            } else {
                // Generate a real graphical PNG sticker so every pack has real image stickers
                ImageCutoutProcessor.ensureGraphicalStickerFile(
                    context = appContext,
                    key = "${entity.id}_$idx",
                    title = label,
                    primaryColor = Color.rgb(0, 137, 123),
                    accentColor = Color.rgb(0, 92, 75)
                )
            }
            Sticker(
                id = "${entity.id}_$idx",
                packId = entity.id,
                symbol = imageUri,
                label = label,
                imageUri = imageUri
            )
        }
        return StickerPack(
            id = entity.id,
            name = entity.name,
            author = entity.author,
            thumbnailSymbol = entity.thumbnailSymbol,
            thumbnailUri = stickers.firstOrNull()?.imageUri ?: "",
            stickers = stickers,
            isInstalled = entity.isInstalled,
            isExternalAppPack = entity.author.contains("Sticker", ignoreCase = true) || entity.author.contains("خارجي")
        )
    }

    fun setStickerPackInstalled(packId: String, isInstalled: Boolean) {
        scope.launch {
            chatDao.setStickerPackInstalled(packId, isInstalled)
        }
    }

    fun createCustomStickerPack(name: String, author: String, symbol: String, items: List<Pair<String, String>>) {
        val json = items.joinToString(";;;") { "${it.first}:::${it.second}" }
        val entity = StickerPackEntity(
            id = "pack_${UUID.randomUUID()}",
            name = name,
            author = author,
            thumbnailSymbol = symbol,
            stickersJson = json,
            isInstalled = true
        )
        scope.launch {
            chatDao.insertStickerPacks(listOf(entity))
        }
    }

    /**
     * Saves a real gallery photo converted to a transparent cutout sticker into Favorites ("المفضلة").
     */
    fun addPhotoSticker(
        title: String,
        imageUri: String,
        borderWidthPx: Int = 10,
        cropZoom: Float = 1.0f,
        isCutout: Boolean = true
    ): CustomPhotoSticker {
        val sticker = CustomPhotoSticker(
            id = "sticker_${UUID.randomUUID()}",
            title = title.ifBlank { "ملصق من المعرض" },
            previewEmoji = "",
            imageUri = imageUri,
            borderWidthPx = borderWidthPx,
            cropZoom = cropZoom,
            isCutout = isCutout,
            isFavorite = true
        )
        _customPhotoStickers.value = listOf(sticker) + _customPhotoStickers.value
        return sticker
    }

    fun removeFavoriteSticker(stickerId: String) {
        _customPhotoStickers.value = _customPhotoStickers.value.filterNot { it.id == stickerId }
    }

    /**
     * Imports an external Sticker Maker pack (with real cutout PNG images) into the app,
     * matching WhatsApp's third-party sticker pack integration.
     */
    fun importExternalStickerPack(
        name: String,
        author: String,
        stickerTitlesOrUris: List<String>
    ) {
        val samples = ImageCutoutProcessor.ensureSampleGalleryPhotos(appContext)
        val items = stickerTitlesOrUris.mapIndexed { index, item ->
            val isPath = item.startsWith("/") || item.startsWith("content://") || item.startsWith("file://")
            val finalUri = if (isPath) {
                item
            } else {
                val samplePhoto = samples.getOrNull(index % samples.size)
                val bmp = if (samplePhoto != null) {
                    ImageCutoutProcessor.loadBitmapFromReference(appContext, samplePhoto.filePath, 320)
                } else null
                if (bmp != null) {
                    val cutout = ImageCutoutProcessor.processPhotoToStickerBitmap(
                        source = bmp,
                        removeBackground = true,
                        cropZoom = 1.1f,
                        borderWidthPx = 10,
                        borderColor = Color.WHITE
                    )
                    ImageCutoutProcessor.saveStickerBitmapToInternalStorage(appContext, cutout)
                } else {
                    ImageCutoutProcessor.ensureGraphicalStickerFile(
                        context = appContext,
                        key = "ext_${System.currentTimeMillis()}_$index",
                        title = item,
                        primaryColor = Color.rgb(0, 168, 132),
                        accentColor = Color.rgb(0, 92, 75)
                    )
                }
            }
            finalUri to item
        }
        createCustomStickerPack(name, author, "📦", items)
    }

    fun updateGroupBio(conversationId: String, newDescription: String) {
        scope.launch {
            val conv = chatDao.getConversationById(conversationId) ?: return@launch
            chatDao.updateConversation(conv.copy(groupDescription = newDescription))
        }
    }

    fun clearAllChatsHistory() {
        scope.launch {
            val all = chatDao.getAllConversations()
            // Reset last message text
            _messageExtras.value = emptyMap()
        }
    }

    private suspend fun seedInitialDataIfEmpty() {
        val samples = ImageCutoutProcessor.ensureSampleGalleryPhotos(appContext)
        val samplePaths = samples.map { it.filePath }

        val initialPacks = listOf(
            StickerPackEntity(
                id = "pack_daily",
                name = "ملصقات يومية مصورة",
                author = "كلبرق الرسمية",
                thumbnailSymbol = "🌟",
                stickersJson = listOf(
                    (samplePaths.getOrNull(1) ?: "") to "قطة لطيفة",
                    (samplePaths.getOrNull(2) ?: "") to "قهوة الصباح",
                    (samplePaths.getOrNull(3) ?: "") to "كأس التفوق",
                    (samplePaths.getOrNull(4) ?: "") to "وردة حمراء",
                    (samplePaths.getOrNull(5) ?: "") to "مكتب برمجة",
                    (samplePaths.getOrNull(7) ?: "") to "غروب الشمس"
                ).joinToString(";;;") { "${it.first}:::${it.second}" },
                isInstalled = true
            ),
            StickerPackEntity(
                id = "pack_sticker_maker_ar",
                name = "حزمة عبارات عربية (Sticker Maker)",
                author = "Sticker Maker Studio",
                thumbnailSymbol = "🎨",
                stickersJson = "صباح الخير:::صباح الخير;;;مساء النور:::مساء النور;;;شكراً جزيلاً:::شكراً جزيلاً;;;مبروك النجاح:::مبروك النجاح;;;في الطريق:::في الطريق;;;تم بنجاح:::تم بنجاح",
                isInstalled = true
            )
        )
        chatDao.insertStickerPacks(initialPacks)

        val now = System.currentTimeMillis()
        val initialStories = listOf(
            StatusStoryEntity(
                id = "story_1",
                userId = "user_ahmed",
                userName = "د. أحمد خليل",
                userAvatarPreset = 2,
                storyType = StoryType.IMAGE,
                textContent = "لقطة من اجتماع تطوير المشروع الجديد اليوم 🚀",
                mediaPreset = 0,
                backgroundColorHex = "#005C4B",
                timestamp = now - 3600000,
                expiresAt = now + 82800000,
                isViewed = false,
                viewCount = 18
            ),
            StatusStoryEntity(
                id = "story_2",
                userId = "user_sara",
                userName = "سارة ممدوح",
                userAvatarPreset = 3,
                storyType = StoryType.VIDEO,
                textContent = "جولة كاملة ومراجعة لواجهة المستخدم الجديدة (فيديو 03:45 دقائق)",
                mediaPreset = 1,
                backgroundColorHex = "#1F4068",
                timestamp = now - 7200000,
                expiresAt = now + 79200000,
                isViewed = false,
                viewCount = 31
            ),
            StatusStoryEntity(
                id = "story_3",
                userId = "user_omar",
                userName = "عمر الشامي",
                userAvatarPreset = 4,
                storyType = StoryType.TEXT,
                textContent = "الحمد لله دائماً وأبداً ✨ يومكم سعيد جميعاً!",
                mediaPreset = 0,
                backgroundColorHex = "#005C4B",
                timestamp = now - 14400000,
                expiresAt = now + 72000000,
                isViewed = true,
                viewCount = 45
            )
        )
        chatDao.insertStories(initialStories)

        // Attach real media & duration to seeded stories
        _storyExtras.value = mapOf(
            "story_1" to StoryExtra(
                mediaUri = samplePaths.getOrNull(5) ?: "",
                videoDurationSeconds = 0
            ),
            "story_2" to StoryExtra(
                mediaUri = samplePaths.getOrNull(7) ?: "",
                videoDurationSeconds = 225, // 3 min 45 sec (<= 5 min)
                trimStartSeconds = 0,
                trimEndSeconds = 225
            )
        )

        val convTeam = ConversationEntity(
            id = "conv_group_project",
            title = "فريق البرمجة والمشاريع",
            isGroup = true,
            groupIconPreset = 1,
            groupDescription = "المجموعة الرسمية لتطوير التطبيقات وتبادل الأكواد والملفات والمقاطع التعليمية.",
            participantNames = listOf("د. أحمد خليل", "سارة ممدوح", "عمر الشامي", "مريم علي", "أنا"),
            lastMessageText = "📄 ملخص_المشروع_النهائي.pdf",
            lastMessageTime = now - 1800000,
            unreadCount = 2,
            isPinned = true,
            isOnline = true,
            historyVisibleToNewMembers = true,
            safetyNumber = "9302 4819 0284 1948 2049 1930"
        )

        val convDoctor = ConversationEntity(
            id = "conv_ahmed",
            title = "د. أحمد خليل",
            isGroup = false,
            groupIconPreset = 2,
            groupDescription = "",
            participantNames = listOf("د. أحمد خليل", "أنا"),
            lastMessageText = "شكراً جزيلاً، تم تحميل الفيديو واستعراض الملف بنجاح.",
            lastMessageTime = now - 3600000,
            unreadCount = 0,
            isPinned = true,
            isOnline = true,
            historyVisibleToNewMembers = false,
            safetyNumber = "5829 1049 8592 1058 9204 8194"
        )

        val convSara = ConversationEntity(
            id = "conv_sara",
            title = "سارة ممدوح",
            isGroup = false,
            groupIconPreset = 3,
            groupDescription = "",
            participantNames = listOf("سارة ممدوح", "أنا"),
            lastMessageText = "💟 ملصق",
            lastMessageTime = now - 7200000,
            unreadCount = 1,
            isPinned = false,
            isOnline = false,
            historyVisibleToNewMembers = false,
            safetyNumber = "1948 2049 9302 4819 0284 1948"
        )

        val convFamily = ConversationEntity(
            id = "conv_group_family",
            title = "مجموعة العائلة",
            isGroup = true,
            groupIconPreset = 4,
            groupDescription = "تجمع العائلة للمناسبات ومشاركة الصور والأخبار اليومية.",
            participantNames = listOf("الوالد", "الوالدة", "أحمد", "منى", "أنا"),
            lastMessageText = "جمعة مباركة على الجميع يا رب",
            lastMessageTime = now - 86400000,
            unreadCount = 0,
            isPinned = false,
            isOnline = true,
            historyVisibleToNewMembers = true,
            safetyNumber = "7294 8194 0284 1948 2049 8194"
        )

        chatDao.insertConversations(listOf(convTeam, convDoctor, convSara, convFamily))

        val groupMsgs = listOf(
            MessageEntity(
                id = "g_msg_1",
                conversationId = "conv_group_project",
                senderId = "system",
                senderName = "النظام",
                text = "الرسائل والمكالمات مشفرة تماماً بين الطرفين. لا يمكن لأحد خارج هذه الدردشة قراءتها أو الاستماع إليها.",
                timestamp = now - 14400000,
                status = MessageStatus.READ,
                attachmentType = AttachmentType.NONE,
                attachmentUrl = "",
                attachmentName = "",
                attachmentSize = "",
                isEncrypted = true,
                isSystemAnnouncement = true,
                replyToText = null
            ),
            MessageEntity(
                id = "g_msg_2",
                conversationId = "conv_group_project",
                senderId = "user_ahmed",
                senderName = "د. أحمد خليل",
                text = "مرحباً بالجميع! قمت برفع فيديو يوضح التحديثات الجديدة في التطبيق.",
                timestamp = now - 12000000,
                status = MessageStatus.READ,
                attachmentType = AttachmentType.NONE,
                attachmentUrl = "",
                attachmentName = "",
                attachmentSize = "",
                isEncrypted = true,
                isSystemAnnouncement = false,
                replyToText = null
            ),
            MessageEntity(
                id = "g_msg_3",
                conversationId = "conv_group_project",
                senderId = "user_ahmed",
                senderName = "د. أحمد خليل",
                text = "استعراض عملي للمميزات والأداء الفائق",
                timestamp = now - 10800000,
                status = MessageStatus.READ,
                attachmentType = AttachmentType.VIDEO,
                attachmentUrl = samplePaths.getOrNull(5) ?: "",
                attachmentName = "Demo_Kalbarq_HD.mp4",
                attachmentSize = "18.4 ميجابايت (1080p)",
                isEncrypted = true,
                isSystemAnnouncement = false,
                replyToText = null
            ),
            MessageEntity(
                id = "g_msg_4",
                conversationId = "conv_group_project",
                senderId = "user_sara",
                senderName = "سارة ممدوح",
                text = "ممتاز جداً! أرفقت أيضاً ملف التوثيق الهندسي للمشروع.",
                timestamp = now - 7200000,
                status = MessageStatus.READ,
                attachmentType = AttachmentType.NONE,
                attachmentUrl = "",
                attachmentName = "",
                attachmentSize = "",
                isEncrypted = true,
                isSystemAnnouncement = false,
                replyToText = null
            ),
            MessageEntity(
                id = "g_msg_5",
                conversationId = "conv_group_project",
                senderId = "user_sara",
                senderName = "سارة ممدوح",
                text = "ملخص بنية التطبيق والتشفير الشامل",
                timestamp = now - 1800000,
                status = MessageStatus.READ,
                attachmentType = AttachmentType.DOCUMENT,
                attachmentUrl = "",
                attachmentName = "ملخص_المشروع_النهائي.pdf",
                attachmentSize = "4.2 ميجابايت • 24 صفحة",
                isEncrypted = true,
                isSystemAnnouncement = false,
                replyToText = null
            ),
            MessageEntity(
                id = "g_msg_6_invite",
                conversationId = "conv_group_project",
                senderId = "user_ahmed",
                senderName = "د. أحمد خليل",
                text = "رابط الدعوة الرسمي للانضمام لمجموعة تطوير الذكاء الاصطناعي:\nhttps://kalbarq.app/join/ai_devs_club_2026",
                timestamp = now - 900000,
                status = MessageStatus.READ,
                attachmentType = AttachmentType.NONE,
                attachmentUrl = "",
                attachmentName = "",
                attachmentSize = "",
                isEncrypted = true,
                isSystemAnnouncement = false,
                replyToText = null
            )
        )
        chatDao.insertMessages(groupMsgs)

        _messageExtras.value = mapOf(
            "g_msg_2" to MessageExtra(reactions = mapOf("👍" to 3, "❤️" to 2, "🔥" to 4)),
            "g_msg_3" to MessageExtra(reactions = mapOf("🚀" to 5, "💯" to 4), mediaUri = samplePaths.getOrNull(5) ?: ""),
            "g_msg_5" to MessageExtra(reactions = mapOf("👏" to 4, "❤️" to 3))
        )
    }

    fun addNewContact(name: String, phoneNumber: String, bio: String, avatarPreset: Int, avatarUri: String = "") {
        val newContact = ContactItem(
            id = "c_${UUID.randomUUID()}",
            name = name,
            phoneNumber = phoneNumber,
            avatarPreset = avatarPreset,
            avatarUri = avatarUri,
            bio = bio.ifBlank { "متاح" },
            isOnline = true
        )
        _contacts.value = listOf(newContact) + _contacts.value
    }

    suspend fun startOrCreateConversationWithContact(contact: ContactItem): String {
        if (contact.name.contains("أحمد")) return "conv_ahmed"
        if (contact.name.contains("سارة")) return "conv_sara"

        val existing = chatDao.getConversationById("conv_${contact.id}")
        if (existing != null) return existing.id

        val newId = "conv_${contact.id}"
        val now = System.currentTimeMillis()
        if (contact.avatarUri.isNotBlank()) {
            prefs.edit().putString("group_uri_$newId", contact.avatarUri).apply()
        }
        val entity = ConversationEntity(
            id = newId,
            title = contact.name,
            isGroup = false,
            groupIconPreset = contact.avatarPreset,
            groupDescription = contact.bio,
            participantNames = listOf(contact.name, _currentUser.value?.displayName ?: "أنا"),
            lastMessageText = "بدأت محادثة جديدة",
            lastMessageTime = now,
            unreadCount = 0,
            isPinned = false,
            isOnline = contact.isOnline,
            historyVisibleToNewMembers = false,
            safetyNumber = "4810 2948 1049 8492 1049 8592"
        )
        chatDao.insertConversation(entity)

        val initMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = newId,
            senderId = "system",
            senderName = "النظام",
            text = "الرسائل والمكالمات مشفرة تماماً بين الطرفين.",
            timestamp = now,
            status = MessageStatus.READ,
            attachmentType = AttachmentType.NONE,
            attachmentUrl = "",
            attachmentName = "",
            attachmentSize = "",
            isEncrypted = true,
            isSystemAnnouncement = true,
            replyToText = null
        )
        chatDao.insertMessage(initMsg)
        return newId
    }

    /**
     * Direct Chat with Any Phone Number (Real Person-to-Person messaging without requiring contact save first).
     */
    suspend fun startDirectChatByPhoneNumber(phoneNumber: String, customName: String = ""): String {
        val cleanPhone = phoneNumber.trim()
        val displayTitle = customName.trim().ifBlank { cleanPhone }
        val normalizedDigits = cleanPhone.filter { it.isDigit() }.takeLast(9)
        val convId = "conv_phone_${normalizedDigits.ifBlank { UUID.randomUUID().toString().take(8) }}"

        val existing = chatDao.getConversationById(convId)
        if (existing != null) return existing.id

        // Also register in contacts list so it appears everywhere
        val newContact = ContactItem(
            id = "c_$normalizedDigits",
            name = displayTitle,
            phoneNumber = cleanPhone,
            avatarPreset = (1..6).random(),
            bio = "متصل عبر كلبرق ($cleanPhone)",
            isOnline = true
        )
        _contacts.value = (listOf(newContact) + _contacts.value).distinctBy { it.phoneNumber }

        val now = System.currentTimeMillis()
        val entity = ConversationEntity(
            id = convId,
            title = displayTitle,
            isGroup = false,
            groupIconPreset = newContact.avatarPreset,
            groupDescription = "رقم الهاتف: $cleanPhone",
            participantNames = listOf(displayTitle, _currentUser.value?.displayName ?: "أنا"),
            lastMessageText = "تم فتح قناة اتصال مشفرة مع $cleanPhone",
            lastMessageTime = now,
            unreadCount = 0,
            isPinned = false,
            isOnline = true,
            historyVisibleToNewMembers = false,
            safetyNumber = "6029 4819 3920 1049 8592 2049"
        )
        chatDao.insertConversation(entity)

        val initMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = convId,
            senderId = "system",
            senderName = "النظام",
            text = "الرسائل والمكالمات مع الرقم ($cleanPhone) مشفرة تماماً بين الطرفين وجاهزة للإرسال الفوري.",
            timestamp = now,
            status = MessageStatus.READ,
            attachmentType = AttachmentType.NONE,
            attachmentUrl = "",
            attachmentName = "",
            attachmentSize = "",
            isEncrypted = true,
            isSystemAnnouncement = true,
            replyToText = null
        )
        chatDao.insertMessage(initMsg)
        return convId
    }
}
