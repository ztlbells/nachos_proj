package nachos.threads;

import java.util.LinkedList;
import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
	/*timer list, saving expireTime for each thread.
	LinkedList<ThreadTimes> timerList = new LinkedList<ThreadTimes>();*/
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
    	System.out.println("***timerInterrupt***");
    	/*KThread.currentThread.subTimeSlice();
    	
    	System.out.println("time slice:"+Machine.timer().getTime()+" ["+
    				KThread.currentThread.getName()+"] "+KThread.currentThread.getTimeSlice()) ;
    	//looping through timerList, checking to see if any threads need to be waken up
    	int i;
    	for (i = timerList.size() -1 ; i >= 0 ; i--)
    	{
    		//look through the timer list
    		//let specific thread run before being expired.
    		if (Machine.timer().getTime() >= timerList.get(i).getExpireTime())
    		{
    			timerList.get(i).getThreadName().ready();
    			//System.out.println("!!Get "+timerList.get(i).toString());
    			timerList.remove(i);
    		}
    	}*/
    	KThread.yield();
	
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
	long wakeTime = Machine.timer().getTime() + x;
	while (wakeTime > Machine.timer().getTime())
	    KThread.yield();
    }
}
