package TransImpl;

/*
    The transaction is deadlocked.  Somebody should abort it.
*/

public class InvalidTransactionException extends Exception
{
    private int xid = 0;
    
    public InvalidTransactionException (int xid, String msg)
    {
        super("The transaction " + xid + " is invalid:" + msg);
        this.xid = xid;
    }
    
    int GetXId()
    {
        return xid;
    }
}
