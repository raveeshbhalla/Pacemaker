package in.raveesh.pacemaker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Delegates all work to {@link PacemakerService}.
 * Listens to
 *   - ACTION_BOOT_COMPLETED to reschedule alarm after device rebooted
 *   - ACTION_PACKAGE_REMOVED to sync alarm settings, remove all alarms scheduled by removed package and to decide what app responsible for generating paces
 *   - ACTION_MANAGE_PACEMAKER to add/remove new setting. Listens to all processes and stores settings. If process is master it will take responsibility to generate paces
 *
 * Process will take responsibility when it is oldest among others. If it is oldest, this mean it knows more settings (it was received much more events with settings so it has much more accurate knowledge)
 *
 * @author Badoo
 */
public class PacemakerReceiver extends BroadcastReceiver {
    private static final String TAG = Pacemaker.TAG;
    private static final boolean DEBUG = Pacemaker.DEBUG;

    public PacemakerReceiver() {
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        if (DEBUG) Log.d(TAG, "received intent: " + intent);
        switch (intent.getAction()) {
            case Intent.ACTION_BOOT_COMPLETED:
                PacemakerService.Launcher.startOnBootCompleted(context);
                break;
            case Intent.ACTION_PACKAGE_REMOVED:
                PacemakerService.Launcher.startOnPackageRemoved(context);
                break;
            case PacemakerService.ACTION_MANAGE_PACEMAKER:
                PacemakerService.Launcher.startOnManage(context, intent);
                break;
            default:
                throw new IllegalStateException("Unknown action: " + intent.getAction());
        }
    }
}
