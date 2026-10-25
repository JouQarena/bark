package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.SafirRepository
import com.example.ui.components.AvatarView
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SecurityShieldGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.example.ui.components.RealPhotoPickerSheetDialog

@Composable
fun AuthScreen(
    repository: SafirRepository,
    onLoginSuccess: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1: Phone, 2: OTP, 3: Profile
    var selectedCountryCode by remember { mutableStateOf("+20") }
    var showCountryMenu by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("01012345678") }
    var otpCode by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("مستخدم كلبرق") }
    var bio by remember { mutableStateOf("أهلاً بك! أنا أستخدم كلبرق") }
    var avatarPreset by remember { mutableIntStateOf(1) }
    var avatarUri by remember { mutableStateOf("") }
    var showPhotoPickerSheet by remember { mutableStateOf(false) }
    var isGeneratingKeys by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val simulatedOtp by repository.simulatedSmsOtp.collectAsState()
    val scope = rememberCoroutineScope()

    val countries = listOf(
        "+20" to "مصر (Egypt)",
        "+966" to "السعودية (KSA)",
        "+971" to "الإمارات (UAE)",
        "+965" to "الكويت (Kuwait)",
        "+974" to "قطر (Qatar)",
        "+962" to "الأردن (Jordan)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Simulated SMS push notification banner when code arrives
        AnimatedVisibility(
            visible = simulatedOtp != null,
            enter = slideInVertically() + fadeIn()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E2A30))
                    .border(1.dp, EmeraldPrimary, RoundedCornerShape(12.dp))
                    .clickable {
                        otpCode = simulatedOtp ?: ""
                    }
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = "SMS",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "رسالة نصية قصيرة (SMS)",
                            color = EmeraldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "رمز التحقق لتطبيق كلبرق هو: ${simulatedOtp} (اضغط للتعبئة التلقائية)",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // App Logo (Kalbarq Lightning Bubble Logo)
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(EmeraldPrimary.copy(alpha = 0.15f))
                .border(2.dp, EmeraldPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_kalbarq_logo),
                contentDescription = "شعار كلبرق",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "مرحباً بك في كلبرق",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DarkTextPrimary
        )

        Text(
            text = "مراسلة فورية مشفرة بالكامل بين الطرفين وسهلة الاستخدام",
            fontSize = 13.sp,
            color = DarkTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Step 1: Phone Entry
        if (step == 1) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "أدخل رقم هاتفك لتسجيل الدخول",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkTextPrimary
                )
                Spacer(modifier = Modifier.height(18.dp))

                // Country Selector & Phone Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Row(
                            modifier = Modifier
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceElevated)
                                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                .clickable { showCountryMenu = true }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = selectedCountryCode, color = DarkTextPrimary, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "اختر الدولة", tint = DarkTextSecondary)
                        }

                        DropdownMenu(
                            expanded = showCountryMenu,
                            onDismissRequest = { showCountryMenu = false },
                            modifier = Modifier.background(DarkSurfaceElevated)
                        ) {
                            countries.forEach { (code, name) ->
                                DropdownMenuItem(
                                    text = { Text("$code - $name", color = DarkTextPrimary) },
                                    onClick = {
                                        selectedCountryCode = code
                                        showCountryMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("رقم الهاتف", color = DarkTextSecondary) },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldPrimary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (phoneNumber.isNotBlank()) {
                            repository.requestOtp("$selectedCountryCode$phoneNumber")
                            step = 2
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "إرسال رمز التحقق (OTP)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Step 2: OTP Verification
        if (step == 2) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "تم إرسال رمز التحقق الفوري إلى:",
                    fontSize = 14.sp,
                    color = DarkTextSecondary
                )
                Text(
                    text = "$selectedCountryCode $phoneNumber",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldLight
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Instant OTP Code Display & 1-Tap Auto-Fill Card
                if (!simulatedOtp.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldPrimary.copy(alpha = 0.14f))
                            .border(1.dp, EmeraldPrimary, RoundedCornerShape(12.dp))
                            .clickable {
                                otpCode = simulatedOtp ?: "123456"
                                errorMessage = null
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "كود التحقق الجاهز عبر SMS:",
                                fontSize = 11.5.sp,
                                color = DarkTextSecondary
                            )
                            Text(
                                text = simulatedOtp ?: "123456",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SecurityShieldGold,
                                letterSpacing = 3.sp
                            )
                        }
                        Button(
                            onClick = {
                                otpCode = simulatedOtp ?: "123456"
                                errorMessage = null
                                step = 3
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("تعبئة وتأكيد فوري ⚡", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = otpCode,
                    onValueChange = {
                        if (it.length <= 6) {
                            otpCode = it
                            errorMessage = null
                        }
                    },
                    placeholder = { Text("أدخل 6 أرقام", color = DarkTextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        textAlign = TextAlign.Center,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 6.sp
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFF15C6D),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val effectiveCode = otpCode.ifBlank { simulatedOtp ?: "" }
                        if (effectiveCode.length >= 4) {
                            otpCode = effectiveCode
                            step = 3
                        } else {
                            errorMessage = "يرجى إدخال رمز التحقق كاملاً"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "تحقق من الرمز",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                TextButton(
                    onClick = {
                        val newCode = repository.requestOtp("$selectedCountryCode$phoneNumber")
                        otpCode = newCode
                    }
                ) {
                    Text("إعادة إرسال رمز التحقق وتعبئته تلقائياً", color = EmeraldPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Step 3: Profile Setup & Key Generation
        if (step == 3) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "إعداد ملفك الشخصي المشفر",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTextPrimary
                )
                Text(
                    text = "اختر صورة شخصية واسماً ليظهر لأصدقائك وفي المجموعات",
                    fontSize = 12.sp,
                    color = DarkTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Real Gallery Photo Picker for Profile
                AvatarView(
                    preset = avatarPreset,
                    name = displayName,
                    size = 92.dp,
                    isOnline = true,
                    imageUri = avatarUri,
                    onClick = { showPhotoPickerSheet = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = { showPhotoPickerSheet = true }) {
                    Text(
                        text = if (avatarUri.isNotBlank()) "تغيير الصورة الشخصية من المعرض" else "إضافة صورة شخصية من المعرض",
                        color = EmeraldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("الاسم المستعار / الشخصي", color = DarkTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("الحالة (الأخبار / نبذة)", color = DarkTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Key Generation simulation
                if (isGeneratingKeys) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceElevated)
                            .padding(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = EmeraldPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "جاري توليد مفاتيح التشفير End-to-End وحفظ البيانات...",
                            color = DarkTextPrimary,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            isGeneratingKeys = true
                            scope.launch {
                                delay(900)
                                val success = repository.verifyOtpAndLogin(
                                    phoneNumber = "$selectedCountryCode$phoneNumber",
                                    inputCode = otpCode.ifBlank { "123456" },
                                    displayName = displayName,
                                    avatarPreset = avatarPreset,
                                    bio = bio,
                                    avatarUri = avatarUri
                                )
                                isGeneratingKeys = false
                                if (success) {
                                    onLoginSuccess()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "التالي",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                if (showPhotoPickerSheet) {
                    RealPhotoPickerSheetDialog(
                        title = "الصورة الشخصية",
                        currentImageUri = avatarUri,
                        onDismiss = { showPhotoPickerSheet = false },
                        onPhotoSelected = { uri -> avatarUri = uri },
                        onRemovePhoto = { avatarUri = "" }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Privacy Guarantee Card
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF142129))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "قفل التشفير",
                tint = SecurityShieldGold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "جميع الرسائل والملفات ومقاطع الفيديو مشفرة تماماً من طرف إلى طرف (E2EE) وتضمن أقصى درجات الخصوصية.",
                fontSize = 11.sp,
                color = DarkTextSecondary,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
