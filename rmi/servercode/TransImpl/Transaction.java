package TransImpl;

import ResInterface.*;
import java.util.*;
import java.io.*;
import java.rmi.RemoteException;

public class Transaction
{
	public static final int TIME2LIVE = 1000; // 1s in ms
	public int id;
	public HashSet<Integer> activeRM_Ids;
	public Set<ResourceManager> activeRMs;
	public int status; // 1 for active, 0 for finished
	private long time;

	public Transaction(int id) {
	 	this.id = id;
		this.activeRM_Ids = new HashSet<Integer>();
	 	this.activeRMs = new HashSet<ResourceManager>();
	 	this.status = 1;
	 	this.time = (new Date()).getTime();
	}

	public void add(ResourceManager rm, int hashKey) {
		this.activeRMs.add(rm);
		this.activeRM_Ids.add(hashKey)

		// Create temporary rm shadow file:
		// name should be `id``banner`.ser,
		// where `id` is our transaction id,
		// and banner is the name of our rm.
		// try {
		// 		String filename = this.id + rm.getBanner() + ".ser";
		// 		rm.store(filename);
		// }
		// catch (Exception e) {
		// 	e.printStackTrace();
		// }

	}

	public void setTime(long time) {
		this.time = time;
	}

	public long getTime() {
		return this.time;
	}
}
