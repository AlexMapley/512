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
  private static int default_tables = 4;


	public HashVault(ArrayList<RMHashtable> masterRecords) {
    this.vault = new HashMap<Integer, RMHashtable>();

    // Add default RMHashtables
    for (int i = 0; i < default_tables; i++) {
      this.vault.put(i, masterRecords.get(i));
    }

	}

	public void store(int hashKey, RMHashtable shadow) {
		this.vault.put(hashKey, shadow);
	}

  public RMHashtable retrieve(int hashKey) {
    return (RMHashtable) this.vault.get(hashKey);
  }

  // Want to store the vault to its stable file? Call this function
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
  }

  // Want to retrive the vault from its stable file? Call this function
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
    catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
  }
}
