package nachos.threads;

import nachos.machine.*;
import nachos.threads.RoundRobinScheduler.FifoQueue;

import java.util.LinkedList;
import java.util.Vector;
import java.util.Iterator;

/**
 * A round-robin scheduler tracks waiting threads in FIFO queues, implemented
 * with linked lists. When a thread begins waiting for access, it is appended
 * to the end of a list. The next thread to receive access is always the first
 * thread in the list. This causes access to be given on a first-come
 * first-serve basis.
 */
public class RoundRobinScheduler extends Scheduler {
    /**
     * Allocate a new round-robin scheduler.
     */
    public RoundRobinScheduler() {
    }
    
    /**
     * Allocate a new FIFO thread queue.
     *
     * @param	transferPriority	ignored. Round robin schedulers have
     *					no priority.
     * @return	a new FIFO thread queue.
     */
    
    
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new FifoQueue(0);
    }
    
    /**
     * [from PriorityScheduler]with member public Vector<FifoQueue> subPriorityQueue;
	 */
    public class FifoQueue extends ThreadQueue {
	/**
	 * Add a thread to the end of the wait queue.
	 *
	 * @param	thread	the thread to append to the queue.
	 */ 
    	public FifoQueue(int priority){
    		//constructor with @param priority
    		this.initializer(priority);
    	}
	public void waitForAccess(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
		       
	    waitQueue.add(thread);
	}

	/**
	 * Remove a thread from the beginning of the queue.
	 *
	 * @return	the first thread on the queue, or <tt>null</tt> if the
	 *	       	queue is
	 *		empty.
	 */
	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
		       
	    if (waitQueue.isEmpty())
		return null;

	    return (KThread) waitQueue.removeFirst();
	}

	/**
	 * The specified thread has received exclusive access, without using
	 * <tt>waitForAccess()</tt> or <tt>nextThread()</tt>. Assert that no
	 * threads are waiting for access.
	 */
	public void acquire(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
		       
	    Lib.assertTrue(waitQueue.isEmpty());
	}

	/**
	 * Print out the contents of the queue.
	 */
	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());

	    for (Iterator i=waitQueue.iterator(); i.hasNext(); )
		System.out.print((KThread) i.next() + " ");
	}

	private LinkedList<KThread> waitQueue = new LinkedList<KThread>();
	/**
	 * Vector subPriorityQueue is to implement multi-queue dispatching.
	 * Each subPriorityQueue has its own queue with specific priority.
	 * 
	 * At the same time, as timeSlice varies from priority to priority,
	 * threads with high priority should have more timeslices.
	 * 
	 * In priorityScheduler, the range of priority is 0~7.
	 * To simplify dispatching, we define the initialized timeSlice for each priority as below:
	 * 					timeSlice=(priority+1)*10
	 * 	considering kernelTick=10.
	 */
	
	public int priority;
	public int timeSlice;
	
	/**
	 * To simplify the initialization of subPriorityQueue, we offer some functions here.
	 */
	
	public void initializer(int priority){
		this.priority=priority;
		this.timeSlice=(priority+1)*Stats.KernelTick;
	}
	
    }
}
