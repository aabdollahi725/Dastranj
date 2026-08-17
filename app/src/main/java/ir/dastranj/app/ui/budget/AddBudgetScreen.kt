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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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

                ThresholdPicker(
                    selected = state.thresholdPercent,
                    onSelect = viewModel::setThreshold,
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

/** The design's 50–95 threshold range, as chips rather than a slider. */
@Composable
private fun ThresholdPicker(selected: Int, onSelect: (Int) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.add_budget_threshold_label),
            style = Dastranj.type.label,
            color = Dastranj.colors.muted,
            modifier = Modifier.padding(bottom = 8.dp, start = 2.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AddBudgetUiState.THRESHOLD_OPTIONS.forEach { percent ->
                val active = percent == selected
                val label = stringResource(
                    R.string.add_budget_percent,
                    PersianNumbers.toPersianDigits(percent.toString()),
                )
                Box(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(CircleShape)
                        .background(if (active) Dastranj.colors.brandTint else Dastranj.colors.card)
                        .border(
                            width = if (active) 2.dp else 1.dp,
                            color = if (active) Dastranj.colors.brand else Dastranj.colors.hairline,
                            shape = CircleShape,
                        )
                        .pressScaleClickable(
                            onClick = { onSelect(percent) },
                            role = androidx.compose.ui.semantics.Role.RadioButton,
                        )
                        .semantics(mergeDescendants = true) { contentDescription = label }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = Dastranj.type.label,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) Dastranj.colors.title else Dastranj.colors.muted,
                    )
                }
            }
        }
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
