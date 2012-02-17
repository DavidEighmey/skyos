package nachos.threads;

import nachos.machine.*;
//import java.util.Queue; // for sleeping threads -- didn't need used condition variables

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
	/**
	 * Allocate a new communicator.
	 */
	private Lock mutex;
	Condition listenerArrived;
	Condition speakArrived;

	private int toTransfer; // holds the word that needs to be transfered
	//Queue<Integer>words; used condition variables instead

	boolean speaker; // is there a speaker?
	boolean listener;// is there a listener?


	public Communicator() {
		mutex = new Lock();
		listenerArrived = new Condition(mutex);
		speakArrived = new Condition(mutex);

		this.speaker = false; // start off with no speaker
		this.listener = false;// start off with no listener
	}
	
	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 *
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 *
	 * @param	word	the integer to transfer.
	 */
	public void speak(int word)
	{
		this.mutex.acquire(); // get the lock
		this.speaker = true; // there is now a speaker
		while(listener == false) // while there is nothing to get
		{
			this.speakArrived.sleep(); // put the thread to sleep
		}
		this.listenerArrived.wake(); // wake all before releasing
		this.toTransfer = word; // store the word to a global variable
		//this.speaker = false; // reset to no speaker -- broke
		//this.speaker = true;
		//this.listener = true; //
		this.mutex.release(); // now release the lock before returning
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return
	 * the <i>word</i> that thread passed to <tt>speak()</tt>.
	 *
	 * @return	the integer transferred.
	 */    
	public int listen() 
	{
			this.mutex.acquire(); // get the lock
			this.listener = true; // there is now a listener
			while(speaker == false) // if no speaker wait for the speaker
			{
				this.listenerArrived.sleep();// put to sleep while waiting
			}
			this.speakArrived.wake(); // wake all or just one ?
			int word = this.toTransfer;
			//this.listener = false; // reset to no listener -- this broke it
			//this.speaker = false; 
			this.mutex.release(); // release lock before returning the word
			
			return word;
		}
}
