package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.SafirRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CallsScreen
import com.example.ui.screens.ChannelsScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatsListScreen
import com.example.ui.screens.CommunitiesScreen
import com.example.ui.screens.ContactPickerScreen
import com.example.ui.screens.CreateGroupScreen
import com.example.ui.screens.LinkedDevicesScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StickerStoreScreen
import com.example.ui.theme.SafirTheme

sealed interface Screen {
    data object ChatsList : Screen
    data class ChatDetail(val conversationId: String) : Screen
    data object CreateGroup : Screen
    data object ContactPicker : Screen
    data object StickerStore : Screen
    data object Profile : Screen
    data class Settings(val initialSubPage: String = "MAIN") : Screen
    data object Communities : Screen
    data object Channels : Screen
    data object LinkedDevices : Screen
    data object Calls : Screen
}

@Composable
fun MainApp(
    repository: SafirRepository,
    initialInviteLink: String? = null
) {
    val currentUser by repository.currentUser.collectAsState()
    val isDark by repository.isDarkTheme.collectAsState()
    val settings by repository.settings.collectAsState()
    val liveAlert by repository.liveAlert.collectAsState()
    val allConversations by repository.conversations.collectAsState(initial = emptyList())

    var currentScreen by remember { mutableStateOf<Screen>(Screen.ChatsList) }
    var deepLinkInviteUrl by remember(initialInviteLink) { mutableStateOf(initialInviteLink) }

    androidx.compose.runtime.LaunchedEffect(liveAlert?.id) {
        if (liveAlert != null) {
            kotlinx.coroutines.delay(5500)
            repository.dismissLiveAlert()
        }
    }

    SafirTheme(
        darkTheme = isDark,
        colorPresetId = settings.uiColorPresetId
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (currentUser == null) {
                AuthScreen(
                    repository = repository,
                    onLoginSuccess = {
                        currentScreen = Screen.ChatsList
                    }
                )
            } else {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        (slideInHorizontally { width -> width / 3 } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> -width / 3 } + fadeOut())
                    },
                    label = "screen_transition"
                ) { target ->
                    when (target) {
                        is Screen.ChatsList -> {
                            ChatsListScreen(
                                repository = repository,
                                onOpenChat = { id ->
                                    currentScreen = Screen.ChatDetail(id)
                                },
                                onCreateGroup = {
                                    currentScreen = Screen.CreateGroup
                                },
                                onOpenContactPicker = {
                                    currentScreen = Screen.ContactPicker
                                },
                                onOpenStickerStore = {
                                    currentScreen = Screen.StickerStore
                                },
                                onOpenProfile = {
                                    currentScreen = Screen.Profile
                                },
                                onOpenSettings = {
                                    currentScreen = Screen.Settings("MAIN")
                                },
                                onOpenStarredMessages = {
                                    currentScreen = Screen.Settings("STARRED")
                                },
                                onOpenCommunities = {
                                    currentScreen = Screen.Communities
                                },
                                onOpenChannels = {
                                    currentScreen = Screen.Channels
                                },
                                onOpenLinkedDevices = {
                                    currentScreen = Screen.LinkedDevices
                                },
                                onOpenCalls = {
                                    currentScreen = Screen.Calls
                                },
                                onOpenStatusViewer = { _ -> },
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                            )
                        }

                        is Screen.ContactPicker -> {
                            BackHandler {
                                currentScreen = Screen.ChatsList
                            }
                            ContactPickerScreen(
                                repository = repository,
                                onOpenChat = { id ->
                                    currentScreen = Screen.ChatDetail(id)
                                },
                                onCreateGroup = {
                                    currentScreen = Screen.CreateGroup
                                },
                                onBack = {
                                    currentScreen = Screen.ChatsList
                                }
                            )
                        }

                        is Screen.ChatDetail -> {
                            BackHandler {
                                currentScreen = Screen.ChatsList
                            }
                            ChatDetailScreen(
                                conversationId = target.conversationId,
                                repository = repository,
                                onBack = { currentScreen = Screen.ChatsList },
                                onOpenStickerStore = { currentScreen = Screen.StickerStore },
                                onOpenChat = { newConvId -> currentScreen = Screen.ChatDetail(newConvId) }
                            )
                        }

                        is Screen.CreateGroup -> {
                            BackHandler {
                                currentScreen = Screen.ChatsList
                            }
                            CreateGroupScreen(
                                repository = repository,
                                onGroupCreated = { newGroupId ->
                                    currentScreen = Screen.ChatDetail(newGroupId)
                                },
                                onBack = { currentScreen = Screen.ChatsList }
                            )
                        }

                        is Screen.StickerStore -> {
                            BackHandler {
                                currentScreen = Screen.ChatsList
                            }
                            StickerStoreScreen(
                                repository = repository,
                                onBack = { currentScreen = Screen.ChatsList }
                            )
                        }

                        is Screen.Profile -> {
                            BackHandler {
                                currentScreen = Screen.Settings("MAIN")
                            }
                            ProfileScreen(
                                repository = repository,
                                onBack = { currentScreen = Screen.Settings("MAIN") }
                            )
                        }

                        is Screen.Settings -> {
                            BackHandler {
                                currentScreen = Screen.ChatsList
                            }
                            SettingsScreen(
                                repository = repository,
                                onOpenProfile = { currentScreen = Screen.Profile },
                                onOpenLinkedDevices = { currentScreen = Screen.LinkedDevices },
                                onOpenStickerStore = { currentScreen = Screen.StickerStore },
                                onBack = { currentScreen = Screen.ChatsList },
                                initialSubPage = target.initialSubPage
                            )
                        }

                        is Screen.Communities -> {
                            BackHandler {
                                currentScreen = Screen.ChatsList
                            }
                            Box(modifier = Modifier.statusBarsPadding().navigationBarsPadding()) {
                                CommunitiesScreen(
                                    repository = repository,
                                    onOpenChat = { id -> currentScreen = Screen.ChatDetail(id) },
                                    onBack = { currentScreen = Screen.ChatsList }
                                )
                            }
                        }

                        is Screen.Channels -> {
                            BackHandler {
                                currentScreen = Screen.ChatsList
                            }
                            Box(modifier = Modifier.statusBarsPadding().navigationBarsPadding()) {
                                ChannelsScreen(
                                    repository = repository,
                                    onBack = { currentScreen = Screen.ChatsList }
                                )
                            }
                        }

                        is Screen.LinkedDevices -> {
                            BackHandler {
                                currentScreen = Screen.ChatsList
                            }
                            Box(modifier = Modifier.statusBarsPadding().navigationBarsPadding()) {
                                LinkedDevicesScreen(
                                    repository = repository,
                                    onBack = { currentScreen = Screen.ChatsList }
                                )
                            }
                        }

                        is Screen.Calls -> {
                            BackHandler {
                                currentScreen = Screen.ChatsList
                            }
                            Box(modifier = Modifier.statusBarsPadding().navigationBarsPadding()) {
                                CallsScreen(
                                    repository = repository,
                                    onBack = { currentScreen = Screen.ChatsList }
                                )
                            }
                        }
                    }
                }

                // Deep-link Group Invite Preview Sheet when launched via https://kalbarq.app/join/* or https://chat.whatsapp.com/*
                if (!deepLinkInviteUrl.isNullOrBlank()) {
                    val rawLink = deepLinkInviteUrl!!
                    val token = repository.extractInviteToken(rawLink)
                    val matchedConv = allConversations.find {
                        it.inviteToken.equals(token, ignoreCase = true)
                    }
                    com.example.ui.components.GroupJoinPreviewSheet(
                        conversation = matchedConv,
                        isRevoked = matchedConv?.isInviteLinkRevoked == true,
                        currentUserName = currentUser?.displayName ?: "أنا",
                        repository = repository,
                        onDismiss = { deepLinkInviteUrl = null },
                        onNavigateToChat = { convId ->
                            deepLinkInviteUrl = null
                            currentScreen = Screen.ChatDetail(convId)
                        },
                        inviteLinkOrToken = rawLink
                    )
                }
            }

            // Global Floating Notification Banner for Status Viewers ("شاهد فلان حالتك") & Online Contact Toasts
            androidx.compose.animation.AnimatedVisibility(
                visible = liveAlert != null,
                modifier = Modifier
                    .align( androidx.compose.ui.Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                liveAlert?.let { alert ->
                    androidx.compose.material3.Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        color = androidx.compose.ui.graphics.Color(0xFF0E223D),
                        tonalElevation = 10.dp,
                        shadowElevation = 10.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            com.example.ui.theme.EmeraldPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { repository.dismissLiveAlert() }
                    ) {
                         androidx.compose.foundation.layout.Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                                androidx.compose.material3.Text(
                                    text = alert.title,
                                    color = com.example.ui.theme.EmeraldLight,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                androidx.compose.material3.Text(
                                    text = alert.subtitle,
                                    color = androidx.compose.ui.graphics.Color.White,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                            androidx.compose.material3.TextButton(onClick = { repository.dismissLiveAlert() }) {
                                androidx.compose.material3.Text(
                                    text = "حسناً",
                                    color = com.example.ui.theme.EmeraldPrimary,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
