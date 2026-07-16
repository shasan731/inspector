package com.shahriarhasan.usedphoneinspector.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shahriarhasan.usedphoneinspector.R
import com.shahriarhasan.usedphoneinspector.core.utilities.findActivity
import com.shahriarhasan.usedphoneinspector.feature.history.HistoryScreen
import com.shahriarhasan.usedphoneinspector.feature.history.HistoryViewModel
import com.shahriarhasan.usedphoneinspector.feature.home.HomeScreen
import com.shahriarhasan.usedphoneinspector.feature.home.HomeViewModel
import com.shahriarhasan.usedphoneinspector.feature.inspection.InspectionNavigation
import com.shahriarhasan.usedphoneinspector.feature.inspection.InspectionSetupScreen
import com.shahriarhasan.usedphoneinspector.feature.inspection.InspectionSetupViewModel
import com.shahriarhasan.usedphoneinspector.feature.inspection.InspectionViewModel
import com.shahriarhasan.usedphoneinspector.feature.inspection.InspectionWizardScreen
import com.shahriarhasan.usedphoneinspector.feature.onboarding.OnboardingScreen
import com.shahriarhasan.usedphoneinspector.feature.onboarding.OnboardingViewModel
import com.shahriarhasan.usedphoneinspector.feature.review.ReviewScreen
import com.shahriarhasan.usedphoneinspector.feature.review.ReviewViewModel
import com.shahriarhasan.usedphoneinspector.feature.settings.SettingsScreen
import com.shahriarhasan.usedphoneinspector.feature.settings.SettingsViewModel
import com.shahriarhasan.usedphoneinspector.feature.upgrade.UpgradeScreen
import com.shahriarhasan.usedphoneinspector.feature.upgrade.UpgradeViewModel

private object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETUP = "inspection/setup"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val WIZARD = "inspection/{inspectionId}"
    const val REVIEW = "review/{inspectionId}"
    const val UPGRADE = "upgrade"
    fun wizard(id: String) = "inspection/$id"
    fun review(id: String) = "review/$id"
}

private data class PrimaryDestination(val route: String, val label: Int, val icon: ImageVector)

private val primaryDestinations = listOf(
    PrimaryDestination(Routes.HOME, R.string.nav_home, Icons.Default.Home),
    PrimaryDestination(Routes.SETUP, R.string.nav_new_inspection, Icons.Default.AddCircle),
    PrimaryDestination(Routes.HISTORY, R.string.nav_history, Icons.Default.History),
    PrimaryDestination(Routes.SETTINGS, R.string.nav_settings, Icons.Default.Settings),
)

@Composable
fun InspectorApp(onboardingComplete: Boolean) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = if (onboardingComplete) Routes.HOME else Routes.ONBOARDING) {
        composable(Routes.ONBOARDING) {
            val viewModel: OnboardingViewModel = hiltViewModel()
            OnboardingScreen {
                viewModel.complete()
                navController.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            }
        }
        composable(Routes.HOME) {
            val viewModel: HomeViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            PrimaryScaffold(navController, Routes.HOME) {
                HomeScreen(
                    state = state,
                    onNewInspection = { navController.navigate(Routes.SETUP) },
                    onResume = { navController.navigate(Routes.wizard(it)) },
                    onView = { navController.navigate(Routes.review(it)) },
                    onUpgrade = { navController.navigate(Routes.UPGRADE) },
                )
            }
        }
        composable(Routes.SETUP) {
            val viewModel: InspectionSetupViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            LaunchedEffect(viewModel) {
                viewModel.navigation.collect { id -> navController.navigate(Routes.wizard(id)) }
            }
            PrimaryScaffold(navController, Routes.SETUP) {
                InspectionSetupScreen(state, viewModel::onEvent)
            }
        }
        composable(Routes.HISTORY) {
            val viewModel: HistoryViewModel = hiltViewModel()
            val results by viewModel.results.collectAsState()
            val filter by viewModel.filter.collectAsState()
            LaunchedEffect(viewModel) { viewModel.navigation.collect { navController.navigate(Routes.wizard(it)) } }
            PrimaryScaffold(navController, Routes.HISTORY) {
                HistoryScreen(
                    results,
                    filter,
                    viewModel,
                    onOpen = { navController.navigate(Routes.review(it)) },
                    onResume = { navController.navigate(Routes.wizard(it)) },
                )
            }
        }
        composable(Routes.SETTINGS) {
            val viewModel: SettingsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            PrimaryScaffold(navController, Routes.SETTINGS) {
                SettingsScreen(state, viewModel) { navController.navigate(Routes.UPGRADE) }
            }
        }
        composable(Routes.WIZARD) {
            val viewModel: InspectionViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            LaunchedEffect(viewModel) {
                viewModel.navigation.collect { event ->
                    if (event == InspectionNavigation.Review) navController.navigate(Routes.review(viewModel.inspectionId))
                }
            }
            InspectionWizardScreen(state, viewModel) {
                navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
            }
        }
        composable(Routes.REVIEW) {
            val viewModel: ReviewViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val inspectionId = state.details?.inspection?.id
            ReviewScreen(
                state,
                viewModel,
                onEdit = { inspectionId?.let { navController.navigate(Routes.wizard(it)) } },
                onUpgrade = { navController.navigate(Routes.UPGRADE) },
            )
        }
        composable(Routes.UPGRADE) {
            val viewModel: UpgradeViewModel = hiltViewModel()
            val state by viewModel.billingRepository.state.collectAsState()
            val activity = LocalContext.current.findActivity()
            UpgradeScreen(
                state,
                onBuy = { activity?.let(viewModel::buy) },
                onRestore = viewModel::restore,
            )
        }
    }
}

@Composable
private fun PrimaryScaffold(navController: NavHostController, currentRoute: String, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 600.dp) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail {
                    primaryDestinations.forEach { destination ->
                        NavigationRailItem(
                            selected = currentRoute == destination.route,
                            onClick = { navController.navigatePrimary(destination.route) },
                            icon = { Icon(destination.icon, contentDescription = stringResource(destination.label)) },
                            label = { Text(stringResource(destination.label)) },
                        )
                    }
                }
                Box(Modifier.weight(1f)) { content() }
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        primaryDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = currentRoute == destination.route,
                                onClick = { navController.navigatePrimary(destination.route) },
                                icon = { Icon(destination.icon, contentDescription = stringResource(destination.label)) },
                                label = { Text(stringResource(destination.label)) },
                            )
                        }
                    }
                },
            ) { padding -> Box(Modifier.padding(padding)) { content() } }
        }
    }
}

private fun NavHostController.navigatePrimary(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
