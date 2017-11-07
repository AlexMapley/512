package TransImpl;

import ResInterface.*;
import java.util.*;

public class TransactionManager
{
	private int transactionCounter;

	// Hashmap of ongoing transactions, compared to key value
	HashMap<Integer, Transaction> transactions = new HashMap<Integer, Transaction>();
	// time of last operation

	public TransactionManager() {
		transactionCounter = 0;

	}

	public int start() {
		System.out.println("\n\nTransaction Started in Manager!\n\n");
	 	transactionCounter++;
	 	transactions.put(transactionCounter, new Transaction(transactionCounter));
	 	return transactionCounter;
	}

	public boolean abort(int id) {
	  			Transaction toCommit = transactions.get(id);
		if(toCommit != null) {
			Iterator<ResourceManager> iterator = toCommit.activeRMs.iterator();
			while(iterator.hasNext()) {
				System.out.println("an rm");
				//send abort
			}
			return true;
		}
		else
			return false;
	}

	public boolean commit(int id) {
		Transaction toCommit = transactions.get(id);
		if(toCommit != null) {
			Iterator<ResourceManager> iterator = toCommit.activeRMs.iterator();
			while(iterator.hasNext()) {
				System.out.println("an rm");
				//send commit
			}
			return true;
		}
		else
			return false;
	}

	public void enlist(int id, ResourceManager rm) {
		transactions.get(id).add(rm);
	}

	public int getCounter() {
		return this.transactionCounter;
	}
}
