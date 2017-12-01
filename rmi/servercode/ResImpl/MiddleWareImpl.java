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

    protected static RMHashtable m_itemHT = new RMHashtable();
    private static HashMap<Integer, RMHashtable> transactionImages = new HashMap<Integer, RMHashtable>();

    static ResourceManager CarRM = null;
    static ResourceManager HotelRM = null;
    static ResourceManager FlightRM = null;

    static HashMap<String, ResourceManager> rms;
    static HashMap<String, String> hostnames;

    private static TransactionManager TM;

    public static void main(String[] args) {
        String server = "localhost";  // creates middlware on current machine

        // collect inputted RMs
        rms = new HashMap<String, ResourceManager>();
        hostnames = new HashMap<String, String>();

        if (args.length == 3) {
            try
            {
                for(int i=0; i<3;i++) {
                    String name = "none";
                    String hostname = args[i];
                    // get a reference to the rmiregistry
                    Registry registry = LocateRegistry.getRegistry(args[i], port);
                    // get the proxy and the remote reference by rmiregistry lookup
                    ResourceManager rm = (ResourceManager) registry.lookup("group_21");
                    if(rm != null)
                    {
                        if(i == 0){
                            CarRM = rm;
                            name = "Car";
                        }
                        else if(i == 1){
                            FlightRM = rm;
                            name = "Flight";
                        }
                        else {
                            HotelRM = rm;
                            name = "Hotel";
                        }
                        
                        rms.put(name, rm);
                        hostnames.put(name, hostname);
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
            TM.enlist(id, FlightRM);
            Trace.info("RM::addFlight(" + id + ", " + flightNum + ", $" + flightPrice + ", " + flightSeats + ") called" );
            if(FlightRM.addFlight(id,flightNum,flightSeats,flightPrice))
                // call succesfull
                Trace.info("RM::addFlight(" + id + ") created or modified flight " + flightNum + ", seats=" +
                    flightSeats + ", price=$" + flightPrice );
            else {
                Trace.info("RM::addFlight encountered an unknown error");
            }
            return(true);
        } catch (RemoteException e) {
            System.out.println("Flight server crashed");
            rebind("Flight");
            throw e;
        }

    }

    public boolean deleteFlight(int id, int flightNum)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, FlightRM);
            return FlightRM.deleteFlight(id, flightNum);
        } catch (RemoteException e) {
            System.out.println("Flight server crashed");
            rebind("Flight");
            throw e;
        }
    }

    // Create a new room location or add rooms to an existing location
    //  NOTE: if price <= 0 and the room location already exists, it maintains its current price
    public boolean addRooms(int id, String location, int count, int price)
        throws RemoteException, TransactionAbortedException
    {
        try{
            TM.enlist(id, HotelRM);
            Trace.info("RM::addRooms(" + id + ", " + location + ", " + count + ", $" + price + ") called" );
            if(HotelRM.addRooms(id,location,count,price))
                // call succesfull
                Trace.info("RM::addRooms(" + id + ") created or modified room location " + location + ", count=" + count + ", price=$" + price );
            else {
                Trace.info("RM::addRooms encountered an unknown error");
            }
        } catch (RemoteException e) {
            System.out.println("Hotel server crashed");
            rebind("Hotel");
            throw e;
        }
            
        return(true);
    }

    // Delete rooms from a location
    public boolean deleteRooms(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, HotelRM);
            return HotelRM.deleteRooms(id, location);
        } catch (RemoteException e) {
            System.out.println("Hotel server crashed");
            rebind("Hotel");
            throw e;
        }

    }

    // Create a new car location or add cars to an existing location
    //  NOTE: if price <= 0 and the location already exists, it maintains its current price
    public boolean addCars(int id, String location, int count, int price)
        throws RemoteException, TransactionAbortedException
    {
        try {
            if(CarRM.addCars(id,location,count,price))
                // call succesfull
                Trace.info("RM::addCars(" + id + ") created or modified location " + location + ", count=" + count + ", price=$" + price );
            else {
                Trace.info("RM::addCar encountered an unknown error");
            }
        } catch (RemoteException e) {
            System.out.println("Car server crashed");
            rebind("Car");
            throw e;
        }
        
        return(true);
    }


    // Delete cars from a location
    public boolean deleteCars(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, CarRM);
            return CarRM.deleteCars(id, location);
        } catch (RemoteException e) {
            System.out.println("Car server crashed");
            rebind("Car");
            throw e;
        }
    }



    // Returns the number of empty seats on this flight
    public int queryFlight(int id, int flightNum)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, FlightRM);
            return FlightRM.queryFlight(id,flightNum);
        } catch (RemoteException e) {
            System.out.println("Flight server crashed");
            rebind("Flight");
            throw e;
        }
    }

    // Returns price of this flight
    public int queryFlightPrice(int id, int flightNum )
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, FlightRM);
            return FlightRM.queryFlightPrice(id, flightNum);
        } catch (RemoteException e) {
            System.out.println("Flight server crashed");
            rebind("Flight");
            throw e;
        }
    }


    // Returns the number of rooms available at a location
    public int queryRooms(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, HotelRM);
            return HotelRM.queryRooms(id, location);
        } catch (RemoteException e) {
            System.out.println("Hotel server crashed");
            rebind("Hotel");
            throw e;
        }
    }

    // Returns room price at this location
    public int queryRoomsPrice(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, HotelRM);
            return HotelRM.queryRoomsPrice(id, location);
        } catch (RemoteException e) {
            System.out.println("Hotel server crashed");
            rebind("Hotel");
            throw e;
        }
    }


    // Returns the number of cars available at a location
    public int queryCars(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, CarRM);
            return CarRM.queryCars(id, location);
        } catch (RemoteException e) {
            System.out.println("Car server crashed");
            rebind("Car");
            throw e;
        }
    }


    // Returns price of cars at this location
    public int queryCarsPrice(int id, String location)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, CarRM);
            return CarRM.queryCarsPrice(id, location);
        } catch (RemoteException e) {
            System.out.println("Car server crashed");
            rebind("Car");
            throw e;
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
        //     rebind("Flight");
        //     return null;
        // }
        return null;
    }

    // return a bill
    public String queryCustomerInfo(int id, int customerID)
        throws RemoteException, TransactionAbortedException
    {
        TM.enlist(id, CarRM);
        TM.enlist(id, FlightRM);
        TM.enlist(id, HotelRM);
        String c,f,h;
        Trace.info("RM::queryCustomerInfo(" + id + ", " + customerID + ") called" );
        try { 
            // Do this for each RM and concat the strings for return
            c = CarRM.queryCustomerInfo(id, customerID);
        } catch (RemoteException e) {
            System.out.println("Car server crashed");
            rebind("Car");
            throw e;
        }    
        try {
            f = FlightRM.queryCustomerInfo(id, customerID);
        } catch (RemoteException e) {
            System.out.println("Flight server crashed");
            rebind("Flight");
            throw e;
        } 
        try {
            h = HotelRM.queryCustomerInfo(id, customerID);
        } catch (RemoteException e) {
            System.out.println("Car server crashed");
            rebind("Car");
            throw e;
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
        
            TM.enlist(id, CarRM);
            TM.enlist(id, FlightRM);
            TM.enlist(id, HotelRM);
            Trace.info("INFO: RM::newCustomer(" + id + ") called" );
            int decision;
            // Have Car rm decide the id then
            try {
                decision = CarRM.newCustomer(id);
            } catch (RemoteException e) {
                System.out.println("Flight server crashed");
                rebind("Flight");
                throw e;
            }
            try {
                FlightRM.newCustomer(id, decision);
            } catch (RemoteException e) {
                System.out.println("Flight server crashed");
                rebind("Flight");
                throw e;
            }
            try {
                HotelRM.newCustomer(id, decision);
            } catch (RemoteException e) {
                System.out.println("Flight server crashed");
                rebind("Flight");
                throw e;
            }
            Trace.info("RM::newCustomer(" + decision + ") returns ID=" + decision );
            return decision;
    }

    // I opted to pass in customerID instead. This makes testing easier
    public boolean newCustomer(int id, int customerID )
        throws RemoteException, TransactionAbortedException
    {
            TM.enlist(id, CarRM);
            TM.enlist(id, FlightRM);
            TM.enlist(id, HotelRM);
            Trace.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") called" );
            boolean result;
            try {
                result = CarRM.newCustomer(id,customerID);
            } catch (RemoteException e) {
                System.out.println("Flight server crashed");
                rebind("Flight");
                throw e;
            }
            try {
                result = result && FlightRM.newCustomer(id,customerID);
            } catch (RemoteException e) {
                System.out.println("Flight server crashed");
                rebind("Flight");
                throw e;
            }
            try {
                result = result && HotelRM.newCustomer(id,customerID);
            } catch (RemoteException e) {
                System.out.println("Flight server crashed");
                rebind("Flight");
                throw e;
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
            TM.enlist(id, CarRM);
            TM.enlist(id, FlightRM);
            TM.enlist(id, HotelRM);
            Trace.info("RM::deleteCustomer(" + id + ", " + customerID + ") called" );
            boolean result;
            // remove customer info from all rms
            try {
                result = FlightRM.newCustomer(id,customerID);
            } catch (RemoteException e) {
                System.out.println("Flight server crashed");
                rebind("Flight");
                throw e;
            }
            try {
                result = result && CarRM.newCustomer(id,customerID);
            } catch (RemoteException e) {
                System.out.println("Flight server crashed");
                rebind("Flight");
                throw e;
            }
            try {
                result = result && HotelRM.newCustomer(id,customerID);
            } catch (RemoteException e) {
                System.out.println("Flight server crashed");
                rebind("Flight");
                throw e;
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
            TM.enlist(id, CarRM);
            return CarRM.reserveCar(id, customerID, location);
        } catch (RemoteException e) {
            System.out.println("Flight server crashed");
            rebind("Flight");
            throw e;
        }
    }


    // Adds room reservation to this customer.
    public boolean reserveRoom(int id, int customerID, String location)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, HotelRM);
            return HotelRM.reserveRoom(id, customerID, location);
        } catch (RemoteException e) {
            System.out.println("Flight server crashed");
            rebind("Flight");
            throw e;
        }
    }

    // Adds flight reservation to this customer.
    public boolean reserveFlight(int id, int customerID, int flightNum)
        throws RemoteException, TransactionAbortedException
    {
        try {
            TM.enlist(id, FlightRM);
            return FlightRM.reserveFlight(id, customerID, flightNum);
        } catch (RemoteException e) {
            System.out.println("Flight server crashed");
            rebind("Flight");
            throw e;
        }
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
        
        if(!flightNumbers.isEmpty()) {
            Iterator<Integer> flights = flightNumbers.iterator();
            while(flights.hasNext()) {
            // Reserve all flights
                int flightNum = flights.next();
                try {
                    success = success && FlightRM.reserveFlight(id, customer, flightNum);
                } catch (RemoteException e) {
                    System.out.println("Flight server crashed");
                    rebind("Flight");
                    throw e;
                }
            }

            //Reserve Car
            try {
                if(Car)
                    success = success && CarRM.reserveCar(id, customer, location);
            } catch (RemoteException e) {
                System.out.println("Car server crashed");
                rebind("Car");
                throw e;
            }
            //Reserve Room
            try {
                if(Room)
                    success = success && HotelRM.reserveCar(id, customer, location);
            } catch (RemoteException e) {
                System.out.println("Hotel server crashed");
                rebind("Hotel");
                throw e;
            }
        }
        else {
            return false;
        }

        return success;
    }

    public int start(int transactionId) throws RemoteException {
        transactionId = TM.start();
        transactionImages.put(transactionId, (RMHashtable) m_itemHT.clone());
        return transactionId;
    }

    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        boolean result = TM.prepare(transactionId);
        
        if(result) {
            TM.commit(transactionId);
        }
        else {
            throw new TransactionAbortedException(transactionId, "Unable to commit");
        }
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

    public String getBanner() {
      return null;
    }

    public static void rebind(String name) {
        String hostname = hostnames.get(name);
        System.out.println("Attempting reconnect with " + name + " RM");
        int i = 0;
        while(true) {
            try {
                Registry registry = LocateRegistry.getRegistry(hostname, port);
                ResourceManager rm = (ResourceManager) registry.lookup("group_21");
                rm.getBanner();

                rms.put(name, rm);
                CarRM = rms.get(name);
                FlightRM = rms.get(name);
                HotelRM = rms.get(name);

                System.out.println(name + " RM successfully reconnected!");
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
}