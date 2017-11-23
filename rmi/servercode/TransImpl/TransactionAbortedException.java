package TransImpl;

/*
    The transaction is deadlocked.  Somebody should abort it.
*/

public class TransactionAbortedException extends Exception
{
    private int xid = 0;
    
    public TransactionAbortedException (int xid, String msg)
    {
        super("The transaction " + xid + " should be aborted:");
        this.xid = xid;
    }
    
    int GetXId()
    {
        return xid;
    }
}
