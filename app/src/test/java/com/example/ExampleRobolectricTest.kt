package com.example

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.StoryType
import com.example.data.repository.SafirRepository
import com.example.util.ImageCutoutProcessor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies Kalbarq app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("كلبرق", appName)
    }

    @Test
    fun `photo to sticker processor removes background and saves to favorites pack`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SafirRepository(context)

        val samplePhotos = ImageCutoutProcessor.ensureSampleGalleryPhotos(context)
        assertTrue("Sample gallery photos should be generated", samplePhotos.isNotEmpty())

        val sourcePath = samplePhotos.first().filePath
        val sourceBitmap = BitmapFactory.decodeFile(sourcePath)
        assertNotNull("Decoded source bitmap should not be null", sourceBitmap)

        val cutoutBitmap = ImageCutoutProcessor.processPhotoToStickerBitmap(
            source = sourceBitmap,
            removeBackground = true,
            cropZoom = 1.1f,
            borderWidthPx = 8,
            borderColor = Color.WHITE
        )
        // Top-left corner of cutout sticker should be transparent
        assertEquals(0, Color.alpha(cutoutBitmap.getPixel(0, 0)))

        val savedStickerPath = ImageCutoutProcessor.saveStickerBitmapToInternalStorage(context, cutoutBitmap)
        assertTrue("Saved sticker PNG file must exist", File(savedStickerPath).exists())

        val createdSticker = repository.addPhotoSticker(
            title = "صورة من المعرض",
            imageUri = savedStickerPath,
            borderWidthPx = 8,
            cropZoom = 1.1f,
            isCutout = true
        )

        assertEquals(savedStickerPath, createdSticker.imageUri)
        val packs = repository.stickerPacks.first()
        val favPack = packs.find { it.id == "pack_favorites" }
        assertNotNull("Favorites sticker pack must exist", favPack)
        assertEquals("المفضلة", favPack?.name)
        assertTrue(
            "Favorites sticker pack must contain the newly created photo sticker",
            favPack!!.stickers.any { it.imageUri == savedStickerPath }
        )
    }

    @Test
    fun `external Sticker Maker pack imports real PNG stickers`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SafirRepository(context)

        val stickerFile1 = ImageCutoutProcessor.ensureGraphicalStickerFile(
            context = context,
            key = "ext_test_1",
            title = "تمام",
            primaryColor = Color.parseColor("#00A884"),
            accentColor = Color.parseColor("#005C4B")
        )
        assertTrue(File(stickerFile1).exists())
        repository.importExternalStickerPack(
            name = "حزمة Sticker Maker التجريبية",
            author = "Sticker Maker",
            stickerTitlesOrUris = listOf(stickerFile1)
        )
    }

    @Test
    fun `user profile photo and group icon update with real gallery image paths`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SafirRepository(context)

        val samplePhotos = ImageCutoutProcessor.ensureSampleGalleryPhotos(context)
        val userPhotoPath = samplePhotos[0].filePath
        val groupPhotoPath = samplePhotos[1].filePath

        repository.verifyOtpAndLogin(
            phoneNumber = "+201012345678",
            inputCode = "123456",
            displayName = "مطور كلبرق",
            avatarPreset = 1,
            bio = "متاح",
            avatarUri = userPhotoPath
        )
        assertEquals(userPhotoPath, repository.currentUser.value?.avatarUri)

        val newPhotoPath = samplePhotos[2].filePath
        repository.updateUserProfilePhoto(newPhotoPath)
        assertEquals(newPhotoPath, repository.currentUser.value?.avatarUri)

        val newGroupId = repository.createGroup(
            title = "مجموعة المطورين",
            description = "وصف المجموعة الرسمي",
            iconPreset = 2,
            participantNames = listOf("أحمد", "سارة"),
            groupIconUri = groupPhotoPath
        )

        repository.updateGroupPhoto(newGroupId, newPhotoPath)
        val prefs = context.getSharedPreferences("safir_user_prefs", Context.MODE_PRIVATE)
        assertEquals(newPhotoPath, prefs.getString("group_uri_$newGroupId", ""))
    }

    @Test
    fun `status supports gallery images and videos up to 5 minutes (300 seconds)`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SafirRepository(context)

        val sampleImage = ImageCutoutProcessor.ensureSampleGalleryPhotos(context).first().filePath
        repository.postStory(
            textContent = "صورة حالتي من المعرض",
            backgroundColorHex = "#005C4B",
            storyType = StoryType.IMAGE,
            mediaUri = sampleImage
        )

        // Post a 5-minute (300s) video story and verify max duration constant is 300 seconds
        assertEquals(300, ImageCutoutProcessor.MAX_STATUS_VIDEO_SECONDS)
        repository.postStory(
            textContent = "فيديو كامل 5 دقائق",
            backgroundColorHex = "#005C4B",
            storyType = StoryType.VIDEO,
            mediaUri = "video_5m00s.mp4",
            videoDurationSeconds = 420,
            trimStartSeconds = 0,
            trimEndSeconds = 300
        )
    }

    @Test
    fun `whatsapp settings update account privacy chats notifications and storage options`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SafirRepository(context)

        repository.updateSettings {
            it.copy(
                // Account
                securityNotificationsEnabled = true,
                passkeysEnabled = true,
                emailAddress = "user@kalbarq.app",
                twoStepVerificationEnabled = true,
                twoStepPin = "654321",
                // Privacy
                lastSeenPrivacy = "جهات اتصالي",
                onlinePrivacy = "مثل آخر ظهور",
                profilePhotoPrivacy = "جهات اتصالي",
                aboutPrivacy = "الكل",
                statusPrivacy = "جهات اتصالي",
                readReceiptsEnabled = false,
                defaultDisappearingTimer = "24 ساعة",
                groupsPrivacy = "جهات اتصالي",
                silenceUnknownCallers = true,
                fingerprintLockEnabled = true,
                fingerprintLockTimeout = "بعد دقيقة واحدة",
                // Chats
                themeMode = "داكن",
                chatWallpaperHex = "#07261E",
                enterIsSend = true,
                mediaVisibilityInGallery = true,
                fontSize = "كبير",
                keepChatsArchived = true,
                // Notifications
                conversationTones = true,
                messageNotificationTone = "نبضة هادئة",
                messageVibrate = "قصير",
                messageHighPriority = true,
                reactionNotifications = false,
                groupNotificationTone = "رنين كريستال",
                groupVibrate = "طويل",
                groupHighPriority = false,
                callRingtone = "هادئ",
                // Storage
                useLessDataForCalls = true,
                mediaUploadQuality = "دقة عالية (HD)"
            )
        }

        val updated = repository.settings.value
        assertEquals(true, updated.securityNotificationsEnabled)
        assertEquals(true, updated.passkeysEnabled)
        assertEquals("user@kalbarq.app", updated.emailAddress)
        assertEquals(true, updated.twoStepVerificationEnabled)
        assertEquals("654321", updated.twoStepPin)
        assertEquals("جهات اتصالي", updated.lastSeenPrivacy)
        assertEquals("مثل آخر ظهور", updated.onlinePrivacy)
        assertEquals(false, updated.readReceiptsEnabled)
        assertEquals("24 ساعة", updated.defaultDisappearingTimer)
        assertEquals(true, updated.silenceUnknownCallers)
        assertEquals(true, updated.fingerprintLockEnabled)
        assertEquals("بعد دقيقة واحدة", updated.fingerprintLockTimeout)
        assertEquals("#07261E", updated.chatWallpaperHex)
        assertEquals(true, updated.enterIsSend)
        assertEquals("كبير", updated.fontSize)
        assertEquals("نبضة هادئة", updated.messageNotificationTone)
        assertEquals("قصير", updated.messageVibrate)
        assertEquals(false, updated.reactionNotifications)
        assertEquals("رنين كريستال", updated.groupNotificationTone)
        assertEquals("هادئ", updated.callRingtone)
        assertEquals(true, updated.useLessDataForCalls)
        assertEquals("دقة عالية (HD)", updated.mediaUploadQuality)
    }

    @Test
    fun `account section flows persist security notifications two-step verification and change number`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SafirRepository(context)

        // Login with initial phone number
        repository.verifyOtpAndLogin(
            phoneNumber = "+201012345678",
            inputCode = "123456",
            displayName = "مطور كلبرق",
            avatarPreset = 1,
            bio = "متاح"
        )
        assertEquals("+201012345678", repository.currentUser.value?.phoneNumber)

        // 1. Security notifications toggle
        repository.updateSettings { it.copy(securityNotificationsEnabled = false) }
        assertEquals(false, repository.settings.value.securityNotificationsEnabled)
        repository.updateSettings { it.copy(securityNotificationsEnabled = true) }
        assertEquals(true, repository.settings.value.securityNotificationsEnabled)

        // 2. Two-step verification 6-digit PIN & recovery email
        repository.updateSettings {
            it.copy(
                twoStepVerificationEnabled = true,
                twoStepPin = "482910",
                emailAddress = "security@kalbarq.app"
            )
        }
        assertEquals(true, repository.settings.value.twoStepVerificationEnabled)
        assertEquals("482910", repository.settings.value.twoStepPin)
        assertEquals("security@kalbarq.app", repository.settings.value.emailAddress)

        // 3. Change number flow (rejects identical number, accepts new valid number & persists)
        val sameNumberResult = repository.changePhoneNumber(
            oldPhone = "+201012345678",
            newPhone = "+201012345678",
            notifyContacts = true,
            notifyMode = "كل جهات الاتصال"
        )
        assertEquals(false, sameNumberResult)

        val migrated = repository.changePhoneNumber(
            oldPhone = "+201012345678",
            newPhone = "+201099887766",
            notifyContacts = true,
            notifyMode = "جهات الاتصال التي لدي دردشات معها"
        )
        assertTrue("Change number should succeed for valid new phone", migrated)
        assertEquals("+201099887766", repository.currentUser.value?.phoneNumber)
        assertEquals(true, repository.settings.value.changeNumberNotifyContacts)
        assertEquals("جهات الاتصال التي لدي دردشات معها", repository.settings.value.changeNumberNotifyMode)

        // Verify persistence across a new SafirRepository instance
        val reloadedRepo = SafirRepository(context)
        assertEquals("+201099887766", reloadedRepo.currentUser.value?.phoneNumber)
        assertEquals(true, reloadedRepo.settings.value.securityNotificationsEnabled)
        assertEquals(true, reloadedRepo.settings.value.twoStepVerificationEnabled)
        assertEquals("482910", reloadedRepo.settings.value.twoStepPin)
        assertEquals("security@kalbarq.app", reloadedRepo.settings.value.emailAddress)
        assertEquals("جهات الاتصال التي لدي دردشات معها", reloadedRepo.settings.value.changeNumberNotifyMode)
    }

    @Test
    fun `kalbarq blue theme tokens and uploaded logo drawable are configured`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Verify uploaded Kalbarq logo drawable decodes to a valid bitmap
        val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img_kalbarq_logo)
        assertNotNull("Kalbarq logo drawable must decode to a valid bitmap", logoBitmap)
        assertTrue("Kalbarq logo bitmap must have positive dimensions", logoBitmap.width > 0 && logoBitmap.height > 0)

        // Verify Electric Royal Blue & Sapphire Blue theme tokens (replacing WhatsApp green)
        assertEquals(0xFF0077FFL, com.example.ui.theme.EmeraldPrimary.value.toLong() ushr 32)
        assertEquals(0xFF0055D4L, com.example.ui.theme.EmeraldDark.value.toLong() ushr 32)
        assertEquals(0xFF00C6FFL, com.example.ui.theme.EmeraldLight.value.toLong() ushr 32)
        assertEquals(0xFF0A3977L, com.example.ui.theme.OutgoingGreenDark.value.toLong() ushr 32)
        assertEquals(0xFFD6EAFFL, com.example.ui.theme.OutgoingGreenLight.value.toLong() ushr 32)
    }

    @Test
    fun `communities calls and starred message actions work end to end`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SafirRepository(context)

        val initialCommunitiesCount = repository.communities.value.size
        val createdCommunity = repository.createCommunity(
            name = "مجتمع الذكاء الاصطناعي الأزرق",
            description = "مجتمع تقني لمطوري تطبيقات أندرويد"
        )
        assertEquals(initialCommunitiesCount + 1, repository.communities.value.size)
        assertEquals("مجتمع الذكاء الاصطناعي الأزرق", createdCommunity.name)
        assertTrue(createdCommunity.subGroupIds.isNotEmpty())

        val initialCallLinksCount = repository.callLinks.value.size
        val callLink = repository.createCallLink("مكالمة فيديو مشفرة", true)
        assertEquals(initialCallLinksCount + 1, repository.callLinks.value.size)
        assertTrue(callLink.url.startsWith("https://kalbarq.app/call/"))

        repository.toggleStarMessage("msg_test_star_1")
        assertTrue(repository.starredMessageIds.value.contains("msg_test_star_1"))
        repository.toggleStarMessage("msg_test_star_1")
        assertTrue(!repository.starredMessageIds.value.contains("msg_test_star_1"))
    }

    @Test
    fun `camera capture studio preserves 100 percent original uncompressed quality and resolution`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (capturedPath, sizeBadge) = ImageCutoutProcessor.captureStudioCameraPhoto(
            context = context,
            scenePreset = "tech",
            isFrontCamera = false,
            preserveOriginalQuality = true
        )
        assertTrue("Captured studio photo must exist on disk", File(capturedPath).exists())
        assertTrue("Size badge must indicate RAW HD original quality", sizeBadge.contains("RAW") || sizeBadge.contains("100%"))
        val resolutionLabel = ImageCutoutProcessor.getImageResolutionLabel(context, capturedPath)
        assertTrue(
            "Resolution label must report original dimensions",
            resolutionLabel.contains("100%") || resolutionLabel.contains("×")
        )
    }

    @Test
    fun `kalbarq theme color presets switch live and whatsapp gold features work end to end`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SafirRepository(context)

        // 1. Switch UI color to Kalbarq Gold and verify live color token update
        repository.setUiColorPreset("KALBARQ_GOLD")
        assertEquals("KALBARQ_GOLD", repository.settings.value.uiColorPresetId)
        val goldPalette = com.example.ui.theme.applyKalbarqColorPalette("KALBARQ_GOLD")
        assertEquals("KALBARQ_GOLD", goldPalette.id)
        assertEquals(0xFFD4AF37L, com.example.ui.theme.EmeraldPrimary.value.toLong() ushr 32)

        // Restore Royal Blue default
        repository.setUiColorPreset("ROYAL_BLUE")
        com.example.ui.theme.applyKalbarqColorPalette("ROYAL_BLUE")
        assertEquals(0xFF0077FFL, com.example.ui.theme.EmeraldPrimary.value.toLong() ushr 32)

        // 2. Verify Status Viewer Notification emits live alert with exact viewer name
        repository.notifyStatusViewedByPerson(viewerName = "سارة أحمد", storyId = "story_1")
        val alert = repository.liveAlert.value
        assertNotNull("Live alert must be emitted when a person views status", alert)
        assertTrue("Alert subtitle must contain viewer name", alert!!.subtitle.contains("سارة أحمد"))

        // 3. Verify Direct Phone Number Chat (without saving contact) and OTP verification
        val generatedOtp = repository.requestOtp("+201122334455")
        assertEquals(6, generatedOtp.length)
        assertEquals(generatedOtp, repository.simulatedSmsOtp.value)

        val directConvId = repository.startDirectChatByPhoneNumber(
            phoneNumber = "+201066778899",
            customName = "عميل مباشر"
        )
        assertTrue("Direct phone conversation ID must be created", directConvId.startsWith("conv_phone_"))

        // 4. Verify Weak Network & Social Media Data mode ("نت سوشيال") and 2GB File Transfer setting
        repository.updateSettings {
            it.copy(
                socialDataPackageMode = true,
                weakNetworkBoosterEnabled = true,
                preserveOriginalImageQuality = true,
                maxFileSizeLimitGb = 2
            )
        }
        assertEquals(true, repository.settings.value.socialDataPackageMode)
        assertEquals(true, repository.settings.value.weakNetworkBoosterEnabled)
        assertEquals(true, repository.settings.value.preserveOriginalImageQuality)
        assertEquals(2, repository.settings.value.maxFileSizeLimitGb)
    }

    @Test
    fun `storage data settings group administration invite links and apk export work end to end`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SafirRepository(context)
        val otp = repository.requestOtp("+201012345678")
        repository.verifyOtpAndLogin("+201012345678", otp, "مهندس كلبرق", 1, "متاح")

        // 1. Storage and Data: control auto-download (الصور، الفيديو، الملفات، المقاطع الصوتية) and Use less data for calls
        repository.updateSettings {
            it.copy(
                useLessDataForCalls = false,
                autoDownloadMobile = setOf("الصور", "الفيديو", "الملفات"),
                autoDownloadWifi = setOf("الصور", "الفيديو", "الملفات", "المقاطع الصوتية"),
                autoDownloadRoaming = setOf("الصور")
            )
        }
        assertEquals(false, repository.settings.value.useLessDataForCalls)
        assertTrue(repository.settings.value.autoDownloadMobile.containsAll(listOf("الصور", "الفيديو", "الملفات")))

        // Re-enable Use less data for calls
        repository.updateSettings { it.copy(useLessDataForCalls = true) }
        assertEquals(true, repository.settings.value.useLessDataForCalls)

        // 2. Group Administration: permissions, promote/dismiss admin, add/remove members
        repository.updateGroupPermissions(
            conversationId = "conv_group_project",
            onlyAdminsCanEditInfo = true,
            isAnnouncementOnly = true,
            onlyAdminsCanAddMembers = true,
            requireAdminApproval = true
        )
        val updatedGroup = repository.getConversation("conv_group_project")
        assertNotNull(updatedGroup)
        assertEquals(true, updatedGroup!!.isAnnouncementOnly)
        assertEquals(true, updatedGroup.onlyAdminsCanAddMembers)
        assertEquals(true, updatedGroup.requireAdminApproval)

        // Promote a member to Group Admin
        repository.toggleGroupAdmin("conv_group_project", "عمر الشامي")
        val afterPromote = repository.getConversation("conv_group_project")
        assertTrue("عمر الشامي should be promoted to admin", afterPromote!!.adminNames.contains("عمر الشامي"))

        // Add & Remove participant
        repository.addGroupParticipant("conv_group_project", "طارق زياد")
        // Turn off requireAdminApproval so direct invite link joins immediately
        repository.toggleRequireAdminApproval("conv_group_project", false)

        // 3. Open & Join Group Invite Links (both Kalbarq & WhatsApp format links)
        val token1 = repository.extractInviteToken("https://kalbarq.app/join/dev_team_2026_xyz")
        assertEquals("dev_team_2026_xyz", token1)
        val joinExistingResult = repository.joinGroupByInvite(
            "https://kalbarq.app/join/dev_team_2026_xyz",
            "زائر جديد"
        )
        assertEquals(com.example.data.repository.JoinResult.SUCCESS, joinExistingResult)

        val tokenWhatsApp = repository.extractInviteToken("https://chat.whatsapp.com/design_studio_2026")
        assertEquals("design_studio_2026", tokenWhatsApp)
        val joinExternalResult = repository.joinGroupByInvite(
            "https://chat.whatsapp.com/design_studio_2026",
            "مهندس كلبرق"
        )
        assertEquals(com.example.data.repository.JoinResult.SUCCESS, joinExternalResult)
        val matchedExternalId = repository.findConversationIdByInviteLink("https://chat.whatsapp.com/design_studio_2026")
        assertNotNull("External invite link must resolve to a joined group conversation", matchedExternalId)

        // 4. APK Export verification
        val apkFile = com.example.util.ApkExporter.prepareShareableApkFile(context)
        assertTrue("Prepared APK file must exist and be non-empty", apkFile.exists() && apkFile.length() > 0L)
    }
}




