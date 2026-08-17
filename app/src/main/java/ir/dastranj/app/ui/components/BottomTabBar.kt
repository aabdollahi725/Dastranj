package ir.dastranj.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ir.dastranj.app.R
import ir.dastranj.app.ui.navigation.TopLevelTab
import ir.dastranj.app.ui.theme.Dastranj
import ir.dastranj.app.ui.theme.Ink0
import ir.dastranj.app.ui.theme.Ink400
import ir.dastranj.app.ui.theme.Ink900

/**
 * The floating bottom bar: a dark pill holding four tabs, with a separate gradient FAB beside it.
 *
 * Geometry is taken from `Dastranj Home.dc.html` — 18dp from the bottom, 20dp side padding, 10dp
 * between the nav and the FAB, 6dp inner padding, 52dp tab height, 56dp FAB.
 *
 * The bar is [Ink900] in **both** themes. That is not an oversight in the design: the bar is a
 * floating object over the page rather than a surface of it, so it keeps one colour and the pill
 * stays white throughout.
 */
@Composable
fun BottomTabBar(
    selectedTab: TopLevelTab,
    onTabSelected: (TopLevelTab) -> Unit,
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = Dastranj.spacing
    val motion = Dastranj.motion

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.x5),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .shadow(Dastranj.elevation.raised, CircleShape)
                .clip(CircleShape)
                .background(Ink900)
                .padding(6.dp),
        ) {
            // The pill spans one quarter of the inner width; the design computes this as
            // `calc((100% - 12px) / 4)` where 12dp is the 6dp padding on both sides.
            val tabWidth = maxWidth / TopLevelTab.entries.size

            // In RTL the first tab sits at the right edge, so the pill's offset is measured from
            // the start edge and Compose mirrors it — `offset` here is direction-aware.
            val pillOffset by animateDpAsState(
                targetValue = tabWidth * selectedTab.ordinal,
                animationSpec = tween(260, easing = motion.easeOut),
                label = "tabPillOffset",
            )

            Box(
                modifier = Modifier
                    .offset(x = pillOffset)
                    .width(tabWidth)
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Ink0),
            )

            Row(Modifier.fillMaxWidth()) {
                TopLevelTab.entries.forEach { tab ->
                    TabItem(
                        tab = tab,
                        selected = tab == selectedTab,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        AddTransactionFab(onClick = onAddTransaction)
    }
}

@Composable
private fun TabItem(
    tab: TopLevelTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(tab.labelRes)

    // The label sits directly under the icon, so the icon must not also be announced.
    val contentColor by animateColorAsState(
        targetValue = if (selected) Ink900 else Ink400,
        animationSpec = tween(Dastranj.motion.base, easing = Dastranj.motion.easeOut),
        label = "tabColor",
    )

    Column(
        modifier = modifier
            .height(52.dp)
            .clip(CircleShape)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                // No ripple: the design's only feedback is the pill sliding and the colour change.
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(tab.iconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = Dastranj.type.micro,
            color = contentColor,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * The brand gradient's single appearance on the main screens — the DS guide caps the gradient at
 * one use per screen, and this is it.
 */
@Composable
private fun AddTransactionFab(onClick: () -> Unit) {
    val colors = Dastranj.colors

    Box(
        modifier = Modifier
            .size(56.dp)
            // `--sh-brand` is a green glow rather than a neutral drop shadow, so the brand colour
            // is carried into the shadow itself.
            .shadow(
                elevation = Dastranj.elevation.brand,
                shape = CircleShape,
                ambientColor = colors.brand,
                spotColor = colors.brand,
            )
            .clip(CircleShape)
            .background(colors.brandGradient)
            .pressScaleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = stringResource(R.string.action_add_transaction),
            tint = Ink0,
            modifier = Modifier.size(24.dp),
        )
    }
}
