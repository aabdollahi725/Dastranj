package ir.dastranj.app.ui.util

import androidx.annotation.DrawableRes
import ir.dastranj.app.R

/**
 * Maps a stored Lucide slug to its bundled drawable.
 *
 * Categories store an icon *name* rather than a resource id, because a resource id is not stable
 * across builds — R fields are regenerated, so persisting one would silently point at a different
 * icon after an unrelated change. The name is stable and human-readable in a database dump.
 *
 * Generated from the drawables actually present in the project; keep it in step when icons are
 * added.
 */
object IconRegistry {

    private val icons: Map<String, Int> = mapOf(
        "accessibility" to R.drawable.ic_accessibility,
        "archive" to R.drawable.ic_archive,
        "arrow-down-right" to R.drawable.ic_arrow_down_right,
        "arrow-down-up" to R.drawable.ic_arrow_down_up,
        "arrow-up-left" to R.drawable.ic_arrow_up_left,
        "award" to R.drawable.ic_award,
        "baby" to R.drawable.ic_baby,
        "banknote" to R.drawable.ic_banknote,
        "bell" to R.drawable.ic_bell,
        "bell-off" to R.drawable.ic_bell_off,
        "bike" to R.drawable.ic_bike,
        "briefcase" to R.drawable.ic_briefcase,
        "bus" to R.drawable.ic_bus,
        "calendar" to R.drawable.ic_calendar,
        "calendar-check" to R.drawable.ic_calendar_check,
        "calendar-days" to R.drawable.ic_calendar_days,
        "camera" to R.drawable.ic_camera,
        "car" to R.drawable.ic_car,
        "car-front" to R.drawable.ic_car_front,
        "chart-column" to R.drawable.ic_chart_column,
        "chart-pie" to R.drawable.ic_chart_pie,
        "check" to R.drawable.ic_check,
        "chevron-left" to R.drawable.ic_chevron_left,
        "chevron-right" to R.drawable.ic_chevron_right,
        "circle-alert" to R.drawable.ic_circle_alert,
        "circle-check-big" to R.drawable.ic_circle_check_big,
        "circle-dashed" to R.drawable.ic_circle_dashed,
        "clock" to R.drawable.ic_clock,
        "delete" to R.drawable.ic_delete,
        "ellipsis" to R.drawable.ic_ellipsis,
        "eye" to R.drawable.ic_eye,
        "gift" to R.drawable.ic_gift,
        "graduation-cap" to R.drawable.ic_graduation_cap,
        "grid-3x3" to R.drawable.ic_grid_3x3,
        "hand-coins" to R.drawable.ic_hand_coins,
        "handshake" to R.drawable.ic_handshake,
        "heart-pulse" to R.drawable.ic_heart_pulse,
        "history" to R.drawable.ic_history,
        "house" to R.drawable.ic_house,
        "image" to R.drawable.ic_image,
        "key-round" to R.drawable.ic_key_round,
        "keyboard" to R.drawable.ic_keyboard,
        "keyboard-music" to R.drawable.ic_keyboard_music,
        "landmark" to R.drawable.ic_landmark,
        "lightbulb" to R.drawable.ic_lightbulb,
        "lock" to R.drawable.ic_lock,
        "message-square-off" to R.drawable.ic_message_square_off,
        "move-vertical" to R.drawable.ic_move_vertical,
        "palmtree" to R.drawable.ic_palmtree,
        "paperclip" to R.drawable.ic_paperclip,
        "paw-print" to R.drawable.ic_paw_print,
        "pen-line" to R.drawable.ic_pen_line,
        "piggy-bank" to R.drawable.ic_piggy_bank,
        "pizza" to R.drawable.ic_pizza,
        "plus" to R.drawable.ic_plus,
        "popcorn" to R.drawable.ic_popcorn,
        "receipt" to R.drawable.ic_receipt,
        "receipt-text" to R.drawable.ic_receipt_text,
        "rotate-ccw" to R.drawable.ic_rotate_ccw,
        "scroll-text" to R.drawable.ic_scroll_text,
        "search" to R.drawable.ic_search,
        "settings" to R.drawable.ic_settings,
        "shield-check" to R.drawable.ic_shield_check,
        "shirt" to R.drawable.ic_shirt,
        "shopping-bag" to R.drawable.ic_shopping_bag,
        "shopping-basket" to R.drawable.ic_shopping_basket,
        "sliders-horizontal" to R.drawable.ic_sliders_horizontal,
        "smartphone" to R.drawable.ic_smartphone,
        "spell-check-2" to R.drawable.ic_spell_check_2,
        "sprout" to R.drawable.ic_sprout,
        "stethoscope" to R.drawable.ic_stethoscope,
        "store" to R.drawable.ic_store,
        "target" to R.drawable.ic_target,
        "ticket" to R.drawable.ic_ticket,
        "trending-up" to R.drawable.ic_trending_up,
        "triangle-alert" to R.drawable.ic_triangle_alert,
        "utensils" to R.drawable.ic_utensils,
        "wallet" to R.drawable.ic_wallet,
        "wand-sparkles" to R.drawable.ic_wand_sparkles,
        "x" to R.drawable.ic_x,
    )

    /**
     * @return the drawable for [name], or a neutral fallback when the name is unknown.
     *
     * Falls back rather than throwing: a category whose icon was renamed in a later version must
     * still render, because the user's spending history is more important than its glyph.
     */
    @DrawableRes
    fun drawableFor(name: String): Int = icons[name] ?: R.drawable.ic_circle_dashed

    /** True when [name] resolves to a real icon — used by tests to catch a broken seed. */
    fun has(name: String): Boolean = icons.containsKey(name)
}
