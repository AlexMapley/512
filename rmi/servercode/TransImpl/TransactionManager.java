package TransImpl;

import ResInterface.*;
import LockImpl.*;
import ResImpl.*;

import java.util.*;
import java.io.*;
import java.rmi.RemoteException;

public class TransactionManager implements Serializable
{
	private static volatile int transactionCounter;
	private static volatile int hashKey;
	private static HashVault shadowVault;

	// We want indexe's 0,1,2,3 in our Vault to be reserved for our master records
	private int hashKey_index_start = 4;

	// Hashmap of ongoing transactions, compared to key value
	public static HashMap<Integer, Transaction> transactions = new HashMap<Integer, Transaction>();
	// private CrashDetection CD;

	HashMap<RMEnum, ResourceManager> rms;
	//Instantiate with access to MiddleWareImpl
	public TransactionManager() {
		transactionCounter = 0;
<<<<<<< HEAD
		hashKey = hashKey_index_start;

=======
		this.rms = rms;

		// CD = new CrashDetection(this);
>>>>>>> 0756f233f267080eb7eb98f8b2ea02e8cebc67a7
		// startDetector();
		CD = new CrashDetection(this);
		System.out.println("Transaction Manager Started...");


		// initialize Vault
		ArrayList<RMHashtable> default_Maps = new ArrayList<RMHashtable>();
		for (int i = 0; i < hashKey_index_start; i++) {
				RMHashtable empty_table = new RMHashtable();
				default_Maps.add(empty_table);
		}
		System.out.println("Creating Empty Vault...");
		shadowVault = new HashVault(default_Maps);

		// Do we have a Vault already?
		File vault_spy = new File("shadowVault.ser");
		if ( vault_spy.exists() ) {
			try {
				shadowVault.serialize_in();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			try {
				shadowVault.serialize_out();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized int start() {
		transactionCounter++;
		synchronized(transactions) {
	 		transactions.put(transactionCounter, new Transaction(transactionCounter));
	 	}
	 	System.out.println("Transaction " + transactionCounter + " Started in Manager");
	 	return transactionCounter;
	}

<<<<<<< HEAD
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
=======
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
>>>>>>> 0756f233f267080eb7eb98f8b2ea02e8cebc67a7
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
<<<<<<< HEAD
		if(toCommit.status == 1) {
			toCommit.status = 0;
			boolean result = true;
			if(toCommit != null) {
				Iterator<ResourceManager> rm_Iterator = toCommit.activeRMs.iterator();
				while(rm_Iterator.hasNext()) {
					try {
						ResourceManager rm_pointer = rm_Iterator.next();
						result = rm_pointer.commit(id);


						//
					} catch (Exception e) {
						throw new TransactionAbortedException(id, "RM commit encountered an error and needs to abort");
					}
					if (!result) {
						break;
					}
=======
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
>>>>>>> 0756f233f267080eb7eb98f8b2ea02e8cebc67a7

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
	    	for(RMEnum rm : toPrepare.activeRMs) {
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
<<<<<<< HEAD
			else
				return true;
=======
			 
			return true;
>>>>>>> 0756f233f267080eb7eb98f8b2ea02e8cebc67a7
		}
		else
			throw new InvalidTransactionException(id, "Transaction not found for prepare");
	}

	public void startDetector() {
		// CD.start();
	}

	public void enlist(int id, RMEnum rm, HashMap<RMEnum, ResourceManager> rms) throws RemoteException, TransactionAbortedException {
		Transaction transaction = transactions.get(id);
<<<<<<< HEAD

		// Add rm to transaction, with it's associated Vault hash key
		transaction.add(rm, hashKey++);
=======
		if(!transaction.activeRMs.contains(rm)) {
			rms.get(rm).start(id);
			transaction.add(rm);
		}	
>>>>>>> 0756f233f267080eb7eb98f8b2ea02e8cebc67a7
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

	public void createVault(ArrayList<RMHashtable> masterRecords) {
		this.shadowVault = new HashVault(masterRecords);
	}
}
