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
	private static HashVault shadowVault;

	// We want indexe's 0,1,2,3 in our Vault to be reserved for our master records
	private int hashKey_index_start = 4;

	// Hashmap of ongoing transactions, compared to key value
	public static HashMap<Integer, Transaction> transactions = new HashMap<Integer, Transaction>();
	private CrashDetection CD;

	//Instantiate with access to MiddleWareImpl
	public TransactionManager() {
		transactionCounter = 0;
		hashKey = hashKey_index_start;

		// startDetector();
		CD = new CrashDetection(this);
		System.out.println("Transaction Manager Started...");

		// Create empty vault
		shadowVault = createNewVault();

		// Do we have a Vault already?
		File vault_spy = new File("shadowVault.ser");
		if ( vault_spy.exists() ) {
			try {
				System.out.println("Serialized in Vault");
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


						//
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
				// For NOW:
				// We are committing our transactions to master record
				// once all RM's have voted yes.

				// Clone master record table
				HashVault masterClone = createNewVault();
				masterClone.serialize_in();

				// Write transaction commits to masterClone
				System.out.println("Storing temporary transaction tables...");
				for (int i = 0; i < toCommit.RM_Commit_Ids.size(); i++) {
					try {
						// Storing temporary HashVault Backup
						masterClone.store(toCommit.RM_Commit_Ids.get(i), toCommit.activeRMs.get(i).getHash());
						masterClone.serialize_out_temp(id);
					}
					catch (IOException e) {
						e.printStackTrace();
						return false;
					}
				}

				// Serialize masterClone as the new Master Record
				System.out.println("Commiting to master tables...");
				masterClone.serialize_out();

				// CURRENTLY COMMENTED OUT:
				// This will destroy our temporary tables after we securely commit
				// I just uncommented it for now we can debug anything to do
				// with our shadows a bit more easily

				//destroy(id);


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

	public HashVault createNewVault() {
		// initialize Vault
		ArrayList<RMHashtable> default_tables = new ArrayList<RMHashtable>();
		for (int i = 0; i < hashKey_index_start; i++) {
				RMHashtable empty_table = new RMHashtable();
				default_tables.add(empty_table);
		}
		System.out.println("Creating Empty Vault...");
		HashVault emptyVault = new HashVault(default_tables);
		return emptyVault;
	}

	// Want to store the vault to its stable file? Call this function
  public void destroy(int transactionId) {
    File shadowFile = new File("shadowVault_" + transactionId + ".ser");
    if (shadowFile.exists()) {
      shadowFile.delete();
    }
  }

	public RMHashtable[] get_Masters() {
		shadowVault.serialize_in();
		RMHashtable[] master_tables = { shadowVault.retrieve(0), shadowVault.retrieve(1), shadowVault.retrieve(2), shadowVault.retrieve(3) };
		System.out.println("\n\n" + master_tables);
		System.out.println(master_tables[0]);
		System.out.println(master_tables[1]);
		System.out.println(master_tables[2]);
		System.out.println(master_tables[3]);
		return master_tables;
	}

}
