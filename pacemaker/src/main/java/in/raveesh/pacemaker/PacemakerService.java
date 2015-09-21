package in.raveesh.pacemaker;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.util.List;

/**
 * Android Service which generates pace to GCM.
 *
 * Service will store all settings. Also if process is master, it will take responsibility to generate paces.
 * Master process is the oldest process in system that can receive broadcast events with action "in.raveesh.pacemaker.ACTION_MANAGE_PACEMAKER".
 * All other processes just stores settings. Once master process uninstalled from system, next oldest process takes responsibility to be master.
 * Older process is - more settings it collected from others, that is why oldest one will be master.
 *
 * Service should be started
 *  - when some app requested to add new pace
 *  - when some app requested cancel of pace
 *  - when package uninstalled (called from PacemakerReceiver)
 *  - when device restarted (called from PacemakerReceiver)
 *  - when pace should be generated (called by AlarmManager)
 *
 * @author Badoo
 */
public class PacemakerService extends IntentService {
    public static final String ACTION_MANAGE_PACEMAKER = "in.raveesh.pacemaker.ACTION_MANAGE_PACEMAKER";

    private static final boolean DEBUG = Pacemaker.DEBUG;
    private static final String TAG = Pacemaker.TAG;

    private static final Intent GTALK_HEART_BEAT_INTENT = new Intent("com.google.android.intent.action.GTALK_HEARTBEAT");
    private static final Intent MCS_MCS_HEARTBEAT_INTENT = new Intent("com.google.android.intent.action.MCS_HEARTBEAT");

    private static final int TYPE_LINEAR = 1;

    private static final int COMMAND_ADD = 1;    // new pace settings
    private static final int COMMAND_PACE = 2;   // time to generate pace
    private static final int COMMAND_SYNC = 3;   // when any package been unistalled
    private static final int COMMAND_BOOT = 4;   // when device restarted
    private static final int COMMAND_CANCEL = 5; // remove of one pace setting

    private static final String EXTRA_COMMAND = "command";
    private static final String EXTRA_TYPE = "type";
    private static final String EXTRA_DELAY = "delay";
    private static final String EXTRA_PACKAGE_NAME = "package";

    private static final int UNKNOWN_VALUE = 0;
    private Scheduler mScheduler;

    public PacemakerService() {
        super("PacemakerScheduler");
    }

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) { // Surprise :)
            return;
        }
        if (!ACTION_MANAGE_PACEMAKER.equals(intent.getAction())) {
            return;
        }
        int command = intent.getIntExtra(EXTRA_COMMAND, UNKNOWN_VALUE);
        int type = intent.getIntExtra(EXTRA_TYPE, UNKNOWN_VALUE);
        switch (command) {
            case COMMAND_ADD:
                onAdd(type, intent);
                break;
            case COMMAND_PACE:
                onPace();
                break;
            case COMMAND_SYNC:
                onSync();
                break;
            case COMMAND_BOOT:
                onBoot();
                break;
            case COMMAND_CANCEL:
                onCancel(type, intent);
                break;
        }
    }

    private void onBoot() {
        if (DEBUG) Log.d(TAG, "onBoot");
        getScheduler().resetStartTime();
        scheduleAlarmIfProcessIsMaster();
    }

    private void onSync() {
        if (DEBUG) Log.d(TAG, "onSync");
        boolean updated = getScheduler().syncKeys(new Scheduler.KeysValidator() {
            @Override
            public boolean isValid(String key) {
                try {
                    getPackageManager().getPackageInfo(key, 0);
                    return true;
                }
                catch (PackageManager.NameNotFoundException e) {
                    return false;
                }
            }
        });
        if (updated) {
            scheduleAlarmIfProcessIsMaster();
        }
    }

    private void onAdd(int type, Intent intent) {
        final String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
        switch (type) {
            case TYPE_LINEAR:
                onAddLinear(packageName, intent.getLongExtra(EXTRA_DELAY, UNKNOWN_VALUE));
                break;
        }
    }

    private void onCancel(int type, Intent intent) {
        final String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
        switch (type) {
            case TYPE_LINEAR:
                onCancelLinear(packageName);
                break;
        }
    }

    private void onCancelLinear(String packageName) {
        if (DEBUG) Log.d(TAG, "onCancelLinear from " + packageName);
        if (getScheduler().removeLinearPace(packageName)) {
            scheduleAlarmIfProcessIsMaster();
        }
    }

    private void onAddLinear(String packageName, long delay) {
        if (DEBUG) Log.d(TAG, "onAddLinear from " + packageName);
        if (getScheduler().addLinearPace(packageName, delay)) {
            scheduleAlarmIfProcessIsMaster();
        }
    }

    private void onPace() {
        try {
            if (DEBUG) {
                Log.d(TAG, "onPace");
                return;
            }

            sendBroadcast(GTALK_HEART_BEAT_INTENT);
            sendBroadcast(MCS_MCS_HEARTBEAT_INTENT);
        }
        finally {
            scheduleAlarmIfProcessIsMaster();
        }
    }

    private void scheduleAlarmIfProcessIsMaster() {
        final Scheduler scheduler = getScheduler();
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, Launcher.createPaceIntent(this), PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long nextTriggerTime = scheduler.getNextTriggerTime();
        if (isMaster() && nextTriggerTime > 0) {
            if (DEBUG) {
                Log.d(TAG, "registering next alarm at " + nextTriggerTime + " which will occur in " + ((nextTriggerTime - SystemClock.elapsedRealtime()) / 1000) + " seconds");
            }
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextTriggerTime, pendingIntent);
        }
        else {
            if (DEBUG) Log.d(TAG, "cancelling alarms, if they exist");
            alarmManager.cancel(pendingIntent); // We could be master in the past, but now we are not anymore
        }
    }

    private boolean isMaster() {
        Intent intent = new Intent(ACTION_MANAGE_PACEMAKER);
        if (Build.VERSION.SDK_INT >= 12) {
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        }
        List<ResolveInfo> infos = getPackageManager().queryBroadcastReceivers(intent, 0);
        if (infos == null) {
            return false;
        }
        if (infos.isEmpty()) {
            return false;
        }
        PackageInfo masterPackage = null;
        for (ResolveInfo i : infos) {
            try {
                String packageName = i.activityInfo.packageName;
                PackageInfo currPackage = getPackageManager().getPackageInfo(packageName, 0);
                if (masterPackage == null) {
                    masterPackage = currPackage;
                }
                else if (currPackage.firstInstallTime < masterPackage.firstInstallTime){
                    masterPackage = currPackage;
                } else if (currPackage.firstInstallTime == masterPackage.firstInstallTime && currPackage.packageName.compareTo(masterPackage.packageName) < 0) {
                    masterPackage = currPackage;
                }

            }
            catch (Exception e) {
                Log.w(TAG, "Failed to get package info", e);
            }
        }
        return masterPackage != null && getPackageName().equals(masterPackage.packageName);
    }

    private Scheduler getScheduler() {
        if (mScheduler == null) {
            mScheduler = new Scheduler();
            mScheduler.setData(new File(getFilesDir(), "pacemaker.data"));
        }
        return mScheduler;
    }


    /**
     * Work with service using this utility methods
     */
    public static class Launcher {

        static void startOnBootCompleted(Context ctx) {
            Intent intent = new Intent(ctx, PacemakerService.class);
            intent.setAction(ACTION_MANAGE_PACEMAKER);
            intent.putExtra(EXTRA_COMMAND, COMMAND_BOOT);
            ctx.startService(intent);
        }

        static void startOnPackageRemoved(Context ctx) {
            Intent intent = new Intent(ctx, PacemakerService.class);
            intent.setAction(ACTION_MANAGE_PACEMAKER);
            intent.putExtra(EXTRA_COMMAND, COMMAND_BOOT);
            ctx.startService(intent);
        }

        static void startOnManage(Context ctx, Intent caller) {
            Intent intent = new Intent(ctx, PacemakerService.class);
            intent.setAction(ACTION_MANAGE_PACEMAKER);
            copyExtras(caller, intent);
            ctx.startService(intent);
        }

        static void scheduleLinear(Context ctx, long delay) {
            Intent intent = new Intent(ACTION_MANAGE_PACEMAKER);
            if (Build.VERSION.SDK_INT >= 12) {
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            }
            intent.putExtra(EXTRA_COMMAND, COMMAND_ADD);
            intent.putExtra(EXTRA_TYPE, TYPE_LINEAR);
            intent.putExtra(EXTRA_DELAY, delay);
            intent.putExtra(EXTRA_PACKAGE_NAME, ctx.getPackageName());
            ctx.sendBroadcast(intent);
        }

        public static void cancelLinear(Context ctx) {
            Intent intent = new Intent(ACTION_MANAGE_PACEMAKER);
            if (Build.VERSION.SDK_INT >= 12) {
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            }
            intent.putExtra(EXTRA_COMMAND, COMMAND_CANCEL);
            intent.putExtra(EXTRA_TYPE, TYPE_LINEAR);
            intent.putExtra(EXTRA_PACKAGE_NAME, ctx.getPackageName());
            ctx.sendBroadcast(intent);
        }

        private static Intent createPaceIntent(Context ctx) {
            Intent result = new Intent(ctx, PacemakerService.class);
            result.setAction(ACTION_MANAGE_PACEMAKER);
            result.putExtra(EXTRA_COMMAND, COMMAND_PACE);
            return result;
        }

        private static void copyExtras(Intent from, Intent to) {
            to.putExtra(EXTRA_COMMAND, from.getIntExtra(EXTRA_COMMAND, UNKNOWN_VALUE));
            to.putExtra(EXTRA_DELAY, from.getLongExtra(EXTRA_DELAY, UNKNOWN_VALUE));
            to.putExtra(EXTRA_TYPE, from.getIntExtra(EXTRA_TYPE, UNKNOWN_VALUE));
            to.putExtra(EXTRA_PACKAGE_NAME, from.getStringExtra(EXTRA_PACKAGE_NAME));
        }
    }
}
