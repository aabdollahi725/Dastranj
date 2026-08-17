package ir.dastranj.app.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ir.dastranj.app.R
import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation Compose routes (PRD §13.1).
 */
sealed interface Route {

    @Serializable data object Home : Route

    @Serializable data object Budget : Route

    @Serializable data object Report : Route

    @Serializable data object More : Route

    @Serializable data object AddTransaction : Route

    @Serializable data object AddAccount : Route

    @Serializable data object AddBudget : Route
}

/**
 * The four bottom-nav tabs, in the order the design lays them out (right to left in RTL:
 * خانه، بودجه، گزارش، بیشتر).
 *
 * The order is load-bearing: the sliding pill is positioned by ordinal, so reordering this enum
 * reorders the tab bar.
 */
enum class TopLevelTab(
    val route: Route,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    HOME(Route.Home, R.string.tab_home, R.drawable.ic_house),
    BUDGET(Route.Budget, R.string.tab_budget, R.drawable.ic_chart_pie),
    REPORT(Route.Report, R.string.tab_report, R.drawable.ic_chart_column),
    MORE(Route.More, R.string.tab_more, R.drawable.ic_ellipsis),
}
