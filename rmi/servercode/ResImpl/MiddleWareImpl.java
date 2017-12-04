package ResImpl;

import ResInterface.*;
import TransImpl.*;

import java.util.*;
import java.io.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RMISecurityManager;

public class MiddleWareImpl implements ResourceManager
{

    private static String banner = "MW";
    private static final int port = 5959;
    private static int useCase = 0;

    public static HashMap<RMEnum, ResourceManager> rms;
    public static HashMap<RMEnum, String> hostnames = new HashMap<RMEnum, String>();
    private static TransactionManager TM;
    private static ResourceManager mw;
    private static File master;


    public static Boolean crash[];

    public static void main(String[] args) {
        String server = "localhost";  // creates middlware on current machine

        // collect inputted RMs
        rms = new HashMap<RMEnum, ResourceManager>();
        RMEnum remotes[] = new RMEnum[] { RMEnum.CAR, RMEnum.FLIGHT, RMEnum.HOTEL};

        if (args.length == 3) {
            try
            {
                for(int i=0; i<3;i++) {
                    // get a reference to the rmiregistry
                    Registry registry = LocateRegistry.getRegistry(args[i], port);

                    // get the proxy and the remote reference by rmiregistry lookup
                    ResourceManager rm = (ResourceManager) registry.lookup("group_21");

                    rm.getBanner();
                    rms.put(remotes[i], rm);
                    hostnames.put(remotes[i], args[i]);

                    System.out.println("Successful Connection to RM: " + i);
                }
            }
            catch (Exception e)
            {
                System.out.println("Unsuccessful Connecting to RM");
                System.err.println("MiddleWare exception: " + e.toString());
                e.printStackTrace();
            }

        } else {
            System.err.println ("Wrong usage");
            System.out.println("Usage: java ResImpl.MiddleWareImpl [rmaddress] X 3 ");
            System.exit(1);
        }

        //Start middleware server
        try {
            // create a new Server object
            MiddleWareImpl obj = new MiddleWareImpl();

            // dynamically generate the stub (client proxy)
            ResourceManager mw = (ResourceManager) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry(port);
            registry.rebind("group_21", mw);

            System.err.println("MiddleWare Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }

        String path = "Shadows/" + banner + "/";
        master = new File(path + "master.ser");
        // Setup transaction manager
        // TM = new TransactionManager();

        // Attempt recovery
        if(recover()) {
            System.out.println(banner + " recovered it's state");
        }
        else {
            System.out.println(banner + " started fresh");
        }



    }

    public MiddleWareImpl() throws RemoteException {
        // timeToLive();
    }


    // Reads a data item
    private RMItem readData( int id, String key ) throws TransactionAbortedException
    {
        return null;
    }

    // Writes a data item
    private void writeData( int id, String key, RMItem value ) throws TransactionAbortedException
    {
    }

    // Remove the item out of storage
    protected RMItem removeData(int id, String key) {
        return null;
    }


    // deletes the entire item
    protected boolean deleteItem(int id, String key)
    {
        return false;
    }


    // query the number of available seats/rooms/cars
    protected int queryNum(int id, String key) {
        return -1;
    }

    // query the price of an item
    protected int queryPrice(int id, String key) {
        return -1;
    }

    // reserve an item
    protected boolean reserveItem(int id, int customerID, String key, String location) {
        return false;
    }

    //  Create a new flight, or add seats to existing flight
    //  NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, RMEnum.FLIGHT, rms);
            writeFile(master, TM);
            Trace.info("RM::addFlight(" + id + ", " + flightNum + ", $" + flightPrice + ", " + flightSeats + ") called" );
            if(rms.get(RMEnum.FLIGHT).addFlight(id,flightNum,flightSeats,flightPrice))
                // call succesfull
                Trace.info("RM::addFlight(" + id + ") created or modified flight " + flightNum + ", seats=" +
                    flightSeats + ", price=$" + flightPrice );
            else {
                Trace.info("RM::addFlight encountered an unknown error");
            }
            return(true);
        } catch (RemoteException e) {
            System.out.println("Flight server crashed");
            rebind(RMEnum.FLIGHT);
            return addFlight(id, flightNum, flightSeats, flightPrice);
        }

    }

    public boolean deleteFlight(int id, int flightNum)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, RMEnum.FLIGHT, rms);
            writeFile(master, TM);
            return rms.get(RMEnum.FLIGHT).deleteFlight(id, flightNum);
        } catch (RemoteException e) {
            System.out.println("Flight server crashed");
            rebind(RMEnum.FLIGHT);
            return deleteFlight( id, flightNum);
        }
    }

    // Create a new room location or add rooms to an existing location
    //  NOTE: if price <= 0 and the room location already exists, it maintains its current price
    public boolean addRooms(int id, String location, int count, int price)
        throws RemoteException, TransactionAbortedException
    {
        try{
            TM.enlist(id, RMEnum.HOTEL, rms);
            writeFile(master, TM);
            Trace.info("RM::addRooms(" + id + ", " + location + ", " + count + ", $" + price + ") called" );
            if(rms.get(RMEnum.HOTEL).addRooms(id,location,count,price))
                // call succesfull
                Trace.info("RM::addRooms(" + id + ") created or modified room location " + location + ", count=" + count + ", price=$" + price );
            else {
                Trace.info("RM::addRooms encountered an unknown error");
            }
        } catch (RemoteException e) {
            System.out.println("Hotel server crashed");
            rebind(RMEnum.HOTEL);
            return addRooms(id, location, count, price);
        }

        return(true);
    }

    // Delete rooms from a location
    public boolean deleteRooms(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, RMEnum.HOTEL, rms);
            writeFile(master, TM);
            return rms.get(RMEnum.HOTEL).deleteRooms(id, location);
        } catch (RemoteException e) {
            System.out.println("Hotel server crashed");
            rebind(RMEnum.HOTEL);
            return deleteRooms(id, location);
        }

    }

    // Create a new car location or add cars to an existing location
    //  NOTE: if price <= 0 and the location already exists, it maintains its current price
    public boolean addCars(int id, String location, int count, int price)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, RMEnum.CAR,rms);
            writeFile(master, TM);
            if(rms.get(RMEnum.CAR).addCars(id,location,count,price))
                // call succesfull
                Trace.info("RM::addCars(" + id + ") created or modified location " + location + ", count=" + count + ", price=$" + price );
            else {
                Trace.info("RM::addCar encountered an unknown error");
            }
        } catch (RemoteException e) {
            System.out.println("Car server crashed");
            rebind(RMEnum.CAR);
            return addCars(id, location, count, price);
        }

        return(true);
    }

    // Delete cars from a location
    public boolean deleteCars(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, RMEnum.CAR,rms);
            writeFile(master, TM);
            return rms.get(RMEnum.CAR).deleteCars(id, location);
        } catch (RemoteException e) {
            System.out.println("Car server crashed");
            rebind(RMEnum.CAR);
            return deleteCars(id, location);
        }
    }

    // Returns the number of empty seats on this flight
    public int queryFlight(int id, int flightNum)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, RMEnum.FLIGHT, rms);
            writeFile(master, TM);
            return rms.get(RMEnum.FLIGHT).queryFlight(id,flightNum);
        } catch (RemoteException e) {
            System.out.println("Flight server crashed");
            rebind(RMEnum.FLIGHT);
            return queryFlight(id, flightNum);
        }
    }

    // Returns price of this flight
    public int queryFlightPrice(int id, int flightNum )
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, RMEnum.FLIGHT, rms);
            writeFile(master, TM);
            return rms.get(RMEnum.FLIGHT).queryFlightPrice(id, flightNum);
        } catch (RemoteException e) {
            System.out.println("Flight server crashed");
            rebind(RMEnum.FLIGHT);
            return queryFlightPrice(id, flightNum );
        }
    }

    // Returns the number of rooms available at a location
    public int queryRooms(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, RMEnum.HOTEL, rms);
            writeFile(master, TM);
            return rms.get(RMEnum.HOTEL).queryRooms(id, location);
        } catch (RemoteException e) {
            System.out.println("Hotel server crashed");
            rebind(RMEnum.HOTEL);
            return queryRooms(id, location);
        }
    }

    // Returns room price at this location
    public int queryRoomsPrice(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, RMEnum.HOTEL, rms);
            writeFile(master, TM);
            return rms.get(RMEnum.HOTEL).queryRoomsPrice(id, location);
        } catch (RemoteException e) {
            System.out.println("Hotel server crashed");
            rebind(RMEnum.HOTEL);
            return queryRoomsPrice(id, location);
        }
    }


    // Returns the number of cars available at a location
    public int queryCars(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, RMEnum.CAR,rms);
            writeFile(master, TM);
            return rms.get(RMEnum.CAR).queryCars(id, location);
        } catch (RemoteException e) {
            System.out.println("Car server crashed");
            rebind(RMEnum.CAR);
            return queryCars(id, location);
        }
    }


    // Returns price of cars at this location
    public int queryCarsPrice(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, RMEnum.CAR,rms);
            writeFile(master, TM);
            return rms.get(RMEnum.CAR).queryCarsPrice(id, location);
        } catch (RemoteException e) {
            System.out.println("Car server crashed");
            rebind(RMEnum.CAR);
            return queryCarsPrice(id, location);
        }
    }

    // Returns data structure containing customer reservation info. Returns null if the
    //  customer doesn't exist. Returns empty RMHashtable if customer exists but has no
    //  reservations.
    public RMHashtable getCustomerReservations(int id, int customerID)
        throws RemoteException, TransactionAbortedException
    {
        // try {
        //     Trace.info("RM::getCustomerReservations(" + id + ", " + customerID + ") called" );
        //     Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        //     if ( cust == null ) {
        //         Trace.warn("RM::getCustomerReservations failed(" + id + ", " + customerID + ") failed--customer doesn't exist" );
        //         return null;
        //     } else {
        //         return cust.getReservations();
        //     } // if
        // } catch (RemoteException e) {
        //     System.out.println("Flight server crashed");
        //     rebind(RMEnum.FLIGHT);
        //     return null;
        // }
        return null;
    }

    // return a bill
    public String queryCustomerInfo(int id, int customerID)
        throws RemoteException, TransactionAbortedException
    {
        String c,f,h;
        Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + ") called" );
        try {
            TM.enlist(id, RMEnum.CAR,rms);
            writeFile(master, TM);
            // Do this for each RM and concat the strings for return
            c = rms.get(RMEnum.CAR).queryCustomerInfo(id, customerID);
        } catch (RemoteException e) {
            System.out.println("Car server crashed");
            rebind(RMEnum.CAR);
            return queryCustomerInfo(id, customerID);
        }
        try {
            TM.enlist(id, RMEnum.FLIGHT, rms);
            writeFile(master, TM);
            f = rms.get(RMEnum.FLIGHT).queryCustomerInfo(id, customerID);
        } catch (RemoteException e) {
            System.out.println("Flight server crashed");
            rebind(RMEnum.FLIGHT);
            return queryCustomerInfo(id, customerID);        }
        try {
            TM.enlist(id, RMEnum.HOTEL, rms);
            writeFile(master, TM);
            h = rms.get(RMEnum.HOTEL).queryCustomerInfo(id, customerID);
        } catch (RemoteException e) {
            System.out.println("Hotel server crashed");
            rebind(RMEnum.HOTEL);
            return queryCustomerInfo(id, customerID);
        }
        Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + "), bill follows..." );

        // Could make this look nicer
        System.out.println( c + f + h );
        return c + f + h;

    }

    // customer functions
    // new customer just returns a unique customer identifier

    public int newCustomer(int id)
        throws RemoteException, TransactionAbortedException
    {
            Trace.info("INFO: RM::newCustomer(" + id + ") called" );
            int decision;
            // Have Car rm decide the id then
            try {
                TM.enlist(id, RMEnum.CAR,rms);
                writeFile(master, TM);
                decision = rms.get(RMEnum.CAR).newCustomer(id);
            } catch (RemoteException e) {
                System.out.println("Car server crashed");
                rebind(RMEnum.CAR);
                return newCustomer(id);
            }
            try {
                TM.enlist(id, RMEnum.FLIGHT, rms);
                writeFile(master, TM);
                rms.get(RMEnum.FLIGHT).newCustomer(id, decision);
            } catch (RemoteException e) {
                System.out.println("Flight server crashed");
                rebind(RMEnum.FLIGHT);
                return newCustomer(id);            }
            try {
                TM.enlist(id, RMEnum.HOTEL, rms);
                writeFile(master, TM);
                rms.get(RMEnum.HOTEL).newCustomer(id, decision);
            } catch (RemoteException e) {
                System.out.println("Hotel server crashed");
                rebind(RMEnum.HOTEL);
                return newCustomer(id);
            }
            Trace.info("RM::newCustomer(" + decision + ") returns ID=" + decision );
            return decision;
    }

    // I opted to pass in customerID instead. This makes testing easier
    public boolean newCustomer(int id, int customerID )
        throws RemoteException, TransactionAbortedException
    {
            Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") called" );
            boolean result;
            try {
                TM.enlist(id, RMEnum.CAR,rms);
                writeFile(master, TM);
                result = rms.get(RMEnum.CAR).newCustomer(id,customerID);
            } catch (RemoteException e) {
                System.out.println("Car server crashed");
                rebind(RMEnum.CAR);
                return newCustomer(id, customerID );
            }
            try {
                TM.enlist(id, RMEnum.FLIGHT, rms);
                writeFile(master, TM);
                result = result && rms.get(RMEnum.FLIGHT).newCustomer(id,customerID);
            } catch (RemoteException e) {
                System.out.println("Flight server crashed");
                rebind(RMEnum.FLIGHT);
                return newCustomer(id, customerID );
            }
            try {
                TM.enlist(id, RMEnum.HOTEL, rms);
                writeFile(master, TM);
                result = result && rms.get(RMEnum.HOTEL).newCustomer(id,customerID);
            } catch (RemoteException e) {
                System.out.println("Hotel server crashed");
                rebind(RMEnum.HOTEL);
                return newCustomer(id, customerID );
            }

            if(result) {
                Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") created a new customer" );
                return true;
            }
            else {
                Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") failed--customer already exists");
                return false;
            }
    }


    // Deletes customer from the database.
    public boolean deleteCustomer(int id, int customerID)
        throws RemoteException, TransactionAbortedException
    {
            Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") called" );
            boolean result;
            // remove customer info from all rms
            try {
                TM.enlist(id, RMEnum.CAR,rms);
                writeFile(master, TM);
                result = rms.get(RMEnum.FLIGHT).deleteCustomer(id,customerID);
            } catch (RemoteException e) {
                System.out.println("Car server crashed");
                rebind(RMEnum.CAR);
                return deleteCustomer(id, customerID);
            }
            try {
                TM.enlist(id, RMEnum.FLIGHT, rms);
                writeFile(master, TM);
                result = result && rms.get(RMEnum.CAR).deleteCustomer(id,customerID);
            } catch (RemoteException e) {
                System.out.println("Flight server crashed");
                rebind(RMEnum.FLIGHT);
                return deleteCustomer(id, customerID);
            }
            try {
                TM.enlist(id, RMEnum.HOTEL, rms);
                writeFile(master, TM);
                result = result && rms.get(RMEnum.HOTEL).deleteCustomer(id,customerID);
            } catch (RemoteException e) {
                System.out.println("Hotel server crashed");
                rebind(RMEnum.HOTEL);
                return deleteCustomer(id, customerID);
            }

            if(result) {
                Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") succeeded" );
                return true;
            }
            else {
                Trace.warn("RM::deleteCustomer(" + id + ", " + customerID + ") failed--customer doesn't exist" );
                return false;
            }
    }

    // Adds car reservation to this customer.
    public boolean reserveCar(int id, int customerID, String location)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, RMEnum.CAR,rms);
            writeFile(master, TM);
            return rms.get(RMEnum.CAR).reserveCar(id, customerID, location);
        } catch (RemoteException e) {
            System.out.println("Car server crashed");
            rebind(RMEnum.CAR);
            return reserveCar(id, customerID, location);
        }
    }


    // Adds room reservation to this customer.
    public boolean reserveRoom(int id, int customerID, String location)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, RMEnum.HOTEL, rms);
            writeFile(master, TM);
            return rms.get(RMEnum.HOTEL).reserveRoom(id, customerID, location);
        } catch (RemoteException e) {
            System.out.println("Hotel server crashed");
            rebind(RMEnum.HOTEL);
            return reserveRoom(id, customerID, location);
        }
    }

    // Adds flight reservation to this customer.
    public boolean reserveFlight(int id, int customerID, int flightNum)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, RMEnum.FLIGHT, rms);
            writeFile(master, TM);
            return rms.get(RMEnum.FLIGHT).reserveFlight(id, customerID, flightNum);
        } catch (RemoteException e) {
            System.out.println("Flight server crashed");
            rebind(RMEnum.FLIGHT);
            return reserveFlight(id, customerID, flightNum);
        }
    }

    // Reserve an itinerary
    public boolean itinerary(int id,int customer,Vector<Integer> flightNumbers,String location,boolean Car,boolean Room)
        throws RemoteException, TransactionAbortedException
    {
        Trace.info("RM::itinerary(" + id + ", " + customer + ") called" );
        boolean success = true;

        if(!flightNumbers.isEmpty()) {
            Iterator<Integer> flights = flightNumbers.iterator();
            while(flights.hasNext()) {
            // Reserve all flights
                int flightNum = flights.next();
                try {
                    TM.enlist(id, RMEnum.FLIGHT, rms);
                    writeFile(master, TM);
                    success = success && rms.get(RMEnum.FLIGHT).reserveFlight(id, customer, flightNum);
                } catch (RemoteException e) {
                    System.out.println("Flight server crashed");
                    rebind(RMEnum.FLIGHT);
                    return itinerary(id,customer, flightNumbers, location, Car, Room);
                }
            }

            //Reserve Car
            try {
                if(Car) {
                    TM.enlist(id, RMEnum.CAR,rms);
                    writeFile(master, TM);
                    success = success && rms.get(RMEnum.CAR).reserveCar(id, customer, location);
                }
            } catch (RemoteException e) {
                System.out.println("Car server crashed");
                rebind(RMEnum.CAR);
                return rms.get(RMEnum.CAR).reserveCar(id, customer, location);
            }
            //Reserve Room
            try {
                if(Room) {
                    TM.enlist(id, RMEnum.HOTEL, rms);
                    writeFile(master, TM);
                    success = success && rms.get(RMEnum.HOTEL).reserveRoom(id, customer, location);
                }
            } catch (RemoteException e) {
                System.out.println("Hotel server crashed");
                rebind(RMEnum.HOTEL);
                return rms.get(RMEnum.HOTEL).reserveRoom(id, customer, location);
            }
        }
        else {
            return false;
        }

        return success;
    }

    public int start(int transactionId, int crashCase) throws RemoteException {
        useCase = crashCase;
        if (crashCase != 0) {
          System.out.println("Attempting Crash Case #" + useCase + "!!!");
        }
        transactionId = TM.start(useCase);
        writeFile(master, TM);
        return transactionId;
    }

    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException {

        // CRASH CASE 11
        if (useCase == 11) {
  				crash();
  			}

        try {
            TM.prepare(transactionId, rms);
            TM.commit(transactionId, rms);
            writeFile(master, TM);
        } catch (RemoteException e) {
            for(RMEnum rm : TM.transactions.get(transactionId).activeRMs) {
                rebind(rm);
            }
            return commit(transactionId);
        } catch (InvalidTransactionException e) {
            return false;
        }
        return true;
    }

    public void abort(int transactionId) throws InvalidTransactionException, TransactionAbortedException {

        // CRASH CASE 11
        if (useCase == 1) {
  				crash();
  			}

        try {
            TM.abort(transactionId, rms);
            writeFile(master, TM);
        } catch (RemoteException e) {
            for(RMEnum rm : TM.transactions.get(transactionId).activeRMs) {
                rebind(rm);
            }
            abort(transactionId);
        }
    }

    public boolean shutdown() throws RemoteException {
        // Set<ResourceManager> active = TM.checkActive();
        // Set<ResourceManager> shutdown = new HashSet<ResourceManager>(rms);
        // shutdown.removeAll(active);

        // if(shutdown.size() != 0) {
        //     Iterator<ResourceManager> iterator = shutdown.iterator();
        //     while(iterator.hasNext()) {
        //         iterator.next().shutdown();
        //         System.out.println("Restarting a Resource Manager...");
        //     }
        // }

        // if(shutdown.size() == 3) {
        //     System.out.println("Restarting Middleware...");
        //     m_itemHT.clear();
        //     TM.restart();
        // }

        return true;
    }

    public boolean vote(int transactionId) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        // MW does not vote
        return false;
    }

    public String getBanner() throws RemoteException {
      return null;
    }

    public static void rebind(RMEnum en) {
        System.out.println("Attempting reconnect with " + en.toString() + " RM");
        int i = 0;
        while(true) {
            i++;
            try {
                Registry registry = LocateRegistry.getRegistry(hostnames.get(en), port);
                ResourceManager rm = (ResourceManager) registry.lookup("group_21");

                rm.getBanner(); // ping rm
                rms.put(en, rm); // reset rm object to new connected one

                System.out.println(en.toString() + " RM successfully reconnected!");
                return;
            } catch (Exception e) {
                System.out.print(".");
                if(i % 5 == 0)
                    System.out.println("");
                try {
                    Thread.sleep(3000); // sleep for 3 seconds
                } catch(InterruptedException ee) {

                }
            }
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

    public static boolean recover() {
        // Assumes if master exists the rest do

        // CRASH CASE 8
        if (useCase == 8) {
  				crash();
  			}

        if(master.exists()) {
                System.out.println("recovery files found, recovering...");
                // recover TM
                TM = (TransactionManager) readFile(master);

                //Updates transaction timestamps
                TM.resetTimeStamps();

                // reset all time to lives
            return true;
        }
        else {
            // create master, transactions, history files
            System.out.println("recovery files not found, creating new ones...");

            try {
                master.createNewFile();

                // write TM into master class file
                TM = new TransactionManager(new HashMap<Integer, Transaction>());
                writeFile(master, TM);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    public void resolve() {
        for(Transaction t : TM.transactions.values()) {
            try {
                if(t.status == StatusEnum.ACTIVE)
                    continue;
                if(t.status == StatusEnum.PREPARED)
                    commit(t.id);
                if(t.status == StatusEnum.COMMITED)
                    commit(t.id);
                if(t.status == StatusEnum.ABORTED)
                    abort(t.id);
            } catch(InvalidTransactionException | TransactionAbortedException e) {
                System.out.println("Transaction " + t.id + "could not be resolved");
            } catch(RemoteException e) {
                System.out.println("Unknown error called while resolving transaction " + t.id);
            }
        }
    }

    private void timeToLive() {
        Thread timeToLive = new Thread() {

          public void run() {
            while(true) {
              for (Transaction transaction : TM.transactions.values()) {
                long current = (new Date()).getTime();

                if((current - transaction.getTime()) >= transaction.TIME2LIVE) {
                  System.out.println("Transaction: " + transaction.id + " hanging, aborting...");
                  try {
                    abort(transaction.id);
                  } catch (Exception e) {
                    System.out.println(e);
                  }
                }
              }

              try {
                Thread.currentThread().sleep(1000);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }
          }
        };
    timeToLive.start();
    }

    public static void crash() {
      System.exit(0);
    }
}
