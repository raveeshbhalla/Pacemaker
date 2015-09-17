package in.raveesh.pacemaker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receiver that listens for an ordered broadcast and stops further propagation
 *
 * @author Arun Sasidharan
 */
public class Scheduler extends BroadcastReceiver {

    public Scheduler() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int delayInMins = intent.getExtras().getInt(Constants.KEY_DELAY);
        long timeGapInMillis = delayInMins * 60 * 1000;

        intent.setClass(context, HeartbeatReceiver.class);

        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, delayInMins, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + timeGapInMillis, timeGapInMillis,
                alarmIntent);

        abortBroadcast();
    }
}
