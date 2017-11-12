package TransImpl;

import ResInterface.*;
import java.util.*;

public class Transaction
{
	public static final int TIME2LIVE = 1000; // 10s in ms
	public int id;
	public Set<ResourceManager> activeRMs;
	public int status; // 0 for active, 1 for finished
	private long time;

	public Transaction(int id) {
	 	this.id = id;
	 	this.activeRMs = new HashSet<ResourceManager>();
	 	this.status = 1;
	 	this.time = (new Date()).getTime();
	}

	public void add(ResourceManager rm) {
		this.activeRMs.add(rm);
	}

	public void setTime(long time) {
		this.time = time;
	}

	public long getTime() {
		return this.time;
	}
}
