package com.google.threadpool;

public interface IThreadListener {
	/**
	 * Operación realizada por el hilo en su ejecución
	 */
	public void onRun();
	/**
	 * Operación realizada por el hilo en caso de cancelación del mismo
	 */
	public void onAbort();
	
}
