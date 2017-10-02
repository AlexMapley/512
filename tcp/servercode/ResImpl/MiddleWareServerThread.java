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

  MiddleWareServerThread (Socket socket, MiddleWareImpl host) {
    this.clientSocket = socket;
    this.host = host;
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
      Socket flightSocket = new Socket("lab1-6", 5959);     // lab1-3 has a rogue Gaylen Proc running on port 5959 atm :(
      BufferedReader inFromFlightRM = new BufferedReader(new InputStreamReader(flightSocket.getInputStream()));
  		PrintWriter outToFlightRM = new PrintWriter(flightSocket.getOutputStream(), true);

      // Cars
      Socket carSocket = new Socket("lab1-4", 5959);
      BufferedReader inFromCarRM = new BufferedReader(new InputStreamReader(carSocket.getInputStream()));
  		PrintWriter outToCarRM = new PrintWriter(carSocket.getOutputStream(), true);

      // Rooms
      Socket roomSocket = new Socket("lab1-5", 5959);
      BufferedReader inFromRoomRM = new BufferedReader(new InputStreamReader(roomSocket.getInputStream()));
  		PrintWriter outToRoomRM = new PrintWriter(roomSocket.getOutputStream(), true);

      while ((message = inFromClient.readLine())  != null) {
    		System.out.println("\nmessage from client: " + message);

        // Parsing commands for RM:

        // Flights
    		String[] params =  message.split(",");
        if (params[0].compareToIgnoreCase("newflight")==0 || params[0].compareToIgnoreCase("deleteflight")==0
            || params[0].compareToIgnoreCase("queryflight")==0  || params[0].compareToIgnoreCase("queryflightprice")==0
            || params[0].compareToIgnoreCase("reserveflight")==0 )
            {
              System.out.println("Out to Flight RM");
              outToFlightRM.println(message);
              res = inFromFlightRM.readLine();
        }
        // Cars
        else if ( params[0].compareToIgnoreCase("newcar")==0 || params[0].compareToIgnoreCase("deletecar")==0
            || params[0].compareToIgnoreCase("querycar")==0  || params[0].compareToIgnoreCase("querycarprice")==0
            || params[0].compareToIgnoreCase("reservecar")==0 )
            {
              System.out.println("Out to Car RM");
              outToCarRM.println(message);
              res = inFromCarRM.readLine();
        }
        // Rooms
        else if ( params[0].compareToIgnoreCase("newroom")==0 || params[0].compareToIgnoreCase("deleteroom")==0
            || params[0].compareToIgnoreCase("queryroom")==0  || params[0].compareToIgnoreCase("queryroomprice")==0
            || params[0].compareToIgnoreCase("reserveroom")==0 )
            {
              System.out.println("Out to Room RM");
              outToRoomRM.println(message);
              res = inFromRoomRM.readLine();
        }
        // Other
        else if ( params[0].compareToIgnoreCase("newcustomer")==0 || params[0].compareToIgnoreCase("deletecustomer")==0
            || params[0].compareToIgnoreCase("itinerary")==0  || params[0].compareToIgnoreCase("newcustomerid")==0 )
            {
              System.out.println("General MW/RM command: " + params[0]);
              outToRoomRM.println(message);
              res = inFromRoomRM.readLine();
              outToCarRM.println(message);
              res = inFromCarRM.readLine();
              outToFlightRM.println(message);
              res = inFromFlightRM.readLine();

              if (params[0].compareToIgnoreCase("newcustomer")==0) {
                try {
                  callMethod(message);
                }
                catch (Exception e) {
                  System.out.println(e);
                }
              }
        }
        else {
          System.out.println("Invalid Command, not sent");
        }

    		outToClient.println("hello client from server THREAD, your result is: " + res );
  		}

    clientSocket.close();
    flightSocket.close();
  	}
  	catch (IOException e) {
  	}
  }

  // Takes a command as an input,
  // in the form of something like "newflight,1,2,3,4"
  // and executes that command on the ResourceManagerImpl instance
  public void callMethod(String command) throws Exception {
    String[] args = command.split(",");

    // Uses method reflection to call instance methods by name
    // - Pretty much just a higher level implementation
    // of that whole dictionary pattern we talked about
    Object paramsObj[] = new Object[args.length - 1];
    Class params[] = new Class[paramsObj.length];
    for (int i = 0; i < paramsObj.length; i++) {
      paramsObj[i] = args[i+1];
      if (isInteger(paramsObj[i])) {
        System.out.println("Arg is an int!");
        paramsObj[i] = Integer.parseInt( (String) paramsObj[i] );
        params[i] = int.class;
      }
      else {
        params[i] = String.class;
      }
    }

    try {
      Class thisClass = Class.forName("ResImpl.MiddleWareImpl");
      Method m = thisClass.getDeclaredMethod(convertCommand((String) args[0]), params);
      System.out.println(m.invoke(this.host, paramsObj).toString());
    }
    catch(NoSuchMethodException e) {
      System.out.println("Incorrect Args given, No Response");
    }

  }


  // Checks for String -> Integer Conversion
  public boolean isInteger( Object input ) {
    System.out.println( (String) input );
    try {
      Integer.parseInt( (String) input );
      return true;
    }
    catch (Exception e) {
      return false;
    }
  }


  public String convertCommand(String command) {

    // Comand Parsing Dictionary
    Map<String,String> commandMap = new HashMap<String,String>();
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

    return commandMap.get(command);
  }
}
