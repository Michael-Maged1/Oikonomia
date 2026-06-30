package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.remote.FirebaseManager
import com.example.presentation.viewmodel.SopViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: SopViewModel,
    onBack: () -> Unit
) {
    val language by viewModel.language.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val nickname by viewModel.nickname.collectAsState()

    var isRegisteringMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var syncSuccessMessage by remember { mutableStateOf("") }
    var lastSyncTime by remember { mutableStateOf("Not synced yet") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Localization.translate("account", language), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Info Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Profile Avatar (Google Photo URL or circular initial)
                    if (currentUser != null && !currentUser!!.isAnonymous && currentUser!!.photoUrl != null) {
                        AsyncImage(
                            model = currentUser!!.photoUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(2.dp, PremiumAccent, CircleShape)
                        )
                    } else {
                        // Spring/Premium Circle Avatar
                        val initialChar = if (currentUser != null && !currentUser!!.isAnonymous) {
                            (currentUser!!.displayName ?: currentUser!!.email ?: "U").take(1).uppercase()
                        } else {
                            nickname.take(1).ifEmpty { "U" }.uppercase()
                        }
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(PremiumBlue.copy(alpha = 0.15f))
                                .border(2.dp, PremiumAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initialChar,
                                color = PremiumAccent,
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    if (currentUser != null && !currentUser!!.isAnonymous) {
                        // User Logged In Details
                        val dispName = currentUser!!.displayName ?: nickname.ifEmpty { if (language == "ar") "مستخدم أويكونوميا" else "أويكونوميا User" }
                        val userEmail = currentUser!!.email ?: "No Email Linked"

                        Text(
                            text = dispName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        // Connected badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SuccessGreen.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SuccessGreen))
                                Text(
                                    text = Localization.translate("cloud_synced", language),
                                    color = SuccessGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Last Sync Info
                        Text(
                            text = if (language == "ar") "آخر تزامن: $lastSyncTime" else "Last Sync: $lastSyncTime",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Manual Sync Button
                        Button(
                            onClick = {
                                viewModel.triggerSyncNow()
                                val formatter = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                                lastSyncTime = formatter.format(Date())
                                syncSuccessMessage = if (language == "ar") "اكتملت مزامنة السحابة الصامتة!" else "Silent cloud sync complete!"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync Icon")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Localization.translate("sync_now", language), fontWeight = FontWeight.Bold)
                        }

                        if (syncSuccessMessage.isNotEmpty()) {
                            Text(text = syncSuccessMessage, color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Logout Button
                            Button(
                                onClick = {
                                    viewModel.performLogout()
                                    syncSuccessMessage = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(44.dp).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            ) {
                                Text(Localization.translate("logout", language), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            }

                            // Delete Account Button
                            Button(
                                onClick = {
                                    showDeleteConfirm = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text(if (language == "ar") "حذف الحساب" else "Delete", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                    } else {
                        // Anonymous Local Mode
                        Text(
                            text = Localization.translate("anonymous_mode", language),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(WarningOrange.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(WarningOrange))
                                Text(
                                    text = Localization.translate("not_logged_in", language),
                                    color = WarningOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = if (language == "ar")
                                "يمكن لمستخدمي الوضع المجهول حفظ وتعديل كافة المهام محلياً بدون فقدان البيانات. سجل دخولك بالأسفل لمزامنتها سحابياً والوصول إليها من أي جهاز آخر."
                            else
                                "Anonymous users can use every single feature offline safely. Sign up below if you wish to back up and synchronize tasks across multiple devices.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            // Delete Confirmation Dialog
            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text(if (language == "ar") "حذف الحساب بشكل نهائي؟" else "Permanently Delete Account?", color = MaterialTheme.colorScheme.onSurface) },
                    text = { Text(if (language == "ar") "هل أنت متأكد من رغبتك في حذف حسابك؟ سيتم إزالة جميع البيانات السحابية المرتبطة بهذا الحساب بشكل نهائي ولا يمكن استرجاعها." else "Are you sure you want to permanently delete your account? This will remove your cloud storage data and cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteConfirm = false
                                isLoading = true
                                viewModel.deleteUserAccount(context) { success ->
                                    isLoading = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                        ) {
                            Text(if (language == "ar") "تأكيد الحذف" else "Confirm Delete", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text(Localization.translate("cancel", language), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }

            // Authentication Form (Only if logged out or anonymous)
            if (currentUser == null || currentUser!!.isAnonymous) {
                val context = androidx.compose.ui.platform.LocalContext.current
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (isRegisteringMode) Localization.translate("create_account", language)
                            else Localization.translate("login", language),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        // Google Sign-In Button
                        Button(
                            onClick = {
                                isLoading = true
                                viewModel.signInWithGoogle(context) { success ->
                                    isLoading = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Google Icon",
                                    tint = PremiumAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (language == "ar") "الدخول بواسطة Google" else "Sign in with Google",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // OR separator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)))
                            Text(
                                text = if (language == "ar") "أو" else "OR",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)))
                        }

                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text(Localization.translate("email", language), color = PremiumAccent) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = PremiumAccent,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(Localization.translate("password", language), color = PremiumAccent) },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = PremiumAccent,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (authError != null) {
                            Text(text = authError!!, color = DangerRed, fontSize = 12.sp)
                        }

                        // Submit Button
                        Button(
                            onClick = {
                                if (email.trim().isNotEmpty() && password.trim().isNotEmpty()) {
                                    isLoading = true
                                    if (isRegisteringMode) {
                                        viewModel.linkWithEmail(email, password) { success ->
                                            isLoading = false
                                        }
                                    } else {
                                        viewModel.loginEmail(email, password) { success ->
                                            isLoading = false
                                        }
                                    }
                                }
                            },
                            enabled = email.isNotEmpty() && password.isNotEmpty() && !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = if (isRegisteringMode) Localization.translate("register", language)
                                    else Localization.translate("login", language),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Toggle Sign-in / Sign-up state
                        TextButton(
                            onClick = { isRegisteringMode = !isRegisteringMode },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isRegisteringMode) "Already have an account? Log In"
                                else "Don't have an account? Sign Up",
                                color = PremiumAccent,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
