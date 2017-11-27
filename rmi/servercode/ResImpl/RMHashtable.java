// -------------------------------
// Kevin T. Manley
// CSE 593
// -------------------------------
package ResImpl;

import java.util.*;
import java.io.Serializable;


// A specialization of Hashtable with some
// extra diagnostics

public class RMHashtable extends Hashtable {


    RMHashtable() {
		    super();
  	}


 	public String toString()  {
		String s = "--- BEGIN RMHashtable ---\n";
		Object key = null;
		for (Enumeration e = keys(); e.hasMoreElements(); ) {
	  		key = e.nextElement();
	  		String value = (String) get( key ).toString();
	  		s = s + "[KEY='"+key+"']" + value + "\n";
		}
		s = s + "--- END RMHashtable ---";
		return s;
  }


  public void dump() {
		System.out.println( toString() );
  }


  public void store(String filename) {
    try {

        // Initialize File/Serailization Streams
		    FileOutputStream file_pipe = new FileOutputStream(new File("shadows/" + filename));
		    ObjectOutputStream object_pipe = new ObjectOutputStream(file_pipe);

		    // Serializes RM to file
		    object_pipe.writeObject(this);

        file_pipe.close();
        object_pipe.close();
	  }
    catch (IOException e) {
		     System.out.println("Error initializing stream");
         e.printStackTrace();
	  }
  }
}
