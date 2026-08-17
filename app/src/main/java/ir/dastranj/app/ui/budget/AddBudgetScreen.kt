package ir.dastranj.app.ui.budget

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dastranj.app.R
import ir.dastranj.app.ui.components.NumericKeypad
import ir.dastranj.app.ui.components.pressScaleClickable
import ir.dastranj.app.ui.theme.Dastranj
import ir.dastranj.app.ui.theme.Ink0
import ir.dastranj.app.ui.transaction.CategoryOption
import ir.dastranj.app.ui.util.HexColor
import ir.dastranj.app.ui.util.IconRegistry
import ir.dastranj.app.ui.util.PersianNumbers

/**
 * Add-budget screen (`Dastranj Add Budget Screen.dc.html`).
 *
 * This is also where `POST_NOTIFICATIONS` is requested (PRD §11) — at the moment the user creates
 * the thing that would notify them, not at first launch. Asking here means the request arrives with
 * an obvious reason attached, which is the difference between a considered "allow" and a reflexive
 * "deny".
 */
@Composable
fun AddBudgetScreen(
    onClose: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddBudgetViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        // Nothing to do either way: a budget works with or without notifications, and re-prompting
        // someone who declined is worse than the silence.
        onResult = { },
    )

    // Requested once, when the screen opens. Below API 33 the permission does not exist and the
    // notification simply posts.
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Dastranj.colors.page),
    ) {
        Column(Modifier.fillMaxSize()) {
            AddBudgetToolbar(onClose = onClose)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                AmountBlock(
                    amountText = state.amountFormatted,
                    words = state.amountInWords,
                    isEmpty = state.amountToman.isEmpty(),
                )

                CategoryField(
                    category = state.selectedCategory,
                    onClick = viewModel::openCategorySheet,
                )

                ThresholdSlider(
                    selected = state.thresholdPercent,
                    onSelect = viewModel::setThresholdFromSlider,
                )

                RepeatToggle(
                    checked = state.autoRepeat,
                    onCheckedChange = viewModel::setAutoRepeat,
                )
            }

            SaveBar(enabled = state.canSave, onSave = viewModel::save)
        }

        if (!state.categorySheetOpen) {
            Box(Modifier.align(Alignment.BottomCenter)) {
                NumericKeypad(
                    onDigit = viewModel::onKeypadDigit,
                    onDelete = viewModel::onKeypadDelete,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                )
            }
        }

        if (state.categorySheetOpen) {
            CategorySheet(
                categories = state.availableCategories,
                selectedId = state.selectedCategoryId,
                onSelect = viewModel::selectCategory,
                onDismiss = viewModel::closeCategorySheet,
            )
        }
    }
}

@Composable
private fun AddBudgetToolbar(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .height(52.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.add_budget_title),
            style = Dastranj.type.title2,
            color = Dastranj.colors.title,
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp)
                .clip(CircleShape)
                .pressScaleClickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_x),
                contentDescription = stringResource(R.string.action_close),
                tint = Dastranj.colors.title,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun AmountBlock(amountText: String, words: String, isEmpty: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.add_budget_amount_label),
            style = Dastranj.type.label,
            color = Dastranj.colors.muted,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = amountText,
                style = Dastranj.type.amount,
                color = if (isEmpty) Dastranj.colors.faint else Dastranj.colors.title,
                maxLines = 1,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.currency_unit),
                style = Dastranj.type.title3,
                color = Dastranj.colors.muted,
            )
        }
        // Reserved height so the layout does not jump as the words appear.
        Box(
            modifier = Modifier.heightIn(min = 20.dp).padding(top = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = words,
                style = Dastranj.type.caption,
                fontWeight = FontWeight.Medium,
                color = Dastranj.colors.muted,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CategoryField(category: CategoryOption?, onClick: () -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.add_budget_category_label),
            style = Dastranj.type.label,
            color = Dastranj.colors.muted,
            modifier = Modifier.padding(bottom = 8.dp, start = 2.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Dastranj.colors.card)
                .border(1.dp, Dastranj.colors.hairline, RoundedCornerShape(16.dp))
                .pressScaleClickable(onClick = onClick)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (category != null) {
                val accent = remember(category.colorHex) {
                    Color(HexColor.parseOr(category.colorHex, HexColor.FALLBACK))
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(IconRegistry.drawableFor(category.iconName)),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = category?.name
                    ?: stringResource(R.string.add_budget_category_placeholder),
                style = Dastranj.type.body,
                color = if (category == null) Dastranj.colors.faint else Dastranj.colors.title,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = null,
                tint = Dastranj.colors.faint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * The warning-threshold slider — the design's `range` control, 50–95 in steps of 5.
 *
 * Configured with discrete stops rather than a free range: only the ten multiples of five are
 * meaningful, and a slider that lands on 82 would put the budget bar's warning tick somewhere the
 * design never draws it. The ViewModel snaps the reported value as well, so the stored threshold is
 * always exactly one of the allowed options regardless of what the widget emits mid-drag.
 *
 * The current value is shown above the track, because a slider with no readout makes the user
 * guess which stop they landed on.
 */
@Composable
private fun ThresholdSlider(selected: Int, onSelect: (Float) -> Unit) {
    val valueLabel = stringResource(
        R.string.add_budget_percent,
        PersianNumbers.toPersianDigits(selected.toString()),
    )
    val fieldLabel = stringResource(R.string.add_budget_threshold_label)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp, start = 2.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = fieldLabel,
                style = Dastranj.type.label,
                color = Dastranj.colors.muted,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueLabel,
                style = Dastranj.type.label,
                fontWeight = FontWeight.Bold,
                color = Dastranj.colors.title,
            )
        }

        Slider(
            value = selected.toFloat(),
            onValueChange = onSelect,
            valueRange = AddBudgetUiState.THRESHOLD_MIN.toFloat()..
                AddBudgetUiState.THRESHOLD_MAX.toFloat(),
            steps = AddBudgetUiState.THRESHOLD_SLIDER_STEPS,
            colors = SliderDefaults.colors(
                thumbColor = Dastranj.colors.brand,
                activeTrackColor = Dastranj.colors.brand,
                inactiveTrackColor = Dastranj.colors.sunken,
                activeTickColor = Ink0,
                inactiveTickColor = Dastranj.colors.faint,
            ),
            // One node speaking «هشدار در، ۸۰٪», rather than an unlabelled slider reading a bare
            // number.
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = "$fieldLabel، $valueLabel"
            },
        )
    }
}

@Composable
private fun RepeatToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Dastranj.colors.card)
            .border(1.dp, Dastranj.colors.hairline, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.add_budget_repeat_label),
            style = Dastranj.type.body,
            color = Dastranj.colors.title,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SaveBar(enabled: Boolean, onSave: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Dastranj.colors.card)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) Dastranj.colors.brandGradient
                    else androidx.compose.ui.graphics.SolidColor(Dastranj.colors.sunken),
                )
                .pressScaleClickable(onClick = onSave, enabled = enabled),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.add_budget_save),
                style = Dastranj.type.title3,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Ink0 else Dastranj.colors.faint,
            )
        }
    }
}

@Composable
private fun CategorySheet(
    categories: List<CategoryOption>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Dastranj.colors.scrim)
            .pressScaleClickable(onClick = onDismiss, scaleTo = 1f),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Dastranj.shapes.sheetShape)
                .background(Dastranj.colors.card)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.add_budget_category_placeholder),
                style = Dastranj.type.title2,
                color = Dastranj.colors.title,
            )
            Spacer(Modifier.height(12.dp))

            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(categories, key = { it.id }) { category ->
                    val accent = Color(HexColor.parseOr(category.colorHex, HexColor.FALLBACK))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .pressScaleClickable(onClick = { onSelect(category.id) })
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accent.copy(alpha = 0.13f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(
                                    IconRegistry.drawableFor(category.iconName),
                                ),
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = category.name,
                            style = Dastranj.type.body,
                            color = Dastranj.colors.title,
                            modifier = Modifier.weight(1f),
                        )
                        if (category.id == selectedId) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = Dastranj.colors.brand,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
