package ir.dastranj.app.ui.budget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dastranj.app.R
import ir.dastranj.app.domain.budget.BudgetLevel
import ir.dastranj.app.ui.components.pressScaleClickable
import ir.dastranj.app.ui.theme.Dastranj
import ir.dastranj.app.ui.util.HexColor
import ir.dastranj.app.ui.util.IconRegistry
import ir.dastranj.app.ui.util.PersianNumbers

/**
 * Budget (`Dastranj Budget Screen.dc.html`).
 *
 * A month stepper, then one row per budget: category, spend against limit, a progress bar with a
 * tick at the warning threshold, and a cue line that changes wording and colour with the level.
 *
 * Rows arrive sorted by proportion spent, so the budgets in trouble are the ones the user sees
 * first without scrolling.
 */
@Composable
fun BudgetScreen(
    onAddBudget: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        MonthStepper(
            monthLabel = state.monthLabel,
            canGoForward = state.canGoForward,
            onPrevious = viewModel::goToPreviousMonth,
            onNext = viewModel::goToNextMonth,
        )

        when {
            state.showEmptyState -> EmptyBudgets(onAddBudget = onAddBudget)
            state.rows.isNotEmpty() -> BudgetList(
                rows = state.rows,
                onAddBudget = onAddBudget,
            )
            else -> Unit
        }
    }
}

/**
 * «ماه پیش» ← مرداد ۱۴۰۵ → «ماه آینده».
 *
 * The forward arrow is disabled on the current month rather than hidden, so the control keeps its
 * shape and the label stays centred.
 */
@Composable
private fun MonthStepper(
    monthLabel: String,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperArrow(
            iconRes = R.drawable.ic_chevron_right,
            contentDescription = stringResource(R.string.budget_prev_month),
            enabled = true,
            onClick = onPrevious,
        )

        Text(
            text = monthLabel,
            style = Dastranj.type.title3,
            color = Dastranj.colors.title,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )

        StepperArrow(
            iconRes = R.drawable.ic_chevron_left,
            contentDescription = if (canGoForward) {
                stringResource(R.string.budget_next_month)
            } else {
                stringResource(R.string.budget_next_month_disabled)
            },
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
            // The design dims the disabled arrow to .4 rather than removing it.
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
private fun BudgetList(rows: List<BudgetRow>, onAddBudget: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 6.dp,
            // Clears the floating tab bar.
            bottom = 150.dp,
        ),
    ) {
        item(key = "card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(Dastranj.elevation.card, RoundedCornerShape(Dastranj.shapes.card))
                    .clip(RoundedCornerShape(Dastranj.shapes.card))
                    .background(Dastranj.colors.card)
                    .padding(horizontal = 18.dp),
            ) {
                rows.forEachIndexed { index, row ->
                    BudgetRowView(row = row, showDivider = index > 0)
                }
            }
        }
        item(key = "add") {
            Spacer(Modifier.height(12.dp))
            AddBudgetButton(onClick = onAddBudget)
        }
    }
}

@Composable
private fun BudgetRowView(row: BudgetRow, showDivider: Boolean) {
    val colors = Dastranj.colors
    val accent = remember(row.colorHex) { Color(HexColor.parseOr(row.colorHex, HexColor.FALLBACK)) }

    val barColor = when (row.level) {
        BudgetLevel.EXCEEDED -> colors.danger
        BudgetLevel.WARNING -> colors.warning
        BudgetLevel.SAFE -> colors.moneyIn
    }
    val cueColor = when (row.level) {
        BudgetLevel.EXCEEDED -> colors.danger
        // The gold used for a bar is too light for text; the design pairs it with a darker ink.
        BudgetLevel.WARNING -> if (colors.isDark) colors.warning else Color(0xFF8A6200)
        BudgetLevel.SAFE -> colors.muted
    }

    val percentText = PersianNumbers.toPersianDigits(row.percentUsed.toString())
    val cue = when (row.level) {
        BudgetLevel.EXCEEDED ->
            stringResource(R.string.budget_cue_exceeded, percentText, row.overspendText)
        BudgetLevel.WARNING -> stringResource(R.string.budget_cue_warning, percentText)
        BudgetLevel.SAFE -> stringResource(R.string.budget_cue_normal, percentText)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (showDivider) {
                    Modifier.drawTopHairline(colors.hairline)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 14.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${row.categoryName}، $cue"
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(IconRegistry.drawableFor(row.iconName)),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = row.categoryName,
                style = Dastranj.type.bodySm,
                fontWeight = FontWeight.SemiBold,
                color = colors.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (row.level != BudgetLevel.SAFE) {
                Icon(
                    painter = painterResource(
                        if (row.level == BudgetLevel.EXCEEDED) R.drawable.ic_circle_alert
                        else R.drawable.ic_triangle_alert,
                    ),
                    contentDescription = null,
                    tint = cueColor,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = cue,
                style = Dastranj.type.caption,
                fontWeight = if (row.level == BudgetLevel.SAFE) FontWeight.Normal else FontWeight.SemiBold,
                color = cueColor,
                maxLines = 1,
            )
        }

        Spacer(Modifier.height(10.dp))
        ProgressTrack(
            fraction = row.barFraction,
            thresholdFraction = row.thresholdPercent / 100f,
            barColor = barColor,
            dimThreshold = row.level == BudgetLevel.EXCEEDED,
        )

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = row.spentText,
                style = Dastranj.type.label,
                fontWeight = FontWeight.SemiBold,
                color = colors.title,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.budget_of_limit, row.limitText),
                style = Dastranj.type.caption,
                color = colors.muted,
            )
        }
    }
}

/**
 * The progress bar, with a tick marking the warning threshold.
 *
 * The tick is what makes the threshold legible before it is crossed — without it, a bar at 78%
 * looks the same as one at 62%, and the user has no way to see the warning coming.
 */
@Composable
private fun ProgressTrack(
    fraction: Float,
    thresholdFraction: Float,
    barColor: Color,
    dimThreshold: Boolean,
) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(Dastranj.motion.base, easing = Dastranj.motion.easeOut),
        label = "budgetBar",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(Dastranj.colors.sunken),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedFraction)
                .height(8.dp)
                .clip(CircleShape)
                .background(barColor),
        )
        // Positioned by fraction of the track's own width, so it stays put at any screen size.
        Box(
            modifier = Modifier
                .fillMaxWidth(thresholdFraction.coerceIn(0f, 1f))
                .height(8.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(8.dp)
                    .background(
                        Dastranj.colors.title.copy(alpha = if (dimThreshold) 0.18f else 0.32f),
                    ),
            )
        }
    }
}

@Composable
private fun AddBudgetButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(Dastranj.colors.card)
            .pressScaleClickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = null,
            tint = Dastranj.colors.brandInk,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.budget_add),
            style = Dastranj.type.label,
            fontWeight = FontWeight.SemiBold,
            color = Dastranj.colors.brandInk,
        )
    }
}

@Composable
private fun EmptyBudgets(onAddBudget: () -> Unit) {
    val colors = Dastranj.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 60.dp)
            .shadow(Dastranj.elevation.card, RoundedCornerShape(Dastranj.shapes.card))
            .clip(RoundedCornerShape(Dastranj.shapes.card))
            .background(colors.card)
            .padding(start = 22.dp, end = 22.dp, top = 34.dp, bottom = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.sunken),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chart_pie),
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.budget_empty_title),
            style = Dastranj.type.title3,
            color = colors.title,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(22.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(CircleShape)
                .background(colors.cta)
                .pressScaleClickable(onClick = onAddBudget),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.budget_empty_cta),
                style = Dastranj.type.title3,
                fontWeight = FontWeight.SemiBold,
                color = colors.ctaInk,
            )
        }
    }
}

/**
 * A hairline along the top edge — the design's `inset 0 1px 0` divider between rows.
 *
 * Drawn rather than composed as a Divider so it does not take part in layout: the rows' 14dp
 * padding stays symmetrical instead of being nudged by a 1px child.
 */
private fun Modifier.drawTopHairline(color: Color): Modifier = drawBehind {
    drawLine(
        color = color,
        start = Offset(0f, 0f),
        end = Offset(size.width, 0f),
        strokeWidth = 1f,
    )
}
