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

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
    	PriorityQueue(boolean transferPriority) {
	    this.transferPriority = transferPriority;
	    //create subPriorityQueue for each priority (range of priority:0~7)
	    for(int i=0;i<=7;i++){
	    	subPriorityQueue.addElement(new FifoQueue(i));
	    	subPriorityQueue.get(i).initializer(i);
	    }
	   
	}
    	public void waitForAccess(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());

	    if(thread.getPreviousPriority()==thread.getPriority()){
	    	//priority was not changed
	    	if (!this.subPriorityQueue.get(thread.getPreviousPriority()).isEmpty()){
	    		// queue is not empty
	    		if(this.subPriorityQueue.get(thread.getPreviousPriority()).
	    												waitQueueHead().getPID()==thread.getPID() ){
	    			//current thread should be moved to the end of queue, 
	    			// with unchanged priority.
	    				//System.out.println("move to end");
	    				this.subPriorityQueue.get(thread.getPreviousPriority()).removeHead();
		    			this.subPriorityQueue.get(thread.getPreviousPriority()).waitForAccess(thread);	    			
	    		}  
	    		else
	    			//just add it 
	    			this.subPriorityQueue.get(thread.getPreviousPriority()).waitForAccess(thread);
	    	}
	    	else
	    		//queue is empty
	    		//new ready thread
	    		this.subPriorityQueue.get(thread.getPriority()).waitForAccess(thread);
	 
	    	System.out.println("QUEUE OF PRIORITY "+thread.getPreviousPriority()+":");
	    	this.subPriorityQueue.get(thread.getPreviousPriority()).print();
	    }
	    
	    else{
	    	//priority was changed 	
	    	//System.out.println("priority was changed, thread ["+thread.getName()+"] pp="+thread.getPreviousPriority()+" p="+thread.getPriority());
	    	this.subPriorityQueue.get(thread.getPreviousPriority()).removeHead();
	    	this.subPriorityQueue.get(thread.getPriority()).waitForAccess(thread);
	    	
	    	System.out.println("QUEUE OF PRIORITY "+thread.getPriority()+":");
	    	this.subPriorityQueue.get(thread.getPriority()).print();
	    	
	    	System.out.println("QUEUE OF PREVIOUSPRIORITY "+thread.getPreviousPriority()+":");
	    	this.subPriorityQueue.get(thread.getPreviousPriority()).print();
	    }
	    
	}
    	public void acquire(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    Lib.assertTrue(this.isEmpty());
	}
    	public boolean isEmpty(){
    		boolean empty=true;
    		for(int i=0;i<this.subPriorityQueue.size();i++){
    			empty&=this.subPriorityQueue.get(i).isEmpty();
    		}
    		return empty;
    	}
    	
    	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    	return (KThread) this.subPriorityQueue.get(this.currentHighestPriority()).waitQueueHead();
    	}
	    /**
	     * (1)check whether the current thread is completed by considering the value of totalNeededTimeSlice
	     * (2)if the thread is completed its work, which is equivalent to totalNeededTimeSlice, then remove it.
	     * 		else add it to specific subPriorityQueue.
	     * (3)
	     */
	   

	/**
	 	* Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
    	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());
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
    		int i=subPriorityQueue.size()-1;
    		while(subPriorityQueue.get(i).isEmpty() && i>=0){
    			i--;
    		}
    		//System.out.println("i="+i);
    		return i;
    	}
    }
   
  
}
