package com.example.presentation.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.SopViewModel
import com.example.ui.theme.PremiumAccent
import com.example.ui.theme.PremiumBlue
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    viewModel: SopViewModel,
    onComplete: () -> Unit
) {
    var nickname by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        val cleanName = nickname.trim()
        viewModel.finishOnboarding(cleanName, "ar")
        onComplete()
    }

    LaunchedEffect(Unit) {
        delay(1000)
        showForm = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        PremiumBlue.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.background
                    ),
                    radius = 1500f
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Centered Application Logo with a pulsing aura
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f))
                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(PremiumBlue.copy(alpha = 0.15f))
                        .border(1.dp, PremiumAccent.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "أويكونوميا Mic Logo",
                        tint = PremiumAccent,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // App Title and Subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "مرحباً بك في أويكونوميا",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 0.5.sp
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "مساعدك الذكي لإدارة حياتك اليومية وتنظيم مهامك.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        lineHeight = 24.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Form Entry
            AnimatedVisibility(
                visible = showForm,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 50 })
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                        .padding(24.dp)
                ) {
                    Text(
                        text = "ما الاسم الذي تحب أن أناديك به؟",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = nickname,
                        onValueChange = {
                            nickname = it
                            if (showError) showError = false
                        },
                        placeholder = {
                            Text(
                                text = "اكتب اسمك",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PremiumAccent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        isError = showError
                    )

                    if (showError) {
                        Text(
                            text = validationMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            val cleanName = nickname.trim()
                            if (cleanName.isEmpty()) {
                                validationMessage = "الاسم مطلوب ولا يمكن تركه فارغاً"
                                showError = true
                            } else if (cleanName.length > 30) {
                                validationMessage = "يجب أن يكون الاسم أقل من ٣٠ حرفاً"
                                showError = true
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.finishOnboarding(cleanName, "ar")
                                    onComplete()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PremiumBlue,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text(
                            text = "ابدأ",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}
