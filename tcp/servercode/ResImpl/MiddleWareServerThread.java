package ResImpl;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import ResImpl.MiddleWareImpl;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class MiddleWareServerThread extends Thread {
  Socket clientSocket;
  MiddleWareImpl host;
  String[] servers;
  static protected RMHashtable m_itemHT = new RMHashtable();
  static Map<String,String> commandMap = new HashMap<String,String>();

  MiddleWareServerThread (Socket socket, MiddleWareImpl host, String[] servers) {
    this.clientSocket = socket;
    this.host = host;
    this.servers = servers;
    createCommandMap();
  }

  public void run() {

  	try {
      // Declaring Socket Variables:
      String message = null;
      String res = "@ no RM response @";

      // Client
      BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
  		PrintWriter outToClient = new PrintWriter(clientSocket.getOutputStream(), true);

      // Flights
      Socket flightSocket = new Socket(servers[0], 5959);     // lab1-3 has a rogue Galen Proc running on port 5959 atm :(
      BufferedReader inFromFlightRM = new BufferedReader(new InputStreamReader(flightSocket.getInputStream()));
  		PrintWriter outToFlightRM = new PrintWriter(flightSocket.getOutputStream(), true);

      // Cars
      Socket carSocket = new Socket(servers[1], 5959);
      BufferedReader inFromCarRM = new BufferedReader(new InputStreamReader(carSocket.getInputStream()));
  		PrintWriter outToCarRM = new PrintWriter(carSocket.getOutputStream(), true);

      // Rooms
      Socket roomSocket = new Socket(servers[2], 5959);
      BufferedReader inFromRoomRM = new BufferedReader(new InputStreamReader(roomSocket.getInputStream()));
  		PrintWriter outToRoomRM = new PrintWriter(roomSocket.getOutputStream(), true);

      while ((message = inFromClient.readLine())  != null) {
        System.out.println("\nmessage from client: " + message);

        // Parse and forward to corresponding RM
        String[] params =  message.split(",");
        String command = params[0];
        params[0] = commandMap.get(command);
        // message = buildString(params);

        if(command != null) {  // known command
          if (command.compareToIgnoreCase("newflight")==0 || command.compareToIgnoreCase("deleteflight")==0
              || command.compareToIgnoreCase("queryflight")==0  || command.compareToIgnoreCase("queryflightprice")==0
              || command.compareToIgnoreCase("reserveflight")==0 )
          {
                System.out.println("Out to Flight RM");
                outToFlightRM.println(message);
                res = inFromFlightRM.readLine();
          }
          // Cars
          else if ( command.compareToIgnoreCase("newcar")==0 || command.compareToIgnoreCase("deletecar")==0
              || command.compareToIgnoreCase("querycar")==0  || command.compareToIgnoreCase("querycarprice")==0
              || command.compareToIgnoreCase("reservecar")==0 )
          {
                System.out.println("Out to Car RM");
                outToCarRM.println(message);
                res = inFromCarRM.readLine();
          }
          // Rooms
          else if ( command.compareToIgnoreCase("newroom")==0 || command.compareToIgnoreCase("deleteroom")==0
              || command.compareToIgnoreCase("queryroom")==0  || command.compareToIgnoreCase("queryroomprice")==0
              || command.compareToIgnoreCase("reserveroom")==0 )
          {
                System.out.println("Out to Room RM");
                outToRoomRM.println(message);
                res = inFromRoomRM.readLine();
          }
          // newcustomer
          else if ( command.compareToIgnoreCase("newcustomer")==0 ) {
            System.out.println("General MW/RM command: " + params[0]);

            // MW call
            int cid = newCustomer(Integer.parseInt(params[1]));

            //RM forwarding
            String[] newParams = {"newcustomerid", params[1], Integer.toString(cid)};
            String newMessage = buildMessage(newParams);
            outToRoomRM.println(newMessage);
            resR = inFromRoomRM.readLine();
            outToCarRM.println(newMessage);
            resC = inFromCarRM.readLine();
            outToFlightRM.println(newMessage);
            resF = inFromFlightRM.readLine();
            res = resR + "\n" + resC + "\n" + resF;
          }
          // newcustomerid
          else if ( command.compareToIgnoreCase("newcustomerid")==0 ) {
            System.out.println("General MW/RM command: " + params[0]);

            // MW call
            newCustomer(Integer.parseInt(params[1]), Integer.parseInt(params[2]));

            //RM forwarding
            outToRoomRM.println(message);
            resR = inFromRoomRM.readLine();
            outToCarRM.println(message);
            resC = inFromCarRM.readLine();
            outToFlightRM.println(message);
            resF = inFromFlightRM.readLine();
            res = resR + "\n" + resC + "\n" + resF;
          }
          else if ( command.compareToIgnoreCase("deletecustomer")==0 ) {
            System.out.println("General MW/RM command: " + params[0]);

            // MW call
            deleteCustomer(Integer.parseInt(params[1]), Integer.parseInt(params[2]));

            //RM forwarding
            outToRoomRM.println(message);
            res = inFromRoomRM.readLine();
            outToCarRM.println(message);
            res = inFromCarRM.readLine();
            outToFlightRM.println(message);
            res = inFromFlightRM.readLine();
          }
          else if ( command.compareToIgnoreCase("querycustomer")==0 ) {
            System.out.println("General MW/RM command: " + params[0]);
            outToRoomRM.println(message);
            String resR = inFromRoomRM.readLine();
            outToCarRM.println(message);
            String resC = inFromCarRM.readLine();
            outToFlightRM.println(message);
            String resF = inFromFlightRM.readLine();
            res = resR + "\n" + resC + "\n" + resF;
          }
          else if ( command.compareToIgnoreCase("itinerary")==0 ) {
            System.out.println("General MW/RM command: " + params[0]);
            String id = params[1];
            String customerid = params[2];
            String[] flightnumbers = new String[10];  //No one's going to book more than 10 flights
            int index = 3;
            int numflights = 0;
            while (index < 13) {
              if (isInteger(params[index])) {
                flightnumbers[numflights] = params[index];
                index++;
                numflights++;
              }
              else {
                break;
              }
            }
            String location = params[index];
            String car = params[index+1];
            String room = params[index+1];
            String flightresponses = new String[numflights];
            // General
            if (flightnumbers[0]) {
              for (int i = 0; i < numflights; i++) {
                if (flightnumbers[i]) {
                    flightresponse[i] = "reserveflight," + id + "," + customerid + "," + flightnumbers[i];
                    outToFlightRM.println(flightresponse[i]);
                    flightresponse[i] = inFromFlightRM.readLine();
                }
                else
                  break
              }
            }
            if (car) {
              String message2 = "reservecar," + id + "," + customerid + "," + location + "," + "1";
              outToCarRM.println(message);
              String resC = inFromCarRM.readLine();
            }
            if(room) {
              String message3 = "reserveroom," + id + "," + customerid + "," + location + "," + "1";
              outToRoomRM.println(message);
              String resC = inFromRoomRM.readLine();
            }
            for (int i = 0; i < numflights; i++) {
              res += flightresponse[i] + "\n";
            }
            res += message2 + "\n";
            res += message3;
          }

        }
        else {  // unknown command
          System.out.println("unknown client command input");
        }
        outToClient.println(res);
      }

      clientSocket.close();
      flightSocket.close();
      carSocket.close();
      roomSocket.close();
    } catch (IOException e) {
      System.out.println("Middleware IOException: " + e);
    }
  }

  public  boolean isInteger(String s) {
    try {
        Integer.parseInt(s);
    } catch(NumberFormatException e) {
        return false;
    } catch(NullPointerException e) {
        return false;
    }
    return true;
}

  /// Concatenates a string array
  private String buildMessage(String[] params) {
    StringBuilder strBuilder = new StringBuilder();
    for (int i = 0; i < params.length; i++) {
       strBuilder.append(params[i]+",");
    }
    String str = strBuilder.toString();
    return str.substring(0, str.length() - 1);
  }

  public void createCommandMap() {
    // Comand Parsing Dictionary
    commandMap.put("newflight", "addFlight");
    commandMap.put("newcar", "addCars");
    commandMap.put("newroom", "addRooms");
    commandMap.put("newcustomer", "newCustomer");
    commandMap.put("newcusomterid", "newCustomer");
    commandMap.put("deleteflight", "deleteFlight");
    commandMap.put("deletecar", "deleteCars");
    commandMap.put("deleteroom", "deleteRooms");
    commandMap.put("deletecustomer", "deleteCustomer");
    commandMap.put("queryflight", "queryFlight");
    commandMap.put("querycar", "queryCars");
    commandMap.put("queryroom", "queryRooms");
    commandMap.put("querycustomer", "queryCustomerInfo");
    commandMap.put("queryflightprice", "queryFlightPrice");
    commandMap.put("querycarprice", "queryCarsPrice");
    commandMap.put("queryroomprice", "queryRoomsPrice");
    commandMap.put("reserveflight", "reserveFlight");
    commandMap.put("reservecar", "reserveCar");
    commandMap.put("reserveroom", "reserveRoom");
    commandMap.put("itinerary", "itinerary");
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

    // Returns data structure containing customer reservation info. Returns null if the
    //  customer doesn't exist. Returns empty RMHashtable if customer exists but has no
    //  reservations.
    // public RMHashtable getCustomerReservations(int id, int customerID)
    //     throws IOException
    // {
    //     Trace.info("RM::getCustomerReservations(" + id + ", " + customerID + ") called" );
    //     Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
    //     if ( cust == null ) {
    //         Trace.warn("RM::getCustomerReservations failed(" + id + ", " + customerID + ") failed--customer doesn't exist" );
    //         return null;
    //     } else {
    //         return cust.getReservations();
    //     } // if
    // }

    // return a bill
    public String queryCustomerInfo(int id, int customerID)
        throws IOException
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
        throws IOException
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
        throws IOException
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
        throws IOException
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

    // Reserve an itinerary
    public boolean itinerary(int id,int customer,Vector flightNumbers,String location,boolean Car,boolean Room)
        throws IOException
    {
        boolean success = true;




        return success;
    }
}
