package in.raveesh.pacemaker;

import android.content.Context;
import android.util.Log;

/**
 * Utility to add/remove paces which suppose to wake up GCM
 *
 * Several apps with this library will share responsibility to generate paces in battery efficient way:
 * - Only one app will generate paces
 * - When it is uninstalled, another will take responsibility
 * - When device is restarted, paces will be recovered
 *
 * Only one pace delay is accepted from each app. If several apps requires different pace delays then shorter delay will take place
 *
 * Created by Raveesh on 23/07/15.
 *
 * Modifications made by Badoo 21/09/15
 */
public class Pacemaker {
    static final String TAG = "Pacemaker";
    static final boolean DEBUG = false;

    /**
     * Starts a linear repeated alarm that sends a broadcast to Play Services, which in turn sends a heartbeat
     *
     * Only one delay can be registered per one app. Each time you call to scheduleLinear, it will overload previous settings
     *
     * @param context Context from your application
     * @param delay Gap between heartbeats in minutes
     */
    public static void scheduleLinear(Context context, int delay) {
        long delayInSec = delay * 60 * 1000;
        Log.d("Heartbeater", "Scheduled repeating");
        PacemakerService.Launcher.scheduleLinear(context, delayInSec);
    }

    /**
     * Function to cancel your linear alarms if required
     * @param context Context from your application
     */
    public static void cancelLinear(Context context) {
        Log.d("Heartbeater", "Cancelling repeating");
        PacemakerService.Launcher.cancelLinear(context);
    }
}
