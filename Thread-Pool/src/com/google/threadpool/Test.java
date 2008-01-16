package com.google.threadpool;


public class Test {

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try{
			ThreadManager.getInstance().newThread( new TReenvioRHIN() );
			ThreadManager.getInstance().newThread( new TReenvioRHIN() );
			ThreadManager.getInstance().newThread( new TReenvioRHIN() );
			ThreadManager.getInstance().newThread( new TReenvioRHIN() );

			ThreadManager.getInstance().newThread( new TReenvioRHIN() );
			ThreadManager.getInstance().newThread( new TReenvioRHIN() );
			Thread.sleep(2000);
			ThreadManager.getInstance().newThread( new TReenvioRHIN() );
			ThreadManager.getInstance().newThread( new TReenvioRHIN() );
			//Thread.sleep(1000);
			ThreadManager.getInstance().newThread( new TReenvioRHIN() );
		}catch(Exception ex){
			System.err.println(ex.getMessage());
		}
	}

}
