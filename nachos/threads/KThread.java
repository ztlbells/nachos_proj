package nachos.threads;

import nachos.machine.*;

/**
 * A KThread is a thread that can be used to execute Nachos kernel code. Nachos
 * allows multiple threads to run concurrently.
 *
 * To create a new thread of execution, first declare a class that implements
 * the <tt>Runnable</tt> interface. That class then implements the <tt>run</tt>
 * method. An instance of the class can then be allocated, passed as an
 * argument when creating <tt>KThread</tt>, and forked. For example, a thread
 * that computes pi could be written as follows:
 *
 * <p><blockquote><pre>
 * class PiRun implements Runnable {
 *     public void run() {
 *         // compute pi
 *         ...
 *     }
 * }
 * </pre></blockquote>
 * <p>The following code would then create a thread and start it running:
 *
 * <p><blockquote><pre>
 * PiRun p = new PiRun();
 * new KThread(p).fork();
 * </pre></blockquote>
 */
public class KThread {
    /**
     * Get the current thread.
     *
     * @return	the current thread.
     */
    public static KThread currentThread() {
	Lib.assertTrue(currentThread != null);
	return currentThread;
    }
    
    public KThread(Runnable target) {
    	
    	this();
    	//System.out.println("run this.");
    	//System.out.println("target thread created. name:["+this.name+"] status:"+this.status);
    	this.target = target;
    	//System.out.println("======constructor for "+this.name+"=========");
    	
        }
    
    /**
     * Allocate a new <tt>KThread</tt>. If this is the first <tt>KThread</tt>,
     * create an idle thread as well.
     */
    public KThread() {
	if (currentThread != null) {
	    tcb = new TCB();
	}	    
	else {
	    readyQueue = ThreadedKernel.scheduler.newThreadQueue(false);
	    readyQueue.acquire(this);	    

	    currentThread = this;
	    tcb = TCB.currentTCB();
	    name = "Main";
	    
	    //System.out.println("main created. No current thread.");
	    restoreState();
	    
	    
	    createIdleThread();
	}
    }

    /**
     * Allocate a new KThread.
     *
     * @param	target	the object whose <tt>run</tt> method is called.
     */
    public KThread(Runnable target,int originPriority,int totalTimeSlice) {
    	
	this();
	//System.out.println("run this.");
	//System.out.println("target thread created. name:["+this.name+"] status:"+this.status);
	this.target = target;
	//System.out.println("======constructor for "+this.name+"=========");
	this.priority=originPriority;
	this.totalNeededTimeSlice=totalTimeSlice;
    }

    /**
     * Set the target of this thread.
     *
     * @param	target	the object whose <tt>run</tt> method is called.
     * @return	this thread.
     */
    public KThread setTarget(Runnable target) {
	Lib.assertTrue(status == statusNew);
	
	this.target = target;
	return this;
    }

    /**
     * Set the name of this thread. This name is used for debugging purposes
     * only.
     *
     * @param	name	the name to give to this thread.
     * @return	this thread.
     */
    public KThread setName(String name) {
	this.name = name;
	return this;
    }

    /**
     * Get the name of this thread. This name is used for debugging purposes
     * only.
     *
     * @return	the name given to this thread.
     */     
    public String getName() {
	return name;
    }

    /**
     * Get the full name of this thread. This includes its name along with its
     * numerical ID. This name is used for debugging purposes only.
     *
     * @return	the full name given to this thread.
     */
    public String toString() {
	return (name + " (#" + id + ")");
    }

    /**
     * Deterministically and consistently compare this thread to another
     * thread.
     */
    public int compareTo(Object o) {
	KThread thread = (KThread) o;

	if (id < thread.id)
	    return -1;
	else if (id > thread.id)
	    return 1;
	else
	    return 0;
    }

    /**
     * Causes this thread to begin execution. The result is that two threads
     * are running concurrently: the current thread (which returns from the
     * call to the <tt>fork</tt> method) and the other thread (which executes
     * its target's <tt>run</tt> method).
     */
    public void fork() {
	Lib.assertTrue(status == statusNew);
	Lib.assertTrue(target != null);
	
	Lib.debug(dbgThread,
		  "Forking thread: " + toString() + " Runnable: " + target);

	boolean intStatus = Machine.interrupt().disable();

	//tcb.start(target runnable)
	tcb.start(new Runnable() {
		public void run() {
			//System.out.println("[tcb]from tcb.start");
		    runThread();
		}
	    });

	ready();
	
	Machine.interrupt().restore(intStatus);
    }

    private void runThread() {
	begin();
	//System.out.println("[fork @runThread.tcb]from tcb.start->runThread."+target.toString());
	target.run();
	finish();
    }

    private void begin() {
	Lib.debug(dbgThread, "Beginning thread: " + toString());
	
	Lib.assertTrue(this == currentThread);

	restoreState();

	Machine.interrupt().enable();
    }

    /**
     * Finish the current thread and schedule it to be destroyed when it is
     * safe to do so. This method is automatically called when a thread's
     * <tt>run</tt> method returns, but it may also be called directly.
     *
     * The current thread cannot be immediately destroyed because its stack and
     * other execution state are still in use. Instead, this thread will be
     * destroyed automatically by the next thread to run, when it is safe to
     * delete this thread.
     */
    public static void finish() {
	Lib.debug(dbgThread, "Finishing thread: " + currentThread.toString());
	
	//System.out.println("**Finishing thread [" + currentThread.getName()+"] invokes sleep()");
	
	Machine.interrupt().disable();

	Machine.autoGrader().finishingCurrentThread();

	Lib.assertTrue(toBeDestroyed == null);
	toBeDestroyed = currentThread;


	currentThread.status = statusFinished;
	
	sleep();
    }

    /**
     * Relinquish the CPU if any other thread is ready to run. If so, put the
     * current thread on the ready queue, so that it will eventually be
     * rescheuled.
     *
     * <p>
     * Returns immediately if no other thread is ready to run. Otherwise
     * returns when the current thread is chosen to run again by
     * <tt>readyQueue.nextThread()</tt>.
     *
     * <p>
     * Interrupts are disabled, so that the current thread can atomically add
     * itself to the ready queue and switch to the next thread. On return,
     * restores interrupts to the previous state, in case <tt>yield()</tt> was
     * called with interrupts disabled.
     */
    public static void yield() {
	Lib.debug(dbgThread, "Yielding thread: " + currentThread.toString());
	
	Lib.assertTrue(currentThread.status == statusRunning);
	
	boolean intStatus = Machine.interrupt().disable();
	//output
	System.out.println("YIELD ["+currentThread.name+"]");
	 //System.out.println("yield thread [" +currentThread.getName()+"] and "+currentThread.getTimeSlice()+ " time slices left");
	 //System.out.println("yield thread [" +currentThread.getName()+"] and status:"+currentThread.status+ " ");
	 
		
	/*
	 * If a thread always invokes yield(), we can come to the conclusion that this thread is I/O intensive, 
	 * thus, we are supposed to raise up its priority, assuming that the priority of next thread is not higher
	 * than current one.
	 * 
	 * In yield(), we will raise up current thread's priority. But we cannot forget to initialize its time slice
	 * before invoking tcb.contextSwith().
	 */
	currentThread.ready();
	
	/*
	 * Clarify the reason why yield() was invoked: preempted by next thread or invoked by current thread itself.
	 * if for preemption, do nothing
	 * otherwise, raise up priority
	 */
	//if(readyQueue.nextThread().priority > currentThread.priority){
		//TODO: it seems that we cannot use nextThread like this here, because in roundrobinScheduler, 
		//function nextThread() will remove the head of queue.
		//Lib.debug(dbgThread,???
		
	//}
	//else{
		//raise up priority
		//if(currentThread.timeSlice!=0){
			//currentThread.setPriority(++currentThread.priority);
			
			/*System.out.println("[!]priority changed.");
			System.out.println("now the current thread ["+currentThread.name+"] with priority="+currentThread.priority);*/
			//Lib.debug(dbgThread,???
		//}
		//TODO
		//Lib.debug(dbgThread,???
	//}
	//output
	 //System.out.println(" now yield thread [" +currentThread.getName()+"] and status:"+currentThread.status+ " ");
	 //System.out.println("*yield run runNextThread()");
	 
	
	//Initialize time slice before switch.
	//currentThread.resetTimeSlice();
	//System.out.println("-----yield ending------");
	runNextThread();
		
	Machine.interrupt().restore(intStatus);
	
    }

    /**
     * Relinquish the CPU, because the current thread has either finished or it
     * is blocked. This thread must be the current thread.
     *
     * <p>
     * If the current thread is blocked (on a synchronization primitive, i.e.
     * a <tt>Semaphore</tt>, <tt>Lock</tt>, or <tt>Condition</tt>), eventually
     * some thread will wake this thread up, putting it back on the ready queue
     * so that it can be rescheduled. Otherwise, <tt>finish()</tt> should have
     * scheduled this thread to be destroyed by the next thread to run.
     */
    public static void sleep() {
	Lib.debug(dbgThread, "Sleeping thread: " + currentThread.toString());
	
	Lib.assertTrue(Machine.interrupt().disabled());
	//output
	 System.out.println("->sleep invokes runNextThread(), current thread ["+currentThread.name+"] will be blocked.");
	 //System.out.println("---");
	// System.out.println("["+currentThread.name+"]");
	 //System.out.println("|");
	 System.out.println("next :["+readyQueue.nextThread().getName()+"]");
	 
	/*
	 * What we will do in sleep is very similar to what we have done in yield.
	 * In many cases, the reason why a thread calls sleep() is waiting for resource. Hence, we can classify 
	 * those who always calls sleep() as I/O intensive threads, which means their priority are supposed to
	 * be raised up.
	 * 
	 * Do not forget to initialize time slice.
	 */
	if (currentThread.status != statusFinished)
	    currentThread.status = statusBlocked;
	
	//if(currentThread.timeSlice!=0){
		//raise up priority
		//currentThread.setPriority(++currentThread.priority);
		//System.out.println("[!]priority changed.");
		//TODO
		//Lib.debug(dbgThread, ???
	//}
	//currentThread.resetTimeSlice();
	runNextThread();
    }

    /**
     * Moves this thread to the ready state and adds this to the scheduler's
     * ready queue.
     */
    public void ready() {
	Lib.debug(dbgThread, "Ready thread: " + toString());
	
	Lib.assertTrue(Machine.interrupt().disabled());
	Lib.assertTrue(status != statusReady);
	
	status = statusReady;
	if (this != idleThread){
		//TODO
		System.out.println( "["+this.name+"] READY ");
		readyQueue.waitForAccess(this);
	}
	    
	//TODO
	//System.out.println("["+this.name+"] status:"+this.status);
	Machine.autoGrader().readyThread(this);
	/**Important: Assuming that the priority of running thread is highest, 
	 * 				so id a new thread is ready, the only thing we are supposed 
	 * 				to do is comparing the priority of current thread and ready thread,
	 * 				when checking the possibility of preemption.
	 *TODO: checking the possibility of preemption.
	 */
    }

    /**
     * Waits for this thread to finish. If this thread is already finished,
     * return immediately. This method must only be called once; the second
     * call is not guaranteed to return. This thread must not be the current
     * thread.
     */
    public void join() {
	Lib.debug(dbgThread, "Joining to thread: " + toString());

	Lib.assertTrue(this != currentThread);

    }

    /**
     * Create the idle thread. Whenever there are no threads ready to be run,
     * and <tt>runNextThread()</tt> is called, it will run the idle thread. The
     * idle thread must never block, and it will only be allowed to run when
     * all other threads are blocked.
     *
     * <p>
     * Note that <tt>ready()</tt> never adds the idle thread to the ready set.
     */
    private static void createIdleThread() {
	Lib.assertTrue(idleThread == null);
	
	idleThread = new KThread(new Runnable() {
	    public void run() { while (true) yield(); }
	},-1,-1);
	idleThread.setName("idle");
	//System.out.println("idle thread created.");
	Machine.autoGrader().setIdleThread(idleThread);
	
	idleThread.fork();
    }
    
    /**
     * Determine the next thread to run, then dispatch the CPU to the thread
     * using <tt>run()</tt>.
     */
    private static void runNextThread() {
	KThread nextThread = readyQueue.nextThread();
	
	if (nextThread == null)
	    nextThread = idleThread;
	//System.out.println("curr:"+currentThread.name);
	System.out.println("SWITCH TO THREAD [" +nextThread.name+"] ");
	nextThread.run();
	
    }

    /**
     * Dispatch the CPU to this thread. Save the state of the current thread,
     * switch to the new thread by calling <tt>TCB.contextSwitch()</tt>, and
     * load the state of the new thread. The new thread becomes the current
     * thread.
     *
     * <p>
     * If the new thread and the old thread are the same, this method must
     * still call <tt>saveState()</tt>, <tt>contextSwitch()</tt>, and
     * <tt>restoreState()</tt>.
     *
     * <p>
     * The state of the previously running thread must already have been
     * changed from running to blocked or ready (depending on whether the
     * thread is sleeping or yielding).
     *
     * @param	finishing	<tt>true</tt> if the current thread is
     *				finished, and should be destroyed by the new
     *				thread.
     */
    private void run() {
	Lib.assertTrue(Machine.interrupt().disabled());

	Machine.yield();

	currentThread.saveState();

	Lib.debug(dbgThread, "Switching from: " + currentThread.toString()
		  + " to: " + toString());

	currentThread = this;

	tcb.contextSwitch();

	currentThread.restoreState();
    }

    /**
     * Prepare this thread to be run. Set <tt>status</tt> to
     * <tt>statusRunning</tt> and check <tt>toBeDestroyed</tt>.
     */
    protected void restoreState() {
	Lib.debug(dbgThread, "Running thread: " + currentThread.toString());
	
	Lib.assertTrue(Machine.interrupt().disabled());
	Lib.assertTrue(this == currentThread);
	Lib.assertTrue(tcb == TCB.currentTCB());

	Machine.autoGrader().runningThread(this);
	//System.out.println("RestoreState():run ["+this.name+"]");
	status = statusRunning;

	if (toBeDestroyed != null) {
	    toBeDestroyed.tcb.destroy();
	    toBeDestroyed.tcb = null;
	    toBeDestroyed = null;
	}
    }

    /**
     * Prepare this thread to give up the processor. Kernel threads do not
     * need to do anything here.
     */
    protected void saveState() {
	Lib.assertTrue(Machine.interrupt().disabled());
	Lib.assertTrue(this == currentThread);
    }

    
    private static class PingTest implements Runnable {
    	//TODO
    	//test class 
	PingTest(int which) {
	    this.which = which;
	    //System.out.println("pingtest "+which+" created.");
	}
	
	public void run() {
	   
		currentThread.yield();
	}

	private int which;
    }
    
   
    /**
     * Tests whether this module is working.
     */
    
    
    public static void selfTest() {
	Lib.debug(dbgThread, "Enter KThread.selfTest");
	//TODO
	//new KThread(new PingTest(0)).setName("Duplicate Main").fork();
	
	
	System.out.println("------------------------welcome to selftest!------------------------");
	KThread thread1 = new KThread(new Runnable(){
		public void run(){
			//TODO:priority changed, with timeslice changed.
			System.out.println("RUN THREAD [thread1]");
			while(currentThread.timeSlice>0){
				//call tick();
				
				Machine.interrupt().enable();			
				Machine.interrupt().disable();
				System.out.println("TIMESLICE OF [thread1] NOW IS "+currentThread.timeSlice);
			}
			currentThread.yield();
		}
	
	},1,100);
	
	KThread thread2 = new KThread(new Runnable(){
		public void run(){
			System.out.println("RUN THREAD [thread2]");
			while(currentThread.timeSlice>0){
				//call tick();
				//currentThread.timeSlice is determined by priority
				
				Machine.interrupt().enable();				
				Machine.interrupt().disable();
				System.out.println("TIMESLICE OF [thread2] NOW IS "+currentThread.timeSlice);
			}
			currentThread.yield();
		}
	},1,50);
	
	thread2.setName("thread2").fork();
	thread1.setName("thread1").fork();
	//new PingTest(1).run();
	
	//priorityTest();
	System.out.println("------------------------Bye selftest!------------------------");
	
	
    }

    private static final char dbgThread = 't';

    /**
     * Additional state used by schedulers.
     *
     * @see	nachos.threads.PriorityScheduler.ThreadState
     */
    public Object schedulingState = null;
    
    //TODO 
    //@param priority for priorityScheduler
    //default priority of every thread is 1
    private int priority=1;
    
    //some functions for priority
    //set function
    public void setPriority(int newPriority){
    	priority=newPriority;
    }
    
    //TODO
    
    private int timeSlice;
    private int totalNeededTimeSlice;
    //some functions for timeSlice
    //set function
    
    public void setTimeSlice(int newtimeSlice){
    	timeSlice=newtimeSlice;
    }
    public int getTimeSlice(){
    	return timeSlice;
    }
    public void resetTimeSlice(){
    	timeSlice=Stats.KernelTick*5;
    }
    
    //TODO
    public void changeSliceAndPriority(int newPriority){
    	
    }
    //sub time slice function
    public int subTimeSlice(){
    	if(timeSlice>0){
    		timeSlice-=Stats.KernelTick;
    		//System.out.println("[Sub time slice]---[ "+currentThread.getName()+"] with time slice "+ currentThread.getTimeSlice());
        	return timeSlice;
    	}
    	// time slice depleted
    	else return -1;
    }

    private static final int statusNew = 0;
    private static final int statusReady = 1;
    private static final int statusRunning = 2;
    private static final int statusBlocked = 3;
    private static final int statusFinished = 4;

    /**
     * The status of this thread. A thread can either be new (not yet forked),
     * ready (on the ready queue but not running), running, or blocked (not
     * on the ready queue and not running).
     */
    private int status = statusNew;
    private String name = "(unnamed thread)";
    private Runnable target;
    private TCB tcb;

    /**
     * Unique identifer for this thread. Used to deterministically compare
     * threads.
     */
    private int id = numCreated++;
    /** Number of times the KThread constructor was called. */
    private static int numCreated = 0;

    private static ThreadQueue readyQueue = null;
    public static KThread currentThread = null;
    private static KThread toBeDestroyed = null;
    private static KThread idleThread = null;
}
