package TransImpl;

import java.util.*;
import java.lang.*;

public class CrashDetection extends Thread
{
	TransactionManager host;
	public static final int sleepTime = 1000; // 1 second in ms
	
	public CrashDetection(TransactionManager host) {
	 	this.host = host;
	 	System.out.println("Crash Detectior Started...");
	}

	public void run() {
		while(true) {
			for (Transaction transaction : host.transactions.values()) {
 				if(transaction.status == 0) {
 					continue;
 				}
 				long current = (new Date()).getTime();
 				if((current - transaction.getTime()) >= transaction.TIME2LIVE) {
 					host.abort(transaction.id);
 				}
 				try {
 					Thread.currentThread().sleep(sleepTime);
 				} catch (InterruptedException e) {
 					System.out.println("Crash detector failed during sleep");
 					System.out.println("Restarting Crash detector...");
 					host.startDetector(this);
 				}
			}		
		}
	}

}
