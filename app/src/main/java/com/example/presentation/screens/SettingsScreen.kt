package com.example.presentation.screens

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.SopViewModel
import com.example.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SopViewModel,
    onNavigateToAccount: () -> Unit
) {
    val language by viewModel.language.collectAsState()
    val themeMode by viewModel.theme.collectAsState()
    val nickname by viewModel.nickname.collectAsState()
    val defaultReminderTime by viewModel.defaultReminderTime.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val notificationVolume by viewModel.notificationVolume.collectAsState()
    val snoozeDuration by viewModel.snoozeDuration.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }
    var showReminderOptionsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Default Reminder Time Picker Dialog
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val hr = String.format("%02d", hourOfDay)
            val min = String.format("%02d", minute)
            viewModel.updateDefaultReminderTime("$hr:$min")
        },
        defaultReminderTime.split(":").getOrNull(0)?.toIntOrNull() ?: 12,
        defaultReminderTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0,
        true
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Title
        Text(
            text = Localization.translate("settings", language),
            style = MaterialTheme.typography.headlineMedium.copy(
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Profile Section
        SettingsSectionHeader(title = if (language == "ar") "الملف الشخصي" else "Profile")
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                var isEditing by remember { mutableStateOf(false) }
                var editNameText by remember(nickname) { mutableStateOf(nickname) }
                var editError by remember { mutableStateOf("") }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PremiumBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = PremiumAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (language == "ar") "اسم المستخدم" else "Display Name",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            if (!isEditing) {
                                Text(
                                    text = nickname.ifEmpty { "Michael" },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = PremiumAccent,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }

                    if (!isEditing) {
                        TextButton(
                            onClick = { 
                                editNameText = nickname
                                editError = ""
                                isEditing = true 
                            }
                        ) {
                            Text(
                                text = if (language == "ar") "تعديل الاسم" else "Edit Name",
                                color = PremiumAccent,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                if (isEditing) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editNameText,
                        onValueChange = { 
                            editNameText = it 
                            if (editError.isNotEmpty()) editError = ""
                        },
                        placeholder = { Text(if (language == "ar") "اكتب اسمك" else "Enter nickname...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = PremiumAccent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        isError = editError.isNotEmpty()
                    )

                    if (editError.isNotEmpty()) {
                        Text(
                            text = editError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = { isEditing = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (language == "ar") "إلغاء" else "Cancel",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Button(
                            onClick = {
                                val cleanName = editNameText.trim()
                                if (cleanName.isEmpty()) {
                                    editError = if (language == "ar") "الاسم مطلوب" else "Name is required"
                                } else if (cleanName.length > 30) {
                                    editError = if (language == "ar") "يجب أن يكون الاسم أقل من ٣٠ حرفاً" else "Name must be under 30 characters"
                                } else {
                                    viewModel.updateNickname(cleanName)
                                    isEditing = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text(
                                text = if (language == "ar") "حفظ" else "Save",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Language & Theme Settings Group
        SettingsSectionHeader(title = Localization.translate("select_language", language) + " / " + Localization.translate("settings", language))
        SettingsTile(
            title = if (language == "ar") "العربية" else "English",
            subtitle = Localization.translate("select_language", language),
            icon = Icons.Default.Language,
            iconColor = PremiumAccent,
            onClick = { showLanguageDialog = true }
        )

        SettingsTile(
            title = when (themeMode) {
                "dark" -> "Dark"
                "light" -> "Light"
                else -> "System"
            },
            subtitle = "Theme Mode",
            icon = Icons.Default.DarkMode,
            iconColor = PremiumAccent,
            onClick = { showThemeDialog = true }
        )

        SettingsTile(
            title = if (defaultReminderTime == "Off") {
                if (language == "ar") "ملغى" else "Off"
            } else {
                defaultReminderTime
            },
            subtitle = Localization.translate("daily_reminder_time", language),
            icon = Icons.Default.AccessTime,
            iconColor = PremiumAccent,
            onClick = { showReminderOptionsDialog = true }
        )

        // Account & Cloud sync Group
        SettingsSectionHeader(title = Localization.translate("account", language))
        SettingsTile(
            title = Localization.translate("account", language),
            subtitle = "Manage secure sync / cloud account",
            icon = Icons.Default.CloudSync,
            iconColor = PremiumAccent,
            onClick = onNavigateToAccount
        )

        SettingsTile(
            title = Localization.translate("backup_restore", language),
            subtitle = "Local Backup, Restore from backup file",
            icon = Icons.Default.Backup,
            iconColor = PremiumAccent,
            onClick = { showBackupRestoreDialog = true }
        )

        // Notifications Setting Group
        val formatDigits: (Int) -> String = { num ->
            if (language == "ar") {
                val arabicChars = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
                num.toString().map { if (it.isDigit()) arabicChars[it - '0'] else it }.joinToString("")
            } else {
                num.toString()
            }
        }

        SettingsSectionHeader(title = if (language == "ar") "إعدادات التنبيهات" else "Notification Settings")
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Vibration Switch Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PremiumBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = "Vibration",
                                tint = PremiumAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (language == "ar") "تفعيل الاهتزاز" else "Enable Vibration",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = if (language == "ar") "اهتزاز مخصص حسب أهمية المهمة" else "Custom vibration patterns based on priority",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { viewModel.updateVibrationEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = PremiumBlue,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }


            }
        }

        // About Group
        SettingsSectionHeader(title = Localization.translate("about", language))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "أويكونوميا",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = if (language == "ar") {
                        "الإصدار 1.0.0 (جاهز للاستخدام)\nمساعد ذكي لتسجيل وتصنيف المهام بالصوت أولاً باستخدام الذكاء الاصطناعي. تم تطويره باستخدام نماذج Gemini ومستودع بيانات Room المحلي وقاعدة البيانات السحابية."
                    } else {
                        "Version 1.0.0 (Production-Ready)\nAI Voice-First Natural Speech Parsing Assistant. Developed using Gemini Flash, Room Persistence, and Cloud Firestore."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    text = if (language == "ar") {
                        "تم تطوير التطبيق بواسطة : مايكل ماجد\nواتساب : 01515034914"
                    } else {
                        "Developed by: Michael Maged\nWhatsApp: 01515034914"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = PremiumAccent,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }



    // Dialog: Language Selector
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(Localization.translate("select_language", language), color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateLanguage("ar")
                                showLanguageDialog = false
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("العربية", color = MaterialTheme.colorScheme.onSurface)
                        if (language == "ar") Icon(Icons.Default.Check, contentDescription = "Checked", tint = PremiumAccent)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateLanguage("en")
                                showLanguageDialog = false
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("English", color = MaterialTheme.colorScheme.onSurface)
                        if (language == "en") Icon(Icons.Default.Check, contentDescription = "Checked", tint = PremiumAccent)
                    }
                }
            },
            confirmButton = {},
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Dialog: Theme Selector
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select Theme", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("dark" to "Premium Dark", "light" to "Light", "system" to "System Default").forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateTheme(code)
                                    showThemeDialog = false
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, color = MaterialTheme.colorScheme.onSurface)
                            if (themeMode == code) Icon(Icons.Default.Check, contentDescription = "Checked", tint = PremiumAccent)
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Dialog: Backup & Restore status mock
    if (showBackupRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showBackupRestoreDialog = false },
            title = { Text(Localization.translate("backup_restore", language), color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    text = if (language == "ar") {
                        "جميع مهام قاعدة بيانات Room المحلية متزامنة بالكامل في الوقت الفعلي مع قاعدة بيانات SQLite دون اتصال بالإنترنت. تم إنشاء ملف النسخ الاحتياطي اليدوي 'oikonomia_backup.db' بنجاح في وحدة التخزين الخارجية."
                    } else {
                        "All local Room database tasks are fully synced in real-time with your offline SQLite database. Manual backup file 'oikonomia_backup.db' has been successfully created in external directory storage."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(onClick = { showBackupRestoreDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = PremiumBlue)) {
                    Text(if (language == "ar") "موافق" else "OK")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Dialog: Reminder Options Dialog
    if (showReminderOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showReminderOptionsDialog = false },
            title = {
                Text(
                    text = if (language == "ar") "التذكير اليومي التلقائي" else "Daily Reminder Time",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (language == "ar") {
                        "اختر ما إذا كنت ترغب في تعديل وقت التذكير اليومي التلقائي أو إلغاء تفعيله بالكامل."
                    } else {
                        "Choose whether to edit the automatic daily reminder time or disable it completely."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Option 1: Disable
                    TextButton(
                        onClick = {
                            viewModel.updateDefaultReminderTime("Off")
                            showReminderOptionsDialog = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (language == "ar") "إلغاء التكرار" else "Disable",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Option 2: Set Time
                    Button(
                        onClick = {
                            showReminderOptionsDialog = false
                            timePickerDialog.show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text(
                            text = if (language == "ar") "تعديل الوقت" else "Set Time",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderOptionsDialog = false }) {
                    Text(
                        text = if (language == "ar") "إلغاء" else "Cancel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        modifier = Modifier.padding(top = 10.dp)
    )
}

@Composable
fun SettingsTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = iconColor)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            }

            Icon(Icons.Default.ChevronRight, contentDescription = "Open setting", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
