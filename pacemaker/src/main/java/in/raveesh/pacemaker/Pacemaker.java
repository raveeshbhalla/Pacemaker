package in.raveesh.pacemaker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Raveesh on 23/07/15.
 */
public class Pacemaker
{

    public static final int TYPE_LINEAR = 1;
    public static final int TYPE_EXPONENTIAL = 2;
    public static final String KEY_DELAY = "KEY_DELAY";
    public static final String KEY_TYPE = "KEY_TYPE";
    public static final String KEY_MAX = "KEY_MAX";

    /**
     * Requests a linear repeated alarm that sends a broadcast to Play Services, which in turn sends a heartbeat
     *
     * @param context Context from your application
     * @param delay   Gap between heartbeats in minutes
     */
    public static void scheduleLinear(Context context, int delay)
    {
        if (FileOperations.exists())
        {
            // If condition satisfied, then only bother setting alarm
            if (delay <= FileOperations.getFileDelay())
            {
                FileOperations.write(context.getPackageName(), delay);
                requestLinear(context, delay);
            }
            else
            {
                Log.d("Heartbeater", "Did not schedule, there is another app with shorter delay");
            }
        }
        else //File doesn't exist - this is the first app to use PaceMaker on this device
        {
            FileOperations.write(context.getPackageName(), delay);
            requestLinear(context, delay);
        }


    }

    /**
     * Starts a linear repeated alarm that sends a broadcast to Play Services, which in turn sends a heartbeat
     *
     * @param context Context from your application
     * @param delay   Gap between heartbeats in minutes
     */
    private static void requestLinear(Context context, int delay)
    {
        Intent intent = new Intent(context, HeartbeatReceiver.class);
        intent.putExtra(KEY_DELAY, delay);
        intent.putExtra(KEY_TYPE, TYPE_LINEAR);

        long timeGap = delay * 60 * 1000;

        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, delay, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), timeGap, alarmIntent);
        Log.d("Heartbeater", "Scheduled repeating by " + context.getPackageName());
    }

    /**
     * Function to cancel your linear alarms if required
     *
     * @param context Context from your application
     * @param delay   Gap between heartbeats that you had set
     */
    public static void cancelLinear(Context context, int delay)
    {
        Log.d("Heartbeater", "Cancelled linear by " + context.getPackageName());
        Intent intent = new Intent(context, HeartbeatReceiver.class);
        intent.putExtra(KEY_DELAY, delay);
        intent.putExtra(KEY_TYPE, TYPE_LINEAR);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, delay, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(alarmIntent);
    }

    /**
     * Starts an exponential alarm that sends a broadcast to Play Services, which in turn sends a heartbeat
     *
     * @param context Context from your application
     * @param delay   Time in which to send first broadcast. Subsequent broadcasts would be at exponential intervals
     * @param max     The max time till which the broadcasts should be sent. Once past this limit, no more heartbeats are sent
     */
    public static void scheduleExponential(Context context, int delay, int max)
    {
        Intent intent = new Intent(context, HeartbeatReceiver.class);
        intent.putExtra(KEY_DELAY, delay);
        intent.putExtra(KEY_TYPE, TYPE_EXPONENTIAL);
        intent.putExtra(KEY_MAX, max);

        long timeGap = delay * 60 * 1000;
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, delay, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeGap, alarmIntent);
        Log.d("Heartbeater", "Scheduled exponential");

    }
}
