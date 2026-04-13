package br.com.anotacoes.presentation

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.anotacoes.R
import br.com.anotacoes.data.settings.SettingsRepositoryImpl
import br.com.anotacoes.domain.model.AppTheme
import br.com.anotacoes.presentation.screen.CustomSplashScreen
import br.com.anotacoes.presentation.screen.PermissionScreen
import br.com.anotacoes.presentation.screen.SettingsScreen
import br.com.anotacoes.presentation.screen.TaskCalendarScreen
import br.com.anotacoes.presentation.screen.TaskFormScreen
import br.com.anotacoes.presentation.screen.TaskHomeScreen
import br.com.anotacoes.presentation.theme.AnotacoesTheme
import br.com.anotacoes.service.TaskLockScreenService
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate

private const val PREFS_NAME = "anotacoes_prefs"
private const val KEY_PERMISSIONS_SHOWN = "permissions_shown"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val appTheme by settingsViewModel.appTheme.collectAsStateWithLifecycle()

            AnotacoesTheme(appTheme = appTheme) {
                Surface {
                    val navController = rememberNavController()

                    val pendingTaskId = remember {
                        mutableStateOf(intent?.getStringExtra(TaskLockScreenService.EXTRA_TASK_ID))
                    }

                    NavHost(navController = navController, startDestination = "splash") {

                        composable("splash") {
                            CustomSplashScreen(
                                appTheme = appTheme,
                                onSplashFinished = {
                                    val permissionsAlreadyDone = areAllCriticalPermissionsGranted()
                                    val permissionsShownBefore = prefs.getBoolean(KEY_PERMISSIONS_SHOWN, false)

                                    val targetRoute = if (!permissionsShownBefore || !permissionsAlreadyDone) {
                                        "permissions"
                                    } else {
                                        "home"
                                    }

                                    navController.navigate(targetRoute) {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("permissions") {
                            PermissionScreen(
                                onAllPermissionsHandled = {
                                    prefs.edit().putBoolean(KEY_PERMISSIONS_SHOWN, true).apply()
                                    navController.navigate("home") {
                                        popUpTo("permissions") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            val viewModel: TaskListViewModel = hiltViewModel()

                            LaunchedEffect(pendingTaskId.value) {
                                val taskId = pendingTaskId.value
                                if (!taskId.isNullOrBlank()) {
                                    pendingTaskId.value = null
                                    navController.navigate("edit/$taskId") {
                                        launchSingleTop = true
                                    }
                                }
                            }

                            TaskHomeScreen(
                                viewModel = viewModel,
                                onNavigateToForm = {
                                    navController.navigate("form") { launchSingleTop = true }
                                },
                                onNavigateToEditTask = { taskId ->
                                    navController.navigate("edit/$taskId") { launchSingleTop = true }
                                },
                                onNavigateToCalendar = {
                                    navController.navigate("calendar") { launchSingleTop = true }
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings") { launchSingleTop = true }
                                }
                            )
                        }

                        composable("calendar") {
                            val viewModel: TaskListViewModel = hiltViewModel()
                            TaskCalendarScreen(
                                viewModel = viewModel,
                                onNavigateToForm = { selectedDateStr ->
                                    navController.navigate("form?date=$selectedDateStr") {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToEditTask = { taskId ->
                                    navController.navigate("edit/$taskId") { launchSingleTop = true }
                                },
                                onBack = { navController.navigateUp() }
                            )
                        }

                        composable(
                            route = "form?date={date}",
                            arguments = listOf(navArgument("date") {
                                type = NavType.StringType
                                defaultValue = ""
                            })
                        ) { backStackEntry ->
                            val dateStr = backStackEntry.arguments?.getString("date")
                            val viewModel: TaskFormViewModel = hiltViewModel()
                            LaunchedEffect(dateStr) {
                                if (!dateStr.isNullOrBlank()) {
                                    val date = runCatching { LocalDate.parse(dateStr) }.getOrNull()
                                    if (date != null) viewModel.setInitialDate(date)
                                }
                            }
                            TaskFormScreen(
                                viewModel = viewModel,
                                onBack = { navController.navigateUp() }
                            )
                        }

                        composable("edit/{taskId}") { backStackEntry ->
                            val taskId = backStackEntry.arguments?.getString("taskId")
                                ?: return@composable
                            val viewModel: TaskFormViewModel = hiltViewModel()
                            LaunchedEffect(taskId) { viewModel.loadTask(taskId) }
                            TaskFormScreen(
                                viewModel = viewModel,
                                onBack = { navController.navigateUp() }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onBack = { navController.navigateUp() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun areAllCriticalPermissionsGranted(): Boolean {
        val notificationsOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true

        val alarmsOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
        } else true

        val batteryOk = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(packageName)

        return notificationsOk && alarmsOk && batteryOk
    }
}