package ir.dastranj.app.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dastranj.app.R
import ir.dastranj.app.domain.date.JalaliDateFormatter
import ir.dastranj.app.domain.report.ChartAxis
import ir.dastranj.app.domain.report.ReportInsight
import ir.dastranj.app.ui.components.pressScaleClickable
import ir.dastranj.app.ui.theme.Dastranj
import ir.dastranj.app.ui.util.PersianNumbers

/**
 * Report (`Dastranj Report Screen.dc.html`).
 *
 * Category chips, a year stepper, the twelve-month chart, and the insight card.
 */
@Composable
fun ReportScreen(viewModel: ReportViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 150.dp),
    ) {
        item(key = "chips") {
            CategoryChips(
                categories = state.categories,
                selectedId = state.selectedCategoryId,
                onSelect = viewModel::selectCategory,
            )
            Spacer(Modifier.height(14.dp))
        }

        item(key = "year") {
            YearStepper(
                year = state.year,
                canGoForward = state.canGoForward,
                onPrevious = viewModel::previousYear,
                onNext = viewModel::nextYear,
            )
            Spacer(Modifier.height(10.dp))
        }

        item(key = "chart") {
            ChartCard(state = state, onMonthTapped = viewModel::toggleMonth)
            Spacer(Modifier.height(12.dp))
        }

        item(key = "insight") {
            InsightCard(state = state)
        }
    }
}

@Composable
private fun CategoryChips(
    categories: List<ir.dastranj.app.ui.transaction.CategoryOption>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // «همهٔ دسته‌ها» always leads, and is the default.
        Chip(
            label = stringResource(R.string.report_all_categories),
            selected = selectedId == null,
            onClick = { onSelect(null) },
        )
        categories.forEach { category ->
            Chip(
                label = category.name,
                selected = category.id == selectedId,
                onClick = { onSelect(category.id) },
            )
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(if (selected) Dastranj.colors.cta else Dastranj.colors.sunken)
            .pressScaleClickable(
                onClick = onClick,
                role = androidx.compose.ui.semantics.Role.Tab,
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = Dastranj.type.label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Dastranj.colors.ctaInk else Dastranj.colors.muted,
        )
    }
}

@Composable
private fun YearStepper(
    year: Int,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperArrow(
            iconRes = R.drawable.ic_chevron_right,
            contentDescription = stringResource(R.string.report_prev_year),
            enabled = true,
            onClick = onPrevious,
        )
        Text(
            text = PersianNumbers.toPersianDigits(year.toString()),
            style = Dastranj.type.title3,
            color = Dastranj.colors.title,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        StepperArrow(
            iconRes = R.drawable.ic_chevron_left,
            contentDescription = stringResource(
                if (canGoForward) R.string.report_next_year
                else R.string.report_next_year_disabled,
            ),
            enabled = canGoForward,
            onClick = onNext,
        )
    }
}

@Composable
private fun StepperArrow(
    iconRes: Int,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .pressScaleClickable(onClick = onClick, enabled = enabled)
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Dastranj.colors.title,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ChartCard(state: ReportUiState, onMonthTapped: (Int) -> Unit) {
    val monthNames = JalaliDateFormatter.monthNames()
    val currencyUnit = stringResource(R.string.currency_unit)
    val futureLabel = stringResource(R.string.report_month_future)
    val currentLabel = stringResource(R.string.report_month_current)

    val currentMonthIndex = if (state.year == ReportUiState.currentJalaliYear()) {
        ReportUiState.currentJalaliMonthIndex()
    } else {
        null
    }

    val bars = state.monthlyTotals.mapIndexed { index, value ->
        val spokenAmount = if (value == null) {
            futureLabel
        } else {
            stringResource(
                R.string.report_month_amount_a11y,
                PersianNumbers.formatGrouped(value),
            )
        }
        val isCurrent = index == currentMonthIndex
        ChartBar(
            monthIndex = index,
            monthName = monthNames[index],
            fraction = ChartAxis.barFraction(value ?: 0L, state.axisTopToman),
            isFuture = value == null,
            isZero = value == 0L,
            isCurrentMonth = isCurrent,
            contentDescription = stringResource(
                R.string.report_month_a11y,
                monthNames[index],
                if (isCurrent) "$spokenAmount — $currentLabel" else spokenAmount,
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(Dastranj.elevation.card, RoundedCornerShape(Dastranj.shapes.card))
            .clip(RoundedCornerShape(Dastranj.shapes.card))
            .background(Dastranj.colors.card)
            .padding(18.dp),
    ) {
        Text(
            text = axisLabel(state.axisTopToman),
            style = Dastranj.type.micro,
            color = Dastranj.colors.faint,
        )
        Spacer(Modifier.height(8.dp))

        SpendingChart(
            bars = bars,
            selectedMonthIndex = state.selectedMonthIndex,
            onMonthTapped = onMonthTapped,
        )

        // The tapped month's figure, in place of a floating tooltip: a tooltip over a 20dp bar in
        // an RTL layout is fiddly to position and easy to clip, and the value is just as legible
        // stated under the chart.
        state.selectedMonthIndex?.let { index ->
            val value = state.monthlyTotals.getOrNull(index)
            if (value != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = monthNames[index],
                        style = Dastranj.type.label,
                        color = Dastranj.colors.muted,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = PersianNumbers.formatGrouped(value),
                        style = Dastranj.type.title3,
                        fontWeight = FontWeight.Bold,
                        color = Dastranj.colors.title,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = currencyUnit,
                        style = Dastranj.type.caption,
                        color = Dastranj.colors.muted,
                    )
                }
            }
        }
    }
}

/** «۲٫۵ میلیون تومان» above a million, otherwise the grouped figure. */
@Composable
private fun axisLabel(axisTopToman: Long): String {
    if (axisTopToman >= 1_000_000L) {
        val millions = axisTopToman.toDouble() / 1_000_000.0
        // One decimal place, with the trailing «٫۰» dropped so 2.0 reads as «۲».
        val text = if (millions == Math.floor(millions)) {
            millions.toLong().toString()
        } else {
            String.format("%.1f", millions)
        }
        // U+066B ARABIC DECIMAL SEPARATOR, not a full stop.
        return stringResource(
            R.string.report_axis_millions,
            PersianNumbers.toPersianDigits(text).replace('.', '٫'),
        )
    }
    return stringResource(
        R.string.report_axis_toman,
        PersianNumbers.formatGrouped(axisTopToman),
    )
}

/**
 * The insight card.
 *
 * Every branch of [ReportInsight] maps to exactly one string, so a new branch is a compile error
 * here rather than a silently missing sentence.
 */
@Composable
private fun InsightCard(state: ReportUiState) {
    val monthNames = JalaliDateFormatter.monthNames()

    val subject = state.selectedCategoryId
        ?.let { id -> state.categories.firstOrNull { it.id == id } }
        ?.let { stringResource(R.string.report_subject_category, it.name) }
        ?: stringResource(R.string.report_subject_all)

    val text = when (val insight = state.insight) {
        is ReportInsight.NoData ->
            stringResource(R.string.report_insight_no_data, subject)

        is ReportInsight.BareFigure -> stringResource(
            R.string.report_insight_bare_figure,
            subject,
            monthNames[insight.monthIndex],
            PersianNumbers.formatGrouped(insight.amountRial),
        )

        is ReportInsight.ZeroBaseline -> stringResource(
            R.string.report_insight_zero_baseline,
            subject,
            monthNames[insight.monthIndex],
        )

        is ReportInsight.LevelWithAverage -> stringResource(
            R.string.report_insight_level,
            subject,
            monthNames[insight.monthIndex],
            PersianNumbers.toPersianDigits(insight.monthsInMean.toString()),
        )

        is ReportInsight.ComparedToAverage -> stringResource(
            if (insight.higher) R.string.report_insight_higher
            else R.string.report_insight_lower,
            subject,
            monthNames[insight.monthIndex],
            PersianNumbers.toPersianDigits(insight.percentDifference.toString()),
            PersianNumbers.toPersianDigits(insight.monthsInMean.toString()),
        )

        is ReportInsight.LowestMonth -> stringResource(
            R.string.report_insight_lowest,
            subject,
            monthNames[insight.monthIndex],
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dastranj.shapes.card))
            .background(Dastranj.colors.brandTint)
            .padding(16.dp)
            .semantics(mergeDescendants = true) { contentDescription = text },
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lightbulb),
            contentDescription = null,
            tint = Dastranj.colors.brandInk,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = Dastranj.type.bodySm,
            color = Dastranj.colors.body,
            modifier = Modifier.heightIn(min = 20.dp),
        )
    }
}
