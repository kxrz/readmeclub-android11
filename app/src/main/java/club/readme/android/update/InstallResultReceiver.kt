package club.readme.android.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import android.widget.Toast
import club.readme.android.R

/** Receives the installer's progress: shows Android's confirmation screen, reports failures. */
class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION") // the typed overload is API 33+
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
                context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PackageInstaller.STATUS_SUCCESS -> Unit // Android restarts the app in its new version.
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Log.w("Updater", "Install failed ($status): $message")
                // Aborted = the user tapped Cancel on the confirmation screen: nothing to report.
                if (status != PackageInstaller.STATUS_FAILURE_ABORTED) {
                    Toast.makeText(context, context.getString(R.string.update_failed, message ?: "$status"), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
