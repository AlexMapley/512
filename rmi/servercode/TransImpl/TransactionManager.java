package TransImpl;

import ResInterface.*;
import LockImpl.*;
import ResImpl.*;

import java.util.*;
import java.io.*;
import java.rmi.RemoteException;

public class TransactionManager
{
	private static volatile int transactionCounter;
	private static volatile int hashKey;

	// We want indexe's 0,1,2,3 in our Vault to be reserved for our master records
	private int hashKey_index_start = 4;

	// Hashmap of ongoing transactions, compared to key value
	public static HashMap<Integer, Transaction> transactions = new HashMap<Integer, Transaction>();
	private CrashDetection CD;

	//Instantiate with access to MiddleWareImpl
	public TransactionManager() {
		transactionCounter = 0;
		hashKey = hashKey_index_start;
		CD = new CrashDetection(this);
		// startDetector();
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
				Iterator<ResourceManager> rm_Iterator = toCommit.activeRMs.iterator();
				while(rm_Iterator.hasNext()) {
					try {
						ResourceManager rm_pointer = rm_Iterator.next();
						rm_pointer.abort(id);
					}
					catch (Exception e) {
						throw new TransactionAbortedException(id, "RM abort encountered an error");
					}
				}
				System.out.println("Transaction " + transactionCounter + " Aborted in Manager");
				// throw new TransactionAbortedException(id, "Transaction: " + id + "aborted in Transaction manager");
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
				Iterator<ResourceManager> rm_Iterator = toCommit.activeRMs.iterator();
				while(rm_Iterator.hasNext()) {
					try {
						ResourceManager rm_pointer = rm_Iterator.next();
						result = rm_pointer.commit(id);
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

	public boolean prepare(int id) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
	    Transaction toPrepare = transactions.get(id);
	    boolean result = true;

	    if(toPrepare != null) {
		    Iterator<ResourceManager> rm_Iterator = toPrepare.activeRMs.iterator();
			while(rm_Iterator.hasNext()) {
				try {
					ResourceManager rm_pointer = rm_Iterator.next();
					//accumulate votes
					result = result && rm_pointer.vote(id);
				} catch (Exception e) {
					System.out.println("Transaction " + transactionCounter + " Error while receiving votes");
					return false;
				}
				if (!result) {
					break;
				}
			}
			if (!result)
				return false;
			else
				return true;
		}
		else
			throw new InvalidTransactionException(id, "Transaction not found for prepare");
	}

	public void startDetector() {
		// CD.start();
	}

	public void enlist(int id, ResourceManager rm) throws RemoteException, TransactionAbortedException {
		Transaction transaction = transactions.get(id);

		// Add rm to transaction, with it's associated Vault hash key
		transaction.add(rm, hashKey++);
		transaction.setTime((new Date()).getTime());
		rm.start(id);
	}

	public Set<ResourceManager> checkActive() {
		Set<ResourceManager> active = new HashSet<ResourceManager>();

		// Iterator<Transaction> transIterator = transactions.iterator();
		for (Transaction transaction : transactions.values()) {
			if(transaction.status == 1) {
				Iterator<ResourceManager> rmIterator = transaction.activeRMs.iterator();
				while(rmIterator.hasNext()) {
					active.add(rmIterator.next());
				}
			}
			else
				continue;
		}
		return active;
	}

	public void restart() {
		this.transactionCounter = 0;
		this.transactions.clear();
		CD = new CrashDetection(this);
	}

	public int getCounter() {
		return this.transactionCounter;
	}
}
