package com.google.threadpool;


/**
 * Class that implements an IThread for using as example.
 * @author Álvaro
 *
 */
public class TReenvioRHIN implements IThreadListener {

	long[] tiempos = { 10000, 20000, 30000 };

	public void onAbort() {

	}

	public void onRun() {
		Trace.print("==>> TReenvioRHIN.onRun()");
		try{
			Thread.sleep(tiempos[ (int)(Math.random()*10)%3 ]);
		}catch(InterruptedException ie) {
			Trace.print(" Excepción en TReenvioRHIN.onRun(); "+ie.toString());
		}
		Trace.print("<<== TReenvioRHIN.onRun()");
	}

}
