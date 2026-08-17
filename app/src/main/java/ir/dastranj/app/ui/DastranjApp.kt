package ir.dastranj.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ir.dastranj.app.ui.account.AddAccountScreen
import ir.dastranj.app.ui.components.BottomTabBar
import ir.dastranj.app.ui.transaction.AddTransactionScreen
import ir.dastranj.app.ui.navigation.Route
import ir.dastranj.app.ui.navigation.TopLevelTab
import ir.dastranj.app.ui.theme.Dastranj

/**
 * The app shell: a centred toolbar, a scrolling page, and a floating bottom bar.
 *
 * Structure follows `Dastranj Home.dc.html`. Two things in that file are *not* reproduced, because
 * they are artifacts of the design preview rather than the product:
 *
 * - the mock status bar (۹:۴۱, signal/wifi/battery) and the 412×892 phone frame — the real app
 *   renders under the device's own status bar;
 * - the «حالت نمایش» segmented control, which is the preview's full/empty data toggle.
 *
 * The settings gear in the More toolbar is also dropped: it is wired to `noop` in the design and
 * goes nowhere in v1, so per PRD §5.3 it is removed rather than shipped dead.
 */
@Composable
fun DastranjApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val selectedTab = TopLevelTab.entries.firstOrNull { tab ->
        currentDestination?.hierarchyContains(tab)
    } ?: TopLevelTab.HOME

    // The bar and toolbar belong to the four top-level tabs only. The add-screens are full-page
    // takeovers in the design (`position:absolute;inset:0;z-index:50`), so they cover both.
    val isTopLevel = currentDestination?.let { dest ->
        TopLevelTab.entries.any { dest.hasRoute(it.route::class) }
    } ?: true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Dastranj.colors.page),
    ) {
        Column(Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = isTopLevel) {
                CenteredToolbar(title = stringResource(selectedTab.labelRes))
            }

            DastranjNavHost(
                navController = navController,
                modifier = Modifier.weight(1f),
            )
        }

        AnimatedVisibility(
            visible = isTopLevel,
            enter = fadeIn(tween(Dastranj.motion.base)) +
                slideInVertically(tween(Dastranj.motion.base)) { it },
            exit = fadeOut(tween(Dastranj.motion.fast)) +
                slideOutVertically(tween(Dastranj.motion.fast)) { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 18.dp),
        ) {
            BottomTabBar(
                selectedTab = selectedTab,
                onTabSelected = { tab -> navController.navigateToTab(tab) },
                onAddTransaction = { navController.navigate(Route.AddTransaction) },
            )
        }
    }
}

/**
 * The toolbar: a centred title, 52dp tall, with the design's 4/20/10 padding.
 *
 * No back arrow and no overflow — the top-level screens have neither in the design.
 */
@Composable
private fun CenteredToolbar(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 10.dp)
            .height(52.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = Dastranj.type.title2,
            color = Dastranj.colors.title,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DastranjNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home,
        modifier = modifier,
    ) {
        composable<Route.Home> { PlaceholderScreen("Home") }
        composable<Route.Budget> { PlaceholderScreen("Budget") }
        composable<Route.Report> { PlaceholderScreen("Report") }
        composable<Route.More> { PlaceholderScreen("More") }

        composable<Route.AddTransaction> {
            AddTransactionScreen(
                onClose = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable<Route.AddAccount> {
            AddAccountScreen(
                onClose = { navController.popBackStack() },
                // The new account is already in the database, so Home picks it up from its own
                // Flow; this only has to dismiss the screen.
                onSaved = { navController.popBackStack() },
            )
        }
        composable<Route.AddBudget> { PlaceholderScreen("AddBudget") }
    }
}

/**
 * Temporary. Each phase replaces one of these with the real screen.
 */
@Composable
private fun PlaceholderScreen(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(name, style = Dastranj.type.body, color = Dastranj.colors.muted)
    }
}

/**
 * Tab switching keeps a single copy of each tab on the back stack and restores its scroll position,
 * which is what the design's "reset scroll on tab change" behaviour composes with.
 */
private fun NavHostController.navigateToTab(tab: TopLevelTab) {
    navigate(tab.route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavDestination.hierarchyContains(tab: TopLevelTab): Boolean =
    hierarchy.any { it.hasRoute(tab.route::class) }
