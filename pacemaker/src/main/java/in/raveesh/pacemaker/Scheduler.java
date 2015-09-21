package in.raveesh.pacemaker;

import android.os.SystemClock;
import android.support.annotation.VisibleForTesting;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Utility to store setting for repeated paces
 *
 * @author Badoo
 */
public class Scheduler {
    interface KeysValidator {
        boolean isValid(String key);
    }

    private final TimeProvider mTimeProvider;
    private File mFile;
    private HashMap<String, Long> mRules = new HashMap<>();
    private long mStartTime;

    public Scheduler() {
        this(new TimeProvider() {
            @Override
            public long getTime() {
                return SystemClock.elapsedRealtime();
            }
        });
    }


    @VisibleForTesting
    Scheduler(TimeProvider timeProvider) {
        mTimeProvider = timeProvider;
    }

    /**
     * Assigns file which is used to store/restore pace settings
     * @param file
     */
    public void setData(File file) {
        mFile = file;
        if (!file.exists()) {
            return;
        }
        ObjectInputStream stream = null;
        try {
            stream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
            //noinspection unchecked
            mRules = (HashMap) stream.readObject();
            mStartTime = stream.readLong();
        }
        catch (Exception e) {
            // rules already been initialized with empty map
        }
        finally {
            if (stream != null) {
                try {
                    stream.close();
                }
                catch (IOException e) {
                    // Nothing to do
                }
            }
        }
    }

    /**
     * Adds new linear pace for specific id. Each id can have only one setting so each call overrides value for id
     *
     * @param id
     * @param delay
     * @return true if changes lead to change nextTriggerTime
     */
    public boolean addLinearPace(String id, long delay) {
        long nextTrigger = getNextTriggerTime();
        if (mRules.isEmpty()) {
            mStartTime = mTimeProvider.getTime();
        }
        mRules.put(id, delay);
        store();
        return nextTrigger != getNextTriggerTime();
    }

    /**
     * Removes linear pace for id
     *
     * @param id
     * @return true if changes lead to change nextTriggerTime
     */
    public boolean removeLinearPace(String id) {
        long nextTrigger = getNextTriggerTime();
        mRules.remove(id);
        if (mRules.isEmpty()) {
            mStartTime = 0;
        }
        store();
        return nextTrigger != getNextTriggerTime();
    }

    /**
     * @return time when next pace should take place according to all rules
     */
    public long getNextTriggerTime() {
        Collection<Long> values = mRules.values();
        if (values.isEmpty()) {
            return -1;
        }
        ArrayList<Long> list = new ArrayList<>();
        list.addAll(values);
        Collections.sort(list);
        long linearStep = list.get(0);
        return (int) ((Math.floor((mTimeProvider.getTime() - mStartTime) / (float) linearStep) + 1) * linearStep) + mStartTime;
    }

    /**
     * Travers through all keys and checks if they still valid (using validator). If key is not valid - it is then removed.
     * @param validator
     * @return
     */
    public boolean syncKeys(KeysValidator validator) {
        long nextTrigger = getNextTriggerTime();

        Iterator<String> keys = mRules.keySet().iterator();
        while (keys.hasNext()) {
            String k = keys.next();
            if (!validator.isValid(k)) {
                keys.remove();
            }
        }
        store();
        return nextTrigger != getNextTriggerTime();
    }

    /**
     * Clears start time
     */
    public void resetStartTime() {
        mStartTime = mTimeProvider.getTime();
        store();
    }

    private void store() {
        if (mFile == null) {
            return;
        }
        ObjectOutputStream stream = null;
        try {
            stream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(mFile)));
            stream.writeObject(mRules);
            stream.writeLong(mStartTime);
        }
        catch (Exception e) {
            // Sad story
        }
        finally {
            if (stream != null) {
                try {
                    stream.close();
                }
                catch (IOException e) {
                    // Nothing to do
                }
            }
        }
    }
}
