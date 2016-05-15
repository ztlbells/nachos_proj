package nachos.threads;

public class ThreadTimes
{
	private KThread thread;
	private long expireTime;
	
	public ThreadTimes(KThread t, long exp)
	{
		thread = t;
		expireTime = exp;
	}
	
	public KThread getThreadName ()
	{
		return thread;
	}
	
	public long getExpireTime ()
	{
		return expireTime;
	}
}