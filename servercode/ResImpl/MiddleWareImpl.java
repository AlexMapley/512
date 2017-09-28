package ResImpl;

import ResInterface.*;

import java.util.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RMISecurityManager;

import java.net.ServerSocket;
import java.net.Socket;

public class MiddleWareImpl implements ResourceManager
{

    protected RMHashtable m_itemHT = new RMHashtable();
    static ResourceManager CarRM = null;
    static ResourceManager HotelRM = null;
    static ResourceManager FlightRM = null;
    public static ResourceManager rm = null; // test
    int port;
    String[] serverNames;
    Socket[] sockets;

    public static void main(String args[]) {

        // Default connection
        port = 5959;
        serverNames[0] = "localhost"
        sockets[0] = new Socket(serverNames[0], port);

        // Specifying target RM servers with args
        if (args.length > 0) {
          for (int i = 0; i < args.length; i++) {
            servers[i] = args[i];
          }
        }

        // Connecting to target RM sockets to args
        if (args.length > 0) {
          for (int i = 9; i < args.length; i++) {
            sockets[i] = new Socket(serverNames[i], port);
          }
        }




        // connect to RMs
        // ArrayList<ResourceManager> rms = new ArrayList<ResourceManager>();
        // if (args.length == 3) {
        //     //List of RM addresses, port hardcoded rn
        //     try
        //     {
        //         for(int i=0; i<3;i++) {
        //             // get a reference to the rmiregistry
        //             Registry registry = LocateRegistry.getRegistry(args[i], port);
        //             // get the proxy and the remote reference by rmiregistry lookup
        //             rms.add((ResourceManager) registry.lookup("group_21"));
        //             if(rms.get(i) != null)
        //             {
        //                 System.out.println("Successful Connection to RM " + i);
        //             }
        //             else
        //             {
        //                 System.out.println("Unsuccessful Connecting to RM" + i);
        //             }
        //         }
        //     }
        //     catch (Exception e)
        //     {
        //         System.err.println("MiddleWare exception: " + e.toString());
        //         e.printStackTrace();
        //     }

        // } else {
        //     System.err.println ("Wrong usage");
        //     System.out.println("Usage: java ResImpl.MiddleWareImpl [rmaddress:port] X 3 ");
        //     System.exit(1);
        // }
        // CarRM = rms.get(0); HotelRM = rms.get(1); FlightRM = rms.get(2);

        //TEST 1 RM ON SPECIFIC MACHINE
        try
        {
            // get a reference to the rmiregistry
            Registry registry = LocateRegistry.getRegistry(server, port);
            // get the proxy and the remote reference by rmiregistry lookup
            rm = (ResourceManager) registry.lookup("group_21");
            if(rm!=null)
            {
                System.out.println("Successful");
                System.out.println("Connected to RM");
            }
            else
            {
                System.out.println("Unsuccessful");
            }
            // make call on remote method
        }
        catch (Exception e)
        {
            System.err.println("Client exception: " + e.toString());
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
        throws RemoteException
    {
        Trace.info("RM::addFlight(" + id + ", " + flightNum + ", $" + flightPrice + ", " + flightSeats + ") called" );

        try {
            // if(FlightRM.addFlight(id,flightNum,flightSeats,flightPrice))
            if(rm.addFlight(id,flightNum,flightSeats,flightPrice))
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
            e.printStackTrace();
        }
        return(true);
    }



    public boolean deleteFlight(int id, int flightNum)
        throws RemoteException
    {
        // return FlightRM.deleteFlight(id, flightNum);
        return rm.deleteFlight(id, flightNum);
    }



    // Create a new room location or add rooms to an existing location
    //  NOTE: if price <= 0 and the room location already exists, it maintains its current price
    public boolean addRooms(int id, String location, int count, int price)
        throws RemoteException
    {
        Trace.info("RM::addRooms(" + id + ", " + location + ", " + count + ", $" + price + ") called" );
        try {
            // if(HotelRM.addRooms(id,location,count,price))
            if(rm.addRooms(id,location,count,price))
                // call succesfull
                Trace.info("RM::addRooms(" + id + ") created or modified room location " + location + ", count=" + count + ", price=$" + price );
            else {
                Trace.info("RM::addCar encountered an unknown error");
            }
        }
        catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return(true);
    }

    // Delete rooms from a location
    public boolean deleteRooms(int id, String location)
        throws RemoteException
    {
        // return HotelRM.deleteRooms(id, location);
        return rm.deleteRooms(id, location);

    }

    // Create a new car location or add cars to an existing location
    //  NOTE: if price <= 0 and the location already exists, it maintains its current price
    public boolean addCars(int id, String location, int count, int price)
        throws RemoteException
    {
        try {
            // if(CarRM.addCars(id,location,numCars,price))
            if(rm.addCars(id,location,count,price))
                // call succesfull
                Trace.info("RM::addCars(" + id + ") created or modified location " + location + ", count=" + count + ", price=$" + price );
            else {
                Trace.info("RM::addCar encountered an unknown error");
            }
        }
        catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return(true);
    }


    // Delete cars from a location
    public boolean deleteCars(int id, String location)
        throws RemoteException
    {
        // return CarRM.deleteCars(id, location);
        return rm.deleteCars(id, location);
    }



    // Returns the number of empty seats on this flight
    public int queryFlight(int id, int flightNum)
        throws RemoteException
    {
        // return FlightRM.queryFlight(id,flightNum);
        return rm.queryFlight(id,flightNum);
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
        throws RemoteException
    {
        // return FlightRM.queryFlightPrice(id, flightNum);
        return rm.queryFlightPrice(id, flightNum);
    }


    // Returns the number of rooms available at a location
    public int queryRooms(int id, String location)
        throws RemoteException
    {
        // return HotelRM.queryRooms(id, location));
        return rm.queryRooms(id, location);
    }

    // Returns room price at this location
    public int queryRoomsPrice(int id, String location)
        throws RemoteException
    {
        // return HotelRM.queryRoomsPrice(id, location);
        return rm.queryRoomsPrice(id, location);
    }


    // Returns the number of cars available at a location
    public int queryCars(int id, String location)
        throws RemoteException
    {
        // return CarRM.queryCars(id, location);
        return rm.queryCars(id, location);
    }


    // Returns price of cars at this location
    public int queryCarsPrice(int id, String location)
        throws RemoteException
    {
        // return CarRM.queryCarsPrice(id, location);
        return rm.queryCarsPrice(id, location);
    }

    // Returns data structure containing customer reservation info. Returns null if the
    //  customer doesn't exist. Returns empty RMHashtable if customer exists but has no
    //  reservations.
    public RMHashtable getCustomerReservations(int id, int customerID)
        throws RemoteException
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
        throws RemoteException
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
        throws RemoteException
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
        throws RemoteException
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
        throws RemoteException
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
        throws RemoteException
    {
        return reserveItem(id, customerID, Car.getKey(location), location);
    }


    // Adds room reservation to this customer.
    public boolean reserveRoom(int id, int customerID, String location)
        throws RemoteException
    {
        return reserveItem(id, customerID, Hotel.getKey(location), location);
    }
    // Adds flight reservation to this customer.
    public boolean reserveFlight(int id, int customerID, int flightNum)
        throws RemoteException
    {
        return reserveItem(id, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
    }

    // Reserve an itinerary
    public boolean itinerary(int id,int customer,Vector flightNumbers,String location,boolean Car,boolean Room)
        throws RemoteException
    {
        return false;
    }

}
