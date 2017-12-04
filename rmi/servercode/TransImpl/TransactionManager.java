package TransImpl;

import ResInterface.*;
import LockImpl.*;
import ResImpl.*;

import java.util.*;
import java.io.*;
import java.rmi.RemoteException;

public class TransactionManager implements Serializable
{
	private volatile int transactionCounter;
	private static int useCase = 0;
	// Hashmap of ongoing transactions, compared to key value
	public HashMap<Integer, Transaction> transactions;

	//Instantiate with access to MiddleWareImpl
	public TransactionManager(HashMap<Integer, Transaction> transactions) {
		transactionCounter = 0;
		this.transactions = transactions;
		System.out.println("Transaction Manager Started...");
	}

	public synchronized int start(int crashCase) {
		useCase = crashCase;
		System.out.println("TM: Use case: " + useCase);
		transactionCounter++;
		synchronized(transactions) {
	 		transactions.put(transactionCounter, new Transaction(transactionCounter));
	 	}
	 	System.out.println("Transaction " + transactionCounter + " Started in Manager");
	 	return transactionCounter;
	}

	public boolean abort(int id, HashMap<RMEnum, ResourceManager> rms) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
		Transaction toAbort = transactions.get(id);
		toAbort.status = StatusEnum.ABORTED;
		if(toAbort != null) {
			// makes copy of array list to avoid concurrent exception
			ArrayList<RMEnum> temp = new ArrayList<RMEnum>();
			for(RMEnum rm : toAbort.activeRMs) {
				temp.add(rm);
			}
			for(RMEnum rm : temp) {
				try {
					rms.get(rm).abort(id);

					// delete rm from transaction list if abort was succesfull
					toAbort.activeRMs.remove(rm);
				}
				catch (RemoteException e) {
					throw e;
				}
			}

			transactions.remove(id);
			System.out.println("Transaction " + transactionCounter + " Aborted in TM");
			return true;
		}
		else
			throw new InvalidTransactionException(id, "Transaction not found for abort");

	}

	public boolean commit(int id, HashMap<RMEnum, ResourceManager> rms) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
		System.out.println("Transaction " + transactionCounter + " starting commit");
		Transaction toCommit = transactions.get(id);
		toCommit.status = StatusEnum.COMMITED;
		boolean result = true;
		if(toCommit != null) {
			// makes copy of array list to avoid concurrent exception
			ArrayList<RMEnum> temp = new ArrayList<RMEnum>();
			for(RMEnum rm : toCommit.activeRMs) {
				temp.add(rm);
			}

			for(RMEnum rm : temp) {
				try {
					result = result && rms.get(rm).commit(id);

					// CRASH CASE 6
					if (useCase == 6) {
						crash();
					}

					// delete rm from transaction list if commit was succesfull
					toCommit.activeRMs.remove(rm);
				} catch (RemoteException e) {
					throw e;
				}
				if (!result) {
					break;
				}
			}
			// delete rms from transaction list if commit was succesfull
			if (result) {
				for(RMEnum rm : temp) {

					toCommit.activeRMs.remove(rm);

					// CRASH CASE 6
					if (useCase == 6) {
						crash();
					}

					if (!result) {
						break;
					}
				}
			}

			// CRASH CASE 7
			if (useCase == 7) {
				crash();
			}

			if (!result)
				return false;
			// success
			transactions.remove(id);
			System.out.println("Transaction " + transactionCounter + " Committed in Manager");
			return true;
		}
		else
			throw new InvalidTransactionException(id, "Transaction not found for commit");

	}

	public boolean prepare(int id, HashMap<RMEnum, ResourceManager> rms) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
	    Transaction toPrepare = transactions.get(id);
	    toPrepare.status = StatusEnum.PREPARED;

	    // CRASH CASE 1
			if (useCase == 1) {
				crash();
			}

	    boolean result = true;
	    if(toPrepare != null) {
	    	// makes copy of array list to avoid concurrent exception
	    	ArrayList<RMEnum> temp = new ArrayList<RMEnum>();
	    	for(RMEnum rm : toPrepare.activeRMs) {
	    		temp.add(rm);
	    	}
		    for(RMEnum rm :temp) {
				try {
					//accumulate votes
					result = result && rms.get(rm).vote(id);

					// CRASH CASE 2,3,10
					if (useCase == 2 || useCase == 3 || useCase == 10) {
						crash();
					}

				} catch (RemoteException e) {
					throw e;
				}
				if (!result) {
					break;
				}
			}

			// CRASH CASE 4 + 5
			if (useCase == 4 || useCase == 5) {
				crash();
			}

			if (!result)
				return false;



			return true;
		}
		else
			throw new InvalidTransactionException(id, "Transaction not found for prepare");
	}

	public void startDetector() {
		// CD.start();
	}

	public void enlist(int id, RMEnum rm, HashMap<RMEnum, ResourceManager> rms) throws RemoteException, TransactionAbortedException {
		Transaction transaction = transactions.get(id);
		if(!transaction.activeRMs.contains(rm)) {
			rms.get(rm).start(id, useCase);
			transaction.add(rm);
		}
		transaction.setTime((new Date()).getTime());
	}

	public Set<ResourceManager> checkActive() {
		// Set<ResourceManager> active = new HashSet<ResourceManager>();

		// // Iterator<Transaction> transIterator = transactions.iterator();
		// for (Transaction transaction : transactions.values()) {
		// 	if(transaction.status == 1) {
		// 		Iterator<ResourceManager> rmIterator = transaction.activeRMs.iterator();
		// 		while(rmIterator.hasNext()) {
		// 			active.add(rmIterator.next());
		// 		}
		// 	}
		// 	else
		// 		continue;
		// }
		return null;
	}

	public void restart() {
		this.transactionCounter = 0;
		this.transactions.clear();
		// /CD = new CrashDetection(this);
	}

	public int getCounter() {
		return this.transactionCounter;
	}

	public void crash() {
		System.exit(0);
	}

	public void resetTimeStamps() {
		if(transactions == null)
			return;
		for (Map.Entry<Integer, Transaction> transaction : transactions.entrySet()) {
			System.out.println("Transaction #" + transaction.getKey());
			System.out.println("Transaction number (real) : " + transaction.getKey());
			System.out.println("Transaction time (old): " + transaction.getValue().getTime());
			transaction.getValue().setTime( (new Date()).getTime() );
			System.out.println("Transaction time now: " + transaction.getValue().getTime());
		}
	}
}
