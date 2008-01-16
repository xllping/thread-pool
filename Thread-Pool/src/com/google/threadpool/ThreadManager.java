package com.google.threadpool;

import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Set;

import com.google.threadpool.IThreadListener;
import com.google.threadpool.IThreadManagerListener;
import com.google.threadpool.SingleThread;
import com.google.threadpool.ThreadManager;
import com.google.threadpool.Trace;



/**
 * 
 * This class creates and manages all extended threads from
 * SingleThread. 
 * @author avf
 *
 */
public class ThreadManager implements IThreadManagerListener{

    private Hashtable<Long, SingleThread> executingThreads ;//It contains the threads executing right now.
    private LinkedList<SingleThread> stoppedThreads ;

    private LinkedList<Long> threadQueue ; //Queue that saves the Id numbers of awaiting threads.
    private Hashtable<Long, IThreadListener> paralelThreadQueue; //Saving ids and the correspondent threads.
    private Hashtable<Long, Long> id_Time ;//Table saving the starting execution times bound to the Id.

    private int _timeOut;
    private int _maxThread;

    private CheckThreads checkThreads;

    private static ThreadManager _instance; //Instance for implementing the singletong pattern, in order to using this way in the manager working.

    public static final int TIME_OUT_DEFAULT = 900; // Seconds
    public static final int MAX_THREAD_DEFAULT = 4;
    public static final int TIME_REFRESH_CHECK = 30000;//Miliseconds


    public ThreadManager( ){
        init();
    }


    private void init()	{
        executingThreads = new Hashtable<Long, SingleThread>();
        stoppedThreads = new LinkedList<SingleThread>();

        threadQueue = new LinkedList<Long>();
        paralelThreadQueue = new Hashtable<Long, IThreadListener>();
        id_Time = new Hashtable<Long, Long>();

        //At the moment, I put these default values in the parameters
        setTimeOut(TIME_OUT_DEFAULT);
        setMaxThread(MAX_THREAD_DEFAULT);

    }

    /**
     * @return It gets a ThreadManager instance, implementing singleton pattern
     */
    public static ThreadManager getInstance () {
    	if ( _instance == null ){
    		_instance = new ThreadManager();
    	}

    	return _instance;
    }
    /**
     * 
     * Method to request executing a new thread. In case of getting the limit of threads in execution,
     * the new ones will be queued until its time to execute comes.
     * @param _operacion Operation to process
    * @return Id number assigned to the thread
     */
    public synchronized long newThread( IThreadListener _operacion ) throws Exception{

        SingleThread thread = null;
        Date time = new Date();
        long currentTime = time.getTime() ;
        long idResult = currentTime + (long)(Math.random()*100);

        System.out.println("==>> newThread(IThreadListener)");
        System.out.println("==>> newThread("+_operacion+")");

        //Getting the limit, now objects are queued
        if( executingThreads.size() == getMaxThread() ){

            threadQueue.add( idResult );
            paralelThreadQueue.put( idResult, _operacion );

            System.out.println("==> newThread(): Thread waiting");

        } else {

        	System.out.println("==> newThread(): Thread on execution");

        	id_Time.put(idResult, currentTime);//Bind the current time to the executing thread

        	if(stoppedThreads.isEmpty()){
        		thread = new SingleThread(idResult , _operacion, this);
        		startThread( thread, idResult);//A new thread is executed
        	}else{
        		thread = stoppedThreads.poll();
        		executingThreads.put(idResult, thread);//Adding into the executing thread table
        		thread.reset(idResult, _operacion);//A thread that was kept waiting, starts executing
        	}


        	//Check the execution of the checking thread. If it is not working, it starts up.
            if( checkThreads!=null && checkThreads.isAlive()){
            	synchronized(checkThreads){
            		checkThreads.notify();
            	}

            }
            else if(checkThreads==null || !checkThreads.isAlive()){
                checkThreads = new CheckThreads ();
                checkThreads.start();
            }

        }

    	Trace.print("*** "+_operacion.toString()+" Thread#ID: "+idResult+" ***");
    	Trace.print("<<== newThread(IThreadListener)");

        return idResult;

    }

    /**
     * Starts the execution of the correspondent thread
     * @param thread Thread to execute
     * @param idResult Id number of the thread
     * @return Id number of the thread
     */
    private long startThread( SingleThread thread , long idResult) {

        executingThreads.put( idResult , thread );

        thread.runThread();

        return idResult;
    }
    /**
     * A thread currently running is stopped
     * @param id Id number of the thread
     *
     * @return True, if it stopped correctly
     */
    protected synchronized boolean stopThread( long id ){
    	if( !executingThreads.containsKey(id) ){
    		//Must throw exception
    		Trace.err(" *** Try to stop a Thread#ID: "+id+", but it is unregistered ***");
    	}

    	Trace.print("==>> stopThread(long)");
    	Trace.print("==>> stopThread("+id+")");

        boolean result = executingThreads.get(id).abort();

        if ( result ){
            SingleThread st = executingThreads.remove(id);
            id_Time.remove(id);
            stoppedThreads.add(st);
            Trace.print(" *** Thread stopped#ID: " +id+ " ***" );
        }

        Trace.print("<<== stopThread(long)");
        Trace.print("<<== "+result);
        return result;
    }
	/**
	 * The thread is removed from the queue by the parameter and starts running.
	 * @param id Id number of the thread in the queue
	 * @return
	 */
    private boolean removeThreadFromQueue( long id ) {
        id = Math.abs(id);
        boolean result = true;

        IThreadListener operacion = paralelThreadQueue.get(id);
        SingleThread hilo = stoppedThreads.poll();

        if( hilo==null )
            result = false;

        if( result ){
            paralelThreadQueue.remove(id);

            //Se asigna nueva ID
            long tiempo = new Date().getTime();
            id = tiempo + (long)(Math.random()*100);//Se crea el número de identificación
            Trace.print(" *** Sale de espera y se ejecuta Thread#ID: "+ id + " ***");

            executingThreads.put(id, hilo);
            id_Time.put(id, tiempo);
            hilo.reset(id , operacion);
        }

        return result;
    }
	/**
	 * It notifies if the thread is currently executing
	 * @param id Id number of the thread
	 * @return
	 */
    public boolean isActive( long id ){
        return ( executingThreads.get(id).getStatus() == SingleThread.THREAD_RUNNING );
    }

    /**
     * It notifies if the thread is waiting for 
     * @param id Id del hilo
     * @return
     */
    public boolean isWaiting( long id ) {
        return paralelThreadQueue.containsKey(id) ;
    }

    public SingleThread getThread( long id ){
        return executingThreads.get(id);
    }

    public SingleThread[] getThreads( ) {
        Collection<SingleThread> col = executingThreads.values();

        return (SingleThread[])col.toArray();
    }

	/**
	 * Setting the max time to allow to execute a thread.
	 * @param seg Seconds for execution
	 */
    public void setTimeOut( int seg ){
        if ( seg>0 ){
            //Converts to miliseconds
            _timeOut = ( seg * 1000 );
        }
    }

    /**
     * Setting the maximum number of thread to execute
     * @param max Number of threads
     */
    public void setMaxThread( int max ){
        if ( max > 0 ){
            _maxThread = max;
        }
    }

    /**
     * @return Gets the maximum time for executing threads (sec)
     */
    public int getTimeOut() {
        return ( _timeOut / 1000 );
    }

    /**
     * @return Gets the maximum number of threads to execute
     */
    public int getMaxThread() {
        return _maxThread;
    }

    /**
     * It deletes all saved thread in the awaiting queue
     */
    public void clearQueue() {
        threadQueue.remove();
        paralelThreadQueue.clear();
    }


    public void onAbort(SingleThread thread) {
    	onEnd(thread);

    }

    /**
     * Method invoked when a thread execution is stopped in the ThreadManager
     */
    public void onEnd(SingleThread thread) {
		Trace.print("Finaliza el hilo "+thread.getID());
		//Deletes execution thread
		if( executingThreads.containsKey(thread.getID()) ) {

			executingThreads.remove(thread.getID());
			id_Time.remove(thread.getID());
			stoppedThreads.add(thread);

			//Check probable thread waiting. If it does so, Its execution starts up
			if( !threadQueue.isEmpty() ) {
				long id = threadQueue.poll();
				removeThreadFromQueue( id );
			}
		}
	}

	public void onStart(SingleThread thread) {
		// TODO Auto-generated method stub

	}

	/**
	 * Only check the threads currently executing. If they have all overcome the maximum execution time and
	 * there is no other thread executing at the same time (below the timeout), this thread falls asleep.
	 * @author Usuario
	 *
	 */
	class CheckThreads extends Thread {

		private static final String NAME_CT = "ThreadsManager";

    	private Hashtable<Long, SingleThread> executionThreadCopy ;

    	public CheckThreads(){
    		super(NAME_CT);
    		executionThreadCopy = new Hashtable<Long, SingleThread>();
    	}

		@Override
		public void run() {

			while(true){

            	Trace.print("==>> Checking threads");

				executionThreadCopy.clear();
				executionThreadCopy.putAll( executingThreads );

				removeThreadByTO();

				try{
	                //If there is no thread, it break the loop (stop thread)
	                if ( executingThreads.isEmpty() ) {
	                	Trace.print("<<== The thread checking stops (no thread executing)");
	                	synchronized(CheckThreads.this) {
	                		CheckThreads.this.wait();
	                	}
	                	//break;
	                }
	                Trace.print("<<== Checking threads");

                    Thread.sleep(TIME_REFRESH_CHECK);
                }catch(InterruptedException ie){
                    break;
                }
			}
		}

		private void removeThreadByTO() {

            Set<Long> ids = executionThreadCopy.keySet();
            long differenceTime = 0;
            Date time = new Date();

            for(long id : ids){
                differenceTime = time.getTime() - id_Time.get(id);
                if( differenceTime > (getTimeOut() * 1000) ) {
                	stopThread( id );
                }
            }
        }
	}

}
