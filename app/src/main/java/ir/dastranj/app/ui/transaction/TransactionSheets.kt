package ir.dastranj.app.ui.transaction

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.dastranj.app.R
import ir.dastranj.app.ui.components.pressScaleClickable
import ir.dastranj.app.ui.theme.Dastranj
import ir.dastranj.app.ui.util.HexColor
import ir.dastranj.app.ui.util.IconRegistry

/**
 * The bottom sheets behind the add-transaction meta chips and the «سایر» category tile.
 *
 * One dispatcher rather than a sheet per case: they share the scrim, the rounded top, the inset
 * handling and the dismiss behaviour, and only their contents differ.
 */
@Composable
fun TransactionSheetHost(
    state: AddTransactionUiState,
    onSelectCategory: (Long) -> Unit,
    onSelectAccount: (Long) -> Unit,
    onSelectTransferTo: (Long) -> Unit,
    onSelectNote: (String?) -> Unit,
    onDismiss: () -> Unit,
    recentNotes: List<String>,
) {
    val sheet = state.openSheet ?: return

    SheetScaffold(
        title = stringResource(
            when (sheet) {
                TransactionSheet.CATEGORY -> R.string.tx_sheet_category
                TransactionSheet.ACCOUNT -> R.string.tx_sheet_account
                TransactionSheet.TRANSFER_FROM -> R.string.tx_sheet_from
                TransactionSheet.TRANSFER_TO -> R.string.tx_sheet_to
                TransactionSheet.DATE -> R.string.tx_sheet_date
                TransactionSheet.NOTE -> R.string.tx_sheet_note
                TransactionSheet.ATTACHMENT -> R.string.tx_sheet_attachment
            },
        ),
        onDismiss = onDismiss,
    ) {
        when (sheet) {
            TransactionSheet.CATEGORY -> CategoryList(
                categories = state.categoriesForType,
                selectedId = state.selectedCategoryId,
                onSelect = onSelectCategory,
            )

            TransactionSheet.ACCOUNT -> AccountList(
                accounts = state.accounts,
                selectedId = state.accountId,
                onSelect = onSelectAccount,
            )

            TransactionSheet.TRANSFER_FROM -> AccountList(
                accounts = state.accounts,
                selectedId = state.accountId,
                onSelect = onSelectAccount,
            )

            TransactionSheet.TRANSFER_TO -> AccountList(
                // The source cannot also be the destination, so it is not offered.
                accounts = state.accounts.filter { it.id != state.accountId },
                selectedId = state.transferToAccountId,
                onSelect = onSelectTransferTo,
            )

            TransactionSheet.NOTE -> NoteList(
                notes = recentNotes,
                selected = state.note,
                onSelect = onSelectNote,
            )

            // Date and attachment need a Jalali date picker and a camera/gallery pipeline
            // respectively — both are subsystems rather than list sheets, and neither is built yet.
            TransactionSheet.DATE, TransactionSheet.ATTACHMENT -> NotYetAvailable()
        }
    }
}

@Composable
private fun SheetScaffold(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Dastranj.colors.scrim)
            // Tapping the scrim dismisses; scaleTo = 1f so the whole page does not flinch.
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
                text = title,
                style = Dastranj.type.title2,
                color = Dastranj.colors.title,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun CategoryList(
    categories: List<CategoryOption>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
) {
    LazyColumn(Modifier.heightIn(max = SHEET_LIST_MAX_HEIGHT)) {
        items(categories, key = { it.id }) { category ->
            val accent = Color(HexColor.parseOr(category.colorHex, HexColor.FALLBACK))
            SheetRow(
                label = category.name,
                selected = category.id == selectedId,
                onClick = { onSelect(category.id) },
                leading = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(alpha = 0.13f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(IconRegistry.drawableFor(category.iconName)),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun AccountList(
    accounts: List<AccountOption>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
) {
    LazyColumn(Modifier.heightIn(max = SHEET_LIST_MAX_HEIGHT)) {
        items(accounts, key = { it.id }) { account ->
            SheetRow(
                label = account.title,
                selected = account.id == selectedId,
                onClick = { onSelect(account.id) },
                leading = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Dastranj.colors.sunken),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(
                                if (account.isCash) R.drawable.ic_wallet
                                else R.drawable.ic_landmark,
                            ),
                            contentDescription = null,
                            tint = Dastranj.colors.title,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun NoteList(
    notes: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    LazyColumn(Modifier.heightIn(max = SHEET_LIST_MAX_HEIGHT)) {
        items(notes, key = { it }) { note ->
            SheetRow(
                label = note,
                selected = note == selected,
                onClick = { onSelect(note) },
                leading = {
                    Icon(
                        painter = painterResource(R.drawable.ic_pen_line),
                        contentDescription = null,
                        tint = Dastranj.colors.muted,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun SheetRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .pressScaleClickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = Dastranj.type.body,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
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

/** Placeholder for the two sheets whose underlying subsystems are not built. */
@Composable
private fun NotYetAvailable() {
    Text(
        text = stringResource(R.string.more_coming_soon),
        style = Dastranj.type.bodySm,
        color = Dastranj.colors.muted,
        modifier = Modifier.padding(vertical = 24.dp),
    )
}

private val SHEET_LIST_MAX_HEIGHT = 420.dp
