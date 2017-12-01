package TransImpl;

import ResInterface.*;
import java.util.*;
import java.io.*;
import java.rmi.RemoteException;

public class Transaction implements Serializable
{
	public static final int TIME2LIVE = 1000; // 1s in ms
	public int id;
<<<<<<< HEAD
	public ArrayList<Integer> activeRM_Ids;
	public ArrayList<ResourceManager> activeRMs;
=======
	public ArrayList<RMEnum> activeRMs;
>>>>>>> 0756f233f267080eb7eb98f8b2ea02e8cebc67a7
	public int status; // 1 for active, 0 for finished
	private long time;

	public Transaction(int id) {
	 	this.id = id;
<<<<<<< HEAD
		this.activeRM_Ids = new ArrayList<Integer>();
	 	this.activeRMs = new ArrayList<ResourceManager>();
=======
	 	this.activeRMs = new ArrayList<RMEnum>();
>>>>>>> 0756f233f267080eb7eb98f8b2ea02e8cebc67a7
	 	this.status = 1;
	 	this.time = (new Date()).getTime();
	}

<<<<<<< HEAD
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
		this.activeRM_Ids.add(hashKey);
=======
	public void add(RMEnum rm) {
		this.activeRMs.add(rm);
>>>>>>> 0756f233f267080eb7eb98f8b2ea02e8cebc67a7
	}

	public void setTime(long time) {
		this.time = time;
	}

	public long getTime() {
		return this.time;
	}
}
