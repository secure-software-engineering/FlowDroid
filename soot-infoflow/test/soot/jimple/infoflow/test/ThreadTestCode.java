package soot.jimple.infoflow.test;

import java.security.AccessController;
import java.security.PrivilegedAction;

import soot.jimple.infoflow.test.android.AccountManager;
import soot.jimple.infoflow.test.android.ConnectionManager;

/**
 * Test code for multi-threading. Some test cases taken from bug reports by
 * hikame on Github.
 * 
 * @author Steven Arzt
 *
 */
public class ThreadTestCode {

	private String field;
	private MyThread threadField;
	public String pubfield;

	class MyThread extends Thread {
		@Override
		public void run() {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(field);
		}
	}

	public void testAnonymousInnerClass() {
		AccountManager am = new AccountManager();
		pubfield = am.getPassword();
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				ConnectionManager cm = new ConnectionManager();
				cm.publish(pubfield);
			}
		});
		thread.start();
	}

	public void testParamMapping() {
		AccountManager am = new AccountManager();
		pubfield = am.getPassword();
		AccessController.doPrivileged(new PrivilegedAction<Object>() {

			@Override
			public Object run() {
				ConnectionManager cm = new ConnectionManager();
				cm.publish(pubfield);
				return null;
			}

		});
	}

	public void testThreadWithField0a() {
		AccountManager am = new AccountManager();
		field = am.getPassword();
		threadField = new MyThread();
		threadField.start();
	}

	public void testThreadWithField0b() {
		AccountManager am = new AccountManager();
		field = am.getPassword();
		MyThread thread = new MyThread();
		thread.start();
	}

}
