package nachos.threads;

import nachos.machine.*;
import nachos.threads.FifoQueue;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.*;


/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	Lib.assertTrue(priority >= priorityMinimum &&
		   priority <= priorityMaximum);
	
	getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMaximum)
	    return false;

	setPriority(thread, priority+1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    public boolean decreasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMinimum)
	    return false;

	setPriority(thread, priority-1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
	if (thread.schedulingState == null)
	    thread.schedulingState = new ThreadState(thread);

	return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
    	PriorityQueue(boolean transferPriority) {
	    this.transferPriority = transferPriority;
	    //create subPriorityQueue for each priority (range of priority:0~7)
	    for(int i=0;i<=7;i++){
	    	subPriorityQueue.addElement(new FifoQueue(i));
	    	System.out.println("subPriorityQueue "+i+" created.");
	    	subPriorityQueue.get(i).initializer(i);
	    	System.out.println("time slice "+subPriorityQueue.get(i).timeSlice);
	    }
	   
	}
    	public void waitForAccess(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).waitForAccess(this);
	}
    	public void acquire(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).acquire(this);
	}

    	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    // implement me
	    //TODO:return the next thread to run, remove previous running thread from queue. 
	    return null;
	}

	/**
	 	* Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
    	protected ThreadState pickNextThread() {
	    // implement me
    	/**
    	 * TODO: return the status of next thread to run, including priority
    	 * 
    	 * Question: how can we find the next thread to run?
    	 * (1)get the highest priority from currentHighestPriority()
    	 * (2)subPriorityQueue.get(i).nextThread()
    	 * [important]:how can we assure that the next thread will run as we expect?
    	 * 
    	 */
	    return null;
	    
	}
	
    	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    // implement me (if you want)
	}

	/**
	 	* <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
    	public boolean transferPriority;
	
	/**
	 	* Vector subPriorityQueue is to implement multi-queue dispatching.
	 * Each subPriorityQueue has its own queue with specific priority.
	 * 
	 * At the same time, as timeSlice varies from priority to priority,
	 * threads with high priority should have more timeslices.
	 * 	
	 * More details about FifoQueue are in RoundRobinScheduler.
	 */
    	public Vector<FifoQueue> subPriorityQueue = new Vector<FifoQueue>(0);
    	
    	
    	public int currentHighestPriority(){
    		int i=subPriorityQueue.size();
    		while(subPriorityQueue.get(i-1).isEmpty()){
    			i--;
    		}
    		return i;
    	}
    }
    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
	/**
	 	* Allocate a new <tt>ThreadState</tt> object and associate it with the
	 * specified thread.
	 *
	 * @param	thread	the thread this state belongs to.
	 */
    	public ThreadState(KThread thread) {
	    this.thread = thread;
	    
	    setPriority(priorityDefault);
	}

	/**
	 	* Return the priority of the associated thread.
	 *
	 * @return	the priority of the associated thread.
	 */
    	public int getPriority() {
    		//TODO:maybe getEffectivePriority() should be called here
	    return priority;
	}

	/**
	 	* Return the effective priority of the associated thread.
	 *
	 * @return	the effective priority of the associated thread.
	 */
    	public int getEffectivePriority() {
	    // implement me
    		//TODO: considering priority inversion. However, as we allow preemption here, it might be useless.
	    return priority;
	}

	/**
	 	* Set the priority of the associated thread to the specified value.
	 *
	 * @param	priority	the new priority.
	 */
    	public void setPriority(int priority) {
	    if (this.priority == priority)
		return;
	    
	    this.priority = priority;
	    
	    // implement me
	}

	/**
	 	* Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
	 * the associated thread) is invoked on the specified priority queue.
	 * The associated thread is therefore waiting for access to the
	 * resource guarded by <tt>waitQueue</tt>. This method is only called
	 * if the associated thread cannot immediately obtain access.
	 *
	 * @param	waitQueue	the queue that the associated thread is
	 *				now waiting on.
	 *
	 * @see	nachos.threads.ThreadQueue#waitForAccess
	 */
    	public void waitForAccess(PriorityQueue waitQueue) {
	    // implement me
    		/**
    		 * TODO:(1)check the priority of thread:
    		 * 				if higher than running thread, preemption.
    		 * 				else
    		 * 					(2)check the existence of subPriorityQueue with certain priority.
    		 * 						if existed, add the thread into a specific FifoQueue.
    		 * 						else
    		 * 							invoke fifoqueue.initializer(specific priority) to create a new fifoQueue
    		 * 							then add the thread into it.
    		 * 					(3)after finishing adding, change the attributes of thread(timeslice priority)
    		 *[warning] Pay attention to idleThread. 
    		 */
	}

	/**
	 	* Called when the associated thread has acquired access to whatever is
	 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
	 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
	 * <tt>thread</tt> is the associated thread), or as a result of
	 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
	 *
	 * @see	nachos.threads.ThreadQueue#acquire
	 * @see	nachos.threads.ThreadQueue#nextThread
	 */
    	public void acquire(PriorityQueue waitQueue) {
	    // implement me
    		Lib.assertTrue(Machine.interrupt().disabled());
		       //TODO:checking that there is no thread to run.
    			//maybe we can use fifo.acquire to return sth like all of the subPrioritySchedule return empty
    		
    	    //Lib.assertTrue(waitQueue.isEmpty());
	}	

    	/** The thread with which this object is associated. */	   
    	protected KThread thread;
    	/** The priority of the associated thread. */
    	protected int priority;
    }
}
