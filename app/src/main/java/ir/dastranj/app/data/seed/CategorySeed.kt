package ir.dastranj.app.data.seed

import ir.dastranj.app.data.db.entity.CategoryEntity
import ir.dastranj.app.data.db.entity.CategoryKind

/**
 * The starting category set, taken verbatim from `Dastranj Add Transaction Screen.dc.html`.
 *
 * Names, icons, colours and ordering are the design's; only the stable [CategoryEntity.seedKey] is
 * added, for the reasons given on that field.
 *
 * `isPrimary` marks the eight categories the picker shows in its grid; the rest appear behind
 * «بیشتر». That split is a design decision about the grid, not a statement about importance, so it
 * is stored rather than recomputed from the ordering.
 *
 * Two labels appear under both kinds — «هدیه» and «سایر». They are genuinely different categories
 * (a gift received is not a gift given), so they get distinct keys and must not be merged.
 */
object CategorySeed {

    private data class Seed(
        val key: String,
        val name: String,
        val icon: String,
        val color: String,
        val primary: Boolean,
    )

    private val expense = listOf(
        // The eight shown in the grid.
        Seed("food", "خوراک", "pizza", "#E4813A", true),
        Seed("transport", "حمل‌ونقل", "car-front", "#6C63C7", true),
        Seed("shopping", "خرید", "shopping-basket", "#0B6FB4", true),
        Seed("bills", "قبوض", "receipt-text", "#3AA9A0", true),
        Seed("entertainment", "تفریح", "popcorn", "#C74B8F", true),
        Seed("health", "سلامت", "stethoscope", "#D4373C", true),
        Seed("clothing", "پوشاک", "shirt", "#8A7A2E", true),
        Seed("housing", "مسکن", "house", "#B4763A", true),
        // Behind «بیشتر».
        Seed("education", "آموزش", "graduation-cap", "#0B6FB4", false),
        Seed("travel", "سفر", "palmtree", "#3AA9A0", false),
        Seed("gift_expense", "هدیه", "gift", "#C74B8F", false),
        Seed("sports", "ورزش", "bike", "#6C63C7", false),
        Seed("tech", "فناوری", "smartphone", "#5A5E66", false),
        Seed("child", "کودک", "baby", "#E4813A", false),
        Seed("pet", "حیوان خانگی", "paw-print", "#B4763A", false),
        Seed("insurance", "بیمه", "shield-check", "#0F8A64", false),
        Seed("loan", "وام و قسط", "banknote", "#D4373C", false),
        Seed("other_expense", "سایر", "circle-dashed", "#7C8085", false),
    )

    private val income = listOf(
        Seed("salary", "حقوق", "wallet", "#16A97A", true),
        Seed("sales", "فروش", "store", "#0B6FB4", true),
        Seed("gift_income", "هدیه", "gift", "#C74B8F", true),
        Seed("interest", "سود بانکی", "piggy-bank", "#3AA9A0", true),
        Seed("rent_income", "اجاره", "key-round", "#B4763A", true),
        Seed("debt_collected", "وصول طلب", "hand-coins", "#6C63C7", true),
        Seed("refund", "بازگشت وجه", "rotate-ccw", "#8A7A2E", true),
        Seed("investment", "سرمایه‌گذاری", "sprout", "#0F8A64", true),
        Seed("bonus", "پاداش", "award", "#E4813A", false),
        Seed("car_rental", "اجارهٔ خودرو", "car", "#5A5E66", false),
        Seed("commission", "کارمزد", "briefcase", "#0B6FB4", false),
        Seed("borrowed", "قرض گرفته‌شده", "handshake", "#C74B8F", false),
        Seed("other_income", "سایر", "circle-dashed", "#7C8085", false),
    )

    /**
     * The rows to insert on first run.
     *
     * `sortOrder` is assigned from list position, so reordering the lists above is the only thing
     * needed to reorder the picker.
     */
    fun categories(): List<CategoryEntity> = buildList {
        expense.forEachIndexed { index, seed -> add(seed.toEntity(CategoryKind.EXPENSE, index)) }
        income.forEachIndexed { index, seed -> add(seed.toEntity(CategoryKind.INCOME, index)) }
    }

    private fun Seed.toEntity(kind: CategoryKind, index: Int) = CategoryEntity(
        seedKey = key,
        name = name,
        kind = kind,
        iconName = icon,
        colorHex = color,
        isPrimary = primary,
        sortOrder = index,
    )
}
