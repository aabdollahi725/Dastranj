package ir.dastranj.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dastranj.app.R
import ir.dastranj.app.ui.components.dashedBorder
import ir.dastranj.app.ui.components.pressScaleClickable
import ir.dastranj.app.ui.theme.Dastranj

/**
 * Home (`Dastranj Home Screen.dc.html`).
 *
 * v1 Home is **accounts only**. The design's «مدیریت» link and the tap target on each account card
 * both went nowhere — there is no account-detail or account-list screen in this release — so per
 * PRD §5.3 they are removed rather than shipped dead. The cards are therefore presentation, not
 * controls, and only the dashed "add account" card is interactive.
 */
@Composable
fun HomeScreen(
    onAddAccount: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val cashLabel = stringResource(R.string.home_wallet_label)
    val currencyUnit = stringResource(R.string.currency_unit)

    // The ViewModel cannot resolve resources itself, so the screen hands them over once.
    LaunchedEffect(cashLabel, currencyUnit) {
        viewModel.start(cashLabel = cashLabel, currencyUnit = currencyUnit)
    }

    Column(Modifier.fillMaxWidth()) {
        SectionHeader()

        when {
            state.showAccountRow -> AccountRow(
                accounts = state.accounts,
                onAddAccount = onAddAccount,
            )
            state.showEmptyState -> EmptyAccounts(onAddAccount = onAddAccount)
            // Loading: draw nothing. The first emission is a local database read, so a spinner
            // would flash for a frame and read as jank rather than as progress.
            else -> Unit
        }
    }
}

@Composable
private fun SectionHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_accounts_title),
            style = Dastranj.type.title3,
            color = Dastranj.colors.title,
        )
    }
}

/**
 * The horizontally snapping row of account cards, ending in the dashed add card.
 *
 * The row bleeds to the screen edges — the design uses `margin-inline:-20px` with matching padding
 * — so a card can sit flush against the edge while the content still aligns to the 20dp gutter.
 * `contentPadding` reproduces that without the negative margin.
 */
@Composable
private fun AccountRow(
    accounts: List<AccountCard>,
    onAddAccount: () -> Unit,
) {
    val listState = rememberLazyListState()

    LazyRow(
        state = listState,
        // `scroll-snap-type: x mandatory` in the design.
        flingBehavior = rememberSnapFlingBehavior(listState),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        itemsIndexed(accounts, key = { _, account -> account.id }) { _, account ->
            AccountCardView(account)
        }
        item(key = ADD_CARD_KEY) {
            AddAccountCard(onClick = onAddAccount)
        }
    }
}

@Composable
private fun AccountCardView(account: AccountCard) {
    val colors = Dastranj.colors

    Column(
        modifier = Modifier
            .width(232.dp)
            .shadow(Dastranj.elevation.card, RoundedCornerShape(Dastranj.shapes.card))
            .clip(RoundedCornerShape(Dastranj.shapes.card))
            .background(colors.card)
            .border(1.dp, colors.ring, RoundedCornerShape(Dastranj.shapes.card))
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp)
            // One node per card. Without merging, TalkBack would read the bank, the mask and the
            // number as three unrelated items.
            .semantics(mergeDescendants = true) {
                contentDescription = account.contentDescription
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.sunken),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_landmark),
                    contentDescription = null,
                    tint = colors.title,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = account.title,
                    style = Dastranj.type.bodySm,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = account.maskedLabel,
                    style = Dastranj.type.micro,
                    color = colors.faint,
                    // The design tracks the masked digits slightly wider so the dots read as a
                    // group rather than as noise.
                    letterSpacing = 0.04.em,
                    maxLines = 1,
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = 18.dp, start = 2.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = account.balanceText,
                style = Dastranj.type.amount,
                color = colors.title,
                maxLines = 1,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.currency_unit),
                style = Dastranj.type.caption,
                fontWeight = FontWeight.SemiBold,
                color = colors.muted,
            )
        }
    }
}

/** The dashed brand-outlined card that ends the row. */
@Composable
private fun AddAccountCard(onClick: () -> Unit) {
    val colors = Dastranj.colors
    val label = stringResource(R.string.home_add_account)

    Column(
        modifier = Modifier
            .width(132.dp)
            .height(ADD_CARD_HEIGHT)
            .clip(RoundedCornerShape(Dastranj.shapes.card))
            .dashedBorder(colors.brand, Dastranj.shapes.card)
            .pressScaleClickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = label },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = null,
            tint = colors.brandInk,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = Dastranj.type.label,
            fontWeight = FontWeight.SemiBold,
            color = colors.brandInk,
        )
    }
}

@Composable
private fun EmptyAccounts(onAddAccount: () -> Unit) {
    val colors = Dastranj.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 60.dp)
            .shadow(Dastranj.elevation.card, RoundedCornerShape(Dastranj.shapes.card))
            .clip(RoundedCornerShape(Dastranj.shapes.card))
            .background(colors.card)
            .border(1.dp, colors.ring, RoundedCornerShape(Dastranj.shapes.card))
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
                painter = painterResource(R.drawable.ic_landmark),
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(26.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.home_empty_title),
            style = Dastranj.type.title3,
            color = colors.title,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.home_empty_body),
            style = Dastranj.type.bodySm,
            color = colors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(262.dp),
        )

        Spacer(Modifier.height(22.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(CircleShape)
                .background(colors.cta)
                .pressScaleClickable(onClick = onAddAccount),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.home_empty_cta),
                style = Dastranj.type.title3,
                fontWeight = FontWeight.SemiBold,
                color = colors.ctaInk,
            )
        }
    }
}

/** Matches the account cards' height so the row's items align. */
private val ADD_CARD_HEIGHT = 132.dp

private const val ADD_CARD_KEY = "add_account_card"
