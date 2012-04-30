package nachos.network;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map.Entry;

import nachos.machine.*;
import nachos.network.Sockets.socketStates;
import nachos.threads.*;

public class TransportLayer  {
	//Set timeout length for each retry 
	public static int reTransmission = 20000;
	//Set max retry here
	public static int maxRetry = 3;
	//Keep a track of ports and sockets that has been used
	public boolean[] freePorts = new boolean[128];
	LinkedList<TCPpackets> messageQueue ;
	//Packet list for a specific port
	private SynchList[] packetList;// socketQueues;
	//Socket queues for the port
	LinkedList<Sockets>[] socketQueues;
	
	private Semaphore messageReceived;      // V'd when a message can be dequeued
	private Semaphore messageSent;  // V'd when a message can be queue
	private Lock sendLock;
	private Lock sendPacketLock;

	public Condition[] packetSignal;
	public Condition sendPacketSignal;
	public HashMap<Integer, Sockets> activeSockets;


	public TransportLayer(){
		//Setting up semaphores
		messageReceived = new Semaphore(0);
		messageSent = new Semaphore(0);

		//Setting up Locks
		sendLock = new Lock();
		sendPacketLock = new Lock();
		sendPacketSignal = new Condition(sendPacketLock);

		//This list will store all packets ready to be sent
		messageQueue = new LinkedList<TCPpackets>();
		activeSockets = new HashMap<Integer,Sockets>();
		
		//Setting up ports
		packetList = new SynchList[TCPpackets.portLimit];
		socketQueues = new LinkedList[TCPpackets.portLimit];
		for (int i=0; i<packetList.length; i++){
			packetList[i] = new SynchList();
			socketQueues[i] = new LinkedList<Sockets>();
		}
		//Setting up Handlers
		Runnable receiveHandler = new Runnable(){ public void run() { receiveInterrupt(); }};
		Runnable sendHandler = new Runnable() { public void run() { sendInterrupt(); }};
		Machine.networkLink().setInterruptHandlers(receiveHandler,sendHandler);

		//Setting up threads
		KThread RecieveGuy = new KThread(new Runnable() { public void run() {packetReceive(); }});
		KThread SendGuy = new KThread(new Runnable(){ public void run() {packetSend();}});
		KThread TimeOutGuy = new KThread(new Runnable(){ public void run() {timeOut();}});

		RecieveGuy.setName("Recieving Thread");
		SendGuy.setName("Sending thread");
		TimeOutGuy.setName("TimeOut Thread");

		RecieveGuy.fork();
		SendGuy.fork();
		TimeOutGuy.fork();
	}

	/*
	 * Recieves a packet and puts it onto the correct ports 
	 */
	
	public void packetReceive(){
		while(true){
			messageReceived.P();
			Packet p = Machine.networkLink().receive();
			TCPpackets mail;

			try{
				mail = new TCPpackets(p);
			}
			catch (MalformedPacketException e) {
				continue;
			}
			if(activeSockets.containsKey(getPacketKey(mail)) && !mail.syn){
				Sockets sckt = activeSockets.get(getPacketKey(mail));
				sckt.receivedPackets.add(mail);
			}
			else{
				activeSockets.get(getPacketKey(mail)).handlePacket(mail);
			}
			// atomically add message to the mailbox and wake a waiting thread		
			//This is the first layer of the ports to hold the packets
		    //packetList[mail.dstPort].add(mail);
			freePorts[mail.dstPort] = true;
			//Need to be kept somewhere on a type of list or something...
		}
	}
	public int getPacketKey(TCPpackets p)
	{
		return p.dstPort + p.packet.dstLink  + p.srcPort + p.packet.srcLink;
	}
	public void timeOut(){
		while(true){
			NetKernel.alarm.waitUntil(reTransmission);
			for(Entry<Integer, Sockets> e: activeSockets.entrySet()){
				e.getValue().timeOutEvent();
			}
		}
	}

	public void packetSend(){
		sendPacketLock.acquire();
		while(true)
		{
			if (messageQueue.size() == 0){
				sendPacketSignal.sleep();
			}
			TCPpackets sendPackets = messageQueue.removeFirst();
			send(sendPackets);
		}
	}

	/*
	 * Add packets to the queue
	 */
	public void addMessage(TCPpackets mail)
	{
		sendPacketLock.acquire();
		messageQueue.add(mail);
		sendPacketSignal.wake();
		sendPacketLock.release();
	}

	/*
	 * Retrieve a message on the specified port, waiting if necessary.
	 */
	public TCPpackets receive(int port) {
		Lib.assertTrue(port >= 0 && port < packetList.length);
		TCPpackets mail = (TCPpackets) packetList[port].removeFirst();
		return mail;
	}

	public void send(TCPpackets mail) {
		sendLock.acquire();
		Machine.networkLink().send(mail.packet);
		messageSent.P();
		sendLock.release();
	}

	private void sendInterrupt() {
		messageSent.V();
	}
	private void receiveInterrupt() {
		messageReceived.V();
	}


	//Ports 0 to 127
	/**
	 * These functions will determine if the current port is used or not.
	 * Will only be used when trying to create a new port on the host.
	 * @return
	 */
	public int findUnusedPort(){
		int PORT = 0;
		while(CheckPort(PORT) == false){
			PORT++;
			if(PORT >= 128){
				PORT = -1;
			}
		}
		return PORT;
	}
	public boolean CheckPort(int port){
		if(freePorts[port] == true){
			return true;    
		}
		return false;
	}


	/**
	 * Read this file starting at the specified position and return the number
	 * of bytes successfully read. If no bytes were read because of a fatal
	 * error, returns -1
	 *
	 * @param       buf     the buffer to store the bytes in.
	 * @param       offset  the offset in the buffer to start storing bytes.
	 * @param       length  the number of bytes to read.
	 * @return      the actual number of bytes successfully read, or -1 on failure.
	 */   

	//Three-way-handshake: SYN, SYN-ACK, ACK
	//Try to connect from the host to the dest
	public boolean createConnection(int _destID, int _destPort, Sockets sckt){
		//Both the dest ID and Dest Port will determine the connection with that socket. As in, this socket must connect to that socket
		sckt.destID = _destID; //The socket ID to connect to, 
		sckt.destPort = _destPort; //The port where to send to
		if(sckt.states == socketStates.CLOSED){
			sckt.sendSYN();
			sckt.states = socketStates.SYNSENT;
			activeSockets.put(sckt.getKey(), sckt);
			int count = 0;
			while((sckt.states == socketStates.SYNSENT) && (count < TransportLayer.maxRetry)){
				NetKernel.alarm.waitUntil(reTransmission);
				count++;
			}
		}
		if(sckt.states == socketStates.SYNRECEIVED){
			return true;
		}
		return false;
	}

	//Try to accept the connection from the sender
	public boolean acceptConnection(Sockets sckt){
		//int port = sckt.hostPort;
		//Should always assume first packet is a syn packet
		//TCPpackets p = (TCPpackets) packetList[port].removeFirst();
			sckt.states = socketStates.ESTABLISHED;
			activeSockets.put(sckt.getKey(), sckt);
			return true;
	}

	//attempt to bind the socket to the selected port
	int bindSocket(int port){
		//states = socketStates.LISTENING;
		return -1;
	}
	public boolean closeConnection(int _destID, int _destPort, Sockets sckt){
		sckt.destID = _destID;
		sckt.destPort = _destPort;

		//have to send a fin packet
		try {
			TCPpackets fin = new TCPpackets(sckt.destID,sckt.destPort,sckt.hostID,sckt.hostPort, new byte[0],false,false,false,true,0);
			sckt.states = socketStates.SYNSENT;
		} catch (MalformedPacketException e) {
			// TODO Auto-generated catch block
			System.out.println("Malformed Packet has been detected");
			//e.printStackTrace();
			return false;
		}
		int count = 0;
		Alarm alarm = new Alarm();
		while(sckt.states== socketStates.SYNSENT && count < TransportLayer.maxRetry){
			alarm.waitUntil(TransportLayer.reTransmission);
			count++;
		}
		//if(states == socketStates.SYNRECEIVED)
		//check if sent
		//keep sending until either timeout is reached or connection 
		//if  received an ack, connection is established, return with a value saying connected
		//else return -1
		return false;
	}

}
