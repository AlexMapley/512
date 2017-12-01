package TransImpl;

import ResImpl.*;
import java.util.*;
import java.lang.*;
import java.io.*;

public class CrashDetection extends Thread implements Serializable
{
	static TransactionManager host;
	public static final int sleepTime = 60000; // 60 seconds

	public CrashDetection(TransactionManager host) {
	 	// this.host = host;
	 	// System.out.println("Crash Detector Started...");
	}

	public void run() {
		// while(true) {
		// 	synchronized(host.transactions) {
		// 		for (Transaction transaction : host.transactions.values()) {
		// 			if(transaction == null) 
		// 				break;
	 // 				if(transaction.status == 0) {
	 // 					continue;
	 // 				}
	 // 				long current = (new Date()).getTime();
	 // 				if((current - transaction.getTime()) >= transaction.TIME2LIVE) {
		// 				System.out.println("Transaction: " + transaction.id + " hanging, aborting...");
		// 				try {
	 // 						host.abort(transaction.id);
		// 				} catch (Exception e) {
		// 					System.out.println(e);
		// 				}
	 // 				}
	 // 				try {
	 // 					Thread.currentThread().sleep(sleepTime);
	 // 				} catch (InterruptedException e) {
	 // 					System.out.println("Crash Detector failed during sleep");
	 // 					System.out.println("Restarting Crash Detector...");
	 // 					host.startDetector();
	 // 				}
		// 		}
		// 	}
		// }
	}

}
