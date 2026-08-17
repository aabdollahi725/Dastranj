package ir.dastranj.app.ui.more

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.dastranj.app.R
import ir.dastranj.app.ui.theme.Dastranj

/**
 * More (`Dastranj More Screen.dc.html`).
 *
 * Every row here is a v2 feature. Per PRD §5.3 they keep their «به‌زودی» badge rather than being
 * removed — unlike the dead controls elsewhere in the app, these communicate something true and
 * useful about what is coming, and the design's own screen is built around them.
 *
 * Two consequences follow, and both are deliberate:
 *
 * - **The rows are not interactive.** They are not disabled buttons that do nothing on tap; they
 *   carry no click handler at all, so there is no press feedback promising a destination.
 * - **The screen has no ViewModel and touches no data.** The design's badges show live counts
 *   («۱ قسط سررسید گذشته», «۳ چک نزدیک سررسید») for features that do not exist in v1, so those are
 *   replaced by the uniform "coming soon" badge rather than being wired to invented numbers.
 */
@Composable
fun MoreScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 150.dp),
    ) {
        item(key = "card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(Dastranj.elevation.card, RoundedCornerShape(Dastranj.shapes.card))
                    .clip(RoundedCornerShape(Dastranj.shapes.card))
                    .background(Dastranj.colors.card)
                    .padding(horizontal = 16.dp),
            ) {
                MoreItem.entries.forEachIndexed { index, item ->
                    MoreRow(item = item, showDivider = index > 0)
                }
            }
        }
    }
}

/** The five v2 features listed in the design, in its order. */
private enum class MoreItem(
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
) {
    PIGGY(R.drawable.ic_piggy_bank, R.string.more_piggy_title, R.string.more_piggy_sub),
    GOALS(R.drawable.ic_target, R.string.more_goals_title, R.string.more_goals_sub),
    DEBTS(R.drawable.ic_handshake, R.string.more_debts_title, R.string.more_debts_sub),
    LOANS(R.drawable.ic_banknote, R.string.more_loans_title, R.string.more_loans_sub),
    CHEQUES(R.drawable.ic_scroll_text, R.string.more_cheques_title, R.string.more_cheques_sub),
}

@Composable
private fun MoreRow(item: MoreItem, showDivider: Boolean) {
    val colors = Dastranj.colors
    val title = stringResource(item.titleRes)
    val subtitle = stringResource(item.subtitleRes)
    val comingSoon = stringResource(R.string.more_coming_soon)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(vertical = 14.dp)
            // One node, announced as unavailable — so a screen-reader user learns the row is not
            // actionable instead of trying to activate it.
            .semantics(mergeDescendants = true) {
                contentDescription = "$title، $subtitle، $comingSoon"
                disabled()
            },
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
                painter = painterResource(item.iconRes),
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = Dastranj.type.bodySm,
                fontWeight = FontWeight.SemiBold,
                color = colors.title,
            )
            Text(
                text = subtitle,
                style = Dastranj.type.micro,
                color = colors.faint,
            )
        }

        Spacer(Modifier.width(8.dp))

        ComingSoonBadge(label = comingSoon)
    }
}

@Composable
private fun ComingSoonBadge(label: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Dastranj.colors.sunken)
            .padding(start = 8.dp, end = 9.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lock),
            contentDescription = null,
            tint = Dastranj.colors.muted,
            modifier = Modifier.size(11.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = Dastranj.type.micro,
            fontWeight = FontWeight.SemiBold,
            color = Dastranj.colors.muted,
        )
    }
}
