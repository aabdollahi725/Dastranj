package ir.dastranj.app.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.dastranj.app.R
import ir.dastranj.app.domain.budget.BudgetAlert
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts the two budget notifications (PRD §11).
 *
 * ## What is deliberately not in the notification
 *
 * No amount, ever — not the spend, not the limit, not the overspend. A notification is rendered on
 * the lock screen and can be read by anyone holding the phone, and CLAUDE.md §2 keeps financial
 * figures off any surface the user did not open. The text names the category and the situation; the
 * number is inside the app, behind FLAG_SECURE.
 *
 * ## Permission
 *
 * `POST_NOTIFICATIONS` is the app's only permission and is requested at first budget creation, not
 * at launch. If it was refused, [notify] silently does nothing — a budget still works, it just does
 * not announce itself, and nagging for a permission the user declined is worse than the silence.
 */
@Singleton
class BudgetNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    /** Idempotent; safe to call more than once. */
    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_budget),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_budget_description)
            // Nothing about a budget is urgent enough to override a silent phone.
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED

    /**
     * @param budgetId used as the notification id, so a later alert for the same budget replaces
     *   the earlier one instead of stacking a second card in the shade.
     */
    fun notify(alert: BudgetAlert, budgetId: Long, categoryName: String) {
        if (alert == BudgetAlert.NONE) return
        if (!hasPermission()) return

        ensureChannel()

        val (titleRes, bodyRes) = when (alert) {
            BudgetAlert.THRESHOLD_REACHED ->
                R.string.notification_budget_threshold_title to
                    R.string.notification_budget_threshold_body
            BudgetAlert.EXCEEDED ->
                R.string.notification_budget_exceeded_title to
                    R.string.notification_budget_exceeded_body
            BudgetAlert.NONE -> return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chart_pie)
            .setContentTitle(context.getString(titleRes))
            .setContentText(context.getString(bodyRes, categoryName))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            // The shade is a public surface, so nothing is hidden on the lock screen only because
            // there is nothing sensitive in the text to begin with.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        // The permission check above can still race a revocation, so this is guarded.
        runCatching {
            notificationManager.notify(budgetId.toInt(), notification)
        }
    }

    private companion object {
        const val CHANNEL_ID = "dastranj_budget"
    }
}
