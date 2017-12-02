package TransImpl;

import ResInterface.*;
import java.util.*;
import java.io.*;
import java.rmi.RemoteException;

public class Transaction implements Serializable
{
	public static final int TIME2LIVE = 10000; // 1s in ms
	public int id;
	public ArrayList<RMEnum> activeRMs;
	public int status; // 1 for active, 0 for finished
	private long time;

	public Transaction(int id) {
	 	this.id = id;
	 	this.activeRMs = new ArrayList<RMEnum>();
	 	this.status = 1;
	 	this.time = (new Date()).getTime();
	}

	public void add(RMEnum rm) {
		this.activeRMs.add(rm);
	}

	public void setTime(long time) {
		this.time = time;
	}

	public long getTime() {
		return this.time;
	}
}
