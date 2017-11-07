package TransImpl;

import ResInterface.*;
import java.util.*;

public class Transaction
{
	// A single transaction object
	public int id;
	public List<ResourceManager> activeRMs;
	// timestamp of last operation?

	public Transaction(int id) {
	 	this.id = id;
	 	this.activeRMs = new ArrayList<ResourceManager>();
	}

	public void add(ResourceManager rm) {
		activeRMs.add(rm);
	}

}
