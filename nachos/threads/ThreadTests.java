package nachos.threads;


import nachos.machine.Machine;


public class ThreadTests {
	
	public ThreadTests()
	{
	
	}
	
	//Thread classes for testing
	private static class PingTest implements Runnable {
		PingTest(int which) {
		    this.which = which;
		}
		
		public void run() {
		    for (int i=0; i<5; i++) {
			System.out.println("*** thread " + which + " looped "
					   + i + " times");
			KThread.currentThread().yield();
		    }
		}

		private int which;
	    }

	    
	
	public static void alarmTest1()
	{
		/*
		 *  Picks a certain amount of ticks between 0 and 1 million and calls 
		 *  Alarm.waitUntil with this amount of ticks. Does this several times
		 *  just to show that it works properly.
		 */
		long ticks;
		Alarm test = new Alarm();
		for (int i =0;i<3;i++)
		{
			ticks=(long)(Math.random()*1000000);
			System.out.println("I'm about to wait for " + ticks + " ticks.");
			test.waitUntil(ticks);
			System.out.println(ticks + " ticks later, I'm done waiting!");
		}
	}
	
	//hogger
	 private static class ThreadHogger implements Runnable{
	    	public int d = 0;
	    		public void run() {
	    			while(d==0){KThread.yield();} 		
	    	}
	    
	    }
	
	public static void priorityTest1()
	{
		/*
		 * Tests priority donation by attempting to create a deadlock
		 */
		System.out.println("Priority TEST #1: Start");
		final Lock theBestLock = new Lock();
		theBestLock.acquire();
		KThread thread1 = new KThread(new Runnable(){
			public void run(){
				System.out.println("Important thread wants the lock");
				theBestLock.acquire();
				System.out.println("Important thread got what it wanted");
				theBestLock.release();
			}
		});
		
		ThreadHogger th = new ThreadHogger();
		KThread thread2 = new KThread(th);
		Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(3);
		ThreadedKernel.scheduler.setPriority(thread1, 7);
		ThreadedKernel.scheduler.setPriority(thread2, 4);
		Machine.interrupt().enable();
		thread1.fork();
		thread2.fork();
		//cant get back without donation
		KThread.yield();
		System.out.println("Main thread letting go of the lock");
		theBestLock.release();
		th.d = 1;
		KThread.yield();
		System.out.println("Priority TEST #1: END");
	}
	
	public static void priorityTest2()
	{
		/*
		 * Creates 3 threads with different priorities and runs them
		 */
		System.out.println("Priority TEST #1: START");
		KThread thread1 = new KThread(new Runnable(){
			public void run(){
				System.out.println("Im first to run");
			}
		});
		KThread thread2 = new KThread(new Runnable(){
			public void run(){
				System.out.println("Im Second to run");
			}
		});
		KThread thread3 = new KThread(new Runnable(){
			public void run(){
				System.out.println("Im Third to run");
			}
		});
		Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(3);
		ThreadedKernel.scheduler.setPriority(thread1, 7);
		ThreadedKernel.scheduler.setPriority(thread2, 5);
		ThreadedKernel.scheduler.setPriority(thread3, 4);
		Machine.interrupt().enable();
		
		thread3.fork();
		thread2.fork();
		thread1.fork();
		KThread.yield();
		System.out.println("Priority TEST #1: END");
	}
	
	
	

}
