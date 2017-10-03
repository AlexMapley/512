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


public class ResourceManagerServerThread extends Thread {
  Socket clientSocket;
  ResourceManagerImpl host;

  ResourceManagerServerThread (Socket socket, ResourceManagerImpl host) {
    this.clientSocket = socket;
    this.host = host;
  }

  public void run() {

  	try {
      // Declaring Socket Variables:
      String message = null;
      String res = "@ no MW response @";

      // MiddleWare Server
      BufferedReader inFromMW = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
  		PrintWriter outToMW = new PrintWriter(clientSocket.getOutputStream(), true);

      while ((message = inFromMW.readLine())  != null) {
    	   System.out.println("\nmessage from MW: " + message);
         String[] params =  message.split(",");
         try {
           res = callMethod(message);
         }
         catch (Exception e) {
           System.out.println(e);
         }
         outToMW.println(res);
  		}

    clientSocket.close();

  	}
  	catch (IOException e) {
  	}
  }

  // Takes a command as an input,
  // in the form of something like "newflight,1,2,3,4"
  // and executes that command on the ResourceManagerImpl instance
  public String callMethod(String command) throws Exception {
    String[] args = command.split(",");
    String res = "RM: Invalid args, method not called";
    // Uses method reflection to call instance methods by name
    // - Pretty much just a higher level implementation
    // of that whole dictionary pattern we talked about
    Object paramsObj[] = new Object[args.length - 1];
    Class params[] = new Class[paramsObj.length];
    for (int i = 0; i < paramsObj.length; i++) {
      paramsObj[i] = args[i+1];
      if (isInteger(paramsObj[i])) {
        paramsObj[i] = Integer.parseInt( (String) paramsObj[i] );
        params[i] = int.class;
      }
      else {
        params[i] = String.class;
      }
    }

    try {
      Class thisClass = Class.forName("ResImpl.ResourceManagerImpl");
      Method m = thisClass.getDeclaredMethod(convertCommand((String) args[0]), params);
      Object object = m.invoke(this.host, paramsObj);
      System.out.println("returned value: " + object);
      res = "RM: Succesffuly called " + args[0] + "return value: " + object;
    }
    catch(NoSuchMethodException e) {
      System.out.println("Incorrect Args given, No Response");
    }
    return res;
  }


  // Checks for String -> Integer Conversion
  public boolean isInteger( Object input ) {
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
    commandMap.put("newcustomerid", "newCustomer");
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
