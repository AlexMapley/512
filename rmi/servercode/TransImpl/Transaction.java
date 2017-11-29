package TransImpl;

import ResInterface.*;
import java.util.*;
import java.io.*;
import java.rmi.RemoteException;

public class Transaction
{
	public static final int TIME2LIVE = 1000; // 1s in ms
	public int id;
	public ArrayList<Integer> RM_Commit_Ids;
	public ArrayList<ResourceManager> activeRMs;
	public int status; // 1 for active, 0 for finished
	private long time;

	public Transaction(int id) {
	 	this.id = id;
		this.RM_Commit_Ids = new ArrayList<Integer>();
	 	this.activeRMs = new ArrayList<ResourceManager>();
	 	this.status = 1;
	 	this.time = (new Date()).getTime();
	}

	public void add(ResourceManager rm, int hashKey) {

		/* Each Transaction keeps a set of hashmaps,
		as well as a corresponding set of keys.
		If it wishes to store or retreive a hashmap shadow,
		from or into one of the activeRM's hashtables,
		it will use the corresponding key at the same index
		of the ActiveRM_Id's ArrayList.
		Note that these datastructures are ArrayLists so that they are
		both indexed as well as iterable.
		*/
		this.activeRMs.add(rm);
		this.RM_Commit_Ids.add(hashKey);
	}

	public void setTime(long time) {
		this.time = time;
	}

	public long getTime() {
		return this.time;
	}
}
