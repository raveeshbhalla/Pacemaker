package in.raveesh.pacemaker;

import junit.framework.TestCase;

/**
 * @author Badoo
 */
public class SchedulerTest extends TestCase {
    private MyTimeProvider mTime;
    private Scheduler mScheduler;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTime = new MyTimeProvider();
        mScheduler = new Scheduler(mTime);
    }

    public void testEmpty() {
        assertEquals(-1, mScheduler.getNextTriggerTime());
    }

    public void testOneRuleWithZeroTimeStart() {
        mTime.time = 0;
        mScheduler.addLinearPace("id", 100);
        assertEquals(100, mScheduler.getNextTriggerTime());
        mTime.time = 100;
        assertEquals(200, mScheduler.getNextTriggerTime());
        mTime.time = 150;
        assertEquals(200, mScheduler.getNextTriggerTime());
    }

    public void testOneRuleWithNonZeroTimeStart() {
        mTime.time = 100;
        mScheduler.addLinearPace("id", 100);
        assertEquals(200, mScheduler.getNextTriggerTime());
        mTime.time = 200;
        assertEquals(300, mScheduler.getNextTriggerTime());
        mTime.time = 250;
        assertEquals(300, mScheduler.getNextTriggerTime());
    }

    public void testOneRuleWithTimeCloseToEvent() {
        mTime.time = 100;
        mScheduler.addLinearPace("id", 100);
        mTime.time = 199;
        assertEquals(200, mScheduler.getNextTriggerTime());
        mTime.time = 201;
        assertEquals(300, mScheduler.getNextTriggerTime());
    }

    public void testInTwoRulesSmallerIsChoosen() {
        mTime.time = 0;
        mScheduler.addLinearPace("id1", 100);
        mTime.time = 50;
        mScheduler.addLinearPace("id2", 10);
        assertEquals(60, mScheduler.getNextTriggerTime());
    }


    private class MyTimeProvider implements TimeProvider {
        public long time;

        @Override
        public long getTime() {
            return time;
        }
    }
}