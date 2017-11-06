package ResImpl;

import LockManager.*;
import java.util.*;

public class TransactionManager
{
    private int transactionNumber;

    // Hashmap of ongoing transactions, compared to key value
    HashMap<int, Transaction> liveTransactions = new HashMap<int, Transaction>();

    public TransactionManager() {
        System.out.println("TM started");

        super();
        this.liveTransactions = new HashMap<int, Transaction>();
        this.transactionNumber = 0;

    }

    public newTransaction() {
      this.transactionNumber++;
      liveTransactions.put(transactionNumber, new Transaction(transactionNumber));
      return transactionNumber;
    }

    public endTransaction(int id) {
      ((Transaction) liveTransactions.get(id)).setStatus("finished");
    }
}
