package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import com.example.data.local.AppDatabase
import com.example.data.repository.TaskRepository
import com.example.presentation.screens.*
import com.example.presentation.viewmodel.SopViewModel
import com.example.presentation.viewmodel.SopViewModelFactory
import com.example.ui.theme.DarkCard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PremiumAccent
import com.example.ui.theme.PremiumBlue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Setup Local DB & Repository
        val db = AppDatabase.getDatabase(this)
        val repository = TaskRepository(db.taskDao())

        setContent {
            // 2. Instantiate ViewModel using customized factory
            val viewModel: SopViewModel = ViewModelProvider(
                this,
                SopViewModelFactory(application, repository)
            )[SopViewModel::class.java]

            val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()
            val themeMode by viewModel.theme.collectAsState()
            val language by viewModel.language.collectAsState()

            MyApplicationTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val updateManager = remember { com.example.update.UpdateManager(this@MainActivity) }
                val updateState by updateManager.updateState.collectAsState()
                val coroutineScope = rememberCoroutineScope()
                var showUpdateDialog by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    updateManager.checkForUpdates()
                }

                // Define routes where bottom Navigation Bar should be visible
                val showBottomBar = currentRoute in listOf("voice", "tasks", "calendar", "statistics", "settings")

                // In-App Update Dialog UI
                if (showUpdateDialog) {
                    when (val state = updateState) {
                        is com.example.update.UpdateState.UpdateAvailable -> {
                            AlertDialog(
                                onDismissRequest = { showUpdateDialog = false },
                                title = {
                                    Text(text = if (language == "ar") "تحديث جديد متوفر" else "New Update Available")
                                },
                                text = {
                                    Text(
                                        text = if (language == "ar") 
                                            "يتوفر إصدار جديد من تطبيق أويكونوميا (${state.latestVersion}). هل ترغب في التحديث الآن؟" 
                                            else "A new version of Oikonomia (${state.latestVersion}) is available. Would you like to update now?"
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                updateManager.startDownload(state.apkUrl)
                                            }
                                        }
                                    ) {
                                        Text(text = if (language == "ar") "تحديث" else "Update")
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { showUpdateDialog = false }
                                    ) {
                                        Text(text = if (language == "ar") "لاحقاً" else "Later")
                                    }
                                }
                            )
                        }
                        is com.example.update.UpdateState.Downloading -> {
                            AlertDialog(
                                onDismissRequest = { },
                                title = {
                                    Text(text = if (language == "ar") "جاري تنزيل التحديث..." else "Downloading Update...")
                                },
                                text = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { state.progress },
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                                        )
                                        Text(
                                            text = "${(state.progress * 100).toInt()}%",
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                confirmButton = {}
                            )
                        }
                        is com.example.update.UpdateState.Downloaded -> {
                            LaunchedEffect(state.apkUri) {
                                updateManager.triggerInstall(state.apkUri)
                            }
                            AlertDialog(
                                onDismissRequest = { },
                                title = {
                                    Text(text = if (language == "ar") "اكتمل التنزيل" else "Download Completed")
                                },
                                text = {
                                    Text(
                                        text = if (language == "ar") 
                                            "جاهز لتثبيت التحديث الجديد." 
                                            else "Ready to install the new update."
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            updateManager.triggerInstall(state.apkUri)
                                        }
                                    ) {
                                        Text(text = if (language == "ar") "تثبيت" else "Install")
                                    }
                                }
                            )
                        }
                        is com.example.update.UpdateState.Error -> {
                            AlertDialog(
                                onDismissRequest = { showUpdateDialog = false },
                                title = {
                                    Text(text = if (language == "ar") "خطأ في التحديث" else "Update Error")
                                },
                                text = {
                                    Text(text = state.message)
                                },
                                confirmButton = {
                                    Button(
                                        onClick = { showUpdateDialog = false }
                                    ) {
                                        Text(text = if (language == "ar") "موافق" else "OK")
                                    }
                                }
                            )
                        }
                        else -> {}
                    }
                }

                LaunchedEffect(isFirstLaunch) {
                    if (isFirstLaunch) {
                        navController.navigate("welcome") {
                            popUpTo("voice") { inclusive = false }
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            Column {
                                // Premium thin top divider line
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color.White.copy(alpha = 0.08f))
                                )
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ) {
                                    NavigationBarItem(
                                        selected = currentRoute == "voice",
                                        onClick = {
                                            navController.navigate("voice") {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(Icons.Default.Mic, contentDescription = "Voice Assistant") },
                                        label = { 
                                            Text(
                                                text = if (language == "ar") "المساعد" else "Voice", 
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            ) 
                                        },
                                        alwaysShowLabel = true,
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = PremiumAccent,
                                            selectedTextColor = PremiumAccent,
                                            indicatorColor = PremiumBlue.copy(alpha = 0.15f),
                                            unselectedIconColor = Color.Gray,
                                            unselectedTextColor = Color.Gray
                                        )
                                    )

                                    NavigationBarItem(
                                        selected = currentRoute == "tasks",
                                        onClick = {
                                            navController.navigate("tasks") {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(Icons.Default.List, contentDescription = "Tasks List") },
                                        label = { 
                                            Text(
                                                text = Localization.translate("my_tasks", language), 
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            ) 
                                        },
                                        alwaysShowLabel = true,
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = PremiumAccent,
                                            selectedTextColor = PremiumAccent,
                                            indicatorColor = PremiumBlue.copy(alpha = 0.15f),
                                            unselectedIconColor = Color.Gray,
                                            unselectedTextColor = Color.Gray
                                        )
                                    )

                                    NavigationBarItem(
                                        selected = currentRoute == "calendar",
                                        onClick = {
                                            navController.navigate("calendar") {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Calendar") },
                                        label = { 
                                            Text(
                                                text = Localization.translate("calendar", language), 
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            ) 
                                        },
                                        alwaysShowLabel = true,
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = PremiumAccent,
                                            selectedTextColor = PremiumAccent,
                                            indicatorColor = PremiumBlue.copy(alpha = 0.15f),
                                            unselectedIconColor = Color.Gray,
                                            unselectedTextColor = Color.Gray
                                        )
                                    )

                                    NavigationBarItem(
                                        selected = currentRoute == "statistics",
                                        onClick = {
                                            navController.navigate("statistics") {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Statistics") },
                                        label = { 
                                            Text(
                                                text = Localization.translate("statistics", language), 
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            ) 
                                        },
                                        alwaysShowLabel = true,
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = PremiumAccent,
                                            selectedTextColor = PremiumAccent,
                                            indicatorColor = PremiumBlue.copy(alpha = 0.15f),
                                            unselectedIconColor = Color.Gray,
                                            unselectedTextColor = Color.Gray
                                        )
                                    )

                                    NavigationBarItem(
                                        selected = currentRoute == "settings",
                                        onClick = {
                                            navController.navigate("settings") {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                        label = { 
                                            Text(
                                                text = Localization.translate("settings", language), 
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            ) 
                                        },
                                        alwaysShowLabel = true,
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = PremiumAccent,
                                            selectedTextColor = PremiumAccent,
                                            indicatorColor = PremiumBlue.copy(alpha = 0.15f),
                                            unselectedIconColor = Color.Gray,
                                            unselectedTextColor = Color.Gray
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = "voice",
                        modifier = Modifier.padding(paddingValues)
                    ) {
                        composable("welcome") {
                            WelcomeScreen(
                                viewModel = viewModel,
                                onComplete = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("voice") {
                            if (isFirstLaunch) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background)
                                )
                            } else {
                                MainVoiceScreen(
                                    viewModel = viewModel,
                                    onNavigateToTasks = {
                                        navController.navigate("tasks")
                                    },
                                    onNavigateToCreate = {
                                        navController.navigate("create_task")
                                    }
                                )
                            }
                        }

                        composable("tasks") {
                            TasksListScreen(
                                viewModel = viewModel,
                                onNavigateToCreate = {
                                    navController.navigate("create_task")
                                },
                                onEditTask = { task ->
                                    navController.navigate("create_task?taskId=${task.id}")
                                }
                            )
                        }

                        composable(
                            route = "create_task?taskId={taskId}",
                            arguments = listOf(
                                navArgument("taskId") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val selectedTaskId = backStackEntry.arguments?.getString("taskId")
                            CreateTaskScreen(
                                viewModel = viewModel,
                                taskId = selectedTaskId,
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("calendar") {
                            CalendarScreen(
                                viewModel = viewModel,
                                onEditTask = { task ->
                                    navController.navigate("create_task?taskId=${task.id}")
                                }
                            )
                        }

                        composable("statistics") {
                            StatisticsScreen(viewModel = viewModel)
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateToAccount = {
                                    navController.navigate("account")
                                }
                            )
                        }

                        composable("account") {
                            AccountScreen(
                                viewModel = viewModel,
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
