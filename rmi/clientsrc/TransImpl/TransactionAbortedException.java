package TransImpl;

/*
    The transaction is deadlocked.  Somebody should abort it.
*/

public class TransactionAbortedException extends Exception
{
    private int xid = 0;
    
    public TransactionAbortedException (int xid, String msg)
    {
        super("The transaction " + xid + " is aborted:" + msg);
        this.xid = xid;
    }
    
    int GetXId()
    {
        return xid;
    }
}
