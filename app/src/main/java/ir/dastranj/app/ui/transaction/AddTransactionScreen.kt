package ir.dastranj.app.ui.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dastranj.app.R
import ir.dastranj.app.data.db.entity.TransactionType
import ir.dastranj.app.ui.components.NumericKeypad
import ir.dastranj.app.ui.components.pressScaleClickable
import ir.dastranj.app.ui.theme.Dastranj
import ir.dastranj.app.ui.theme.Ink0
import ir.dastranj.app.ui.util.HexColor
import ir.dastranj.app.ui.util.IconRegistry

/**
 * Add-transaction screen (`Dastranj Add Transaction Screen.dc.html`) — the app's central flow.
 *
 * Order, top to bottom: type segmented control, the amount with its sign and spelled-out reading,
 * the nine-tile category grid, the meta chips (account / date / note / receipt), then a sticky
 * submit bar with the keypad above it.
 *
 * The amount sits directly under the type control and above everything else because it is the one
 * field the user always fills; the keypad is always open on entry so the flow starts on the
 * keystroke rather than on a tap.
 */
@Composable
fun AddTransactionScreen(
    onClose: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recentNotes by viewModel.recentNotes.collectAsStateWithLifecycle()

    // Side effect, so it belongs here rather than in the composition body.
    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Dastranj.colors.page),
    ) {
        Column(Modifier.fillMaxSize()) {
            TransactionToolbar(onClose = onClose)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
            ) {
                TypeSegmentedControl(
                    selected = state.type,
                    onSelect = viewModel::setType,
                )
                Spacer(Modifier.height(14.dp))

                AmountDisplay(
                    state = state,
                    onClick = viewModel::focusAmount,
                )
                Spacer(Modifier.height(10.dp))

                if (state.isTransfer) {
                    TransferLegs(
                        fromTitle = state.selectedAccount?.title,
                        toTitle = state.transferToAccount?.title,
                        onPickFrom = { viewModel.openSheet(TransactionSheet.TRANSFER_FROM) },
                        onPickTo = { viewModel.openSheet(TransactionSheet.TRANSFER_TO) },
                        onSwap = viewModel::swapTransferLegs,
                    )
                } else {
                    CategoryGrid(
                        tiles = state.categoryTiles(),
                        showHint = state.selectedCategoryId == null,
                        onSelect = viewModel::selectCategory,
                        onOpenSheet = { viewModel.openSheet(TransactionSheet.CATEGORY) },
                    )
                }

                Spacer(Modifier.height(12.dp))

                MetaChips(
                    state = state,
                    onOpenSheet = viewModel::openSheet,
                )
            }

            SubmitBar(
                enabled = state.canSubmit && !state.saving,
                onSubmit = viewModel::submit,
            )
        }

        TransactionSheetHost(
            state = state,
            onSelectCategory = viewModel::selectCategory,
            onSelectAccount = viewModel::selectAccount,
            onSelectTransferTo = viewModel::selectTransferTo,
            onSelectNote = viewModel::setNote,
            onDismiss = viewModel::closeSheet,
            recentNotes = recentNotes,
        )

        AnimatedVisibility(
            visible = state.keypadOpen && state.openSheet == null,
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
    }
}

@Composable
private fun TransactionToolbar(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .height(52.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.add_transaction_title),
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

/** هزینه / درآمد / انتقال. A sunken track with a raised pill on the active segment. */
@Composable
private fun TypeSegmentedControl(
    selected: TransactionType,
    onSelect: (TransactionType) -> Unit,
) {
    val options = listOf(
        TransactionType.EXPENSE to R.string.tx_type_expense,
        TransactionType.INCOME to R.string.tx_type_income,
        TransactionType.TRANSFER to R.string.tx_type_transfer,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Dastranj.colors.sunken)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (type, labelRes) ->
            val active = type == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(if (active) Dastranj.colors.card else Color.Transparent)
                    .pressScaleClickable(
                        onClick = { onSelect(type) },
                        role = androidx.compose.ui.semantics.Role.Tab,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = Dastranj.type.label,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) Dastranj.colors.title else Dastranj.colors.muted,
                )
            }
        }
    }
}

/**
 * The amount line: sign, figure, unit, and the spelled-out reading underneath.
 *
 * The whole block is one accessibility node reading «مبلغ، چهارصد و هشتاد هزار تومان» — the words
 * are the unambiguous form, so they are what a screen reader should say rather than the digits.
 */
@Composable
private fun AmountDisplay(state: AddTransactionUiState, onClick: () -> Unit) {
    val colors = Dastranj.colors
    val signColor = when (state.type) {
        TransactionType.INCOME -> colors.moneyIn
        TransactionType.EXPENSE -> colors.moneyOut
        TransactionType.TRANSFER -> colors.moneyNeutral
    }

    val amountLabel = stringResource(R.string.tx_amount_a11y)
    val spoken = state.amountInWords.ifEmpty { state.amountFormatted }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleClickable(onClick = onClick, scaleTo = 1f)
            .semantics(mergeDescendants = true) {
                contentDescription = "$amountLabel، $spoken"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            state.signGlyph?.let { glyph ->
                Text(
                    text = glyph,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = signColor,
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = state.amountFormatted,
                style = Dastranj.type.amount,
                fontSize = 40.sp,
                color = if (state.isAmountEmpty) colors.faint else colors.title,
                maxLines = 1,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.currency_unit),
                style = Dastranj.type.title3,
                color = colors.muted,
            )
        }

        // Reserved height, so the layout does not jump as words appear and disappear.
        Box(
            modifier = Modifier
                .heightIn(min = 20.dp)
                .padding(top = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = state.amountInWords,
                style = Dastranj.type.caption,
                fontWeight = FontWeight.Medium,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Five columns of tiles, nine in total. */
@Composable
private fun CategoryGrid(
    tiles: List<CategoryTile>,
    showHint: Boolean,
    onSelect: (Long) -> Unit,
    onOpenSheet: () -> Unit,
) {
    Column {
        if (showHint) {
            Text(
                text = stringResource(R.string.tx_category_hint),
                style = Dastranj.type.micro,
                color = Dastranj.colors.faint,
                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp),
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 172.dp),
        ) {
            items(tiles, key = { it.category?.id ?: OTHER_TILE_KEY }) { tile ->
                CategoryTileView(
                    tile = tile,
                    onClick = {
                        val category = tile.category
                        if (tile.opensSheet || category == null) onOpenSheet()
                        else onSelect(category.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun CategoryTileView(tile: CategoryTile, onClick: () -> Unit) {
    val colors = Dastranj.colors
    val accent = remember(tile.colorHex) { parseHexColor(tile.colorHex) }

    Column(
        modifier = Modifier
            .height(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (tile.selected) colors.brandTint else colors.card)
            .then(
                if (tile.selected) {
                    Modifier.border(2.dp, colors.brand, RoundedCornerShape(16.dp))
                } else {
                    Modifier.border(1.dp, colors.hairline, RoundedCornerShape(16.dp))
                },
            )
            .pressScaleClickable(
                onClick = onClick,
                role = androidx.compose.ui.semantics.Role.RadioButton,
            )
            .semantics(mergeDescendants = true) { contentDescription = tile.label },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (tile.selected) colors.brand else accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(IconRegistry.drawableFor(tile.iconName)),
                contentDescription = null,
                tint = if (tile.selected) Ink0 else accent,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = tile.label,
            style = Dastranj.type.micro,
            fontWeight = if (tile.selected) FontWeight.Bold else FontWeight.Medium,
            color = if (tile.selected) colors.title else colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

/** «از حساب» / «به حساب» with a swap button between them. */
@Composable
private fun TransferLegs(
    fromTitle: String?,
    toTitle: String?,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit,
    onSwap: () -> Unit,
) {
    Box {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TransferLegRow(
                caption = stringResource(R.string.tx_transfer_from),
                value = fromTitle,
                iconRes = R.drawable.ic_arrow_up_left,
                onClick = onPickFrom,
            )
            TransferLegRow(
                caption = stringResource(R.string.tx_transfer_to),
                value = toTitle,
                iconRes = R.drawable.ic_arrow_down_right,
                onClick = onPickTo,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(Dastranj.colors.page)
                .border(1.dp, Dastranj.colors.hairline, CircleShape)
                .pressScaleClickable(onClick = onSwap),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_down_up),
                contentDescription = stringResource(R.string.tx_transfer_swap),
                tint = Dastranj.colors.title,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun TransferLegRow(
    caption: String,
    value: String?,
    iconRes: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Dastranj.colors.card)
            .border(1.dp, Dastranj.colors.hairline, RoundedCornerShape(16.dp))
            .pressScaleClickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Dastranj.colors.muted,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(caption, style = Dastranj.type.micro, color = Dastranj.colors.faint)
            Text(
                text = value.orEmpty(),
                style = Dastranj.type.body,
                color = Dastranj.colors.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Account / date / note / receipt chips. The account chip is hidden for transfers. */
@Composable
private fun MetaChips(
    state: AddTransactionUiState,
    onOpenSheet: (TransactionSheet) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!state.isTransfer) {
            MetaChip(
                iconRes = R.drawable.ic_landmark,
                label = state.selectedAccount?.title
                    ?: stringResource(R.string.tx_meta_account),
                dimmed = state.selectedAccount == null,
                onClick = { onOpenSheet(TransactionSheet.ACCOUNT) },
            )
        }
        MetaChip(
            iconRes = R.drawable.ic_calendar,
            label = stringResource(R.string.tx_date_today),
            dimmed = false,
            onClick = { onOpenSheet(TransactionSheet.DATE) },
        )
        MetaChip(
            iconRes = R.drawable.ic_pen_line,
            label = state.note ?: stringResource(R.string.tx_meta_note),
            dimmed = state.note == null,
            onClick = { onOpenSheet(TransactionSheet.NOTE) },
        )
        MetaChip(
            iconRes = R.drawable.ic_paperclip,
            label = if (state.attachmentPath != null) {
                stringResource(R.string.tx_meta_attachment_one)
            } else {
                stringResource(R.string.tx_meta_attachment)
            },
            dimmed = state.attachmentPath == null,
            contentDescription = stringResource(R.string.tx_meta_attachment_a11y),
            onClick = { onOpenSheet(TransactionSheet.ATTACHMENT) },
        )
    }
}

@Composable
private fun MetaChip(
    iconRes: Int,
    label: String,
    dimmed: Boolean,
    onClick: () -> Unit,
    contentDescription: String? = null,
) {
    val description = contentDescription ?: label

    Row(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .background(Dastranj.colors.card)
            .border(1.dp, Dastranj.colors.hairline, CircleShape)
            .pressScaleClickable(onClick = onClick)
            .semantics(mergeDescendants = true) { this.contentDescription = description }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (dimmed) Dastranj.colors.faint else Dastranj.colors.body,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = Dastranj.type.label,
            color = if (dimmed) Dastranj.colors.faint else Dastranj.colors.body,
            maxLines = 1,
        )
    }
}

/**
 * The sticky submit bar.
 *
 * Enabled, it carries the brand gradient and its green glow — the one gradient on this screen, per
 * the DS guide's one-per-screen cap. Disabled, it drops to a flat sunken surface rather than a
 * faded gradient, so "not ready" reads as a different thing rather than a dimmer version.
 */
@Composable
private fun SubmitBar(enabled: Boolean, onSubmit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Dastranj.colors.card)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(CircleShape)
                // Enabled ships the brand gradient — this screen's one use of it, per the DS
                // guide's one-gradient-per-screen cap. Disabled drops to a flat sunken surface
                // rather than a faded gradient, so "not ready yet" reads as a different state
                // rather than a dimmer version of the same one.
                .background(
                    if (enabled) Dastranj.colors.brandGradient else SolidColor(Dastranj.colors.sunken),
                )
                .pressScaleClickable(onClick = onSubmit, enabled = enabled),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.tx_submit),
                style = Dastranj.type.title3,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Ink0 else Dastranj.colors.faint,
            )
        }
    }
}

/** Stable key for the «سایر» tile, which has no category id. */
private const val OTHER_TILE_KEY = -1L

/**
 * A stored colour string as a [Color], never throwing.
 *
 * Category colours are data, and a malformed one must not take down the grid while it renders —
 * see [HexColor] for the reasoning.
 */
private fun parseHexColor(hex: String): Color =
    // Compose's Color(Long) overload reads the low 32 bits as 0xAARRGGBB, which is the shape
    // HexColor already returns. The ULong constructor takes Compose's own packed representation
    // instead, so it must not be used here.
    Color(HexColor.parseOr(hex, HexColor.FALLBACK))
