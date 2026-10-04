package dev.gaborbiro.dailymacros.feedback

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.view.View
import com.shakebugs.shake.Shake
import curtains.Curtains
import curtains.OnRootViewAddedListener
import curtains.phoneWindow

/**
 * Keeps Shake's floating button reachable while a dialog is open.
 *
 * The button is its own small window, added when a screen opens, so any dialog opened afterwards
 * (Daily Targets, Personalise AI, the meal screens, confirmations) is a newer window stacked on
 * top of it. Re-applying the (unchanged) floating-button setting makes Shake remove and re-add
 * the button on the current screen, which puts it above the dialog.
 */
internal object ShakeButtonAboveDialogs {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun install() {
        Curtains.onRootViewsChangedListeners += object : OnRootViewAddedListener {
            override fun onRootViewAdded(view: View) {
                // Posted so the dialog's window is fully added before the button is re-added.
                if (view.isDialogOverAppScreen()) mainHandler.post(::raiseButton)
            }
        }
    }

    private fun raiseButton() {
        val config = Shake.getReportConfiguration()
        if (config.isShowFloatingReportButton) config.isShowFloatingReportButton = true
    }

    /**
     * True for dialog windows (incl. Compose Dialog and ModalBottomSheet) over one of the app's
     * own screens. Activity windows, popups, toasts, Shake's own button window and dialogs inside
     * Shake's UI are left alone.
     */
    private fun View.isDialogOverAppScreen(): Boolean {
        val window = phoneWindow ?: return false
        val activity = window.context.findActivity() ?: return false
        return activity.window !== window && !activity.javaClass.name.startsWith("com.shakebugs.")
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
