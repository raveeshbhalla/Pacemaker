package in.raveesh.pacemaker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Raveesh on 23/07/15.
 */
public class HeartbeatReceiver extends BroadcastReceiver {
    private static final Intent GTALK_HEART_BEAT_INTENT = new Intent("com.google.android.intent.action.GTALK_HEARTBEAT");
    private static final Intent MCS_MCS_HEARTBEAT_INTENT = new Intent("com.google.android.intent.action.MCS_HEARTBEAT");

    @Override
    public void onReceive(Context context, Intent intent) {
        context.sendBroadcast(GTALK_HEART_BEAT_INTENT);
        context.sendBroadcast(MCS_MCS_HEARTBEAT_INTENT);
        Log.v("Heartbeater", "HeartbeatReceiver sent heartbeat request");
        scheduleNext(context, intent);
    }

    /**
     * Schedules the next heartbeat when required
     * @param context Context from the broadcast receiver onReceive
     * @param intent Intent from the broadcast receiver onReceive
     */
    private void scheduleNext(Context context, Intent intent) {
        int type = intent.getIntExtra(Pacemaker.KEY_TYPE, Pacemaker.TYPE_LINEAR);
        if (type == Pacemaker.TYPE_EXPONENTIAL) {
            int delay = intent.getIntExtra(Pacemaker.KEY_DELAY, 5);
            delay = delay*2;
            int max = intent.getIntExtra(Pacemaker.KEY_MAX, 60);
            if (delay > max){
                Log.d("Heartbeater", "Killing Heartbeater as delay now exceeds max");
                return;
            }
            Pacemaker.scheduleExponential(context, delay, max);
        } else {
            Log.d("Heartbeater", "Ignored linear schedule request since it should already be there");
        }
    }
}
