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

    protected RMHashtable m_itemHT = new RMHashtable();
    private static HashMap<Integer, RMHashtable> transactionImages = new HashMap<Integer, RMHashtable>();

    static ResourceManager CarRM = null;
    static ResourceManager HotelRM = null;
    static ResourceManager FlightRM = null;
    static ArrayList<ResourceManager> rms;

    private static TransactionManager TM;

    public static void main(String[] args) {
        int port = 5959;  // hardcoded
        String server = "localhost";  // creates middlware on current machine

        // collect inputted RMs
        rms = new ArrayList<ResourceManager>();

        if (args.length == 3) {
            try
            {
                for(int i=0; i<3;i++) {
                    // get a reference to the rmiregistry
                    Registry registry = LocateRegistry.getRegistry(args[i], port);
                    // get the proxy and the remote reference by rmiregistry lookup
                    rms.add((ResourceManager) registry.lookup("group_21"));
                    if(rms.get(i) != null)
                    {
                        System.out.println("Successful Connection to RM: " + i);
                    }
                    else
                    {
                        System.out.println("Unsuccessful Connecting to RM: " + i);
                    }
                }
            }
            catch (Exception e)
            {
                System.err.println("MiddleWare exception: " + e.toString());
                e.printStackTrace();
            }

        } else {
            System.err.println ("Wrong usage");
            System.out.println("Usage: java ResImpl.MiddleWareImpl [rmaddress] X 3 ");
            System.exit(1);
        }
        // Associate inputted machines to respective RMs
        CarRM = rms.get(0);
        HotelRM = rms.get(1);
        FlightRM = rms.get(2);
        String banner1 = "Cars";
        String banner2 = "Hotels";
        String banner3 = "Flights";

        try {
            System.out.println(CarRM.getBanner());
            System.out.println(HotelRM.getBanner());
            System.out.println(FlightRM.getBanner());
            CarRM.setBanner(banner1);
            HotelRM.setBanner(banner2);
            FlightRM.setBanner(banner3);
        }
        catch (Exception e) {
          e.printStackTrace();
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

        // Setup transaction manager
        TM = new TransactionManager();

    }

    public MiddleWareImpl() throws RemoteException {

    }


    // Reads a data item
    private RMItem readData( int id, String key )
    {
        synchronized(m_itemHT) {
            return (RMItem) m_itemHT.get(key);
        }
    }

    // Writes a data item
    private void writeData( int id, String key, RMItem value )
    {
        synchronized(m_itemHT) {
            m_itemHT.put(key, value);
        }
    }

    // Remove the item out of storage
    protected RMItem removeData(int id, String key) {
        synchronized(m_itemHT) {
            return (RMItem)m_itemHT.remove(key);
        }
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
        Trace.info("RM::reserveItem( " + id + ", customer=" + customerID + ", " +key+ ", "+location+" ) called" );
        // Read customer object if it exists (and read lock it)
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            Trace.warn("RM::reserveCar( " + id + ", " + customerID + ", " + key + ", "+location+")  failed--customer doesn't exist" );
            return false;
        }

        // check if the item is available
        ReservableItem item = (ReservableItem)readData(id, key);
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

    //  Create a new flight, or add seats to existing flight
    //  NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
        throws RemoteException, TransactionAbortedException
    {
        TM.enlist(id, FlightRM);
        Trace.info("RM::addFlight(" + id + ", " + flightNum + ", $" + flightPrice + ", " + flightSeats + ") called" );
        try {
            if(FlightRM.addFlight(id,flightNum,flightSeats,flightPrice))
            // if(rm.addFlight(id,flightNum,flightSeats,flightPrice))
                // call succesfull
                Trace.info("RM::addFlight(" + id + ") created or modified flight " + flightNum + ", seats=" +
                    flightSeats + ", price=$" + flightPrice );

            else {
                Trace.info("RM::addFlight encountered an unknown error");
            }
        }
        catch(Exception e){
          System.out.println("EXCEPTION:");
          System.out.println(e.getMessage());
        }
        return(true);
    }

    public boolean deleteFlight(int id, int flightNum)
        throws RemoteException, TransactionAbortedException
    {
        TM.enlist(id, FlightRM);
        return FlightRM.deleteFlight(id, flightNum);
        // return rm.deleteFlight(id, flightNum);
    }

    // Create a new room location or add rooms to an existing location
    //  NOTE: if price <= 0 and the room location already exists, it maintains its current price
    public boolean addRooms(int id, String location, int count, int price)
        throws RemoteException, TransactionAbortedException
    {
        TM.enlist(id, HotelRM);
        Trace.info("RM::addRooms(" + id + ", " + location + ", " + count + ", $" + price + ") called" );
        try {
            if(HotelRM.addRooms(id,location,count,price))
            // if(rm.addRooms(id,location,count,price))
                // call succesfull
                Trace.info("RM::addRooms(" + id + ") created or modified room location " + location + ", count=" + count + ", price=$" + price );
            else {
                Trace.info("RM::addRooms encountered an unknown error");
            }
        }
        catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
        }
        return(true);
    }

    // Delete rooms from a location
    public boolean deleteRooms(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        TM.enlist(id, HotelRM);
        return HotelRM.deleteRooms(id, location);
        // return rm.deleteRooms(id, location);

    }

    // Create a new car location or add cars to an existing location
    //  NOTE: if price <= 0 and the location already exists, it maintains its current price
    public boolean addCars(int id, String location, int count, int price)
        throws RemoteException, TransactionAbortedException
    {
        TM.enlist(id, CarRM);
        try {
            if(CarRM.addCars(id,location,count,price))
            // if(rm.addCars(id,location,count,price))
                // call succesfull
                Trace.info("RM::addCars(" + id + ") created or modified location " + location + ", count=" + count + ", price=$" + price );
            else {
                Trace.info("RM::addCar encountered an unknown error");
            }
        }
        catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
        }
        return(true);
    }


    // Delete cars from a location
    public boolean deleteCars(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        TM.enlist(id, CarRM);
        return CarRM.deleteCars(id, location);
    }



    // Returns the number of empty seats on this flight
    public int queryFlight(int id, int flightNum)
        throws RemoteException, TransactionAbortedException
    {
        TM.enlist(id, FlightRM);
        return FlightRM.queryFlight(id,flightNum);
    }

    // Returns the number of reservations for this flight.
//    public int queryFlightReservations(int id, int flightNum)
//        throws RemoteException
//    {
//        Trace.info("RM::queryFlightReservations(" + id + ", #" + flightNum + ") called" );
//        RMInteger numReservations = (RMInteger) readData( id, Flight.getNumReservationsKey(flightNum) );
//        if ( numReservations == null ) {
//            numReservations = new RMInteger(0);
//        } // if
//        Trace.info("RM::queryFlightReservations(" + id + ", #" + flightNum + ") returns " + numReservations );
//        return numReservations.getValue();
//    }


    // Returns price of this flight
    public int queryFlightPrice(int id, int flightNum )
        throws RemoteException, TransactionAbortedException
    {
        TM.enlist(id, FlightRM);
        return FlightRM.queryFlightPrice(id, flightNum);
    }


    // Returns the number of rooms available at a location
    public int queryRooms(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        TM.enlist(id, HotelRM);
        return HotelRM.queryRooms(id, location);
    }

    // Returns room price at this location
    public int queryRoomsPrice(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        TM.enlist(id, HotelRM);
        return HotelRM.queryRoomsPrice(id, location);
    }


    // Returns the number of cars available at a location
    public int queryCars(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        TM.enlist(id, CarRM);
        return CarRM.queryCars(id, location);
    }


    // Returns price of cars at this location
    public int queryCarsPrice(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        TM.enlist(id, CarRM);
        return CarRM.queryCarsPrice(id, location);
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
        TM.enlist(id, CarRM);
        TM.enlist(id, FlightRM);
        TM.enlist(id, HotelRM);
        Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + ") called" );
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            Trace.warn("RM::queryCustomerInfo(" + id + ", " + customerID + ") failed--customer doesn't exist" );
            return "";   // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
        } else {
            // Do this for each RM and concat the strings for return
            String c = CarRM.queryCustomerInfo(id, customerID);
            String f = FlightRM.queryCustomerInfo(id, customerID);
            String h = HotelRM.queryCustomerInfo(id, customerID);
            Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + "), bill follows..." );

            // Could make this look nicer
            System.out.println( c + f + h );
            return c + f + h;
        } // if
    }

    // customer functions
    // new customer just returns a unique customer identifier

    public int newCustomer(int id)
        throws RemoteException, TransactionAbortedException
    {
        TM.enlist(id, CarRM);
        TM.enlist(id, FlightRM);
        TM.enlist(id, HotelRM);
        Trace.info("INFO: RM::newCustomer(" + id + ") called" );
        // Generate a globally unique ID for the new customer
        int cid = Integer.parseInt( String.valueOf(id) +
                                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                                String.valueOf( Math.round( Math.random() * 100 + 1 )));
        Customer cust = new Customer( cid );
        writeData( id, cust.getKey(), cust );
        Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid );

        // Create this customer on every rm but pass in the created cid
         try {
                CarRM.newCustomer(id,cid);
                FlightRM.newCustomer(id,cid);
                HotelRM.newCustomer(id,cid);
            }
            catch(Exception e) {
                System.out.println("EXCEPTION:");
                System.out.println(e.getMessage());
            }
        return cid;
    }

    // I opted to pass in customerID instead. This makes testing easier
    public boolean newCustomer(int id, int customerID )
        throws RemoteException, TransactionAbortedException
    {
        TM.enlist(id, CarRM);
        TM.enlist(id, FlightRM);
        TM.enlist(id, HotelRM);
        Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") called" );
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            cust = new Customer(customerID);
            writeData( id, cust.getKey(), cust );
            Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") created a new customer" );

            try {
                // If customer doesn't exist in middleware it won't exist in any rms
                CarRM.newCustomer(id,customerID);
                FlightRM.newCustomer(id,customerID);
                HotelRM.newCustomer(id,customerID);
            }
            catch(Exception e) {
                System.out.println("EXCEPTION:");
                System.out.println(e.getMessage());
            }

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
        TM.enlist(id, CarRM);
        TM.enlist(id, FlightRM);
        TM.enlist(id, HotelRM);
        Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") called" );
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            Trace.warn("RM::deleteCustomer(" + id + ", " + customerID + ") failed--customer doesn't exist" );
            return false;
        } else {
            // remove customer info from all rms
            CarRM.deleteCustomer(id, customerID);
            FlightRM.deleteCustomer(id, customerID);
            HotelRM.deleteCustomer(id, customerID);

            // remove the customer from the middleware storage
            removeData(id, cust.getKey());

            Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") succeeded" );
            return true;
        } // if
    }

    /*
    // Frees flight reservation record. Flight reservation records help us make sure we
    // don't delete a flight if one or more customers are holding reservations
    public boolean freeFlightReservation(int id, int flightNum)
        throws RemoteException
    {
        Trace.info("RM::freeFlightReservations(" + id + ", " + flightNum + ") called" );
        RMInteger numReservations = (RMInteger) readData( id, Flight.getNumReservationsKey(flightNum) );
        if ( numReservations != null ) {
            numReservations = new RMInteger( Math.max( 0, numReservations.getValue()-1) );
        } // if
        writeData(id, Flight.getNumReservationsKey(flightNum), numReservations );
        Trace.info("RM::freeFlightReservations(" + id + ", " + flightNum + ") succeeded, this flight now has "
                + numReservations + " reservations" );
        return true;
    }
    */

    // Adds car reservation to this customer.
    public boolean reserveCar(int id, int customerID, String location)
        throws RemoteException, TransactionAbortedException
    {
        TM.enlist(id, CarRM);
        return CarRM.reserveCar(id, customerID, location);
        // return rm.reserveCar(id, customerID, location);
    }


    // Adds room reservation to this customer.
    public boolean reserveRoom(int id, int customerID, String location)
        throws RemoteException, TransactionAbortedException
    {
        TM.enlist(id, HotelRM);
        return HotelRM.reserveRoom(id, customerID, location);
        // return rm.reserveRoom(id, customerID, location);
    }

    // Adds flight reservation to this customer.
    public boolean reserveFlight(int id, int customerID, int flightNum)
        throws RemoteException, TransactionAbortedException
    {
        TM.enlist(id, FlightRM);
        return FlightRM.reserveFlight(id, customerID, flightNum);
        // return rm.reserveFlight(id, customerID, flightNum);
    }

    // Reserve an itinerary
    public boolean itinerary(int id,int customer,Vector<Integer> flightNumbers,String location,boolean Car,boolean Room)
        throws RemoteException, TransactionAbortedException
    {
        TM.enlist(id, CarRM);
        TM.enlist(id, FlightRM);
        TM.enlist(id, HotelRM);
        Trace.info("RM::itinerary(" + id + ", " + customer + ") called" );
        boolean success = true;
        try {
            if(!flightNumbers.isEmpty()) {
                Iterator<Integer> flights = flightNumbers.iterator();
                while(flights.hasNext()) {
                // Reserve all flights
                    // System.out.println(flights.next());
                    int flightNum = flights.next();
                    success = success && FlightRM.reserveFlight(id, customer, flightNum);
                }

                //Reserve Car
                if(Car)
                    success = success && CarRM.reserveCar(id, customer, location);

                //Reserve Room
                if(Room)
                    success = success && HotelRM.reserveRoom(id, customer, location);
            }
            else {
                return false;
            }
        } catch(Exception e) {
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
        }

        return success;
    }

    public void spamAllNewItem(int transactionId) throws RemoteException, TransactionAbortedException {
        Trace.info("RM::spamAllNewItem(" + transactionId + ") called" );
        TM.enlist(transactionId, CarRM);
        TM.enlist(transactionId, HotelRM);
        TM.enlist(transactionId, FlightRM);
        try {
            if(CarRM.addCars(transactionId,"a",1,1))
                Trace.info("RM::addCars(" + transactionId + ") created or modified location " + "a" + ", count=" + 1 + ", price=$" + 1 );

            if(HotelRM.addRooms(transactionId,"a",1,1))
                Trace.info("RM::addRooms(" + transactionId + ") created or modified flight " + 1 + ", seats=" +
                    1 + ", price=$" + 1 );
            if(FlightRM.addFlight(transactionId,1,1,1))
                Trace.info("RM::addFlight(" + transactionId + ") created or modified flight " + 1 + ", seats=" +
                    1 + ", price=$" + 1 );
        } catch(Exception e) {
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
        }

    }

    public void spamFlightNewItem(int transactionId) throws RemoteException, TransactionAbortedException {
        Trace.info("RM::spamAllNewItem(" + transactionId + ") called" );
        TM.enlist(transactionId, FlightRM);
        try {
            if(FlightRM.addFlight(transactionId,1,1,1))
                Trace.info("RM::addFlight(" + transactionId + ") created or modified flight " + 1 + ", seats=" +
                    1 + ", price=$" + 1 );
            if(FlightRM.addFlight(transactionId,2,1,1))
                Trace.info("RM::addFlight(" + transactionId + ") created or modified flight " + 2 + ", seats=" +
                    1 + ", price=$" + 1 );
            if(FlightRM.addFlight(transactionId,3,1,1))
                Trace.info("RM::addFlight(" + transactionId + ") created or modified flight " + 3 + ", seats=" +
                    1 + ", price=$" + 1 );

        } catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
        }

    }

    public int start(int transactionId) throws RemoteException {
        transactionId = TM.start();
        transactionImages.put(transactionId, (RMHashtable) m_itemHT.clone());
        return transactionId;
    }

    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        boolean result = TM.commit(transactionId);
        return result;
    }

    public void abort(int transactionId) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        // Reset DB from transaction image
        // System.out.println(m_itemHT.get(Customer.getKey(customerID)));
        RMHashtable reset = transactionImages.get(transactionId);
        if(reset != null) {
            m_itemHT = (RMHashtable) reset.clone();
        }

        TM.abort(transactionId);
    }

    public boolean shutdown() throws RemoteException {
        Set<ResourceManager> active = TM.checkActive();
        Set<ResourceManager> shutdown = new HashSet<ResourceManager>(rms);
        shutdown.removeAll(active);

        if(shutdown.size() != 0) {
            Iterator<ResourceManager> iterator = shutdown.iterator();
            while(iterator.hasNext()) {
                iterator.next().shutdown();
                System.out.println("Restarting a Resource Manager...");
            }
        }

        if(shutdown.size() == 3) {
            System.out.println("Restarting Middleware...");
            m_itemHT.clear();
            TM.restart();
        }

        return true;
    }

    public void store(String filename) {
      // Do nothing. We don't shadow the MiddleWare Hashtable.
      // Not yet at least, i'll do it later.
    }

    public void setBanner(String name) {
    }
    public String getBanner() {
      return "MiddleWare";
    }

    public RMHashtable getHash() {
      return this.m_itemHT;
    }

    public RMHashtable setHash(RMHashtable shadow) {
      return this.m_itemHT = shadow;
    }

}
