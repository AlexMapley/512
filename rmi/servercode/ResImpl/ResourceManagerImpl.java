// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
//
package ResImpl;

import ResInterface.*;
import LockImpl.*;
import TransImpl.*;
import java.util.*;
import java.io.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RMISecurityManager;

public class ResourceManagerImpl implements ResourceManager
{

    private static String banner = "default_banner";
    public static RMHashtable m_itemHT = new RMHashtable();
    private static LockManager LM = new LockManager();
    private static HashMap<Integer, RMHashtable> transactionImages = new HashMap<Integer, RMHashtable>();
    
    // references to shadow files
    private static File master;
    private static File transactions;
    private static File locks;

    // special attribute for a "null" RMItem
    private final RMItem nullItem = new Customer(Integer.MIN_VALUE);

    public static void main(String args[]) {
        // Figure out where server is running
        String server = "localhost";
        int port = 5959;

        if (args.length == 1) {
            banner = args[0].trim();
        } else {
          System.out.println("Only one argument: name of RM server being created");
        }

        try {
            // create a new Server object
            ResourceManagerImpl obj = new ResourceManagerImpl();
            // dynamically generate the stub (client proxy)
            ResourceManager rm = (ResourceManager) UnicastRemoteObject.exportObject(obj, 0);
            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry(port);
            registry.rebind("group_21", rm);
            System.err.println(banner + " RM Server ready");
          } catch (Exception e) {
            System.err.println("RM Server exception: " + e.toString());
            e.printStackTrace();
          }

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }

        String path = "Shadows/" + banner + "/";
        master = new File(path + "master.ser");
        transactions = new File(path + "transactions.ser");
        locks = new File(path + "locks.ser");

        // Attempt recovery
        if(recover()) {
            System.out.println(banner + " RM recovered it's state");
        }
        else {
            System.out.println(banner + " RM started fresh");
        }
    }

    public ResourceManagerImpl() throws RemoteException {
    }


    // Reads a data item
    private RMItem readData( int id, String key ) throws TransactionAbortedException
    {
        try {
            LM.Lock(id, key, LM.READ);
            writeFile(locks, LM);
            RMHashtable table = (RMHashtable) transactionImages.get(id);
            RMItem item;

            RMHashtable masterTable = (RMHashtable) readFile(master);
            if(table.containsKey(key)) {
                item = (RMItem) table.get(key);
            }
            else {
                if(!masterTable.containsKey(key))
                    return null;
                else
                    item = (RMItem) masterTable.get(key);
            }

            if(item == nullItem) {
                // was a deleted item
                System.out.println("someone tried to read a deleted object");
                return null;
            }

            return item;
        } catch (DeadlockException e) {
            throw new TransactionAbortedException(id, "");
        }
    }

    // Writes a data item
    private void writeData( int id, String key, RMItem value ) throws TransactionAbortedException
    {
        try {
            LM.Lock(id, key, LM.WRITE);
            writeFile(locks, LM);
            // write to hashmap in memory
            transactionImages.get(id).put(key, value);
            // write to hashtable in file    
            writeFile(transactions, transactionImages);

        } catch (DeadlockException e) {
            throw new TransactionAbortedException(id, "");
        }
    }

    // Remove the item out of storage
    protected RMItem removeData(int id, String key) throws TransactionAbortedException
    {
        try {
            LM.Lock(id, key, LM.WRITE);
            writeFile(locks, LM);
            // write to hashmap in memory
            RMHashtable table = (RMHashtable) transactionImages.get(id);
            RMItem delete = (RMItem) table.put(key, nullItem);
            // item = (RMItem) table.remove(key);
            // instead make a "null" RMItem value with that key and push to local t table

            // write to hashtable in file    
            writeFile(transactions, transactionImages);

            return delete;
        } catch (DeadlockException e) {
            throw new TransactionAbortedException(id, "");
        }
    }


    // deletes the entire item
    protected boolean deleteItem(int id, String key) throws TransactionAbortedException
    {
        Trace.info("RM::deleteItem(" + id + ", " + key + ") called" );
        ReservableItem curObj = (ReservableItem) readData( id, key );
        // Check if there is such an item in the storage
        if ( curObj == null ) {
            Trace.warn("RM::deleteItem(" + id + ", " + key + ") failed--item doesn't exist" );
            return false;
        } else {
            if (curObj.getReserved()==0) {
                removeData(id, curObj.getKey());
                Trace.info("RM::deleteItem(" + id + ", " + key + ") item deleted" );
                return true;
            }
            else {
                Trace.info("RM::deleteItem(" + id + ", " + key + ") item can't be deleted because some customers reserved it" );
                return false;
            }
        } // if
    }


    // query the number of available seats/rooms/cars
    protected int queryNum(int id, String key) throws TransactionAbortedException {
        Trace.info("RM::queryNum(" + id + ", " + key + ") called" );
        ReservableItem curObj;
        try {
            curObj = (ReservableItem) readData( id, key);
        } catch (ClassCastException e) {
            System.out.println("This item was probably deleted");
            curObj = null;
        }
        int value = 0;
        if ( curObj != null ) {
            value = curObj.getCount();
        } // else
        Trace.info("RM::queryNum(" + id + ", " + key + ") returns count=" + value);
        return value;
    }

    // query the price of an item
    protected int queryPrice(int id, String key) throws TransactionAbortedException {
        Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") called" );
        ReservableItem curObj;
        try {
            curObj = (ReservableItem) readData( id, key);
        } catch (ClassCastException e) {
            System.out.println("This item was probably deleted");
            curObj = null;
        }
        int value = 0;
        if ( curObj != null ) {
            value = curObj.getPrice();
        } // else
        Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") returns cost=$" + value );
        return value;
    }

    // reserve an item
    protected boolean reserveItem(int id, int customerID, String key, String location) throws TransactionAbortedException {
        Trace.info("RM::reserveItem( " + id + ", customer=" + customerID + ", " +key+ ", "+location+" ) called" );
        // Read customer object if it exists (and read lock it)
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key + ", "+location+")  failed--customer doesn't exist" );
            return false;
        }

        // check if the item is available
        ReservableItem item;
        try {
            item = (ReservableItem) readData( id, key);
        } catch (ClassCastException e) {
            System.out.println("This item was probably deleted");
            item = null;
        }
        if ( item == null ) {
            Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key+", " +location+") failed--item doesn't exist" );
            return false;
        } else if (item.getCount()==0) {
            Trace.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key+", " + location+") failed--No more items" );
            return false;
        } else {
            cust.reserve( key, location, item.getPrice());
            writeData( id, cust.getKey(), cust );

            // decrease the number of available items in the storage
            item.setCount(item.getCount() - 1);
            item.setReserved(item.getReserved()+1);

            Trace.info("RM::reserveItem( " + id + ", " + customerID + ", " + key + ", " +location+") succeeded" );
            return true;
        }
    }

    // Create a new flight, or add seats to existing flight
    //  NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
        throws RemoteException, TransactionAbortedException
    {
        Trace.info("RM::addFlight(" + id + ", " + flightNum + ", $" + flightPrice + ", " + flightSeats + ") called" );
        Flight curObj = (Flight) readData( id, Flight.getKey(flightNum) );
        if ( curObj == null ) {
            // doesn't exist...add it
            Flight newObj = new Flight( flightNum, flightSeats, flightPrice );
            writeData( id, newObj.getKey(), newObj );
            Trace.info("RM::addFlight(" + id + ") created new flight " + flightNum + ", seats=" +
                    flightSeats + ", price=$" + flightPrice );
        } else {
            // add seats to existing flight and update the price...
            curObj.setCount( curObj.getCount() + flightSeats );
            if ( flightPrice > 0 ) {
                curObj.setPrice( flightPrice );
            } // if
            writeData( id, curObj.getKey(), curObj );
            Trace.info("RM::addFlight(" + id + ") modified existing flight " + flightNum + ", seats=" + curObj.getCount() + ", price=$" + flightPrice );
        } // else
        return(true);
    }



    public boolean deleteFlight(int id, int flightNum)
        throws RemoteException, TransactionAbortedException
    {
        return deleteItem(id, Flight.getKey(flightNum));
    }



    // Create a new room location or add rooms to an existing location
    //  NOTE: if price <= 0 and the room location already exists, it maintains its current price
    public boolean addRooms(int id, String location, int count, int price)
        throws RemoteException, TransactionAbortedException
    {
        Trace.info("RM::addRooms(" + id + ", " + location + ", " + count + ", $" + price + ") called" );
        Hotel curObj = (Hotel) readData( id, Hotel.getKey(location) );
        if ( curObj == null ) {
            // doesn't exist...add it
            Hotel newObj = new Hotel( location, count, price );
            writeData( id, newObj.getKey(), newObj );
            Trace.info("RM::addRooms(" + id + ") created new room location " + location + ", count=" + count + ", price=$" + price );
        } else {
            // add count to existing object and update price...
            curObj.setCount( curObj.getCount() + count );
            if ( price > 0 ) {
                curObj.setPrice( price );
            } // if
            writeData( id, curObj.getKey(), curObj );
            Trace.info("RM::addRooms(" + id + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price );
        } // else
        return(true);
    }

    // Delete rooms from a location
    public boolean deleteRooms(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        return deleteItem(id, Hotel.getKey(location));

    }

    // Create a new car location or add cars to an existing location
    //  NOTE: if price <= 0 and the location already exists, it maintains its current price
    public boolean addCars(int id, String location, int count, int price)
        throws RemoteException, TransactionAbortedException
    {
        Trace.info("RM::addCars(" + id + ", " + location + ", " + count + ", $" + price + ") called" );
        Car curObj = (Car) readData( id, Car.getKey(location) );
        if ( curObj == null ) {
            // car location doesn't exist...add it
            Car newObj = new Car( location, count, price );
            writeData( id, newObj.getKey(), newObj );
            Trace.info("RM::addCars(" + id + ") created new location " + location + ", count=" + count + ", price=$" + price );
        } else {
            // add count to existing car location and update price...
            curObj.setCount( curObj.getCount() + count );
            if ( price > 0 ) {
                curObj.setPrice( price );
            } // if
            writeData( id, curObj.getKey(), curObj );
            Trace.info("RM::addCars(" + id + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price );
        } // else
        return(true);
    }

    // Delete cars from a location
    public boolean deleteCars(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        return deleteItem(id, Car.getKey(location));
    }


    // Returns the number of empty seats on this flight
    public int queryFlight(int id, int flightNum)
        throws RemoteException, TransactionAbortedException
    {
        return queryNum(id, Flight.getKey(flightNum));
    }

    // Returns price of this flight
    public int queryFlightPrice(int id, int flightNum )
        throws RemoteException, TransactionAbortedException
    {
        return queryPrice(id, Flight.getKey(flightNum));
    }

    // Returns the number of rooms available at a location
    public int queryRooms(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        return queryNum(id, Hotel.getKey(location));
    }

    // Returns room price at this location
    public int queryRoomsPrice(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        return queryPrice(id, Hotel.getKey(location));
    }

    // Returns the number of cars available at a location
    public int queryCars(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        return queryNum(id, Car.getKey(location));
    }

    // Returns price of cars at this location
    public int queryCarsPrice(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        return queryPrice(id, Car.getKey(location));
    }

    // Returns data structure containing customer reservation info. Returns null if the
    //  customer doesn't exist. Returns empty RMHashtable if customer exists but has no
    //  reservations.
    public RMHashtable getCustomerReservations(int id, int customerID)
        throws RemoteException, TransactionAbortedException
    {
        Trace.info("RM::getCustomerReservations(" + id + ", " + customerID + ") called" );
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            Trace.warn("RM::getCustomerReservations failed(" + id + ", " + customerID + ") failed--customer doesn't exist" );
            return null;
        } else {
            return cust.getReservations();
        } // if
    }

    // return a bill
    public String queryCustomerInfo(int id, int customerID)
        throws RemoteException, TransactionAbortedException
    {
        Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + ") called" );
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            Trace.warn("RM::queryCustomerInfo(" + id + ", " + customerID + ") failed--customer doesn't exist" );
            return "";   // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
        } else {
                String s = cust.printBill();
                Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + "), bill follows..." );
                System.out.println( s );
                return s;
        } // if
    }

    // customer functions
    // new customer just returns a unique customer identifier

    public int newCustomer(int id)
        throws RemoteException, TransactionAbortedException
    {
        Trace.info("INFO: RM::newCustomer(" + id + ") called" );
        // Generate a globally unique ID for the new customer
        int cid = Integer.parseInt( String.valueOf(id) +
                                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                                String.valueOf( Math.round( Math.random() * 100 + 1 )));
        Customer cust = new Customer( cid );
        writeData( id, cust.getKey(), cust );
        Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid );
        return cid;
    }

    // I opted to pass in customerID instead. This makes testing easier
    public boolean newCustomer(int id, int customerID )
        throws RemoteException, TransactionAbortedException
    {
        Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") called" );
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            cust = new Customer(customerID);
            writeData( id, cust.getKey(), cust );
            Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") created a new customer" );
            return true;
        } else {
            Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") failed--customer already exists");
            return false;
        } // else
    }


    // Deletes customer from the database.
    public boolean deleteCustomer(int id, int customerID)
        throws RemoteException, TransactionAbortedException
    {
        Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") called" );
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            Trace.warn("RM::deleteCustomer(" + id + ", " + customerID + ") failed--customer doesn't exist" );
            return false;
        } else {
            // Increase the reserved numbers of all reservable items which the customer reserved.
            RMHashtable reservationHT = cust.getReservations();
            for (Enumeration e = reservationHT.keys(); e.hasMoreElements();) {
                String reservedkey = (String) (e.nextElement());
                ReservedItem reserveditem = cust.getReservedItem(reservedkey);
                Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") has reserved " + reserveditem.getKey() + " " +  reserveditem.getCount() +  " times"  );
                ReservableItem item  = (ReservableItem) readData(id, reserveditem.getKey());
                Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") has reserved " + reserveditem.getKey() + "which is reserved" +  item.getReserved() +  " times and is still available " + item.getCount() + " times"  );
                item.setReserved(item.getReserved()-reserveditem.getCount());
                item.setCount(item.getCount()+reserveditem.getCount());
            }

            // remove the customer from the storage
            removeData(id, cust.getKey());

            Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") succeeded" );
            return true;
        } // if
    }

    // Adds car reservation to this customer.
    public boolean reserveCar(int id, int customerID, String location)
        throws RemoteException, TransactionAbortedException
    {
        return reserveItem(id, customerID, Car.getKey(location), location);
    }


    // Adds room reservation to this customer.
    public boolean reserveRoom(int id, int customerID, String location)
        throws RemoteException, TransactionAbortedException
    {
        return reserveItem(id, customerID, Hotel.getKey(location), location);
    }
    // Adds flight reservation to this customer.
    public boolean reserveFlight(int id, int customerID, int flightNum)
        throws RemoteException, TransactionAbortedException
    {
        return reserveItem(id, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
    }

    // Reserve an itinerary
    public boolean itinerary(int id,int customer,Vector<Integer> flightNumbers,String location,boolean Car,boolean Room)
        throws RemoteException, TransactionAbortedException
    {
        return false;
    }

    public int start(int transactionId) throws RemoteException {
        // Place an empty hashtable for that tid
        if(!transactionImages.containsKey(transactionId)) {
            transactionImages.put(transactionId, new RMHashtable());
            System.out.println("Transaction: " + transactionId + " Started");
        }
        else
            System.out.println("Transaction: " + transactionId + " submits an operation");
        return 0;
    }

    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        // Release transaction locks
        if(LM.UnlockAll(transactionId)) {
            writeFile(locks, LM);

            // push changes to in memory and file master hashtables
            update(transactionId);
            writeFile(master, m_itemHT);

            // // reset in memory master
            // m_itemHT = (RMHashtable) readFile(master);

            // delete transaction from memory and file hashmap
            transactionImages.remove(transactionId);
            writeFile(transactions, transactionImages);

            System.out.println("Transaction: " + transactionId + " Commited");
            return true;
        }

        throw new TransactionAbortedException(transactionId, "Error during commit on transaction: " + transactionId);
    }

    public void abort(int transactionId) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        // Release transaction locks
        if(LM.UnlockAll(transactionId)) {
            writeFile(locks, LM);
            // delete transaction from memory and file hashmap
            transactionImages.remove(transactionId);
            writeFile(transactions, transactionImages);

            //update in memory hashtable
            // m_itemHT = (RMHashtable) readFile(master);

            System.out.println("Transaction: " + transactionId + " Aborted");
        }
        else
            throw new TransactionAbortedException(transactionId, "Locks could not be released");
    }

    public boolean shutdown() throws RemoteException {
        System.out.println("Restarting...");
        m_itemHT.clear();
        LM = new LockManager();
        transactionImages.clear();
        return true;
    }

    public boolean vote(int transactionId) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        // Do something more here??
        // check if that transaction has a table
        // if not throw exception

        System.out.println("Sending Yes vote for transaction: " + transactionId);
        return true;
    }

    public String getBanner() {
      return banner;
    }

    public static boolean recover() {
        // Assumes if master exists the rest do
        if(master.exists()) {
                System.out.println("recovery files found, recovering...");

                // recover transactionImages hash map
                transactionImages = (HashMap<Integer, RMHashtable>) readFile(transactions);

                // recover lock table
                LM = (LockManager) readFile(locks);

                // recovering m_itemHT not necessary as it happens in a commit
            return true;
        }
        else {
            // create master, transactions, history files
            System.out.println("recovery files not found, creating new ones...");
            
            try {
                master.createNewFile();
                transactions.createNewFile();
                locks.createNewFile();

                // write empty hash table into master class file
                writeFile(master, m_itemHT);
                // write empty hash map into transactions class file
                writeFile(transactions, transactionImages);
                // write empty lock manager class file
                writeFile(locks, LM);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    public static Object readFile(File file) {
        // helper function that returns a input stream 
        // "master" for master file
        // "transactions" for transaction file
        // "history" for log file
        try {
            FileInputStream pipe = new FileInputStream(file.getAbsolutePath());
            InputStream buffer = new BufferedInputStream(pipe);
            ObjectInputStream object_pipe = new ObjectInputStream(buffer);
            
            Object object = object_pipe.readObject();

            pipe.close();
            buffer.close();
            object_pipe.close();
            return object;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void writeFile(File file, Object object) {
        // helper function that writes an object to a file
        // "master" for master file
        // "transactions" for transaction file
        // "history" for log file

        try {
            FileOutputStream pipe = new FileOutputStream(file.getAbsolutePath());
            ObjectOutputStream object_pipe = new ObjectOutputStream(pipe);
            
            object_pipe.writeObject(object);

            pipe.close();
            object_pipe.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void update(int id) {
        // retrieve working set
        RMHashtable transaction = (RMHashtable) transactionImages.get(id);
        
        // Update in memory master hash table
        synchronized(m_itemHT) {
            m_itemHT = (RMHashtable) readFile(master);
            Set<String> keys = transaction.keySet();
            for(String key : keys) {
                if(transaction.get(key) == nullItem) {
                    if(m_itemHT.containsKey(key)) {
                        m_itemHT.remove(key);
                    }
                }
                else
                    m_itemHT.put(key, transaction.get(key));
            }
        }
    }   
}
