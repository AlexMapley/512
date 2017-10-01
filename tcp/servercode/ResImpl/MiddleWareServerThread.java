package ResImpl;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.io.BufferedReader;
import java.io.InputStreamReader;


public class MiddleWareServerThread extends Thread {
  Socket clientSocket;

  MiddleWareServerThread (Socket socket) {
    this.clientSocket = socket;
  }

  public void run() {

  	try {
      // Declaring Socket Variables:
      String message = null;
      String res = "@ no RM response @";

      //Client
      BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
  		PrintWriter outToClient = new PrintWriter(clientSocket.getOutputStream(), true);

      //Flights
      Socket flightSocket = new Socket("lab1-6", 5959);     // lab1-3 has a rogue Gaylen Proc running on port 5959 atm :(
      BufferedReader inFromFlightRM = new BufferedReader(new InputStreamReader(flightSocket.getInputStream()));
  		PrintWriter outToFlightRM = new PrintWriter(flightSocket.getOutputStream(), true);

      //Cars
      Socket carSocket = new Socket("lab1-4", 5959);
      BufferedReader inFromCarRM = new BufferedReader(new InputStreamReader(carSocket.getInputStream()));
  		PrintWriter outToCarRM = new PrintWriter(carSocket.getOutputStream(), true);

      // //Rooms
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
        else if (params[0].compareToIgnoreCase("newcar")==0 || params[0].compareToIgnoreCase("deletecar")==0
            || params[0].compareToIgnoreCase("querycar")==0  || params[0].compareToIgnoreCase("querycarprice")==0
            || params[0].compareToIgnoreCase("reservecar")==0 )
            {
              System.out.println("Out to Car RM");
              outToCarRM.println(message);
              res = inFromCarRM.readLine();
        }
        // Rooms
        else if (params[0].compareToIgnoreCase("newroom")==0 || params[0].compareToIgnoreCase("deleteroom")==0
            || params[0].compareToIgnoreCase("queryroom")==0  || params[0].compareToIgnoreCase("queryroomprice")==0
            || params[0].compareToIgnoreCase("reserveroom")==0 )
            {
              System.out.println("Out to Room RM");
              outToRoomRM.println(message);
              res = inFromRoomRM.readLine();
        }
        // Other
        else if (params[0].compareToIgnoreCase("newcustomer")==0 || params[0].compareToIgnoreCase("deletecustomer")==0
            || params[0].compareToIgnoreCase("itinerary")==0  || params[0].compareToIgnoreCase("newcustomerid")==0
            || params[0].compareToIgnoreCase("quit")==0 )
            {
              System.out.println("Out to No RM / General Command");
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
}
