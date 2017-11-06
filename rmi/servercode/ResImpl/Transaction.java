package ResImpl;
import LockManager.*;
import java.util.*;

public class Transaction
{
    // A single transaction object
    private int id;
    private String status;
    private boolean[] rms = new int[3];

    public Transaction(int id) {

      this.id = id
      this.status = "ongoing";
      this.rms = {
        false,
        false,
        false
      };


    }
}
