package TransImpl;

import ResInterface.*;
import ResImpl.*;

import java.util.*;
import java.io.*;
import java.rmi.RemoteException;

public class HashVault
{
  // The HashVault Class:
  // Hashmap of transaction hashtable shadows, as well as our master shadows
  // Each Key is unique int, the hashKey referenced in TransactionManager
  // Each Value is a temporary or master hashtable
  private static HashMap<Integer, RMHashtable> vault;


	public HashVault() {
    this.shadowVault = new HashMap<Integer, RMHashtable>();
	}

	public void store(int hashKey, RMHashtable shadow) {
		this.vault.put(hashKey, shadow);
	}

  public RMHashtable retrieve(int hashKey) {
    return this.vault.get(hashKey);
  }

  public void serialize_out() {
    try {
      // Initialize File/Serailization Streams
      FileOutputStream file_pipe = new FileOutputStream("shadowVault.ser");
      ObjectOutputStream object_pipe = new ObjectOutputStream(file_pipe);

      // Serialize runtime Vault into stable Vault
      object_pipe.writeObject(this.vault);

      // Closes Streams
      file_pipe.close();
      object_pipe.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    catch (REmoteExcpetion e) {
      e.printStackTrace();
    }
  }

  public void serialize_in() {

    try {
      // Initialize File/Serailization Streams
      FileInputStream file_pipe = new FileInputStream("shadowVault.ser");
      InputStream input_buffer = new BufferedInputStream(file_pipe);
      ObjectInputStream object_pipe = new ObjectInputStream(input_buffer);

      // Serialize stable Vault into runtime Vault
      this.vault = (HashMap<Integer, RMHashtable>) object_pipe.readObject();

      // Closes Streams
      file_pipe.close();
      input_buffer.close();
      object_pipe.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    catch (REmoteExcpetion e) {
      e.printStackTrace();
    }
  }
}
