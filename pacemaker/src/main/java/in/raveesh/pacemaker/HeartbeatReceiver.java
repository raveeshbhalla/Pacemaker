package in.raveesh.pacemaker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Raveesh on 23/07/15.
 */
public class HeartbeatReceiver extends BroadcastReceiver
{
    private static final Intent GTALK_HEART_BEAT_INTENT = new Intent("com.google.android.intent.action.GTALK_HEARTBEAT");
    private static final Intent MCS_MCS_HEARTBEAT_INTENT = new Intent("com.google.android.intent.action.MCS_HEARTBEAT");

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (shouldReceive(context, intent))
        {
            context.sendBroadcast(GTALK_HEART_BEAT_INTENT);
            context.sendBroadcast(MCS_MCS_HEARTBEAT_INTENT);
            Log.v("Heartbeater", context.getPackageName() + " sent Heartbeat request");
            scheduleNext(context, intent);
        }
        else
        {
            //Exponential need not be cancelled as it won't run unless we explicitly have it run in scheduleNext
            cancelIfLinear(context, intent);
        }
    }

    /**
     * Schedules the next heartbeat when required
     *
     * @param context Context from the broadcast receiver onReceive
     * @param intent  Intent from the broadcast receiver onReceive
     */
    private void scheduleNext(Context context, Intent intent)
    {
        int type = intent.getIntExtra(Pacemaker.KEY_TYPE, Pacemaker.TYPE_LINEAR);
        if (type == Pacemaker.TYPE_EXPONENTIAL)
        {
            int delay = intent.getIntExtra(Pacemaker.KEY_DELAY, 5);
            delay = delay * 2;
            int max = intent.getIntExtra(Pacemaker.KEY_MAX, 60);
            if (delay > max)
            {
                Log.d("Heartbeater", "Killing Heartbeater as delay now exceeds max");
                return;
            }
            Pacemaker.scheduleExponential(context, delay, max);
        }
        else
        {
            Log.d("Heartbeater", "Ignored linear schedule request since it should already be there");
        }
    }

    /**
     * Checks whether the Broadcast should be recieved (or cancelled)
     *
     * @param context Context from the broadcast receiver onReceive
     * @param intent  Intent from the broadcast receiver onReceive
     */
    private boolean shouldReceive(Context context, Intent intent)
    {
        //The file must exist
        if (FileOperations.exists())
        {
            //Check if the file has been modified since the last broadcast
            if (FileOperations.isFileModified())
            {
                //If so, read the file
                FileOperations.read();
                //Check if the new delay is less than ours. If so, then we don't need to receive any more.
                if (FileOperations.getFileDelay() <= intent.getIntExtra(Pacemaker.KEY_DELAY, 5))
                {
                    return false;
                }
                else
                {
                    FileOperations.write(context.getPackageName(), intent.getIntExtra(Pacemaker.KEY_DELAY, 5));
                }
            }
            // Making sure that we are the intended receiver
            if (FileOperations.getFilePackage().equals(context.getPackageName())) return true;
        }
        return false;
    }

    /**
     * Cancel the Broadcast Receiver if it is of Linear Type
     *
     * @param context Context from the broadcast receiver onReceive
     * @param intent  Intent from the broadcast receiver onReceive
     */
    private void cancelIfLinear(Context context, Intent intent)
    {
        if (intent.getIntExtra(Pacemaker.KEY_TYPE, Pacemaker.TYPE_LINEAR) == Pacemaker.TYPE_LINEAR)
        {
            Log.d("Heartbeater", "Cancelling The Linear Broadcast by " + context.getPackageName());
            Pacemaker.cancelLinear(context, intent.getIntExtra(Pacemaker.KEY_DELAY, 5));
        }
    }
}
