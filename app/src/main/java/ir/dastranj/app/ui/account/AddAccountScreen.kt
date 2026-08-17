package ir.dastranj.app.ui.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dastranj.app.R
import ir.dastranj.app.data.seed.Bank
import ir.dastranj.app.data.seed.BankCatalog
import ir.dastranj.app.ui.components.NumericKeypad
import ir.dastranj.app.ui.components.fieldSurface
import ir.dastranj.app.ui.components.pressScaleClickable
import ir.dastranj.app.ui.theme.Dastranj
import ir.dastranj.app.ui.util.HexColor
import ir.dastranj.app.ui.util.PersianNumbers

/**
 * Add-account screen (`Dastranj Add Account Screen.dc.html`).
 *
 * Structure, top to bottom: a live preview card, then the bank / title / last-four / balance
 * fields, the card-theme picker, and a sticky action bar. The bank picker and the numeric keypad
 * are overlays.
 *
 * The preview card is the reason the form is ordered this way — every field the user fills is
 * reflected in the card immediately above it, so they are always looking at the thing they are
 * building rather than at a form.
 */
@Composable
fun AddAccountScreen(
    onClose: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: AddAccountViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigation is a side effect, so it belongs in LaunchedEffect rather than the composition
    // body — calling onSaved inline would re-fire it on every recomposition after the save.
    LaunchedEffect(state.savedAccountId) {
        state.savedAccountId?.let(onSaved)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Dastranj.colors.page),
    ) {
        Column(Modifier.fillMaxSize()) {
            AddAccountToolbar(onClose = onClose)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                AccountPreviewCard(state = state)

                BankField(
                    bank = state.selectedBank,
                    onClick = viewModel::openBankSheet,
                )

                TitleField(
                    title = state.title,
                    onTitleChange = viewModel::onTitleChange,
                    onChipSelected = viewModel::onTitleChange,
                )

                Last4Field(
                    last4 = state.last4,
                    showError = state.showLast4Error,
                    focused = state.focusedNumericField == NumericField.LAST4,
                    onFocus = { viewModel.focusField(NumericField.LAST4) },
                )

                BalanceField(
                    balanceToman = state.balanceToman,
                    words = state.balanceInWords,
                    focused = state.focusedNumericField == NumericField.BALANCE,
                    onFocus = { viewModel.focusField(NumericField.BALANCE) },
                )

                ThemePicker(
                    selected = state.cardTheme,
                    onSelect = viewModel::selectTheme,
                )

                Spacer(Modifier.height(8.dp))
            }

            ActionBar(
                enabled = state.canSave && !state.saving,
                onSave = viewModel::save,
            )
        }

        // The keypad sits above the action bar and only while a numeric field has focus.
        AnimatedVisibility(
            visible = state.focusedNumericField != null && !state.bankSheetOpen,
            enter = slideInVertically(tween(Dastranj.motion.base)) { it },
            exit = slideOutVertically(tween(Dastranj.motion.fast)) { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            NumericKeypad(
                onDigit = viewModel::onKeypadDigit,
                onDelete = viewModel::onKeypadDelete,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
            )
        }

        if (state.bankSheetOpen) {
            BankPickerSheet(
                banks = state.filteredBanks,
                query = state.bankQuery,
                selected = state.selectedBank,
                onQueryChange = viewModel::onBankQueryChange,
                onSelect = viewModel::selectBank,
                onDismiss = viewModel::closeBankSheet,
            )
        }
    }
}

@Composable
private fun AddAccountToolbar(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .height(52.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.add_account_title),
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

/**
 * The live preview of the card being created.
 *
 * Colours come from [CardTheme] rather than the semantic palette: a card theme is the user's choice
 * about this one card, so «جوهری» stays dark even in light mode.
 */
@Composable
private fun AccountPreviewCard(state: AddAccountUiState) {
    val palette = state.cardTheme.palette()
    val bank = state.selectedBank

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(Dastranj.elevation.card, RoundedCornerShape(Dastranj.shapes.card))
            .clip(RoundedCornerShape(Dastranj.shapes.card))
            .background(palette.background)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.tileBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(
                        if (bank == null || bank.generic) R.drawable.ic_circle_dashed
                        else R.drawable.ic_landmark,
                    ),
                    contentDescription = null,
                    tint = palette.subdued,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = bank?.displayName ?: stringResource(R.string.add_account_bank_label),
                style = Dastranj.type.label,
                color = palette.subdued,
            )
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = state.balanceFormatted,
                style = Dastranj.type.amount,
                color = palette.ink,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.currency_unit),
                style = Dastranj.type.label,
                color = palette.subdued,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        Text(
            text = state.title.trim().ifBlank {
                stringResource(R.string.add_account_title_placeholder)
            },
            style = Dastranj.type.bodySm,
            color = palette.subdued,
        )
    }
}

@Composable
private fun BankField(bank: Bank?, onClick: () -> Unit) {
    FieldContainer(label = stringResource(R.string.add_account_bank_label)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(16.dp))
                .fieldSurface()
                .pressScaleClickable(onClick = onClick)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = bank?.displayName ?: stringResource(R.string.add_account_bank_placeholder),
                style = Dastranj.type.body,
                color = if (bank == null) Dastranj.colors.faint else Dastranj.colors.title,
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

@Composable
private fun TitleField(
    title: String,
    onTitleChange: (String) -> Unit,
    onChipSelected: (String) -> Unit,
) {
    val chips = listOf(
        stringResource(R.string.add_account_chip_salary),
        stringResource(R.string.add_account_chip_savings),
        stringResource(R.string.add_account_chip_daily),
        stringResource(R.string.add_account_chip_main_card),
    )

    FieldContainer(label = stringResource(R.string.add_account_title_label)) {
        PlainTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = stringResource(R.string.add_account_title_placeholder),
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            chips.forEach { chip ->
                SuggestionChip(label = chip, onClick = { onChipSelected(chip) })
            }
        }
    }
}

@Composable
private fun Last4Field(
    last4: String,
    showError: Boolean,
    focused: Boolean,
    onFocus: () -> Unit,
) {
    FieldContainer(label = stringResource(R.string.add_account_last4_label)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(16.dp))
                .fieldSurface(error = showError, focused = focused)
                .pressScaleClickable(onClick = onFocus)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = PersianNumbers.toPersianDigits(last4).ifEmpty { "••••" },
                style = Dastranj.type.body,
                color = if (last4.isEmpty()) Dastranj.colors.faint else Dastranj.colors.title,
            )
        }
        if (showError) {
            Text(
                text = stringResource(R.string.add_account_last4_error),
                style = Dastranj.type.micro,
                color = Dastranj.colors.danger,
                modifier = Modifier.padding(top = 8.dp, start = 2.dp),
            )
        }
    }
}

@Composable
private fun BalanceField(
    balanceToman: String,
    words: String,
    focused: Boolean,
    onFocus: () -> Unit,
) {
    FieldContainer(label = stringResource(R.string.add_account_balance_label)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(16.dp))
                .fieldSurface(focused = focused)
                .pressScaleClickable(onClick = onFocus)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = PersianNumbers.formatGrouped(balanceToman.toLongOrNull() ?: 0L)
                    .takeIf { balanceToman.isNotEmpty() } ?: "۰",
                style = Dastranj.type.body,
                color = if (balanceToman.isEmpty()) Dastranj.colors.faint else Dastranj.colors.title,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.currency_unit),
                style = Dastranj.type.label,
                color = Dastranj.colors.muted,
            )
        }

        // The amount in words — the app's guard against a mistyped zero.
        Text(
            text = words.ifEmpty { stringResource(R.string.add_account_balance_hint) },
            style = Dastranj.type.micro,
            color = if (words.isEmpty()) Dastranj.colors.faint else Dastranj.colors.muted,
            fontWeight = if (words.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp, start = 2.dp),
        )
    }
}

@Composable
private fun ThemePicker(selected: CardTheme, onSelect: (CardTheme) -> Unit) {
    FieldContainer(label = stringResource(R.string.add_account_theme_label)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CardTheme.entries.forEach { theme ->
                ThemeSwatch(
                    theme = theme,
                    selected = theme == selected,
                    onClick = { onSelect(theme) },
                )
            }
        }
    }
}

@Composable
private fun ThemeSwatch(theme: CardTheme, selected: Boolean, onClick: () -> Unit) {
    val palette = theme.palette()
    val label = stringResource(theme.labelRes())
    val description = stringResource(R.string.add_account_theme_a11y, label)

    Column(
        modifier = Modifier
            .width(76.dp)
            .pressScaleClickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(68.dp)
                .height(46.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(palette.background)
                .then(
                    if (selected) {
                        Modifier.border(2.dp, Dastranj.colors.brand, RoundedCornerShape(13.dp))
                    } else {
                        Modifier.border(1.dp, palette.hairline, RoundedCornerShape(13.dp))
                    },
                ),
        ) {
            // Two abstract text lines standing in for the card's content.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(9.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.68f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(palette.ink),
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.40f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(palette.subdued),
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = label,
            style = Dastranj.type.micro,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Dastranj.colors.title else Dastranj.colors.muted,
        )
    }
}

@Composable
private fun ActionBar(enabled: Boolean, onSave: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Dastranj.colors.card)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(CircleShape)
                .background(if (enabled) Dastranj.colors.cta else Dastranj.colors.sunken)
                .pressScaleClickable(onClick = onSave, enabled = enabled),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.add_account_save),
                style = Dastranj.type.title3,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Dastranj.colors.ctaInk else Dastranj.colors.faint,
            )
        }
    }
}

@Composable
private fun BankPickerSheet(
    banks: List<Bank>,
    query: String,
    selected: Bank?,
    onQueryChange: (String) -> Unit,
    onSelect: (Bank) -> Unit,
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
                text = stringResource(R.string.add_account_bank_sheet_title),
                style = Dastranj.type.title2,
                color = Dastranj.colors.title,
            )
            Spacer(Modifier.height(14.dp))

            PlainTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.add_account_bank_search_hint),
            )
            Spacer(Modifier.height(8.dp))

            if (banks.isEmpty()) {
                Text(
                    text = stringResource(R.string.add_account_bank_none),
                    style = Dastranj.type.bodySm,
                    color = Dastranj.colors.muted,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(banks, key = { it.id }) { bank ->
                        BankRow(
                            bank = bank,
                            selected = bank.id == selected?.id,
                            onClick = { onSelect(bank) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BankRow(bank: Bank, selected: Boolean, onClick: () -> Unit) {
    // Never throws on a malformed stored value — see HexColor.
    val brand = Color(HexColor.parseOr(bank.brandColorHex, HexColor.FALLBACK))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .pressScaleClickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                // The design tints the tile with the bank's own colour at 12% (0x1F).
                .background(brand.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(
                    if (bank.generic) R.drawable.ic_circle_dashed else R.drawable.ic_landmark,
                ),
                contentDescription = null,
                tint = brand,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = bank.displayName,
            style = Dastranj.type.body,
            color = Dastranj.colors.title,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = Dastranj.colors.brand,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ---- Small shared pieces --------------------------------------------------------------------

@Composable
private fun FieldContainer(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = label,
            style = Dastranj.type.label,
            color = Dastranj.colors.muted,
            modifier = Modifier.padding(bottom = 8.dp, start = 2.dp),
        )
        content()
    }
}

@Composable
private fun PlainTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .fieldSurface()
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = Dastranj.type.body.copy(color = Dastranj.colors.title),
            cursorBrush = SolidColor(Dastranj.colors.brand),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = Dastranj.type.body,
                color = Dastranj.colors.faint,
            )
        }
    }
}

@Composable
private fun SuggestionChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(Dastranj.colors.sunken)
            .pressScaleClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text = label, style = Dastranj.type.label, color = Dastranj.colors.body)
    }
}
