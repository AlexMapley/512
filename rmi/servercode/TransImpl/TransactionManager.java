package TransImpl;

import ResInterface.*;
import LockImpl.*;
import java.util.*;

public class TransactionManager
{
	private int transactionCounter;

	// Hashmap of ongoing transactions, compared to key value
	HashMap<Integer, Transaction> transactions = new HashMap<Integer, Transaction>();
	// time of last operation

	//Instantiate with access to MiddleWareImpl
	public TransactionManager() {
		transactionCounter = 0;
	}

	public int start() {
		transactionCounter++;
		System.out.println("Transaction " + transactionCounter + " Started in Manager");
	 	transactions.put(transactionCounter, new Transaction(transactionCounter));
	 	return transactionCounter;
	}

	public boolean abort(int id) {
	  Transaction toCommit = transactions.get(id);
		if(toCommit != null) {
			Iterator<ResourceManager> iterator = toCommit.activeRMs.iterator();
			while(iterator.hasNext()) {
				try {
					iterator.next().abort(id);
				} catch (Exception e) {
				}
			}
			return true;
		}
		else
			return false;
	}

	public boolean commit(int id) {
		Transaction toCommit = transactions.get(id);
		boolean result = true;
		if(toCommit != null) {
			Iterator<ResourceManager> iterator = toCommit.activeRMs.iterator();
			while(iterator.hasNext()) {
				try {
					result = iterator.next().commit(id);
				} catch (Exception e) {
					break;
				}
				if (!result)
					break;
			}
			if (!result)
				return false;
			return true;
		}

		// This case will trigger an abort?
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
