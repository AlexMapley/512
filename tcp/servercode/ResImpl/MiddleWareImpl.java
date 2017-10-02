package ResImpl;

import ResInterface.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


public class MiddleWareImpl
{
    static int port = 5959;
    static int maxClients = 10;

    public static void main(String args[]) throws IOException {
      MiddleWareImpl server = new MiddleWareImpl();
      try  {
        server.runServerThread(server);
      }
      catch (IOException e) {
      }
    }

    public void runServerThread(MiddleWareImpl host) throws IOException {

      final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);
      String[] servers = {"lab1-7", "lab1-8", "lab1-10"};


      Runnable serverTask = new Runnable() {
          @Override
          public void run() {
              try {
                  ServerSocket serverSocket = new ServerSocket(port);
                  System.out.println("Waiting for clients to connect...");
                  while (true) {
                      Socket clientSocket = serverSocket.accept();
                      clientProcessingPool.submit(new MiddleWareServerThread(clientSocket, host, servers));
                  }
              }
              catch (IOException e) {
                  System.err.println("Unable to process client request");
                  e.printStackTrace();
              }
          }
      };
      Thread serverThread = new Thread(serverTask);
      serverThread.start();
    }



    public MiddleWareImpl() throws IOException {

    }

}
