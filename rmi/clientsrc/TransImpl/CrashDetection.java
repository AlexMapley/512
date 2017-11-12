package TransImpl;

import ResImpl.*;
import java.util.*;
import java.lang.*;

public class CrashDetection extends Thread
{
	static TransactionManager host;
	public static final int sleepTime = 1000; // 1 second in ms

	public CrashDetection(TransactionManager host) {
	 	this.host = host;
	 	System.out.println("Crash Detector Started...");
	}

	public void run() {
		while(true) {
			for (Transaction transaction : host.transactions.values()) {
				if(transaction == null)
					break;
 				if(transaction.status == 0) {
 					continue;
 				}
 				long current = (new Date()).getTime();
 				if((current - transaction.getTime()) >= transaction.TIME2LIVE) {
					try {
 						host.abort(transaction.id);
					} catch (Exception e) {
						System.out.println(e);
					}
 				}
 				try {
 					Thread.currentThread().sleep(sleepTime);
 				} catch (InterruptedException e) {
 					System.out.println("Crash Detector failed during sleep");
 					System.out.println("Restarting Crash Detector...");
 					host.startDetector();
 				}
			}
		}
	}

}
