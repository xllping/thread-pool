package com.google.threadpool;

import com.google.threadpool.SingleThread;

public interface IThreadManagerListener {
	public void onEnd(SingleThread thread);
	
	public void onStart(SingleThread thread);
	
	public void onAbort(SingleThread thread);
}
