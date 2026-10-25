package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Conversation
import com.example.data.model.StatusStory
import com.example.data.model.StoryType
import com.example.data.repository.SafirRepository
import com.example.ui.components.AvatarView
import com.example.ui.components.DirectPhoneChatDialog
import com.example.ui.components.GroupJoinPreviewSheet
import com.example.ui.components.KalbarqGoldFeaturesDialog
import com.example.ui.components.OpenInviteLinkDialog
import com.example.ui.components.WhatsAppCameraCaptureDialog
import com.example.ui.theme.CheckmarkBlue
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.OnlineGreenGlow
import com.example.ui.theme.SecurityShieldGold
import com.example.util.ApkExporter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatsListScreen(
    repository: SafirRepository,
    onOpenChat: (String) -> Unit,
    onCreateGroup: () -> Unit,
    onOpenContactPicker: () -> Unit,
    onOpenStickerStore: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenStarredMessages: () -> Unit = {},
    onOpenCommunities: () -> Unit,
    onOpenChannels: () -> Unit,
    onOpenLinkedDevices: () -> Unit,
    onOpenCalls: () -> Unit,
    onOpenStatusViewer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val conversations by repository.conversations.collectAsState(initial = emptyList())
    val stories by repository.stories.collectAsState(initial = emptyList())
    val settings by repository.settings.collectAsState()

    // Modern WhatsApp 4-Tab Bottom Bar:
    // 0: الدردشات | 1: التحديثات | 2: المجتمعات | 3: المكالمات
    var selectedTab by remember { mutableIntStateOf(0) }
    // WhatsApp Chat Filters: 0: الكل | 1: غير مقروءة | 2: المفضلة | 3: المجموعات | 4: متصل الآن 🟢
    var selectedChatFilter by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Dialog states for Camera, Kalbarq Gold, Direct Phone Chat, Invite Link, and Avatar Story Viewer
    var showCameraStudio by remember { mutableStateOf(false) }
    var showGoldFeaturesDialog by remember { mutableStateOf(false) }
    var showDirectPhoneDialog by remember { mutableStateOf(false) }
    var showOpenInviteLinkDialog by remember { mutableStateOf(false) }
    var inviteLinkToPreview by remember { mutableStateOf<String?>(null) }
    var storyForAvatarViewer by remember { mutableStateOf<StatusStory?>(null) }
    val currentUser by repository.currentUser.collectAsState()

    // Contextual action triggers for tabs 1, 2, 3
    var statusCreateTrigger by remember { mutableIntStateOf(0) }
    var communityCreateTrigger by remember { mutableIntStateOf(0) }
    var callCreateTrigger by remember { mutableIntStateOf(0) }

    val filteredConversations = remember(conversations, searchQuery, selectedChatFilter) {
        val byFilter = when (selectedChatFilter) {
            1 -> conversations.filter { it.unreadCount > 0 }
            2 -> conversations.filter { it.isFavorite }
            3 -> conversations.filter { it.isGroup }
            4 -> conversations.filter { it.isOnline }
            else -> conversations
        }
        if (searchQuery.isBlank()) byFilter
        else byFilter.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.lastMessageText.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalUnread = remember(conversations) {
        conversations.sumOf { it.unreadCount }
    }

    val unviewedStoriesCount = remember(stories) {
        stories.count { !it.isViewed }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // WhatsApp / Kalbarq Top App Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (isSearchActive) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("البحث في الدردشات...", color = DarkTextSecondary) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary)
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                searchQuery = ""
                                isSearchActive = false
                            }) {
                                Text("إلغاء", color = EmeraldPrimary, fontSize = 13.sp)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_kalbarq_logo),
                                contentDescription = "شعار كلبرق",
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = when (selectedTab) {
                                    0 -> "كلبرق"
                                    1 -> "التحديثات"
                                    2 -> "المجتمعات"
                                    else -> "المكالمات"
                                },
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Kalbarq Gold & Theme Color Customizer Quick Button
                            IconButton(
                                onClick = { showGoldFeaturesDialog = true },
                                modifier = Modifier.testTag("top_gold_features_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "إضافات كلبرق الذهبية وتغيير لون الواجهة",
                                    tint = SecurityShieldGold
                                )
                            }

                            // Instant WhatsApp Camera Capture Button (Photo & Video + Runtime Permissions)
                            IconButton(
                                onClick = { showCameraStudio = true },
                                modifier = Modifier.testTag("top_camera_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "الكاميرا (التقاط صورة أو فيديو)",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = { isSearchActive = true },
                                modifier = Modifier.testTag("top_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "البحث",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier.testTag("main_overflow_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "المزيد من الخيارات",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Official WhatsApp + Kalbarq Gold 3-Dot Overflow Menu
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "إضافات كلبرق الذهبية وتغيير اللون",
                                                color = SecurityShieldGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SecurityShieldGold)
                                        },
                                        modifier = Modifier.testTag("menu_item_gold_features"),
                                        onClick = {
                                            showMenu = false
                                            showGoldFeaturesDialog = true
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("مراسلة رقم غير محفوظ (مباشر)", color = MaterialTheme.colorScheme.onSurface) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldPrimary)
                                        },
                                        modifier = Modifier.testTag("menu_item_direct_phone"),
                                        onClick = {
                                            showMenu = false
                                            showDirectPhoneDialog = true
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("مجموعة جديدة", color = MaterialTheme.colorScheme.onSurface) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Groups, contentDescription = null, tint = EmeraldPrimary)
                                        },
                                        modifier = Modifier.testTag("menu_item_new_group"),
                                        onClick = {
                                            showMenu = false
                                            onCreateGroup()
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("فتح رابط دعوة والانضمام لمجموعة", color = MaterialTheme.colorScheme.onSurface) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Link, contentDescription = null, tint = EmeraldPrimary)
                                        },
                                        modifier = Modifier.testTag("menu_item_open_invite_link"),
                                        onClick = {
                                            showMenu = false
                                            showOpenInviteLinkDialog = true
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("استخراج ومشاركة ملف التطبيق (APK)", color = EmeraldLight, fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Download, contentDescription = null, tint = EmeraldLight)
                                        },
                                        modifier = Modifier.testTag("menu_item_export_apk"),
                                        onClick = {
                                            showMenu = false
                                            ApkExporter.shareApkPackage(context)
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("رسالة جماعية جديدة", color = MaterialTheme.colorScheme.onSurface) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Campaign, contentDescription = null, tint = EmeraldPrimary)
                                        },
                                        modifier = Modifier.testTag("menu_item_broadcast"),
                                        onClick = {
                                            showMenu = false
                                            onOpenContactPicker()
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("الأجهزة المرتبطة", color = MaterialTheme.colorScheme.onSurface) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Devices, contentDescription = null, tint = EmeraldPrimary)
                                        },
                                        modifier = Modifier.testTag("menu_item_linked_devices"),
                                        onClick = {
                                            showMenu = false
                                            onOpenLinkedDevices()
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("الرسائل المميزة بنجمة", color = MaterialTheme.colorScheme.onSurface) },
                                        leadingIcon = {
                                            Icon(Icons.Default.StarBorder, contentDescription = null, tint = EmeraldPrimary)
                                        },
                                        modifier = Modifier.testTag("menu_item_starred"),
                                        onClick = {
                                            showMenu = false
                                            onOpenStarredMessages()
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("الإعدادات", color = MaterialTheme.colorScheme.onSurface) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Settings, contentDescription = null, tint = EmeraldPrimary)
                                        },
                                        modifier = Modifier.testTag("menu_item_settings"),
                                        onClick = {
                                            showMenu = false
                                            onOpenSettings()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Social Data Package & Weak Network Resilience Status Strip
            if (selectedTab == 0 && (settings.socialDataPackageMode || settings.weakNetworkBoosterEnabled)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EmeraldPrimary.copy(alpha = 0.12f))
                        .clickable { showGoldFeaturesDialog = true }
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WifiTethering,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "نت سوشيال نشط • يدعم الإنترنت الضعيف ونقل ملفات حتى 2 جيجا",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تغيير اللون",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }
            }

            // Main Body Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Modern WhatsApp + Kalbarq Gold Filter Chips Row:
                            // الكل | غير مقروءة | المفضلة | المجموعات | متصل الآن 🟢
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val filters = listOf(
                                    0 to "الكل",
                                    1 to "غير مقروءة",
                                    2 to "المفضلة",
                                    3 to "المجموعات",
                                    4 to "متصل الآن 🟢"
                                )
                                filters.forEach { (idx, label) ->
                                    val selected = selectedChatFilter == idx
                                    FilterChip(
                                        selected = selected,
                                        onClick = { selectedChatFilter = idx },
                                        label = {
                                            Text(
                                                text = label,
                                                fontSize = 13.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.22f),
                                            selectedLabelColor = EmeraldPrimary
                                        ),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                }
                            }

                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(filteredConversations, key = { it.id }) { conversation ->
                                    ConversationItem(
                                        conversation = conversation,
                                        showExternalOnlineBadge = settings.goldShowOnlineOnChatList,
                                        onClick = { onOpenChat(conversation.id) },
                                        onAvatarClick = {
                                            // If contact has an active status story, open it directly like WhatsApp!
                                            val matchedStory = stories.find {
                                                it.id == conversation.activeStoryId || it.userName == conversation.title
                                            }
                                            if (matchedStory != null) {
                                                repository.markStoryViewed(matchedStory.id)
                                                storyForAvatarViewer = matchedStory
                                            } else {
                                                onOpenChat(conversation.id)
                                            }
                                        },
                                        onToggleFavorite = {
                                            repository.toggleFavoriteConversation(conversation.id)
                                            Toast.makeText(
                                                context,
                                                if (!conversation.isFavorite) "تمت الإضافة إلى المفضلة" else "تمت الإزالة من المفضلة",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = 76.dp)
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        StatusScreen(
                            repository = repository,
                            onOpenStatusViewer = onOpenStatusViewer,
                            onOpenChannels = onOpenChannels,
                            externalCreateTrigger = statusCreateTrigger
                        )
                    }

                    2 -> {
                        CommunitiesScreen(
                            repository = repository,
                            onOpenChat = onOpenChat,
                            onBack = { selectedTab = 0 },
                            externalCreateTrigger = communityCreateTrigger
                        )
                    }

                    3 -> {
                        CallsScreen(
                            repository = repository,
                            onBack = { selectedTab = 0 },
                            externalCreateTrigger = callCreateTrigger
                        )
                    }
                }

                // Floating Action Button (Contextual per WhatsApp tab)
                FloatingActionButton(
                    onClick = {
                        when (selectedTab) {
                            1 -> statusCreateTrigger++
                            2 -> communityCreateTrigger++
                            3 -> callCreateTrigger++
                            else -> onOpenContactPicker()
                        }
                    },
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(18.dp)
                        .size(56.dp)
                        .testTag("main_fab_button")
                ) {
                    Icon(
                        imageVector = when (selectedTab) {
                            1 -> Icons.Default.CameraAlt
                            2 -> Icons.Default.Groups
                            3 -> Icons.Default.Call
                            else -> Icons.Default.AddComment
                        },
                        contentDescription = "رسالة جديدة",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Ultra-Clear, High-Contrast Bottom Navigation Bar:
            // الدردشات | التحديثات | المجتمعات | المكالمات
            HorizontalDivider(
                color = EmeraldPrimary.copy(alpha = 0.35f),
                thickness = 1.dp
            )
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 12.dp
            ) {
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = EmeraldPrimary,
                    indicatorColor = EmeraldPrimary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f)
                )

                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    alwaysShowLabel = true,
                    modifier = Modifier.testTag("bottom_nav_chats"),
                    icon = {
                        BadgedBox(
                            badge = {
                                if (totalUnread > 0) {
                                    Badge(containerColor = OnlineGreenGlow, contentColor = Color.White) {
                                        Text(totalUnread.toString(), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "الدردشات",
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = "الدردشات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    },
                    colors = navItemColors
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    alwaysShowLabel = true,
                    modifier = Modifier.testTag("bottom_nav_updates"),
                    icon = {
                        BadgedBox(
                            badge = {
                                if (unviewedStoriesCount > 0) {
                                    Badge(containerColor = OnlineGreenGlow, contentColor = Color.White) {
                                        Text(unviewedStoriesCount.toString(), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.RadioButtonChecked,
                                contentDescription = "التحديثات",
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = "التحديثات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    },
                    colors = navItemColors
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    alwaysShowLabel = true,
                    modifier = Modifier.testTag("bottom_nav_communities"),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Diversity3,
                            contentDescription = "المجتمعات",
                            modifier = Modifier.size(25.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "المجتمعات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    },
                    colors = navItemColors
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    alwaysShowLabel = true,
                    modifier = Modifier.testTag("bottom_nav_calls"),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "المكالمات",
                            modifier = Modifier.size(25.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "المكالمات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    },
                    colors = navItemColors
                )
            }
        }
    }

    // Fullscreen Camera & Video Recorder Studio (triggered from Top App Bar Camera icon)
    if (showCameraStudio) {
        WhatsAppCameraCaptureDialog(
            preserveOriginalQualityDefault = settings.preserveOriginalImageQuality,
            onDismiss = { showCameraStudio = false },
            onMediaCaptured = { filePath, isVideo, caption, _, durationSec ->
                repository.postStory(
                    textContent = caption,
                    backgroundColorHex = "#0055D4",
                    storyType = if (isVideo) StoryType.VIDEO else StoryType.IMAGE,
                    mediaUri = filePath,
                    videoDurationSeconds = if (isVideo) durationSec.coerceAtLeast(15) else 0
                )
                showCameraStudio = false
                selectedTab = 1
                Toast.makeText(context, "تم نشر اللقطة في حالتك بالجودة الأصلية 100% ✓", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Kalbarq Gold Features & Theme Color Customizer Dialog
    if (showGoldFeaturesDialog) {
        KalbarqGoldFeaturesDialog(
            repository = repository,
            onDismiss = { showGoldFeaturesDialog = false }
        )
    }

    // Direct Chat with Unsaved Phone Number Dialog
    if (showDirectPhoneDialog) {
        DirectPhoneChatDialog(
            onDismiss = { showDirectPhoneDialog = false },
            onStartChat = { phone, name ->
                showDirectPhoneDialog = false
                scope.launch {
                    val convId = repository.startDirectChatByPhoneNumber(phone, name)
                    onOpenChat(convId)
                }
            }
        )
    }

    // Direct Status Story Viewer when clicking a contact's avatar with a status ring in the chat list
    storyForAvatarViewer?.let { story ->
        StoryViewerModal(
            story = story,
            onClose = { storyForAvatarViewer = null }
        )
    }

    // Open Invite Link Dialog (paste or pick WhatsApp / Kalbarq group invite URL)
    if (showOpenInviteLinkDialog) {
        OpenInviteLinkDialog(
            onDismiss = { showOpenInviteLinkDialog = false },
            onOpenInvitePreview = { rawUrl ->
                showOpenInviteLinkDialog = false
                inviteLinkToPreview = rawUrl
            }
        )
    }

    // Group Join Preview Sheet when an invite link is opened from ChatsListScreen
    if (inviteLinkToPreview != null) {
        val rawUrl = inviteLinkToPreview!!
        val token = repository.extractInviteToken(rawUrl)
        val matchedConv = conversations.find {
            it.inviteToken.equals(token, ignoreCase = true)
        }
        GroupJoinPreviewSheet(
            conversation = matchedConv,
            isRevoked = matchedConv?.isInviteLinkRevoked == true,
            currentUserName = currentUser?.displayName ?: "أنا",
            repository = repository,
            onDismiss = { inviteLinkToPreview = null },
            onNavigateToChat = { convId ->
                inviteLinkToPreview = null
                onOpenChat(convId)
            },
            inviteLinkOrToken = rawUrl
        )
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    showExternalOnlineBadge: Boolean = true,
    onClick: () -> Unit,
    onAvatarClick: () -> Unit = onClick,
    onToggleFavorite: () -> Unit = {}
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale("ar")) }
    val formattedTime = remember(conversation.lastMessageTime) {
        timeFormat.format(Date(conversation.lastMessageTime))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with WhatsApp Status Ring (when contact has an active status) & clickable to view status!
        Box(contentAlignment = Alignment.Center) {
            AvatarView(
                preset = conversation.groupIconPreset,
                name = conversation.title,
                size = 54.dp,
                isOnline = conversation.isOnline,
                hasActiveStory = conversation.hasActiveStatus,
                isGroup = conversation.isGroup,
                imageUri = conversation.groupIconUri,
                onClick = onAvatarClick
            )
            if (conversation.hasActiveStatus) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (conversation.hasUnviewedStatus) EmeraldPrimary else Color.Gray)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "حالة",
                        color = Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = conversation.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // External Online Status Indicator Badge (WhatsApp Gold feature)
                    if (showExternalOnlineBadge && !conversation.isGroup) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (conversation.isOnline) OnlineGreenGlow.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = 0.8.dp,
                                    color = if (conversation.isOnline) OnlineGreenGlow else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (conversation.isOnline) "متصل الآن 🟢" else "غير متصل",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (conversation.isOnline) OnlineGreenGlow else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = formattedTime,
                    fontSize = 11.sp,
                    color = if (conversation.unreadCount > 0) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (conversation.unreadCount == 0) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = CheckmarkBlue,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = conversation.lastMessageText,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = if (conversation.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "المفضلة",
                            tint = if (conversation.isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (conversation.isPinned) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "مثبتة",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (conversation.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(EmeraldPrimary)
                                .padding(horizontal = 6.5.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = conversation.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
