package TransImpl;

import ResInterface.*;
import LockImpl.*;
import ResImpl.*;

import java.util.*;
import java.io.*;
import java.rmi.RemoteException;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RMISecurityManager;

public class TransactionManager implements Serializable
{
	private static volatile int transactionCounter;

	// Hashmap of ongoing transactions, compared to key value
	public static HashMap<Integer, Transaction> transactions = new HashMap<Integer, Transaction>();
	// private CrashDetection CD;

	// HashMap<RMEnum, ResourceManager> rms;
	//Instantiate with access to MiddleWareImpl
	public TransactionManager() {
		transactionCounter = 0;
		// CD = new CrashDetection(this);
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

	public boolean abort(int id, HashMap<RMEnum, ResourceManager> rms) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
		Transaction toAbort = transactions.get(id);
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

			System.out.println("Transaction " + transactionCounter + " Aborted in TM");
			return true;
		}
		else
			throw new InvalidTransactionException(id, "Transaction not found for abort");

	}

	public boolean commit(int id, HashMap<RMEnum, ResourceManager> rms) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
		System.out.println("Transaction " + transactionCounter + " starting commit");
		Transaction toCommit = transactions.get(id);
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

					// delete rm from transaction list if commit was succesfull
					toCommit.activeRMs.remove(rm);
				} catch (RemoteException e) {
					throw e;
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

	public boolean prepare(int id, HashMap<RMEnum, ResourceManager> rms) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
	    Transaction toPrepare = transactions.get(id);
	    boolean result = true;

	    if(toPrepare != null) {
	    	// makes copy of array list to avoid concurrent exception
	    	ArrayList<RMEnum> temp = new ArrayList<RMEnum>();
	    	for(RMEnum rm : toPrepare.activeRMs) {+
	    		temp.add(rm);
	    	}
		    for(RMEnum rm :temp) {
				try {
					//accumulate votes
					result = result && rms.get(rm).vote(id);
				} catch (RemoteException e) {
					throw e;
				}
				if (!result) {
					break;
				}
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
			rms.get(rm).start(id);
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

	public boolean ping() {
			rm.getBanner();
	}
}
