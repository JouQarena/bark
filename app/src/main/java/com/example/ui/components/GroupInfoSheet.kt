package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Conversation
import com.example.data.repository.SafirRepository
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SecurityShieldGold
import com.example.util.ImageCutoutProcessor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoSheet(
    conversation: Conversation,
    repository: SafirRepository,
    onDismiss: () -> Unit,
    onOpenInviteLinkSheet: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val contacts by repository.contacts.collectAsState()

    var isEditingTitle by remember { mutableStateOf(false) }
    var tempTitle by remember { mutableStateOf("") }

    var isEditingBio by remember { mutableStateOf(false) }
    var currentBio by remember(conversation) {
        mutableStateOf(conversation.groupDescription.ifBlank { "أضف وصفاً للمجموعة..." })
    }
    var currentGroupUri by remember(conversation) { mutableStateOf(conversation.groupIconUri) }
    var tempBio by remember { mutableStateOf("") }
    var showPhotoPickerSheet by remember { mutableStateOf(false) }

    // Group Admin & Member Management states
    var showAddParticipantDialog by remember { mutableStateOf(false) }
    var newParticipantInput by remember { mutableStateOf("") }
    var selectedMemberForAdminActions by remember { mutableStateOf<String?>(null) }

    val directGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageCutoutProcessor.copyUriToInternalStorage(context, uri, "grp_photo")
            val finalUri = savedPath ?: uri.toString()
            currentGroupUri = finalUri
            repository.updateGroupPhoto(conversation.id, finalUri)
            Toast.makeText(context, "تم تحديث صورة المجموعة من المعرض", Toast.LENGTH_SHORT).show()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurfaceElevated,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .testTag("group_info_sheet"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "معلومات وإدارة المجموعة",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Group Avatar with Camera badge to change group photo
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.clickable { showPhotoPickerSheet = true }
            ) {
                AvatarView(
                    preset = conversation.groupIconPreset,
                    name = conversation.title,
                    size = 92.dp,
                    isGroup = true,
                    imageUri = currentGroupUri
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary)
                        .border(2.dp, DarkSurfaceElevated, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "تغيير صورة المجموعة",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    directGalleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldLight)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تغيير صورة المجموعة من المعرض", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Group Title + Edit Subject Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = conversation.title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = {
                        tempTitle = conversation.title
                        isEditingTitle = true
                    },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("edit_group_title_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل اسم المجموعة",
                        tint = EmeraldLight,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = "مجموعة • ${conversation.participantNames.size} من المشاركين • ${conversation.adminNames.size} مشرفين",
                fontSize = 12.5.sp,
                color = DarkTextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Group Description / Bio Card ("وصف المجموعة")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "وصف المجموعة (Group Description)",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight
                            )
                        }

                        IconButton(
                            onClick = {
                                tempBio = currentBio
                                isEditingBio = true
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("edit_group_bio_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل وصف المجموعة", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = currentBio,
                        fontSize = 13.5.sp,
                        color = Color.White,
                        lineHeight = 19.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // WhatsApp Group Permissions & Admin Controls Card ("إعدادات وصلاحيات المجموعة")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.28f))
                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
                    .testTag("group_permissions_card")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = SecurityShieldGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إعدادات وصلاحيات المجموعة (إدارة المشرفين)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecurityShieldGold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                GroupPermissionSwitchRow(
                    title = "تعديل معلومات المجموعة (المشرفون فقط)",
                    subtitle = "تحديد من يمكنه تغيير اسم المجموعة وصورتها ووصفها",
                    checked = conversation.onlyAdminsCanEditInfo,
                    tag = "perm_edit_info_admins_only",
                    onCheckedChange = { enabled ->
                        repository.updateGroupPermissions(
                            conversationId = conversation.id,
                            onlyAdminsCanEditInfo = enabled
                        )
                    }
                )

                HorizontalDivider(color = DarkBorder.copy(alpha = 0.4f))

                GroupPermissionSwitchRow(
                    title = "إرسال الرسائل للمشرفين فقط (وضع الإعلانات)",
                    subtitle = if (conversation.isAnnouncementOnly) "مفعّل: المشرفون فقط يمكنهم إرسال الرسائل" else "مفتوح: يمكن لجميع المشاركين إرسال الرسائل",
                    checked = conversation.isAnnouncementOnly,
                    tag = "perm_announcement_only",
                    onCheckedChange = { enabled ->
                        repository.updateGroupPermissions(
                            conversationId = conversation.id,
                            isAnnouncementOnly = enabled
                        )
                    }
                )

                HorizontalDivider(color = DarkBorder.copy(alpha = 0.4f))

                GroupPermissionSwitchRow(
                    title = "إضافة أعضاء آخرين (المشرفون فقط)",
                    subtitle = "قصر إضافة أعضاء جدد للمجموعة على المشرفين",
                    checked = conversation.onlyAdminsCanAddMembers,
                    tag = "perm_add_members_admins_only",
                    onCheckedChange = { enabled ->
                        repository.updateGroupPermissions(
                            conversationId = conversation.id,
                            onlyAdminsCanAddMembers = enabled
                        )
                    }
                )

                HorizontalDivider(color = DarkBorder.copy(alpha = 0.4f))

                GroupPermissionSwitchRow(
                    title = "الموافقة على الأعضاء الجدد (Approve new participants)",
                    subtitle = "يجب أن يوافق المشرف على أي شخص يطلب الانضمام عبر رابط الدعوة",
                    checked = conversation.requireAdminApproval,
                    tag = "perm_require_admin_approval",
                    onCheckedChange = { enabled ->
                        repository.updateGroupPermissions(
                            conversationId = conversation.id,
                            requireAdminApproval = enabled
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Invite via Link Shortcut Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .clickable {
                        onDismiss()
                        onOpenInviteLinkSheet()
                    }
                    .padding(12.dp)
                    .testTag("group_info_invite_link_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الدعوة عبر رابط (Invite via link)",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "https://kalbarq.app/join/${conversation.inviteToken}",
                        fontSize = 11.5.sp,
                        color = EmeraldLight
                    )
                }

                Icon(Icons.Default.QrCode2, contentDescription = null, tint = SecurityShieldGold)
            }

            // Pending Join Requests inside Group Info if any
            if (conversation.pendingRequests.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SecurityShieldGold.copy(alpha = 0.12f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "طلبات الانضمام المعلقة (${conversation.pendingRequests.size})",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecurityShieldGold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    conversation.pendingRequests.forEach { req ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarView(preset = req.userAvatarPreset, name = req.userName, size = 34.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = req.userName,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    repository.approveJoinRequest(conversation.id, req.userName)
                                    Toast.makeText(context, "تم قبول انضمام ${req.userName}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("approve_join_${req.userName}")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "قبول", tint = EmeraldLight)
                            }
                            IconButton(
                                onClick = {
                                    repository.rejectJoinRequest(conversation.id, req.userName)
                                    Toast.makeText(context, "تم رفض طلب ${req.userName}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("reject_join_${req.userName}")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "رفض", tint = Color(0xFFEF5350))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Participants Header + Add Participant Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${conversation.participantNames.size} من المشاركين",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )

                Button(
                    onClick = { showAddParticipantDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("group_add_member_button")
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة أعضاء", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Participants List with Admin Badges & Click-to-Manage
            conversation.participantNames.forEachIndexed { index, name ->
                val isAdmin = conversation.adminNames.contains(name) || name == "أنا"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            if (name != "أنا") {
                                selectedMemberForAdminActions = name
                            }
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                        .testTag("group_member_row_$name"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarView(preset = (index % 6) + 1, name = name, size = 40.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        if (name != "أنا") {
                            Text(
                                text = "اضغط لإدارة العضو (ترقية لمشرف / إزالة)",
                                fontSize = 10.5.sp,
                                color = DarkTextSecondary
                            )
                        }
                    }
                    if (isAdmin) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.22f))
                                .border(0.8.dp, EmeraldLight.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "مشرف المجموعة",
                                fontSize = 10.5.sp,
                                color = EmeraldLight,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (name != "أنا") {
                        IconButton(
                            onClick = { selectedMemberForAdminActions = name },
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("member_admin_menu_$name")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "إدارة العضو",
                                tint = DarkTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(color = DarkBorder.copy(alpha = 0.45f))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Member Admin Actions Dialog (Promote/Dismiss Admin, Remove Member)
    if (selectedMemberForAdminActions != null) {
        val targetMember = selectedMemberForAdminActions!!
        val isMemberAdmin = conversation.adminNames.contains(targetMember)

        AlertDialog(
            onDismissRequest = { selectedMemberForAdminActions = null },
            title = {
                Text("إدارة العضو: $targetMember", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Promote / Dismiss Admin button
                    Button(
                        onClick = {
                            repository.toggleGroupAdmin(conversation.id, targetMember)
                            Toast.makeText(
                                context,
                                if (isMemberAdmin) "تم إعفاء $targetMember من الإشراف"
                                else "تم تعيين $targetMember مشرفاً للمجموعة",
                                Toast.LENGTH_SHORT
                            ).show()
                            selectedMemberForAdminActions = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("action_toggle_admin_button")
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isMemberAdmin) "إعفاء من إشراف المجموعة (Dismiss as admin)"
                            else "تعيين كمشرف للمجموعة (Make group admin)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Remove from group button
                    OutlinedButton(
                        onClick = {
                            repository.removeGroupParticipant(conversation.id, targetMember)
                            Toast.makeText(
                                context,
                                "تمت إزالة $targetMember من المجموعة",
                                Toast.LENGTH_SHORT
                            ).show()
                            selectedMemberForAdminActions = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("action_remove_member_button")
                    ) {
                        Icon(Icons.Default.PersonRemove, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إزالة $targetMember من المجموعة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMemberForAdminActions = null }) {
                    Text("إغلاق", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // Add New Participant Dialog (from Contacts or by Name/Number)
    if (showAddParticipantDialog) {
        val availableContacts = contacts.filter { c -> !conversation.participantNames.contains(c.name) }
        AlertDialog(
            onDismissRequest = { showAddParticipantDialog = false },
            title = {
                Text("إضافة أعضاء إلى ${conversation.title}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newParticipantInput,
                        onValueChange = { newParticipantInput = it },
                        label = { Text("اسم العضو أو رقم الهاتف", color = DarkTextSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_member_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = EmeraldPrimary
                        ),
                        singleLine = true
                    )

                    if (availableContacts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "اختر من جهات الاتصال:",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        availableContacts.take(4).forEach { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        repository.addGroupParticipant(conversation.id, contact.name)
                                        Toast.makeText(context, "تمت إضافة ${contact.name} إلى المجموعة", Toast.LENGTH_SHORT).show()
                                        showAddParticipantDialog = false
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AvatarView(preset = contact.avatarPreset, name = contact.name, size = 32.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(contact.name, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                    Text(contact.phoneNumber, color = DarkTextSecondary, fontSize = 11.sp)
                                }
                                Text("إضافة +", color = EmeraldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newParticipantInput.isNotBlank()) {
                            repository.addGroupParticipant(conversation.id, newParticipantInput.trim())
                            Toast.makeText(context, "تمت إضافة ${newParticipantInput.trim()} إلى المجموعة", Toast.LENGTH_SHORT).show()
                            newParticipantInput = ""
                            showAddParticipantDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.testTag("confirm_add_member_button")
                ) {
                    Text("إضافة للمجموعة", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddParticipantDialog = false }) {
                    Text("إلغاء", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    if (showPhotoPickerSheet) {
        RealPhotoPickerSheetDialog(
            title = "صورة المجموعة",
            currentImageUri = currentGroupUri,
            onDismiss = { showPhotoPickerSheet = false },
            onPhotoSelected = { selectedUri ->
                currentGroupUri = selectedUri
                repository.updateGroupPhoto(conversation.id, selectedUri)
            },
            onRemovePhoto = {
                currentGroupUri = ""
                repository.updateGroupPhoto(conversation.id, "")
            }
        )
    }

    if (isEditingTitle) {
        AlertDialog(
            onDismissRequest = { isEditingTitle = false },
            title = { Text("تعديل اسم المجموعة", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    label = { Text("اسم المجموعة الجديد", color = DarkTextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_group_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = EmeraldPrimary
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempTitle.isNotBlank()) {
                            repository.updateGroupTitle(conversation.id, tempTitle)
                        }
                        isEditingTitle = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.testTag("save_group_title_button")
                ) {
                    Text("حفظ", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditingTitle = false }) {
                    Text("إلغاء", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    if (isEditingBio) {
        AlertDialog(
            onDismissRequest = { isEditingBio = false },
            title = { Text("وصف المجموعة", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempBio,
                    onValueChange = { tempBio = it },
                    label = { Text("أدخل وصف المجموعة", color = DarkTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = EmeraldPrimary
                    ),
                    maxLines = 4
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempBio.isNotBlank()) {
                            currentBio = tempBio
                            repository.updateGroupBio(conversation.id, tempBio)
                        }
                        isEditingBio = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("موافق", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditingBio = false }) {
                    Text("إلغاء", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}

@Composable
private fun GroupPermissionSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = DarkTextSecondary
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
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
