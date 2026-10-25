package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WhatsAppSettings
import com.example.data.repository.SafirRepository
import com.example.ui.components.AvatarView
import com.example.ui.components.KalbarqGoldFeaturesDialog
import com.example.ui.components.RealPhotoPickerSheetDialog
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.KalbarqThemePalettes
import com.example.ui.theme.SecurityShieldGold
import com.example.util.ImageCutoutProcessor

private enum class SettingsSubPage(val arabicTitle: String, val englishTitle: String) {
    MAIN("الإعدادات", "Settings"),
    ACCOUNT("الحساب (Account)", "Account"),
    ACCOUNT_SECURITY_NOTIFICATIONS("إشعارات الأمان", "Security notifications"),
    ACCOUNT_TWO_STEP_VERIFICATION("التحقق بخطوتين", "Two-step verification"),
    ACCOUNT_CHANGE_NUMBER("تغيير الرقم", "Change number"),
    ACCOUNT_REQUEST_INFO("طلب معلومات الحساب", "Request account info"),
    PRIVACY("الخصوصية (Privacy)", "Privacy"),
    AVATAR("الصورة الرمزية والصورة الشخصية", "Avatar"),
    FAVORITES("المفضلة والقوائم (Lists)", "Favorites"),
    CHATS("الدردشات (Chats)", "Chats"),
    NOTIFICATIONS("الإشعارات (Notifications)", "Notifications"),
    STORAGE("التخزين والبيانات (Storage and data)", "Storage and data"),
    STARRED("الرسائل المميزة بنجمة", "Starred messages"),
    LANGUAGE("لغة التطبيق (App language)", "App language"),
    HELP("المساعدة (Help)", "Help");

    fun parentPage(): SettingsSubPage = when (this) {
        ACCOUNT_SECURITY_NOTIFICATIONS,
        ACCOUNT_TWO_STEP_VERIFICATION,
        ACCOUNT_CHANGE_NUMBER,
        ACCOUNT_REQUEST_INFO -> ACCOUNT
        else -> MAIN
    }
}

private data class SearchableSettingEntry(
    val title: String,
    val subtitle: String,
    val sectionName: String,
    val targetPage: SettingsSubPage,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: SafirRepository,
    onOpenProfile: () -> Unit,
    onOpenLinkedDevices: () -> Unit,
    onOpenStickerStore: () -> Unit,
    onBack: () -> Unit,
    initialSubPage: String = "MAIN"
) {
    val currentUser by repository.currentUser.collectAsState()
    val settings by repository.settings.collectAsState()
    val linkedDevices by repository.linkedDevices.collectAsState()
    val starredIds by repository.starredMessageIds.collectAsState()
    val conversations by repository.conversations.collectAsState(initial = emptyList())

    var currentSubPage by remember(initialSubPage) {
        mutableStateOf(
            runCatching { SettingsSubPage.valueOf(initialSubPage) }.getOrDefault(SettingsSubPage.MAIN)
        )
    }
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var statusBannerText by remember { mutableStateOf<String?>(null) }

    // Selection dialog state
    var optionDialogTitle by remember { mutableStateOf<String?>(null) }
    var optionDialogChoices by remember { mutableStateOf<List<String>>(emptyList()) }
    var optionDialogSelected by remember { mutableStateOf("") }
    var optionDialogCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }

    // QR Code Dialog & Kalbarq Gold Features Dialog
    var showQrDialog by remember { mutableStateOf(false) }
    var showKalbarqGoldDialog by remember { mutableStateOf(false) }

    BackHandler {
        when {
            isSearching -> {
                isSearching = false
                searchQuery = ""
            }
            currentSubPage != SettingsSubPage.MAIN -> {
                currentSubPage = currentSubPage.parentPage()
            }
            else -> onBack()
        }
    }

    val searchableCatalog = remember(settings, linkedDevices.size, starredIds.size) {
        listOf(
            SearchableSettingEntry(
                title = "الحساب • Account",
                subtitle = "إشعارات الأمان، مفاتيح المرور، البريد الإلكتروني، التحقق بخطوتين، تغيير الرقم، طلب معلومات الحساب، حذف الحساب",
                sectionName = "Account",
                targetPage = SettingsSubPage.ACCOUNT,
                icon = Icons.Default.Key
            ),
            SearchableSettingEntry(
                title = "إشعارات الأمان • Security notifications",
                subtitle = "إظهار إشعارات الأمان على هذا الجهاز عند تغير رمز التشفير",
                sectionName = "Account",
                targetPage = SettingsSubPage.ACCOUNT_SECURITY_NOTIFICATIONS,
                icon = Icons.Default.Security
            ),
            SearchableSettingEntry(
                title = "التحقق بخطوتين • Two-step verification",
                subtitle = "تعيين رقم تعريف شخصي (PIN) مكون من 6 أرقام لحماية الحساب",
                sectionName = "Account",
                targetPage = SettingsSubPage.ACCOUNT_TWO_STEP_VERIFICATION,
                icon = Icons.Default.Password
            ),
            SearchableSettingEntry(
                title = "تغيير الرقم • Change number",
                subtitle = "نقل معلومات الحساب والمجموعات والإعدادات إلى رقم هاتف جديد",
                sectionName = "Account",
                targetPage = SettingsSubPage.ACCOUNT_CHANGE_NUMBER,
                icon = Icons.Default.PhoneAndroid
            ),
            SearchableSettingEntry(
                title = "الخصوصية • Privacy",
                subtitle = "آخر ظهور ومتصل الآن، الصورة الشخصية، الأخبار، الحالة، مؤشرات قراءة الرسائل، حظر جهات الاتصال، قفل التطبيق",
                sectionName = "Privacy",
                targetPage = SettingsSubPage.PRIVACY,
                icon = Icons.Default.Lock
            ),
            SearchableSettingEntry(
                title = "آخر ظهور ومتصل الآن • Last seen and online",
                subtitle = "${settings.lastSeenPrivacy} • ${settings.onlinePrivacy}",
                sectionName = "Privacy",
                targetPage = SettingsSubPage.PRIVACY,
                icon = Icons.Default.Visibility
            ),
            SearchableSettingEntry(
                title = "مؤشرات قراءة الرسائل • Read receipts",
                subtitle = if (settings.readReceiptsEnabled) "مفعّلة" else "متوقفة",
                sectionName = "Privacy",
                targetPage = SettingsSubPage.PRIVACY,
                icon = Icons.Default.DoneAll
            ),
            SearchableSettingEntry(
                title = "الرسائل ذاتية الاختفاء • Disappearing messages",
                subtitle = "المؤقت الافتراضي للرسائل: ${settings.defaultDisappearingTimer}",
                sectionName = "Privacy",
                targetPage = SettingsSubPage.PRIVACY,
                icon = Icons.Default.Timer
            ),
            SearchableSettingEntry(
                title = "قفل التطبيق • App lock",
                subtitle = "فتح القفل باستخدام المقاييس الحيوية وبصمة الإصبع",
                sectionName = "Privacy",
                targetPage = SettingsSubPage.PRIVACY,
                icon = Icons.Default.Fingerprint
            ),
            SearchableSettingEntry(
                title = "الدردشات • Chats",
                subtitle = "المظهر (${settings.themeMode})، خلفية الشاشة، حجم الخط، النسخ الاحتياطي للدردشات، نقل الدردشات، سجل الدردشات",
                sectionName = "Chats",
                targetPage = SettingsSubPage.CHATS,
                icon = Icons.AutoMirrored.Filled.Chat
            ),
            SearchableSettingEntry(
                title = "المظهر وخلفية الشاشة • Theme & Wallpaper",
                subtitle = "المظهر: ${settings.themeMode} • خلفية الشاشة: ${settings.chatWallpaperHex}",
                sectionName = "Chats",
                targetPage = SettingsSubPage.CHATS,
                icon = Icons.Default.Palette
            ),
            SearchableSettingEntry(
                title = "النسخ الاحتياطي للدردشات • Chat backup",
                subtitle = "آخر نسخ احتياطي: ${settings.lastBackupTime}",
                sectionName = "Chats",
                targetPage = SettingsSubPage.CHATS,
                icon = Icons.Default.CloudUpload
            ),
            SearchableSettingEntry(
                title = "الإشعارات • Notifications",
                subtitle = "نغمات المحادثات، إشعارات الرسائل والمجموعات والمكالمات، الاهتزاز، الإشعارات ذات الأولوية العالية",
                sectionName = "Notifications",
                targetPage = SettingsSubPage.NOTIFICATIONS,
                icon = Icons.Default.Notifications
            ),
            SearchableSettingEntry(
                title = "التخزين والبيانات • Storage and data",
                subtitle = "إدارة التخزين، استخدام الشبكة، جودة تحميل الوسائط (${settings.mediaUploadQuality})، التنزيل التلقائي",
                sectionName = "Storage and data",
                targetPage = SettingsSubPage.STORAGE,
                icon = Icons.Default.DataUsage
            ),
            SearchableSettingEntry(
                title = "الصورة الشخصية • Avatar & Profile Photo",
                subtitle = "تغيير الصورة الشخصية من المعرض بدون رموز تعبيرية",
                sectionName = "Avatar",
                targetPage = SettingsSubPage.AVATAR,
                icon = Icons.Default.Face
            ),
            SearchableSettingEntry(
                title = "المفضلة والقوائم • Favorites & Lists",
                subtitle = "إدارة جهات الاتصال والمجموعات المفضلة",
                sectionName = "Favorites",
                targetPage = SettingsSubPage.FAVORITES,
                icon = Icons.Default.FavoriteBorder
            ),
            SearchableSettingEntry(
                title = "لغة التطبيق • App language",
                subtitle = settings.appLanguage,
                sectionName = "App language",
                targetPage = SettingsSubPage.LANGUAGE,
                icon = Icons.Default.Language
            ),
            SearchableSettingEntry(
                title = "المساعدة • Help",
                subtitle = "مركز المساعدة، اتصل بنا، الشروط وسياسة الخصوصية، معلومات التطبيق",
                sectionName = "Help",
                targetPage = SettingsSubPage.HELP,
                icon = Icons.AutoMirrored.Filled.HelpOutline
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching && currentSubPage == SettingsSubPage.MAIN) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("بحث في الإعدادات (Search settings)...", fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_search_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "مسح")
                                    }
                                }
                            }
                        )
                    } else {
                        Column {
                            Text(
                                text = currentSubPage.arabicTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentSubPage != SettingsSubPage.MAIN) {
                                Text(
                                    text = currentSubPage.englishTitle,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSearching) {
                                isSearching = false
                                searchQuery = ""
                            } else if (currentSubPage == SettingsSubPage.MAIN) {
                                onBack()
                            } else {
                                currentSubPage = currentSubPage.parentPage()
                            }
                        },
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (currentSubPage == SettingsSubPage.MAIN) {
                        IconButton(
                            onClick = {
                                isSearching = !isSearching
                                if (!isSearching) searchQuery = ""
                            },
                            modifier = Modifier.testTag("settings_search_button")
                        ) {
                            Icon(
                                imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "بحث في الإعدادات"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedVisibility(visible = statusBannerText != null) {
                statusBannerText?.let { msg ->
                    Surface(
                        color = EmeraldPrimary.copy(alpha = 0.14f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { statusBannerText = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (currentSubPage) {
                    SettingsSubPage.MAIN -> {
                        if (isSearching && searchQuery.isNotBlank()) {
                            val results = searchableCatalog.filter {
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                    it.subtitle.contains(searchQuery, ignoreCase = true) ||
                                    it.sectionName.contains(searchQuery, ignoreCase = true)
                            }
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (results.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 48.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "لا توجد نتائج مطابقة لـ \"$searchQuery\"",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                } else {
                                    items(results) { item ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    isSearching = false
                                                    searchQuery = ""
                                                    currentSubPage = item.targetPage
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(item.icon, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(24.dp))
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                    Text(item.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            MainWhatsAppSettingsList(
                                userName = currentUser?.displayName ?: "أنا",
                                userBio = currentUser?.bio ?: "متاح",
                                userPhone = currentUser?.phoneNumber ?: "+20 100 000 0000",
                                userAvatarPreset = currentUser?.avatarPreset ?: 1,
                                userAvatarUri = currentUser?.avatarUri ?: "",
                                settings = settings,
                                linkedDevicesCount = linkedDevices.size,
                                starredCount = starredIds.size,
                                onOpenProfile = onOpenProfile,
                                onOpenQr = { showQrDialog = true },
                                onOpenKalbarqGold = { showKalbarqGoldDialog = true },
                                onNavigateSubPage = { currentSubPage = it },
                                onOpenLinkedDevices = onOpenLinkedDevices,
                                onOpenStickerStore = onOpenStickerStore,
                                onInviteFriend = {
                                    statusBannerText = "تم نسخ رابط دعوة كلبرق لمشاركته مع أصدقائك: https://kalbarq.app/invite"
                                }
                            )
                        }
                    }

                    SettingsSubPage.ACCOUNT -> {
                        AccountSettingsSubPage(
                            settings = settings,
                            userPhone = currentUser?.phoneNumber ?: "+201012345678",
                            onNavigateSubPage = { currentSubPage = it },
                            onUpdate = { repository.updateSettings(it) },
                            onStatusMessage = { statusBannerText = it }
                        )
                    }

                    SettingsSubPage.ACCOUNT_SECURITY_NOTIFICATIONS -> {
                        SecurityNotificationsSubPage(
                            settings = settings,
                            onUpdate = { repository.updateSettings(it) },
                            onStatusMessage = { statusBannerText = it }
                        )
                    }

                    SettingsSubPage.ACCOUNT_TWO_STEP_VERIFICATION -> {
                        TwoStepVerificationSubPage(
                            settings = settings,
                            onUpdate = { repository.updateSettings(it) },
                            onStatusMessage = { statusBannerText = it }
                        )
                    }

                    SettingsSubPage.ACCOUNT_CHANGE_NUMBER -> {
                        ChangeNumberSubPage(
                            currentPhone = currentUser?.phoneNumber ?: "+201012345678",
                            settings = settings,
                            onChangePhoneNumber = { oldPhone, newPhone, notifyContacts, notifyMode ->
                                val ok = repository.changePhoneNumber(
                                    oldPhone = oldPhone,
                                    newPhone = newPhone,
                                    notifyContacts = notifyContacts,
                                    notifyMode = notifyMode
                                )
                                if (ok) {
                                    statusBannerText = "تم تغيير رقم الهاتف بنجاح من $oldPhone إلى $newPhone"
                                    currentSubPage = SettingsSubPage.ACCOUNT
                                }
                                ok
                            },
                            onStatusMessage = { statusBannerText = it }
                        )
                    }

                    SettingsSubPage.ACCOUNT_REQUEST_INFO -> {
                        RequestAccountInfoSubPage(
                            settings = settings,
                            onUpdate = { repository.updateSettings(it) },
                            onStatusMessage = { statusBannerText = it }
                        )
                    }

                    SettingsSubPage.PRIVACY -> {
                        PrivacySettingsSubPage(
                            settings = settings,
                            onUpdate = { repository.updateSettings(it) },
                            onOpenOptionDialog = { title, choices, current, cb ->
                                optionDialogTitle = title
                                optionDialogChoices = choices
                                optionDialogSelected = current
                                optionDialogCallback = cb
                            },
                            onStatusMessage = { statusBannerText = it }
                        )
                    }

                    SettingsSubPage.AVATAR -> {
                        AvatarSettingsSubPage(
                            repository = repository,
                            userAvatarPreset = currentUser?.avatarPreset ?: 1,
                            userAvatarUri = currentUser?.avatarUri ?: "",
                            onOpenStickerStore = onOpenStickerStore
                        )
                    }

                    SettingsSubPage.FAVORITES -> {
                        FavoritesSettingsSubPage(
                            conversations = conversations,
                            onToggleFavorite = { repository.toggleFavoriteConversation(it) }
                        )
                    }

                    SettingsSubPage.CHATS -> {
                        ChatsSettingsSubPage(
                            settings = settings,
                            onUpdate = { repository.updateSettings(it) },
                            onSetTheme = { mode -> repository.setThemeMode(mode) },
                            onOpenOptionDialog = { title, choices, current, cb ->
                                optionDialogTitle = title
                                optionDialogChoices = choices
                                optionDialogSelected = current
                                optionDialogCallback = cb
                            },
                            onClearAllChats = {
                                repository.clearAllChatsHistory()
                                statusBannerText = "تم مسح محتوى جميع الدردشات بنجاح"
                            },
                            onStatusMessage = { statusBannerText = it }
                        )
                    }

                    SettingsSubPage.NOTIFICATIONS -> {
                        NotificationsSettingsSubPage(
                            settings = settings,
                            onUpdate = { repository.updateSettings(it) },
                            onOpenOptionDialog = { title, choices, current, cb ->
                                optionDialogTitle = title
                                optionDialogChoices = choices
                                optionDialogSelected = current
                                optionDialogCallback = cb
                            },
                            onStatusMessage = { statusBannerText = it }
                        )
                    }

                    SettingsSubPage.STORAGE -> {
                        StorageSettingsSubPage(
                            settings = settings,
                            onUpdate = { repository.updateSettings(it) },
                            onOpenOptionDialog = { title, choices, current, cb ->
                                optionDialogTitle = title
                                optionDialogChoices = choices
                                optionDialogSelected = current
                                optionDialogCallback = cb
                            },
                            onStatusMessage = { statusBannerText = it }
                        )
                    }

                    SettingsSubPage.STARRED -> {
                        StarredSettingsSubPage(starredCount = starredIds.size)
                    }

                    SettingsSubPage.LANGUAGE -> {
                        LanguageSettingsSubPage(
                            currentLanguage = settings.appLanguage,
                            onSelectLanguage = { lang ->
                                repository.updateSettings { it.copy(appLanguage = lang) }
                                statusBannerText = "تم ضبط لغة التطبيق إلى: $lang"
                            }
                        )
                    }

                    SettingsSubPage.HELP -> {
                        HelpSettingsSubPage(
                            onStatusMessage = { statusBannerText = it }
                        )
                    }
                }
            }
        }
    }

    if (optionDialogTitle != null) {
        AlertDialog(
            onDismissRequest = { optionDialogTitle = null },
            title = { Text(optionDialogTitle ?: "", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column {
                    optionDialogChoices.forEach { choice ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    optionDialogCallback?.invoke(choice)
                                    optionDialogTitle = null
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = choice == optionDialogSelected,
                                onClick = {
                                    optionDialogCallback?.invoke(choice)
                                    optionDialogTitle = null
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = choice, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { optionDialogTitle = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showQrDialog) {
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = { Text("رمز QR الخاص بحسابك", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AvatarView(
                        preset = currentUser?.avatarPreset ?: 1,
                        name = currentUser?.displayName ?: "أنا",
                        size = 64.dp,
                        imageUri = currentUser?.avatarUri ?: ""
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(currentUser?.displayName ?: "أنا", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("جهات اتصال كلبرق", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .border(2.dp, EmeraldPrimary, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "QR Code",
                            tint = Color(0xFF111B21),
                            modifier = Modifier.size(148.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "رمز QR هذا خاص بك. إذا شاركته مع شخص ما، فبإمكانه مسحه ضوئياً لإضافتك إلى جهات الاتصال.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrDialog = false }) {
                    Text("تم", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showKalbarqGoldDialog) {
        KalbarqGoldFeaturesDialog(
            repository = repository,
            onDismiss = { showKalbarqGoldDialog = false }
        )
    }
}

@Composable
private fun MainWhatsAppSettingsList(
    userName: String,
    userBio: String,
    userPhone: String,
    userAvatarPreset: Int,
    userAvatarUri: String,
    settings: WhatsAppSettings,
    linkedDevicesCount: Int,
    starredCount: Int,
    onOpenProfile: () -> Unit,
    onOpenQr: () -> Unit,
    onOpenKalbarqGold: () -> Unit = {},
    onNavigateSubPage: (SettingsSubPage) -> Unit,
    onOpenLinkedDevices: () -> Unit,
    onOpenStickerStore: () -> Unit,
    onInviteFriend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Profile Header Row (WhatsApp exact layout)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenProfile() }
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .testTag("settings_profile_row"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarView(
                preset = userAvatarPreset,
                name = userName,
                size = 64.dp,
                imageUri = userAvatarUri
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = userBio,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = userPhone,
                    fontSize = 12.sp,
                    color = EmeraldPrimary
                )
            }
            IconButton(onClick = onOpenQr) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = "رمز QR",
                    tint = EmeraldPrimary
                )
            }
            IconButton(onClick = onOpenProfile) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = "تعديل الملف الشخصي",
                    tint = EmeraldPrimary
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

        // Kalbarq Gold & UI Color Customizer Card
        val activeColorName = remember(settings.uiColorPresetId) {
            (KalbarqThemePalettes.find { it.id == settings.uiColorPresetId } ?: KalbarqThemePalettes.first()).arabicName
        }
        WhatsAppSettingRow(
            icon = Icons.Default.AutoAwesome,
            title = "إضافات كلبرق الذهبية ولون الواجهة ⚡",
            subtitle = "اللون: $activeColorName • منع الحذف • ملفات 2GB • نت سوشيال وتحديث مباشر",
            tag = "settings_item_kalbarq_gold",
            onClick = onOpenKalbarqGold
        )

        // Official WhatsApp Settings Sections (Bilingual Arabic + English official titles)
        WhatsAppSettingRow(
            icon = Icons.Default.Key,
            title = "الحساب • Account",
            subtitle = "إشعارات الأمان، تغيير الرقم، التحقق بخطوتين",
            tag = "settings_item_account",
            onClick = { onNavigateSubPage(SettingsSubPage.ACCOUNT) }
        )

        WhatsAppSettingRow(
            icon = Icons.Default.Lock,
            title = "الخصوصية • Privacy",
            subtitle = "حظر جهات الاتصال، الرسائل ذاتية الاختفاء، آخر ظهور",
            tag = "settings_item_privacy",
            onClick = { onNavigateSubPage(SettingsSubPage.PRIVACY) }
        )

        WhatsAppSettingRow(
            icon = Icons.Default.Face,
            title = "الصورة الرمزية • Avatar",
            subtitle = "تغيير الصورة الشخصية، إنشاء الملصقات المخصصة",
            tag = "settings_item_avatar",
            onClick = { onNavigateSubPage(SettingsSubPage.AVATAR) }
        )

        WhatsAppSettingRow(
            icon = Icons.Default.FavoriteBorder,
            title = "المفضلة • Favorites (Lists)",
            subtitle = "إضافة وإعادة ترتيب وحذف المفضلة",
            tag = "settings_item_favorites",
            onClick = { onNavigateSubPage(SettingsSubPage.FAVORITES) }
        )

        WhatsAppSettingRow(
            icon = Icons.AutoMirrored.Filled.Chat,
            title = "الدردشات • Chats",
            subtitle = "المظهر (${settings.themeMode})، خلفيات الشاشة، سجل الدردشات",
            tag = "settings_item_chats",
            onClick = { onNavigateSubPage(SettingsSubPage.CHATS) }
        )

        WhatsAppSettingRow(
            icon = Icons.Default.Notifications,
            title = "الإشعارات • Notifications",
            subtitle = "نغمات الرسائل والمجموعات والمكالمات",
            tag = "settings_item_notifications",
            onClick = { onNavigateSubPage(SettingsSubPage.NOTIFICATIONS) }
        )

        WhatsAppSettingRow(
            icon = Icons.Default.DataUsage,
            title = "التخزين والبيانات • Storage and data",
            subtitle = "استخدام الشبكة، التنزيل التلقائي، جودة تحميل الوسائط",
            tag = "settings_item_storage",
            onClick = { onNavigateSubPage(SettingsSubPage.STORAGE) }
        )

        WhatsAppSettingRow(
            icon = Icons.Default.Devices,
            title = "الأجهزة المرتبطة • Linked devices",
            subtitle = "$linkedDevicesCount أجهزة متصلة حالياً",
            tag = "settings_item_linked_devices",
            onClick = onOpenLinkedDevices
        )

        WhatsAppSettingRow(
            icon = Icons.Default.StarBorder,
            title = "الرسائل المميزة بنجمة • Starred messages",
            subtitle = "$starredCount رسالة محفوظة",
            tag = "settings_item_starred",
            onClick = { onNavigateSubPage(SettingsSubPage.STARRED) }
        )

        WhatsAppSettingRow(
            icon = Icons.Default.Language,
            title = "لغة التطبيق • App language",
            subtitle = settings.appLanguage,
            tag = "settings_item_language",
            onClick = { onNavigateSubPage(SettingsSubPage.LANGUAGE) }
        )

        WhatsAppSettingRow(
            icon = Icons.AutoMirrored.Filled.HelpOutline,
            title = "المساعدة • Help",
            subtitle = "مركز المساعدة، اتصل بنا، سياسة الخصوصية",
            tag = "settings_item_help",
            onClick = { onNavigateSubPage(SettingsSubPage.HELP) }
        )

        WhatsAppSettingRow(
            icon = Icons.Default.GroupAdd,
            title = "دعوة صديق • Invite a friend",
            subtitle = "مشاركة رابط تطبيق كلبرق مع جهات اتصالك",
            tag = "settings_item_invite",
            onClick = onInviteFriend
        )

        val context = LocalContext.current
        WhatsAppSettingRow(
            icon = Icons.Default.CloudUpload,
            title = "استخراج ومشاركة ملف التطبيق (APK) • Export APK",
            subtitle = "حفظ أو إرسال ملف تثبيت التطبيق (${com.example.util.ApkExporter.APK_FILE_NAME})",
            tag = "settings_item_export_apk",
            onClick = {
                com.example.util.ApkExporter.shareApkPackage(context)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "كلبرق للاتصالات المشفرة",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "الإصدار 3.5.0 • تشفير شامل End-to-End",
                fontSize = 11.sp,
                color = EmeraldPrimary
            )
        }
    }
}

@Composable
private fun WhatsAppSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccountSettingsSubPage(
    settings: WhatsAppSettings,
    userPhone: String,
    onNavigateSubPage: (SettingsSubPage) -> Unit,
    onUpdate: ((WhatsAppSettings) -> WhatsAppSettings) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    var showPasskeysDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var emailInput by remember(settings.emailAddress) { mutableStateOf(settings.emailAddress) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deleteConfirmPhoneInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
            .testTag("account_settings_list")
    ) {
        WhatsAppSettingRow(
            icon = Icons.Default.Security,
            title = "إشعارات الأمان (Security notifications)",
            subtitle = if (settings.securityNotificationsEnabled) {
                "إظهار إشعارات الأمان على هذا الجهاز مفعّل"
            } else {
                "إظهار إشعارات الأمان على هذا الجهاز متوقف"
            },
            tag = "account_item_security_notifications",
            onClick = { onNavigateSubPage(SettingsSubPage.ACCOUNT_SECURITY_NOTIFICATIONS) }
        )

        WhatsAppSettingRow(
            icon = Icons.Default.VpnKey,
            title = "مفاتيح المرور (Passkeys)",
            subtitle = if (settings.passkeysEnabled) {
                "مفعّل • تسجيل الدخول باستخدام البصمة أو الوجه أو قفل الشاشة"
            } else {
                "إنشاء مفتاح مرور لتسجيل الدخول بأمان بدون رمز SMS"
            },
            tag = "account_item_passkeys",
            onClick = { showPasskeysDialog = true }
        )

        WhatsAppSettingRow(
            icon = Icons.Default.Email,
            title = "عنوان البريد الإلكتروني (Email address)",
            subtitle = settings.emailAddress.ifBlank { "إضافة بريد إلكتروني للمساعدة في الوصول إلى حسابك" },
            tag = "account_item_email",
            onClick = { showEmailDialog = true }
        )

        WhatsAppSettingRow(
            icon = Icons.Default.Password,
            title = "التحقق بخطوتين (Two-step verification)",
            subtitle = if (settings.twoStepVerificationEnabled) {
                "مفعّل • رقم التعريف الشخصي (PIN) المكوّن من 6 أرقام نشط"
            } else {
                "غير مفعّل • تعيين رقم تعريف شخصي (PIN) لحماية حسابك"
            },
            tag = "account_item_two_step_verification",
            onClick = { onNavigateSubPage(SettingsSubPage.ACCOUNT_TWO_STEP_VERIFICATION) }
        )

        WhatsAppSettingRow(
            icon = Icons.Default.PhoneAndroid,
            title = "تغيير الرقم (Change number)",
            subtitle = "نقل معلومات الحساب والمجموعات والإعدادات ($userPhone)",
            tag = "account_item_change_number",
            onClick = { onNavigateSubPage(SettingsSubPage.ACCOUNT_CHANGE_NUMBER) }
        )

        WhatsAppSettingRow(
            icon = Icons.Default.Description,
            title = "طلب معلومات الحساب (Request account info)",
            subtitle = if (settings.accountReportRequested) {
                "تم طلب التقرير • سيكون جاهزاً خلال 3 أيام"
            } else {
                "إنشاء تقرير بمعلومات حسابك في كلبرق وإعداداته"
            },
            tag = "account_item_request_info",
            onClick = { onNavigateSubPage(SettingsSubPage.ACCOUNT_REQUEST_INFO) }
        )

        WhatsAppSettingRow(
            icon = Icons.Default.PersonAddAlt1,
            title = "إضافة حساب (Add account)",
            subtitle = "إضافة حساب ثانٍ والتبديل السريع بين الحسابات على نفس الجهاز",
            tag = "account_item_add_account",
            onClick = { showAddAccountDialog = true }
        )

        WhatsAppSettingRow(
            icon = Icons.Default.DeleteForever,
            title = "حذف الحساب (Delete account)",
            subtitle = "حذف حسابك نهائياً أو تغيير الرقم بدلاً من الحذف",
            tag = "account_item_delete_account",
            onClick = { showDeleteAccountDialog = true }
        )
    }

    if (showPasskeysDialog) {
        AlertDialog(
            onDismissRequest = { showPasskeysDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = { Text("مفاتيح المرور (Passkeys)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "تتيح لك مفاتيح المرور تسجيل الدخول بأمان إلى حسابك باستخدام بصمة الإصبع أو التعرف على الوجه أو قفل الشاشة، ويتم حفظ مفتاح المرور في مدير كلمات المرور الخاص بك.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsSwitchRow(
                        title = "تفعيل مفاتيح المرور (Enable passkey)",
                        subtitle = if (settings.passkeysEnabled) "مفتاح المرور نشط على هذا الجهاز" else "غير مفعّل حالياً",
                        checked = settings.passkeysEnabled,
                        onCheckedChange = { enabled ->
                            onUpdate { it.copy(passkeysEnabled = enabled) }
                            onStatusMessage(if (enabled) "تم تفعيل مفتاح المرور (Passkey) بنجاح" else "تم إلغاء مفتاح المرور")
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPasskeysDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("تم", color = Color.White)
                }
            }
        )
    }

    if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text("عنوان البريد الإلكتروني (Email address)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "يساعدك البريد الإلكتروني في الوصول إلى حسابك واسترداد رمز التحقق بخطوتين، ولا يظهر لجهات الاتصال الأخرى.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("البريد الإلكتروني (Email)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("account_email_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdate { it.copy(emailAddress = emailInput.trim()) }
                        showEmailDialog = false
                        onStatusMessage("تم حفظ وتأكيد عنوان البريد الإلكتروني")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.testTag("account_email_save_button")
                ) {
                    Text("حفظ", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmailDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showAddAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text("إضافة حساب (Add account)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "الحساب النشط حالياً: $userPhone\nيمكنك إضافة حساب ثانٍ على نفس الهاتف للتبديل الفوري بين حسابك الشخصي وحساب العمل.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAddAccountDialog = false
                        onStatusMessage("الحساب الأساسي ($userPhone) نشط وجاهز للتبديل السريع")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("حسناً", color = Color.White)
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = {
                Text(
                    "حذف هذا الحساب (Delete this account)",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
            },
            text = {
                Column {
                    Text(
                        "إذا كنت تريد تغيير رقم هاتفك فقط، استخدم خيار \"تغيير الرقم\" بدلاً من حذف الحساب للحفاظ على مجموعاتك ورسائلك.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            showDeleteAccountDialog = false
                            onNavigateSubPage(SettingsSubPage.ACCOUNT_CHANGE_NUMBER)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_account_change_number_instead")
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تغيير الرقم بدلاً من ذلك (Change number)", color = EmeraldPrimary)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = deleteConfirmPhoneInput,
                        onValueChange = { deleteConfirmPhoneInput = it },
                        label = { Text("أدخل رقم الهاتف للتأكيد ($userPhone)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        onStatusMessage("تم إلغاء حذف الحساب للحفاظ على بياناتك المشفرة")
                    }
                ) {
                    Text("إبقاء الحساب آمناً", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) { Text("إغلاق") }
            }
        )
    }
}

@Composable
private fun SecurityNotificationsSubPage(
    settings: WhatsAppSettings,
    onUpdate: ((WhatsAppSettings) -> WhatsAppSettings) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("security_notifications_screen")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EnhancedEncryption,
                    contentDescription = "Security notifications",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(46.dp)
                )
            }
        }

        Text(
            text = "دردشاتك ومكالماتك خاصة (Your chats and calls are private)",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "يحافظ التشفير التام بين الطرفين (End-to-end encryption) على أمان رسائلك ومكالماتك الشخصية بينك وبين الأشخاص الذين تختارهم. لا يمكن حتى لتطبيق كلبرق قراءتها أو الاستماع إليها، وهذا يشمل ما يلي:",
            fontSize = 13.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        val encryptedFeatures = listOf(
            Icons.AutoMirrored.Filled.Chat to "الرسائل النصية والصوتية (Text and voice messages)",
            Icons.Default.Call to "المكالمات الصوتية ومكالمات الفيديو (Audio and video calls)",
            Icons.Default.PermMedia to "الصور ومقاطع الفيديو والمستندات (Photos, videos and documents)",
            Icons.Default.LocationOn to "مشاركة الموقع الجغرافي (Location sharing)",
            Icons.Default.DonutLarge to "تحديثات الحالة (Status updates)"
        )

        encryptedFeatures.forEach { (icon, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val next = !settings.securityNotificationsEnabled
                    onUpdate { it.copy(securityNotificationsEnabled = next) }
                    onStatusMessage(
                        if (next) "تم تفعيل إظهار إشعارات الأمان على هذا الجهاز"
                        else "تم إيقاف إظهار إشعارات الأمان على هذا الجهاز"
                    )
                }
                .padding(vertical = 8.dp)
                .testTag("security_notifications_toggle"),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "إظهار إشعارات الأمان على هذا الجهاز (Show security notifications on this device)",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "احصل على إشعار عند تغيّر رمز الأمان الخاص بهاتف إحدى جهات الاتصال في دردشة مشفرة تماماً بين الطرفين. إذا كانت لديك عدة أجهزة مرتبطة، يجب تفعيل هذا الإعداد على كل جهاز تريد تلقي الإشعارات عليه.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 19.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = settings.securityNotificationsEnabled,
                onCheckedChange = { next ->
                    onUpdate { it.copy(securityNotificationsEnabled = next) }
                    onStatusMessage(
                        if (next) "تم تفعيل إظهار إشعارات الأمان على هذا الجهاز"
                        else "تم إيقاف إظهار إشعارات الأمان على هذا الجهاز"
                    )
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = EmeraldPrimary
                )
            )
        }
    }
}

@Composable
private fun TwoStepVerificationSubPage(
    settings: WhatsAppSettings,
    onUpdate: ((WhatsAppSettings) -> WhatsAppSettings) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    // Wizard step: 0 = Status Overview, 1 = Create 6-digit PIN, 2 = Confirm 6-digit PIN, 3 = Recovery Email
    var wizardStep by remember { mutableIntStateOf(0) }
    var pinDraft by remember { mutableStateOf("") }
    var pinConfirmDraft by remember { mutableStateOf("") }
    var emailDraft by remember(settings.emailAddress) { mutableStateOf(settings.emailAddress) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("two_step_verification_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(EmeraldPrimary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Password,
                contentDescription = "Two-step verification",
                tint = EmeraldPrimary,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (wizardStep) {
            0 -> {
                if (!settings.twoStepVerificationEnabled) {
                    Text(
                        text = "التحقق بخطوتين (Two-step verification)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "لمزيد من الأمان، قم بتشغيل التحقق بخطوتين، والذي سيتطلب إدخال رقم تعريف شخصي (PIN) مكوّن من 6 أرقام عند تسجيل رقم هاتفك في كلبرق مجدداً.",
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Button(
                        onClick = {
                            pinDraft = ""
                            pinConfirmDraft = ""
                            errorMessage = null
                            wizardStep = 1
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("two_step_turn_on_button")
                    ) {
                        Text("تشغيل (Turn on)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Surface(
                        color = EmeraldPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "التحقق بخطوتين مفعّل (Two-step verification is ON). ستحتاج إلى إدخال رمز PIN عند إعادة تسجيل رقم هاتفك.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    WhatsAppSettingRow(
                        icon = Icons.Default.Close,
                        title = "إيقاف التشغيل (Turn off)",
                        subtitle = "إيقاف التحقق بخطوتين وإلغاء رمز PIN الحالي",
                        tag = "two_step_turn_off_button",
                        onClick = {
                            onUpdate { it.copy(twoStepVerificationEnabled = false, twoStepPin = "") }
                            onStatusMessage("تم إيقاف التحقق بخطوتين")
                        }
                    )

                    WhatsAppSettingRow(
                        icon = Icons.Default.Pin,
                        title = "تغيير رقم التعريف الشخصي (Change PIN)",
                        subtitle = "تعيين رمز PIN جديد مكوّن من 6 أرقام",
                        tag = "two_step_change_pin_button",
                        onClick = {
                            pinDraft = ""
                            pinConfirmDraft = ""
                            errorMessage = null
                            wizardStep = 1
                        }
                    )

                    WhatsAppSettingRow(
                        icon = Icons.Default.Email,
                        title = "تغيير عنوان البريد الإلكتروني (Change email address)",
                        subtitle = settings.emailAddress.ifBlank { "إضافة بريد إلكتروني لاسترداد رمز PIN" },
                        tag = "two_step_change_email_button",
                        onClick = {
                            errorMessage = null
                            wizardStep = 3
                        }
                    )
                }
            }

            1 -> {
                Text(
                    text = "إنشاء رقم تعريف شخصي مكوّن من 6 أرقام (Create a 6-digit PIN)",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "أدخل رقم تعريف شخصي مكوّناً من 6 أرقام يمكنك تذكره لحماية حسابك.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pinDraft,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(6)
                        pinDraft = digits
                        errorMessage = null
                    },
                    label = { Text("رمز PIN (6 أرقام) • 6-digit PIN") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("two_step_pin_input")
                )

                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(err, color = Color(0xFFEF4444), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { wizardStep = 0 },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            if (pinDraft.length < 6) {
                                errorMessage = "يرجى إدخال 6 أرقام كاملة لرمز PIN"
                            } else {
                                errorMessage = null
                                wizardStep = 2
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("two_step_pin_next_button")
                    ) {
                        Text("التالي (Next)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            2 -> {
                Text(
                    text = "تأكيد رقم التعريف الشخصي (Confirm your PIN)",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "أعد إدخال رمز PIN المكوّن من 6 أرقام لتأكيده.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pinConfirmDraft,
                    onValueChange = { input ->
                        pinConfirmDraft = input.filter { it.isDigit() }.take(6)
                        errorMessage = null
                    },
                    label = { Text("تأكيد رمز PIN • Confirm PIN") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("two_step_pin_confirm_input")
                )

                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(err, color = Color(0xFFEF4444), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { wizardStep = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("السابق")
                    }
                    Button(
                        onClick = {
                            if (pinConfirmDraft != pinDraft) {
                                errorMessage = "رمزا PIN غير متطابقين. يرجى المحاولة مرة أخرى."
                            } else {
                                errorMessage = null
                                wizardStep = 3
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("two_step_confirm_save_button")
                    ) {
                        Text("التالي (Next)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            3 -> {
                Text(
                    text = "إضافة بريد إلكتروني للاسترداد (Add recovery email)",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "أضف بريداً إلكترونياً لإعادة تعيين رمز PIN إذا نسيته مستقبلاً.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = emailDraft,
                    onValueChange = { emailDraft = it },
                    label = { Text("البريد الإلكتروني (Email address)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("two_step_email_input")
                )

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val finalPin = pinDraft.ifBlank { settings.twoStepPin.ifBlank { "123456" } }
                            onUpdate {
                                it.copy(
                                    twoStepVerificationEnabled = true,
                                    twoStepPin = finalPin
                                )
                            }
                            wizardStep = 0
                            onStatusMessage("تم تفعيل التحقق بخطوتين (Two-step verification) بنجاح")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("two_step_skip_email_button")
                    ) {
                        Text("تخطي (Skip)")
                    }
                    Button(
                        onClick = {
                            val finalPin = pinDraft.ifBlank { settings.twoStepPin.ifBlank { "123456" } }
                            onUpdate {
                                it.copy(
                                    twoStepVerificationEnabled = true,
                                    twoStepPin = finalPin,
                                    emailAddress = emailDraft.trim()
                                )
                            }
                            wizardStep = 0
                            onStatusMessage("تم تفعيل التحقق بخطوتين وحفظ البريد الإلكتروني بنجاح")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("two_step_save_all_button")
                    ) {
                        Text("حفظ (Save)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangeNumberSubPage(
    currentPhone: String,
    settings: WhatsAppSettings,
    onChangePhoneNumber: (String, String, Boolean, String) -> Boolean,
    onStatusMessage: (String) -> Unit
) {
    // WhatsApp 3-step Change Number flow: 1 = Intro, 2 = Enter Old & New Numbers, 3 = Notify Contacts & Done
    var step by remember { mutableIntStateOf(1) }
    var oldPhoneInput by remember(currentPhone) { mutableStateOf(currentPhone) }
    var newPhoneInput by remember { mutableStateOf("") }
    var notifyContacts by remember(settings.changeNumberNotifyContacts) {
        mutableStateOf(settings.changeNumberNotifyContacts)
    }
    var notifyMode by remember(settings.changeNumberNotifyMode) {
        mutableStateOf(settings.changeNumberNotifyMode)
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("change_number_screen")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SimCard,
                    contentDescription = "Change number",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        when (step) {
            1 -> {
                Text(
                    text = "سيؤدي تغيير رقم هاتفك إلى نقل معلومات حسابك والمجموعات والإعدادات. (Changing your phone number will migrate your account info, groups & settings.)",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "قبل المتابعة، يُرجى التأكد من قدرتك على تلقي رسائل نصية (SMS) أو مكالمات على رقمك الجديد.",
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "إذا كان لديك هاتف جديد ورقم جديد، فقم أولاً بتغيير رقمك على هاتفك القديم.",
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(28.dp))
                Button(
                    onClick = { step = 2 },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("change_number_step1_next")
                ) {
                    Text("التالي (Next)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            2 -> {
                Text(
                    text = "أدخل رقم هاتفك القديم مع رمز الدولة (Enter your old phone number with country code)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = oldPhoneInput,
                    onValueChange = {
                        oldPhoneInput = it
                        validationError = null
                    },
                    label = { Text("رقم الهاتف القديم (Old phone number)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("change_number_old_input")
                )

                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "أدخل رقم هاتفك الجديد مع رمز الدولة (Enter your new phone number with country code)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPhoneInput,
                    onValueChange = {
                        newPhoneInput = it
                        validationError = null
                    },
                    placeholder = { Text("+20 109 555 1234") },
                    label = { Text("رقم الهاتف الجديد (New phone number)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("change_number_new_input")
                )

                validationError?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(err, color = Color(0xFFEF4444), fontSize = 12.5.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { step = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("السابق")
                    }
                    Button(
                        onClick = {
                            val cleanOld = oldPhoneInput.trim()
                            val cleanNew = newPhoneInput.trim()
                            when {
                                cleanOld.length < 7 -> validationError = "يرجى إدخال رقم الهاتف القديم بشكل صحيح"
                                cleanNew.length < 7 -> validationError = "يرجى إدخال رقم الهاتف الجديد مع رمز الدولة"
                                cleanOld == cleanNew -> validationError = "يجب أن يكون رقم الهاتف الجديد مختلفاً عن الرقم القديم"
                                else -> {
                                    validationError = null
                                    step = 3
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("change_number_step2_next")
                    ) {
                        Text("التالي (Next)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            3 -> {
                Text(
                    text = "أنت على وشك تغيير رقمك من ${oldPhoneInput.trim()} إلى ${newPhoneInput.trim()}",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { notifyContacts = !notifyContacts }
                        .padding(vertical = 12.dp)
                        .testTag("change_number_notify_switch"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "إبلاغ جهات الاتصال (Notify contacts)",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "اختر جهات الاتصال التي سيتم إشعارها برقـمك الجديد",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notifyContacts,
                        onCheckedChange = { notifyContacts = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldPrimary
                        )
                    )
                }

                if (notifyContacts) {
                    val modes = listOf(
                        "كل جهات الاتصال",
                        "جهات الاتصال التي لدي دردشات معها",
                        "مخصص..."
                    )
                    modes.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { notifyMode = mode }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = notifyMode == mode,
                                onClick = { notifyMode = mode },
                                colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(mode, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "سيتم إبلاغ مجموعاتك دائماً بغض النظر عن اختيارك لإبلاغ جهات الاتصال. (Your groups will be notified regardless of whether you choose to notify your contacts.)",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val success = onChangePhoneNumber(
                            oldPhoneInput.trim(),
                            newPhoneInput.trim(),
                            notifyContacts,
                            notifyMode
                        )
                        if (!success) {
                            onStatusMessage("تعذر تغيير الرقم، تأكد من صحة الأرقام المدخلة")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("change_number_done_button")
                ) {
                    Text("تم (Done)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RequestAccountInfoSubPage(
    settings: WhatsAppSettings,
    onUpdate: ((WhatsAppSettings) -> WhatsAppSettings) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("request_account_info_screen")
    ) {
        Text(
            text = "معلومات الحساب (Account information)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = EmeraldPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "أنشئ تقريباً بمعلومات حسابك في كلبرق وإعداداته، والذي يمكنك الوصول إليه أو نقله إلى تطبيق آخر. لا يتضمن هذا التقرير رسائلك المشفرة.",
            fontSize = 13.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        WhatsAppSettingRow(
            icon = if (settings.accountReportRequested) Icons.Default.Schedule else Icons.Default.Download,
            title = if (settings.accountReportRequested) {
                "تم إرسال الطلب (Request sent)"
            } else {
                "طلب التقرير (Request report)"
            },
            subtitle = if (settings.accountReportRequested) {
                "سيكون التقرير جاهزاً خلال 3 أيام تقريباً"
            } else {
                "اضغط لطلب نسخة من إعدادات ومعلومات حسابك"
            },
            tag = "request_account_report_button",
            onClick = {
                onUpdate { it.copy(accountReportRequested = true) }
                onStatusMessage("تم إرسال طلب تقرير معلومات الحساب بنجاح")
            }
        )
    }
}

@Composable
private fun PrivacySettingsSubPage(
    settings: WhatsAppSettings,
    onUpdate: ((WhatsAppSettings) -> WhatsAppSettings) -> Unit,
    onOpenOptionDialog: (String, List<String>, String, (String) -> Unit) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val visibilityOptions = listOf("الكل", "جهات اتصالي", "جهات اتصالي باستثناء...", "لا أحد")
    val onlineOptions = listOf("الكل", "مثل آخر ظهور")
    var showBlockedContactsDialog by remember { mutableStateOf(false) }
    var newBlockedName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Privacy Checkup Banner (WhatsApp exact banner)
        Surface(
            color = EmeraldPrimary.copy(alpha = 0.12f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onStatusMessage("فحص الخصوصية: جميع إعدادات التشفير وحماية الحساب نشطة")
                }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("فحص الخصوصية (Privacy checkup)", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                    Text(
                        "تحكم في خصوصيتك واختر الإعدادات المناسبة لك خطوة بخطوة.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("من يمكنه رؤية معلوماتي الشخصية (Who can see my personal info)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
        Spacer(modifier = Modifier.height(8.dp))

        SettingsOptionRow("آخر ظهور ومتصل الآن (Last seen and online)", "${settings.lastSeenPrivacy} • ${settings.onlinePrivacy}") {
            onOpenOptionDialog("آخر ظهور (Last seen)", visibilityOptions, settings.lastSeenPrivacy) { choice ->
                onUpdate { it.copy(lastSeenPrivacy = choice) }
            }
        }

        SettingsOptionRow("من يمكنه رؤية عندما أكون متصلاً (Who can see when I'm online)", settings.onlinePrivacy) {
            onOpenOptionDialog("متصل الآن (Online)", onlineOptions, settings.onlinePrivacy) { choice ->
                onUpdate { it.copy(onlinePrivacy = choice) }
            }
        }

        SettingsOptionRow("الصورة الشخصية (Profile photo)", settings.profilePhotoPrivacy) {
            onOpenOptionDialog("الصورة الشخصية", visibilityOptions, settings.profilePhotoPrivacy) { choice ->
                onUpdate { it.copy(profilePhotoPrivacy = choice) }
            }
        }

        SettingsOptionRow("الأخبار (About)", settings.aboutPrivacy) {
            onOpenOptionDialog("الأخبار (About)", visibilityOptions, settings.aboutPrivacy) { choice ->
                onUpdate { it.copy(aboutPrivacy = choice) }
            }
        }

        SettingsOptionRow("الحالة (Status)", settings.statusPrivacy) {
            onOpenOptionDialog(
                "خصوصية الحالة (Status privacy)",
                listOf("جهات اتصالي", "جهات اتصالي باستثناء...", "مشاركة فقط مع..."),
                settings.statusPrivacy
            ) { choice ->
                onUpdate { it.copy(statusPrivacy = choice) }
            }
        }

        SettingsSwitchRow(
            title = "مؤشرات قراءة الرسائل (Read receipts)",
            subtitle = "إذا أوقفت تشغيل هذه الميزة، فلن تتمكن من إرسال مؤشرات قراءة الرسائل أو رؤيتها. يتم دائماً إرسال المؤشرات في الدردشات الجماعية.",
            checked = settings.readReceiptsEnabled,
            onCheckedChange = { v -> onUpdate { it.copy(readReceiptsEnabled = v) } }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        Text("الرسائل ذاتية الاختفاء (Disappearing messages)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
        SettingsOptionRow(
            title = "المؤقت الافتراضي للرسائل (Default message timer)",
            subtitle = "بدء دردشات جديدة برسائل ذاتية الاختفاء تختفي بعد المدة المحددة: ${settings.defaultDisappearingTimer}"
        ) {
            onOpenOptionDialog(
                "المؤقت الافتراضي للرسائل",
                listOf("24 ساعة", "7 أيام", "90 يوماً", "إيقاف"),
                settings.defaultDisappearingTimer
            ) { choice ->
                onUpdate { it.copy(defaultDisappearingTimer = choice) }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        SettingsOptionRow("المجموعات (Groups)", settings.groupsPrivacy) {
            onOpenOptionDialog(
                "من يمكنه إضافتي إلى المجموعات",
                listOf("الكل", "جهات اتصالي", "جهات اتصالي باستثناء..."),
                settings.groupsPrivacy
            ) { choice ->
                onUpdate { it.copy(groupsPrivacy = choice) }
            }
        }

        SettingsOptionRow("الموقع المباشر (Live location)", settings.liveLocationSharing) {
            onOpenOptionDialog(
                "الموقع المباشر (Live location)",
                listOf("لا يوجد", "مشاركة لمدة 15 دقيقة", "مشاركة لمدة ساعة واحدة", "مشاركة لمدة 8 ساعات"),
                settings.liveLocationSharing
            ) { choice ->
                onUpdate { it.copy(liveLocationSharing = choice) }
            }
        }

        SettingsSwitchRow(
            title = "المكالمات: إسكات المتصلين المجهولين (Silence unknown callers)",
            subtitle = "سيتم إسكات المكالمات الواردة من أرقام غير معروفة مع استمرار ظهورها في تبويب المكالمات",
            checked = settings.silenceUnknownCallers,
            onCheckedChange = { v -> onUpdate { it.copy(silenceUnknownCallers = v) } }
        )

        SettingsOptionRow(
            title = "جهات الاتصال المحظورة (Blocked contacts)",
            subtitle = "${settings.blockedContacts.size} جهة اتصال (${settings.blockedContacts.joinToString("، ")})"
        ) {
            showBlockedContactsDialog = true
        }

        SettingsSwitchRow(
            title = "قفل التطبيق (App lock)",
            subtitle = "طلب بصمة الإصبع أو الوجه لفتح تطبيق كلبرق",
            checked = settings.fingerprintLockEnabled,
            onCheckedChange = { v -> onUpdate { it.copy(fingerprintLockEnabled = v) } }
        )

        if (settings.fingerprintLockEnabled) {
            SettingsOptionRow(
                title = "قفل تلقائي (Automatically lock)",
                subtitle = settings.fingerprintLockTimeout
            ) {
                onOpenOptionDialog(
                    "قفل تلقائي",
                    listOf("حالاً", "بعد دقيقة واحدة", "بعد 30 دقيقة"),
                    settings.fingerprintLockTimeout
                ) { choice ->
                    onUpdate { it.copy(fingerprintLockTimeout = choice) }
                }
            }
        }

        SettingsOptionRow(
            title = "قفل الدردشة (Chat lock)",
            subtitle = "إخفاء الدردشات المقفلة وقفلها ببصمة الإصبع أو رمز سري"
        ) {
            onStatusMessage("قفل الدردشة جاهز: اضغط مطولاً على أي محادثة لاختيار قفل الدردشة")
        }
    }

    if (showBlockedContactsDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedContactsDialog = false },
            title = { Text("جهات الاتصال المحظورة (Blocked contacts)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    settings.blockedContacts.forEach { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(contact, modifier = Modifier.weight(1f), fontSize = 14.sp)
                            TextButton(
                                onClick = {
                                    val updated = settings.blockedContacts - contact
                                    onUpdate { it.copy(blockedContacts = updated, blockedContactsCount = updated.size) }
                                }
                            ) {
                                Text("إلغاء الحظر", fontSize = 12.sp, color = EmeraldPrimary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newBlockedName,
                        onValueChange = { newBlockedName = it },
                        label = { Text("إضافة رقم أو اسم للحظر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newBlockedName.isNotBlank()) {
                            val updated = settings.blockedContacts + newBlockedName.trim()
                            onUpdate { it.copy(blockedContacts = updated, blockedContactsCount = updated.size) }
                            newBlockedName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("إضافة للحظر", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockedContactsDialog = false }) { Text("إغلاق") }
            }
        )
    }
}

@Composable
private fun AvatarSettingsSubPage(
    repository: SafirRepository,
    userAvatarPreset: Int,
    userAvatarUri: String,
    onOpenStickerStore: () -> Unit
) {
    var showGalleryPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AvatarView(
            preset = userAvatarPreset,
            name = "أنا",
            size = 110.dp,
            imageUri = userAvatarUri
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "الصورة الشخصية من المعرض (بدون إيموجي)",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "اختر صورة حقيقية من المعرض كصورة شخصية لحسابك، أو حوّل صورك الشخصية إلى ملصقات مفرغة الخلفية.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { showGalleryPicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("avatar_pick_gallery_button"),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("اختيار صورة شخصية من المعرض", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onOpenStickerStore,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("تحويل صورة من المعرض إلى ملصق", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
        }
    }

    if (showGalleryPicker) {
        RealPhotoPickerSheetDialog(
            title = "اختيار صورة شخصية من المعرض",
            currentImageUri = userAvatarUri,
            onDismiss = { showGalleryPicker = false },
            onPhotoSelected = { path ->
                repository.updateUserProfilePhoto(path)
                showGalleryPicker = false
            },
            onRemovePhoto = {
                repository.updateUserProfilePhoto("")
                showGalleryPicker = false
            }
        )
    }
}

@Composable
private fun FavoritesSettingsSubPage(
    conversations: List<com.example.data.model.Conversation>,
    onToggleFavorite: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "اعثر بسرعة على الأشخاص والمجموعات المهمة في الدردشات والمكالمات عبر إضافتها إلى المفضلة.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        conversations.forEach { conv ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onToggleFavorite(conv.id) }
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarView(
                    preset = conv.groupIconPreset,
                    name = conv.title,
                    size = 44.dp,
                    isGroup = conv.isGroup,
                    imageUri = conv.groupIconUri
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(conv.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        if (conv.isGroup) "مجموعة" else "جهة اتصال",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onToggleFavorite(conv.id) }) {
                    Icon(
                        imageVector = if (conv.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "مفضلة",
                        tint = if (conv.isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatsSettingsSubPage(
    settings: WhatsAppSettings,
    onUpdate: ((WhatsAppSettings) -> WhatsAppSettings) -> Unit,
    onSetTheme: (String) -> Unit,
    onOpenOptionDialog: (String, List<String>, String, (String) -> Unit) -> Unit,
    onClearAllChats: () -> Unit,
    onStatusMessage: (String) -> Unit
) {
    var showBackupDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("العرض (Display)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
        Spacer(modifier = Modifier.height(8.dp))

        SettingsOptionRow("المظهر (Theme)", settings.themeMode) {
            onOpenOptionDialog(
                "اختر المظهر (Choose theme)",
                listOf("النظام الافتراضي", "فاتح", "داكن"),
                settings.themeMode
            ) { choice ->
                onSetTheme(choice)
            }
        }

        SettingsOptionRow("خلفية الشاشة (Wallpaper)", "تخصيص لون وخلفية شاشة الدردشة (${settings.chatWallpaperHex})") {
            onOpenOptionDialog(
                "خلفية شاشة الدردشة (Wallpaper)",
                listOf("#071120 (أزرق ملكي داكن)", "#0A3977 (ياقوتي عميق)", "#141D26 (ليلي عميق)", "#EAF2FC (سماوي فاتح)"),
                settings.chatWallpaperHex
            ) { choice ->
                val hex = choice.substringBefore(" ")
                onUpdate { it.copy(chatWallpaperHex = hex) }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        Text("إعدادات الدردشة (Chat settings)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
        Spacer(modifier = Modifier.height(8.dp))

        SettingsSwitchRow(
            title = "مفتاح الإدخال للإرسال (Enter is send)",
            subtitle = "سيؤدي الضغط على مفتاح الإدخال إلى إرسال رسالتك",
            checked = settings.enterIsSend,
            onCheckedChange = { v -> onUpdate { it.copy(enterIsSend = v) } }
        )

        SettingsSwitchRow(
            title = "عرض الوسائط (Media visibility)",
            subtitle = "عرض الوسائط التي تم تنزيلها حديثاً في معرض الصور بجهازك",
            checked = settings.mediaVisibilityInGallery,
            onCheckedChange = { v -> onUpdate { it.copy(mediaVisibilityInGallery = v) } }
        )

        SettingsOptionRow("حجم الخط (Font size)", settings.fontSize) {
            onOpenOptionDialog("حجم الخط (Font size)", listOf("صغير", "متوسط", "كبير"), settings.fontSize) { choice ->
                onUpdate { it.copy(fontSize = choice) }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        Text("الدردشات المؤرشفة (Archived chats)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
        SettingsSwitchRow(
            title = "إبقاء الدردشات في الأرشيف (Keep chats archived)",
            subtitle = "ستبقى الدردشات المؤرشفة في الأرشيف عند تلقي رسالة جديدة",
            checked = settings.keepChatsArchived,
            onCheckedChange = { v -> onUpdate { it.copy(keepChatsArchived = v) } }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        SettingsOptionRow(
            title = "النسخ الاحتياطي للدردشات (Chat backup)",
            subtitle = "آخر نسخ احتياطي: ${settings.lastBackupTime} • مشفر بين الطرفين"
        ) {
            showBackupDialog = true
        }

        SettingsOptionRow(
            title = "نقل الدردشات (Transfer chats)",
            subtitle = "نقل سجل الدردشات والوسائط مباشرة إلى هاتف أندرويد آخر"
        ) {
            onStatusMessage("نقل الدردشات جاهز عبر الاتصال المباشر المشفر Wi-Fi Direct")
        }

        SettingsOptionRow(
            title = "سجل الدردشات (Chat history)",
            subtitle = "تصدير دردشة، أرشفة جميع الدردشات، مسح محتوى جميع الدردشات"
        ) {
            showHistoryDialog = true
        }
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("النسخ الاحتياطي للدردشات (Chat backup)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "احتفظ بنسخة احتياطية مشفرة لرسائلك ووسائطك بحيث يمكنك استعادتها عند إعادة تثبيت كلبرق.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("آخر نسخ احتياطي: ${settings.lastBackupTime}", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = settings.backupIncludeVideos,
                            onCheckedChange = { v -> onUpdate { it.copy(backupIncludeVideos = v) } }
                        )
                        Text("تضمين مقاطع الفيديو (Include videos)", fontSize = 13.5.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdate { it.copy(lastBackupTime = "الآن (نسخة محدثة)") }
                        showBackupDialog = false
                        onStatusMessage("تم إنشاء نسخة احتياطية مشفرة للدردشات بنجاح")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("نسخ احتياطي الآن", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text("إغلاق") }
            }
        )
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("سجل الدردشات (Chat history)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showHistoryDialog = false
                            onStatusMessage("تم تجهيز ملف تصدير الدردشات بصيغة .txt")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.IosShare, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تصدير دردشة (Export chat)", modifier = Modifier.weight(1f))
                    }
                    TextButton(
                        onClick = {
                            showHistoryDialog = false
                            onClearAllChats()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مسح محتوى جميع الدردشات (Clear all chats)", color = Color(0xFFEF4444), modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) { Text("إغلاق") }
            }
        )
    }
}

@Composable
private fun NotificationsSettingsSubPage(
    settings: WhatsAppSettings,
    onUpdate: ((WhatsAppSettings) -> WhatsAppSettings) -> Unit,
    onOpenOptionDialog: (String, List<String>, String, (String) -> Unit) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SettingsSwitchRow(
            title = "نغمات المحادثات (Conversation tones)",
            subtitle = "تشغيل الأصوات للرسائل الواردة والصادرة أثناء فتح المحادثة",
            checked = settings.conversationTones,
            onCheckedChange = { v -> onUpdate { it.copy(conversationTones = v) } }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        Text("الرسائل (Messages)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
        SettingsOptionRow("نغمة الإشعار (Notification tone)", settings.messageNotificationTone) {
            onOpenOptionDialog(
                "نغمة الإشعار (Notification tone)",
                listOf("الافتراضية (كلبرق)", "نغمة واتساب الافتراضية", "نبضة هادئة", "ندى الصباح", "صامت"),
                settings.messageNotificationTone
            ) { choice ->
                onUpdate { it.copy(messageNotificationTone = choice) }
            }
        }
        SettingsOptionRow("الاهتزاز (Vibrate)", settings.messageVibrate) {
            onOpenOptionDialog(
                "الاهتزاز (Vibrate)",
                listOf("إيقاف", "افتراضي", "قصير", "طويل"),
                settings.messageVibrate
            ) { choice ->
                onUpdate { it.copy(messageVibrate = choice) }
            }
        }
        SettingsSwitchRow(
            title = "استخدام الإشعارات ذات الأولوية العالية (Use high priority notifications)",
            subtitle = "عرض معاينات الإشعارات في أعلى الشاشة",
            checked = settings.messageHighPriority,
            onCheckedChange = { v -> onUpdate { it.copy(messageHighPriority = v) } }
        )
        SettingsSwitchRow(
            title = "إشعارات التفاعلات (Reaction notifications)",
            subtitle = "إظهار الإشعارات للتفاعلات مع الرسائل التي ترسلها",
            checked = settings.reactionNotifications,
            onCheckedChange = { v -> onUpdate { it.copy(reactionNotifications = v) } }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        Text("المجموعات (Groups)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
        SettingsOptionRow("نغمة إشعار المجموعات (Notification tone)", settings.groupNotificationTone) {
            onOpenOptionDialog(
                "نغمة إشعار المجموعات",
                listOf("الافتراضية (كلبرق)", "نغمة واتساب الافتراضية", "رنين كريستال", "صامت"),
                settings.groupNotificationTone
            ) { choice ->
                onUpdate { it.copy(groupNotificationTone = choice) }
            }
        }
        SettingsOptionRow("الاهتزاز للمجموعات (Vibrate)", settings.groupVibrate) {
            onOpenOptionDialog(
                "الاهتزاز للمجموعات",
                listOf("إيقاف", "افتراضي", "قصير", "طويل"),
                settings.groupVibrate
            ) { choice ->
                onUpdate { it.copy(groupVibrate = choice) }
            }
        }
        SettingsSwitchRow(
            title = "الإشعارات ذات الأولوية العالية للمجموعات (Use high priority notifications)",
            subtitle = "عرض معاينات إشعارات المجموعات في أعلى الشاشة",
            checked = settings.groupHighPriority,
            onCheckedChange = { v -> onUpdate { it.copy(groupHighPriority = v) } }
        )
        SettingsSwitchRow(
            title = "إشعارات التفاعلات للمجموعات (Reaction notifications)",
            subtitle = "إظهار الإشعارات للتفاعلات مع رسائلك داخل المجموعات",
            checked = settings.groupReactionNotifications,
            onCheckedChange = { v -> onUpdate { it.copy(groupReactionNotifications = v) } }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        Text("المكالمات (Calls)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
        SettingsOptionRow("نغمة رنين المكالمات (Ringtone)", settings.callRingtone) {
            onOpenOptionDialog(
                "نغمة رنين المكالمات (Ringtone)",
                listOf("الافتراضية", "رنين كلبرق الكلاسيكي", "هادئ"),
                settings.callRingtone
            ) { choice ->
                onUpdate { it.copy(callRingtone = choice) }
            }
        }
        SettingsOptionRow("الاهتزاز للمكالمات (Vibrate)", settings.callVibrate) {
            onOpenOptionDialog(
                "الاهتزاز للمكالمات",
                listOf("إيقاف", "افتراضي", "قصير", "طويل"),
                settings.callVibrate
            ) { choice ->
                onUpdate { it.copy(callVibrate = choice) }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        SettingsOptionRow(
            title = "إعادة تعيين إعدادات الإشعارات (Reset notification settings)",
            subtitle = "إعادة تعيين جميع إعدادات الإشعارات بما في ذلك نغمات الإشعارات المخصصة للدردشات"
        ) {
            onUpdate {
                it.copy(
                    conversationTones = true,
                    messageNotificationTone = "نغمة واتساب الافتراضية",
                    messageVibrate = "افتراضي",
                    messageHighPriority = true,
                    reactionNotifications = true,
                    groupNotificationTone = "نغمة واتساب الافتراضية",
                    groupVibrate = "افتراضي",
                    groupHighPriority = true,
                    groupReactionNotifications = true,
                    callRingtone = "نغمة رنين الجهاز الافتراضية",
                    callVibrate = "افتراضي"
                )
            }
            onStatusMessage("تمت إعادة تعيين إعدادات الإشعارات إلى القيم الافتراضية")
        }
    }
}

@Composable
private fun StorageSettingsSubPage(
    settings: WhatsAppSettings,
    onUpdate: ((WhatsAppSettings) -> WhatsAppSettings) -> Unit,
    onOpenOptionDialog: (String, List<String>, String, (String) -> Unit) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val allMediaTypes = listOf("الصور", "الفيديو", "الملفات", "المقاطع الصوتية")
    var activeMultiSelectTarget by remember { mutableStateOf<String?>(null) } // "MOBILE", "WIFI", "ROAMING"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("storage_and_data_subpage")
    ) {
        SettingsOptionRow(
            title = "إدارة التخزين (Manage storage)",
            subtitle = "${settings.storageUsedMb} ميجابايت مستخدمة • قاعدة بيانات 2 جيجابايت (WAL) نشطة • اضغط لتنظيف المؤقتة"
        ) {
            onUpdate { it.copy(storageUsedMb = 142f) }
            onStatusMessage("تم تنظيف الملفات المؤقتة وتوفير مساحة التخزين")
        }

        SettingsOptionRow(
            title = "استخدام الشبكة (Network usage)",
            subtitle = "${settings.networkSentMb} ميجابايت مرسلة • ${settings.networkReceivedMb} ميجابايت مستلمة (وضع نت السوشيال مفعّل)"
        ) {
            onStatusMessage("إحصائيات استخدام الشبكة محدثة حتى اللحظة")
        }

        // Prominent "استخدام بيانات أقل للمكالمات" switch requested by user
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(EmeraldPrimary.copy(alpha = 0.10f))
                .clickable {
                    val next = !settings.useLessDataForCalls
                    onUpdate { it.copy(useLessDataForCalls = next) }
                    onStatusMessage(
                        if (next) "تم تفعيل استخدام بيانات أقل للمكالمات لتوفير الإنترنت"
                        else "تم إيقاف توفير البيانات للمكالمات"
                    )
                }
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .testTag("storage_use_less_data_calls_row"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "استخدام بيانات أقل للمكالمات (Use less data for calls)",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "تقليل استهلاك بيانات الهاتف وتوفير الإنترنت أثناء المكالمات الصوتية والمرئية",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = settings.useLessDataForCalls,
                onCheckedChange = { v ->
                    onUpdate { it.copy(useLessDataForCalls = v) }
                    onStatusMessage(
                        if (v) "تم تفعيل استخدام بيانات أقل للمكالمات لتوفير الإنترنت"
                        else "تم إيقاف توفير البيانات للمكالمات"
                    )
                },
                modifier = Modifier.testTag("storage_use_less_data_calls_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = EmeraldPrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        SettingsOptionRow(
            title = "الوكيل (Proxy)",
            subtitle = if (settings.proxyEnabled) "مفعّل (${settings.proxyAddress.ifBlank { "127.0.0.1:8080" }})" else "إيقاف"
        ) {
            onUpdate { it.copy(proxyEnabled = !it.proxyEnabled) }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        Text("جودة الوسائط (Media quality)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
        SettingsOptionRow("جودة تحميل الوسائط (Media upload quality)", settings.mediaUploadQuality) {
            onOpenOptionDialog(
                "جودة تحميل الوسائط",
                listOf("الجودة الأصلية 100% بدون ضغط (RAW/HD)", "دقة عالية (HD)", "الجودة القياسية"),
                settings.mediaUploadQuality
            ) { choice ->
                onUpdate { it.copy(mediaUploadQuality = choice) }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        Text(
            text = "التنزيل التلقائي للوسائط (Media auto-download)",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = EmeraldPrimary
        )
        Text(
            text = "لا يتم احتساب الرسائل الصوتية ضمن التنزيل التلقائي لأنها تُنزّل دائماً لضمان أفضل تجربة تواصل.",
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
        )

        // 1. Mobile Data Auto-Download
        AutoDownloadConnectionSection(
            title = "أثناء استخدام بيانات المحمول (When using mobile data)",
            selectedMedia = settings.autoDownloadMobile,
            allMediaTypes = allMediaTypes,
            sectionTag = "auto_download_mobile",
            onOpenDialog = { activeMultiSelectTarget = "MOBILE" },
            onToggleMedia = { mediaType ->
                val current = settings.autoDownloadMobile.toMutableSet()
                if (current.contains(mediaType)) current.remove(mediaType) else current.add(mediaType)
                onUpdate { it.copy(autoDownloadMobile = current) }
                onStatusMessage("تحديث التنزيل التلقائي عبر بيانات المحمول: ${current.joinToString("، ").ifBlank { "بلا وسائط" }}")
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Wi-Fi Auto-Download
        AutoDownloadConnectionSection(
            title = "أثناء الاتصال بـ Wi-Fi (When connected on Wi-Fi)",
            selectedMedia = settings.autoDownloadWifi,
            allMediaTypes = allMediaTypes,
            sectionTag = "auto_download_wifi",
            onOpenDialog = { activeMultiSelectTarget = "WIFI" },
            onToggleMedia = { mediaType ->
                val current = settings.autoDownloadWifi.toMutableSet()
                if (current.contains(mediaType)) current.remove(mediaType) else current.add(mediaType)
                onUpdate { it.copy(autoDownloadWifi = current) }
                onStatusMessage("تحديث التنزيل التلقائي عبر Wi-Fi: ${current.joinToString("، ").ifBlank { "بلا وسائط" }}")
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Roaming Auto-Download
        AutoDownloadConnectionSection(
            title = "أثناء التجوال (When roaming)",
            selectedMedia = settings.autoDownloadRoaming,
            allMediaTypes = allMediaTypes,
            sectionTag = "auto_download_roaming",
            onOpenDialog = { activeMultiSelectTarget = "ROAMING" },
            onToggleMedia = { mediaType ->
                val current = settings.autoDownloadRoaming.toMutableSet()
                if (current.contains(mediaType)) current.remove(mediaType) else current.add(mediaType)
                onUpdate { it.copy(autoDownloadRoaming = current) }
                onStatusMessage("تحديث التنزيل التلقائي أثناء التجوال: ${current.joinToString("، ").ifBlank { "بلا وسائط" }}")
            }
        )
    }

    // WhatsApp Multi-Select Media Auto-Download Dialog
    if (activeMultiSelectTarget != null) {
        val target = activeMultiSelectTarget!!
        val dialogTitle = when (target) {
            "MOBILE" -> "أثناء استخدام بيانات المحمول"
            "WIFI" -> "أثناء الاتصال بـ Wi-Fi"
            else -> "أثناء التجوال"
        }
        val initialSelection = when (target) {
            "MOBILE" -> settings.autoDownloadMobile
            "WIFI" -> settings.autoDownloadWifi
            else -> settings.autoDownloadRoaming
        }
        var tempSelection by remember(target) { mutableStateOf(initialSelection) }

        AlertDialog(
            onDismissRequest = { activeMultiSelectTarget = null },
            title = {
                Text(dialogTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    allMediaTypes.forEach { mediaType ->
                        val isChecked = tempSelection.contains(mediaType)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    tempSelection = if (isChecked) {
                                        tempSelection - mediaType
                                    } else {
                                        tempSelection + mediaType
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    tempSelection = if (checked) {
                                        tempSelection + mediaType
                                    } else {
                                        tempSelection - mediaType
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = mediaType,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (target) {
                            "MOBILE" -> onUpdate { it.copy(autoDownloadMobile = tempSelection) }
                            "WIFI" -> onUpdate { it.copy(autoDownloadWifi = tempSelection) }
                            else -> onUpdate { it.copy(autoDownloadRoaming = tempSelection) }
                        }
                        onStatusMessage("تم حفظ إعدادات التنزيل التلقائي ($dialogTitle)")
                        activeMultiSelectTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("موافق", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeMultiSelectTarget = null }) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun AutoDownloadConnectionSection(
    title: String,
    selectedMedia: Set<String>,
    allMediaTypes: List<String>,
    sectionTag: String,
    onOpenDialog: () -> Unit,
    onToggleMedia: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenDialog() }
                .testTag("${sectionTag}_header")
        ) {
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = selectedMedia.joinToString("، ").ifBlank { "بلا وسائط (لا يتم تنزيل أي وسائط تلقائياً)" },
                fontSize = 12.5.sp,
                color = EmeraldPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Direct interactive media type toggles (الصور، الفيديو، الملفات، المقاطع الصوتية)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            allMediaTypes.forEach { mediaType ->
                val isSelected = selectedMedia.contains(mediaType)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) EmeraldPrimary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surface
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onToggleMedia(mediaType) }
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                        .testTag("${sectionTag}_chip_$mediaType"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSelected) "✓ $mediaType" else mediaType,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun StarredSettingsSubPage(starredCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = SecurityShieldGold,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "لديك $starredCount رسالة مميزة بنجمة",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "اضغط مطولاً على أي رسالة داخل الدردشة ثم اختر أيقونة النجمة للرجوع إليها بسهولة في أي وقت.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LanguageSettingsSubPage(
    currentLanguage: String,
    onSelectLanguage: (String) -> Unit
) {
    val languages = listOf(
        "العربية (لغة الجهاز)",
        "English",
        "Français",
        "Türkçe",
        "Español",
        "Deutsch"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        languages.forEach { lang ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectLanguage(lang) }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = lang == currentLanguage,
                    onClick = { onSelectLanguage(lang) },
                    colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(lang, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun HelpSettingsSubPage(
    onStatusMessage: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(EmeraldPrimary.copy(alpha = 0.12f))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_kalbarq_logo),
                contentDescription = "شعار كلبرق",
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "كلبرق (Kalbarq Messenger)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "الإصدار 3.5.0 • مراسلة فورية مشفرة بسرعة البرق",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsOptionRow("مركز المساعدة (Help Center)", "الأسئلة الشائعة وإرشادات الاستخدام والأمان") {
            onStatusMessage("مركز مساعدة كلبرق: جميع الرسائل والمكالمات مشفرة بمعيار AES-256")
        }
        SettingsOptionRow("اتصل بنا (Contact us)", "هل لديك أسئلة؟ تواصل مع فريق دعم كلبرق") {
            onStatusMessage("تم فتح قناة الدعم الفني الفوري")
        }
        SettingsOptionRow("الشروط وسياسة الخصوصية (Terms and Privacy Policy)", "اطلع على التزامنا الكامل بالتشفير الشامل وحماية البيانات") {
            onStatusMessage("التشفير الشامل بين الطرفين مفعّل افتراضياً لجميع المحادثات")
        }
        SettingsOptionRow("معلومات التطبيق (App info)", "كلبرق الإصدار 3.5.0 • جميع الحقوق محفوظة") {
            onStatusMessage("كلبرق الإصدار 3.5.0 — أحدث إصدار مثبت")
        }
    }
}

@Composable
private fun SettingsOptionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = EmeraldPrimary
            )
        )
    }
}
