package com.google.threadpool;

import com.google.threadpool.IThreadListener;
import com.google.threadpool.IThreadManagerListener;


/**
 * 
 * Class implementing a thread execution. This execution is based on the invoke of an object method 
 * (contained in a ThreadHandledObject). If an unhoped cancel of thread execution turns up,  
 * there is a possibility to pass in by arguments for invoking an alternative operation.
 * @author avf
 *
 */
public class SingleThread implements Runnable{

	private Thread _hilo ;

	private IThreadListener operacion;
	private IThreadManagerListener thread;

	private long id;

	private int estadoActual;

	//The threads is running
	public final static int THREAD_RUNNING = 1;
	//The object is instanced, but not executed
	public final static int THREAD_INSTANCED = 2;
	//The thread is paused
	public final static int THREAD_PAUSED = 3;
	//The thread is aborted
	public final static int THREAD_ABORTED = 4;
	//The thread has correctly stopped 
	public final static int THREAD_STOPPED = 5;


	protected SingleThread(long _id, IThreadListener _operacion, IThreadManagerListener _thread) throws Exception{
		if(_operacion == null){
			Trace.err(" *** The thread could not start. Reason: Undefined operation ***");
			throw new Exception("Undefined operation for SingleThread");
		}

		operacion = _operacion;
		thread = _thread;
		id = _id;
		_hilo = new Thread( this );
		estadoActual = THREAD_INSTANCED;
	}

	public void reset(long _id, IThreadListener _operacion) {
		id = _id;
		operacion = _operacion;

		if(this.estadoActual!=THREAD_RUNNING){
			this.estadoActual = THREAD_INSTANCED;
			synchronized(_hilo){
				_hilo.notifyAll();
			}
		}
	}

	public void run() {
		if(this.estadoActual != THREAD_INSTANCED){
			return ;
		}

		while(true){

			this.estadoActual = THREAD_RUNNING;
			Trace.print(" +++ It starts executing "+getID());

			if(operacion!=null)
				operacion.onRun();

			this.estadoActual = THREAD_STOPPED;
			thread.onEnd(this);

			try{
				synchronized(_hilo){
					while(this.estadoActual != THREAD_INSTANCED){
						_hilo.wait();
					}
				}
			}catch(Exception exc){
				Trace.err(exc.getMessage());
			}finally{
				Trace.print("Back to life");
			}
		}

	}

	public void runThread(){

		if( _hilo!=null )
			_hilo.start();
	}

	/**
	 * @return Gets the status of the current thread
	 */
	public int getStatus() {
		return estadoActual;
	}

	/**
	 * It is executed in case of there is an abort of the thread
	 * @return If it ends satisfactorily, it returns: true.
	 */
	public boolean abort() {
		estadoActual = THREAD_ABORTED;
		boolean resultado = false;

		try{
			if(_hilo.isAlive()){
				operacion.onAbort();
				resultado = true;
				synchronized(_hilo){
					_hilo.wait();
				}
			}
		}catch(Exception se){

		}

		return resultado;
	}

	public long getID() {
		 return id;
	}
}
