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
    	this.target = target;
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
    	//overloading
	this();
	this.target = target;
	
	this.priority=originPriority;
	this.previousPriority=originPriority;

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
			
		    runThread();
		}
	    });
	System.out.println("FORK THREAD "+this.name+" WITH PRIORITY "+this.priority);
	ready();
	System.out.println("from FORK");
	Machine.interrupt().restore(intStatus);
    }

    private void runThread() {
	begin();
	while(currentThread.getTimeSlice()>0)
		target.run();
	System.out.println("STOP RUN THIS THREAD AND TRY TO FINISH IT");
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
	
	System.out.println("**FINISHING THREAD [" + currentThread.getName()+"] ");
	
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
	/*
	 * If a thread always invokes yield(), we can come to the conclusion that this thread is I/O intensive, 
	 * thus, we are supposed to raise up its priority, assuming that the priority of next thread is not higher
	 * than current one.
	 * 
	 * In yield(), we will raise up current thread's priority. But we cannot forget to initialize its time slice
	 * before invoking tcb.contextSwith(). We finish above in ready().
	 */
	boolean intStatus = Machine.interrupt().disable();
	System.out.println("YIELD ["+currentThread.name+"]");
	currentThread.previousPriority=currentThread.priority;
	if(currentThread.timeSlice>0 ){
		//preempted
		if (currentThread.setPriority(currentThread.priority+1))
			System.out.println(currentThread.name+" WAS PREEMPTED, PRIORITY WAS INCREASED "
					+ "TO "+currentThread.priority);
		}	
	else{
		if (currentThread.setPriority(currentThread.priority-1))
			System.out.println(currentThread.name+" WAS NOT PREEMPTED, PRIORITY WAS DECREASED "
					+ "TO "+currentThread.priority);
		}
	currentThread.ready();	
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
	 System.out.println("["+currentThread.name+"] SLEPT");
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
	else{
		//finished
		//remove it from ready queue
		System.out.println("finished");
		readyQueue.waitForAccess(currentThread);
	}
	
	
		
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
		System.out.println( "["+this.name+"] READY WITH PRIORITY "+this.priority);
		readyQueue.waitForAccess(this);
	}

	Machine.autoGrader().readyThread(this);
	/**Important: Assuming that the priority of running thread is highest, 
	 * 				so id a new thread is ready, the only thing we are supposed 
	 * 				to do is comparing the priority of current thread and ready thread,
	 * 				when checking the possibility of preemption.
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
		//in order to avoid running idle thread, we set its priority as -1.
	    public void run() { while (true) yield(); }
	},-1,-1);
	idleThread.setName("idle");
	Machine.autoGrader().setIdleThread(idleThread);
	
	idleThread.fork();
    }
    
    /**
     * Determine the next thread to run, then dispatch the CPU to the thread
     * using <tt>run()</tt>.
     */
    private static void runNextThread() {
    	/**
	     * (1)check whether the current thread is completed by considering the value of totalNeededTimeSlice
	     * (2)if the thread is completed its work, which is equivalent to totalNeededTimeSlice, then remove it.
	     * 		else add it to specific subPriorityQueue.
	     * (3)get the nextThread from readyQueue.nextThread() to run
	     */	
    	
	KThread nextThread = readyQueue.nextThread();
	
	if (nextThread == null)
	    nextThread = idleThread;
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
    	//test class 
	PingTest(String which) {
	    this.which = which;
	}
	
	public void run() {
		System.out.println("RUN PING "+which);
		while(currentThread.timeSlice>0){
			//call tick();				
			Machine.interrupt().tick(true);	
		}
			currentThread.yield();
	}

	private String which;
    }
    
   
    /**
     * Tests whether this module is working.
     */
    public static void newSelfTest(){
    	//fork dummy thread..
    	new KThread(new PingTest("dummy"),5,30).setName("dummy").fork();
    }
    
    public static void selfTest() {
	Lib.debug(dbgThread, "Enter KThread.selfTest");

	
	System.out.println("------------------------selftest()------------------------");
	
	new KThread(new PingTest("one"),5,30).setName("one").fork();
	new KThread(new PingTest("two"),5,40).setName("two").fork();
	new KThread(new PingTest("three"),3,20).setName("three").fork();
	new PingTest("zero").run();

	
	System.out.println("------------------------EOF------------------------");
	
	
    }

    private static final char dbgThread = 't';

    /**
     * Additional state used by schedulers.
     *
     * @see	nachos.threads.PriorityScheduler.ThreadState
     */
    public Object schedulingState = null;
    
    //@param priority for priorityScheduler. default priority of every thread is 1
    private int priority;
    private int previousPriority;
    
    //some functions for priority
    //set function
    public boolean setPriority(int newPriority){
    	if(newPriority>=0 && newPriority<=7){
    		priority=newPriority;
    		return true;
    	}
    	else
    		return false;
    }
    
    public int getPreviousPriority(){
    	return previousPriority;
    }
    
    public int getPriority(){
    	return priority;
    }
    
    private int timeSlice;
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
    
    
    //sub time slice function
    public int subTimeSlice(){
    	if(timeSlice>0 ){
    		timeSlice-=Stats.KernelTick;
    		System.out.println("THREAD ["+currentThread.getName()+"] HAS ONLY "
    										+ currentThread.timeSlice+" TICKS LEFT");
    		return timeSlice;
    	}
    	// time slice is depleted
    	else return -1;
    }
    
    public int getPID(){
    	return this.id;
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
     * Unique identifier for this thread. Used to deterministically compare
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
