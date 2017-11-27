package TransImpl;

import ResInterface.*;
import java.util.*;
import

public class Transaction
{
	public static final int TIME2LIVE = 1000; // 1s in ms
	public int id;
	public Set<ResourceManager> activeRMs;
	public int status; // 1 for active, 0 for finished
	private long time;

	public Transaction(int id) {
	 	this.id = id;
	 	this.activeRMs = new HashSet<ResourceManager>();
	 	this.status = 1;
	 	this.time = (new Date()).getTime();
	}

	public void add(ResourceManager rm) {
		this.activeRMs.add(rm);

		// Create temporary rm shadow file:
		// name should be `id``banner`.ser,
		// where `id` is our transaction id,
		// and banner is the name of our rm.
		String filename = this.id + rm.banner + ".ser"
		rm.store(filename)
	}

	public void setTime(long time) {
		this.time = time;
	}

	public long getTime() {
		return this.time;
	}
}
