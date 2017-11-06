package ResImpl;
import LockManager.*;
import java.util.*;

public class Transaction
{
    // A single transaction object
    private int id;
    private String status;
    private boolean[] rms = new int[3];
    private int[] locks = new int[3];

    public Transaction(int id) {

      this.id = id
      this.status = "ongoing";
      this.rms = {
        false,
        false,
        false
      };
      this.locks = {
        -1,
        -1,
        -1
      };


    }

    public setStatus(String status) {
      if ( status.equals("ongoing") || status.equals("finished") ) {
        this.status = status;
      }
    }

    public getStatus() {
      return this.status;
    }
}
