package TransImpl;

import ResInterface.*;
import LockImpl.*;

import java.util.*;
import java.rmi.RemoteException;

public class TransactionManager
{
	private static volatile int transactionCounter;

	// Hashmap of ongoing transactions, compared to key value
	public static HashMap<Integer, Transaction> transactions = new HashMap<Integer, Transaction>();
	private CrashDetection CD;

	//Instantiate with access to MiddleWareImpl
	public TransactionManager() {
		transactionCounter = 0;
		CD = new CrashDetection(this);
		startDetector();
		System.out.println("Transaction Manager Started...");
	}

	public synchronized int start() {
		transactionCounter++;
		synchronized(transactions) {
	 		transactions.put(transactionCounter, new Transaction(transactionCounter));
	 	}
	 	System.out.println("Transaction " + transactionCounter + " Started in Manager");
	 	return transactionCounter;
	}

	public boolean abort(int id) throws InvalidTransactionException, TransactionAbortedException {
		Transaction toCommit = transactions.get(id);
		if(toCommit.status == 1) {
			toCommit.status = 0;
			if(toCommit != null) {
				Iterator<ResourceManager> iterator = toCommit.activeRMs.iterator();
				while(iterator.hasNext()) {
					try {
						iterator.next().abort(id);
					} catch (Exception e) {
						throw new TransactionAbortedException(id, "RM abort encountered an error");
					}
				}
				return true;
			}
			else
				throw new InvalidTransactionException(id, "Transaction not found for abort");
		}
		else {
			System.out.println("Transaction " + transactionCounter + " Aborted in Manager");
			return true;
		}
	}

	public boolean commit(int id) throws InvalidTransactionException, TransactionAbortedException {
		Transaction toCommit = transactions.get(id);
		if(toCommit.status == 1) {
			toCommit.status = 0;
			boolean result = true;
			if(toCommit != null) {
				Iterator<ResourceManager> iterator = toCommit.activeRMs.iterator();
				while(iterator.hasNext()) {
					try {
						result = iterator.next().commit(id);
					} catch (Exception e) {
						throw new TransactionAbortedException(id, "RM commit encountered an error and needs to abort");
					}
					if (!result) {
						break;
					}
				}
				if (!result)
					return false;
				System.out.println("Transaction " + transactionCounter + " Committed in Manager");
				return true;
			}
			else
				throw new InvalidTransactionException(id, "Transaction not found for commit");
		}
		else {
			System.out.println("Transaction " + transactionCounter + " Committed in Manager");
			return true;
		}
	}

	public void startDetector() {
		CD.start();
	}

	public void enlist(int id, ResourceManager rm) throws RemoteException, TransactionAbortedException {
		Transaction transaction = transactions.get(id);
		transaction.add(rm);
		transaction.setTime((new Date()).getTime());
		rm.start(id);
	}

	public int getCounter() {
		return this.transactionCounter;
	}
}
