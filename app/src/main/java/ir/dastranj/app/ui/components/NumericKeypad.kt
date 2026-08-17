package ir.dastranj.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.dastranj.app.R
import ir.dastranj.app.ui.theme.Dastranj
import ir.dastranj.app.ui.util.PersianNumbers

/**
 * The in-app numeric keypad used by the add screens.
 *
 * The app supplies its own rather than using the IME because the amount field is the centre of the
 * add-transaction flow and the design gives it a fixed, always-visible layout — a system keyboard
 * would vary in height by device and vendor, which is exactly what the sticky submit button below
 * it cannot tolerate.
 *
 * Layout from the design: a 3×4 grid, 58dp rows, 25sp digits at weight 500, with «٬» and a delete
 * key on the bottom row.
 */
@Composable
fun NumericKeypad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Dastranj.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.card)
            .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        KeypadRow {
            KeyDigit('1', onDigit, Modifier.weight(1f))
            KeyDigit('2', onDigit, Modifier.weight(1f))
            KeyDigit('3', onDigit, Modifier.weight(1f))
        }
        KeypadRow {
            KeyDigit('4', onDigit, Modifier.weight(1f))
            KeyDigit('5', onDigit, Modifier.weight(1f))
            KeyDigit('6', onDigit, Modifier.weight(1f))
        }
        KeypadRow {
            KeyDigit('7', onDigit, Modifier.weight(1f))
            KeyDigit('8', onDigit, Modifier.weight(1f))
            KeyDigit('9', onDigit, Modifier.weight(1f))
        }
        KeypadRow {
            // The separator key is inert in the design — grouping is applied automatically, so
            // there is nothing for it to do. It is kept because removing it would leave a hole in
            // the grid and shift the 0 key away from where the thumb expects it.
            KeySeparator(Modifier.weight(1f))
            KeyDigit('0', onDigit, Modifier.weight(1f))
            KeyDelete(onDelete, Modifier.weight(1f))
        }
    }
}

@Composable
private fun KeypadRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        content = content,
    )
}

@Composable
private fun KeyDigit(digit: Char, onDigit: (Char) -> Unit, modifier: Modifier = Modifier) {
    val persian = PersianNumbers.toPersianDigits(digit.toString())

    KeySurface(
        modifier = modifier,
        onClick = { onDigit(digit) },
        contentDescription = persian,
    ) {
        Text(
            text = persian,
            fontFamily = ir.dastranj.app.ui.theme.IranYekanX,
            fontSize = 25.sp,
            fontWeight = FontWeight.Medium,
            color = Dastranj.colors.title,
        )
    }
}

@Composable
private fun KeySeparator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(KEY_HEIGHT)
            .padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = PersianNumbers.THOUSANDS_SEPARATOR.toString(),
            fontFamily = ir.dastranj.app.ui.theme.IranYekanX,
            fontSize = 25.sp,
            fontWeight = FontWeight.Medium,
            // The design dims this to .55 opacity to signal that it is not a live key.
            color = Dastranj.colors.muted.copy(alpha = 0.55f),
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

@Composable
private fun KeyDelete(onDelete: () -> Unit, modifier: Modifier = Modifier) {
    KeySurface(
        modifier = modifier,
        onClick = onDelete,
        contentDescription = stringResource(R.string.keypad_delete_a11y),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = null,
            tint = Dastranj.colors.title,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun KeySurface(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val label = contentDescription

    Box(
        modifier = modifier
            .height(KEY_HEIGHT)
            .padding(3.dp)
            .clip(RoundedCornerShape(10.dp))
            .pressScaleClickable(onClick = onClick)
            // `semantics`, not `clearAndSetSemantics`: the latter would also wipe the click action
            // and role that pressScaleClickable just set, leaving the key unreachable by TalkBack.
            // mergeDescendants folds the digit's own text into this one node instead.
            .semantics(mergeDescendants = true) { this.contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private val KEY_HEIGHT = 58.dp
