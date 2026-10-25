package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Conversation
import com.example.data.repository.JoinResult
import com.example.data.repository.SafirRepository
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SecurityShieldGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupJoinPreviewSheet(
    conversation: Conversation?,
    isRevoked: Boolean,
    currentUserName: String,
    repository: SafirRepository,
    onDismiss: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    inviteLinkOrToken: String = ""
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var joinStatus by remember { mutableStateOf<JoinResult?>(null) }

    val extractedToken = remember(inviteLinkOrToken, conversation) {
        if (inviteLinkOrToken.isNotBlank()) repository.extractInviteToken(inviteLinkOrToken)
        else conversation?.inviteToken ?: ""
    }

    val previewTitle = conversation?.title ?: when {
        extractedToken.contains("ai", ignoreCase = true) -> "مجتمع الذكاء الاصطناعي وتطوير الأندرويد"
        extractedToken.contains("design", ignoreCase = true) -> "نادي مصممي واجهات المستخدم UI/UX"
        extractedToken.contains("family", ignoreCase = true) -> "مجموعة العائلة"
        else -> "مجموعة كلبرق المشتركة (${extractedToken.take(8).ifBlank { "عامة" }})"
    }

    val previewDescription = conversation?.groupDescription?.ifBlank {
        "مجموعة رسمية مشفرة للتواصل الفوري ومشاركة الوسائط والملفات عالية الدقة."
    } ?: "دعوة للانضمام إلى مجموعة عبر رابط دعوة مباشر (${inviteLinkOrToken.ifBlank { "https://kalbarq.app/join/$extractedToken" }})"

    val participantsCount = conversation?.participantNames?.size ?: 18
    val isAlreadyMember = conversation?.participantNames?.contains(currentUserName) == true

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurfaceElevated,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .testTag("group_join_preview_sheet"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isRevoked) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF5350).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "ملغي",
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "رابط الدعوة غير صالح أو تم إلغاؤه",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "قام المشرف بإعادة تعيين هذا الرابط أو انتهت صلاحيته. يرجى طلب رابط دعوة جديد من مشرف المجموعة.",
                    fontSize = 13.sp,
                    color = DarkTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                )

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إغلاق", color = Color.White)
                }
            } else if (joinStatus == JoinResult.PENDING_APPROVAL) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SecurityShieldGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = "معلق",
                        tint = SecurityShieldGold,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "تم إرسال طلب الانضمام للمشرف",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "تتطلب هذه المجموعة موافقة المشرف. سيصلك إشعار فوري بمجرد قبول انضمامك إلى '$previewTitle'.",
                    fontSize = 13.sp,
                    color = DarkTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                )

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("حسناً، فهمت", color = Color.White)
                }
            } else {
                // Valid Group Preview (WhatsApp Invite Link Bottom Sheet)
                AvatarView(
                    preset = conversation?.groupIconPreset ?: 2,
                    name = previewTitle,
                    size = 76.dp,
                    isGroup = true,
                    imageUri = conversation?.groupIconUri ?: ""
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = previewTitle,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = previewDescription,
                    fontSize = 13.sp,
                    color = DarkTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Member count + mutual avatars
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = EmeraldLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$participantsCount مشاركين",
                        fontSize = 12.5.sp,
                        color = EmeraldLight,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                        (1..3).forEach { p ->
                            AvatarView(preset = p, size = 20.dp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.12f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "دعوة مجموعة موثقة ومشفرة • يمكنك تصفح الرسائل والوسائط فور الانضمام.",
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isAlreadyMember && conversation != null) {
                    Button(
                        onClick = {
                            onDismiss()
                            onNavigateToChat(conversation.id)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("join_group_confirm_button")
                    ) {
                        Text("أنت عضو بالفعل • عرض المجموعة", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Button(
                        onClick = {
                            val tokenToUse = extractedToken.ifBlank { "ai_devs_club_2026" }
                            val res = repository.joinGroupByInvite(tokenToUse, currentUserName)
                            if (res == JoinResult.SUCCESS) {
                                val targetId = repository.findConversationIdByInviteLink(tokenToUse)
                                    ?: conversation?.id
                                    ?: "grp_invite_${tokenToUse.lowercase().take(16)}"
                                onDismiss()
                                onNavigateToChat(targetId)
                            } else if (res == JoinResult.PENDING_APPROVAL) {
                                joinStatus = JoinResult.PENDING_APPROVAL
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("join_group_confirm_button")
                    ) {
                        Text(
                            text = if (conversation?.requireAdminApproval == true) "طلب الانضمام للمجموعة" else "الانضمام إلى المجموعة (Join Group)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

/**
 * Dialog allowing the user to paste or choose any WhatsApp / Kalbarq group invite URL
 * (e.g., https://chat.whatsapp.com/... or https://kalbarq.app/join/...) and open the group preview sheet.
 */
@Composable
fun OpenInviteLinkDialog(
    onDismiss: () -> Unit,
    onOpenInvitePreview: (String) -> Unit
) {
    var linkText by remember { mutableStateOf("https://kalbarq.app/join/ai_devs_club_2026") }

    val quickLinks = listOf(
        "مجتمع الذكاء الاصطناعي وتطوير الأندرويد" to "https://kalbarq.app/join/ai_devs_club_2026",
        "فريق البرمجة والمشاريع" to "https://kalbarq.app/join/dev_team_2026_xyz",
        "مجموعة العائلة" to "https://kalbarq.app/join/family_gathering_2026",
        "نادي مصممي واجهات المستخدم (رابط واتساب)" to "https://chat.whatsapp.com/design_studio_2026"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Link, contentDescription = null, tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("فتح رابط دعوة والانضمام لمجموعة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "الصق رابط دعوة كلبرق أو واتساب (https://kalbarq.app/join/... أو https://chat.whatsapp.com/...) لمعاينة المجموعة والانضمام إليها فوراً:",
                    fontSize = 12.5.sp,
                    color = DarkTextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = linkText,
                    onValueChange = { linkText = it },
                    label = { Text("رابط الدعوة للمجموعة", color = DarkTextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invite_link_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = EmeraldPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "روابط مجموعات جاهزة للتجربة والانضمام الفوري:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldLight
                )
                Spacer(modifier = Modifier.height(6.dp))

                quickLinks.forEach { (title, url) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.25f))
                            .clickable {
                                linkText = url
                                onOpenInvitePreview(url)
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = EmeraldLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            Text(url, color = DarkTextSecondary, fontSize = 10.5.sp)
                        }
                        Text("انضمام", color = EmeraldPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (linkText.isNotBlank()) {
                        onOpenInvitePreview(linkText.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("open_invite_link_confirm_button")
            ) {
                Text("فتح الرابط ومعاينة المجموعة", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = DarkTextSecondary)
            }
        },
        containerColor = DarkSurfaceElevated
    )
}
