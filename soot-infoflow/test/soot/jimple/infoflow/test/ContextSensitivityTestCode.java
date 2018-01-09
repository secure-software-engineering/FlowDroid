package soot.jimple.infoflow.test;

import soot.jimple.infoflow.test.android.AccountManager;
import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;

/**
 * Class for testing whether context sensitivity is kept during the analysis
 * 
 * @author Steven Arzt
 *
 */
public class ContextSensitivityTestCode {
	
	private String id(String data) {
		String foo = data;
		return foo;
	}
	
	private String id2(String data) {
		return id(data);
	}
	
	public void contextSensitivityTest1() {
		String tainted1 = TelephonyManager.getDeviceId();
		String tainted2 = new AccountManager().getPassword();
		String str1 = id(tainted1);
		String str2 = id(tainted2);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(str1);
		System.out.println(str2);
	}
	
	public void contextSensitivityTest2() {
		String tainted1 = TelephonyManager.getDeviceId();
		String tainted2 = new AccountManager().getPassword();
		String str1 = id2(tainted1);
		String str2 = id2(tainted2);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(str1);
		System.out.println(str2);
	}
	
	public void multipleCallSiteTest1() {
		String tainted = TelephonyManager.getDeviceId();
		String x = id(tainted);
		String y = id(x);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(y);
	}
	
	private String doGetData(String data) {
		if (data == null)
			throw new RuntimeException("foo");
		return doGetData(null);
	}
	
	public void multipleExitTest1() {
		String tainted = TelephonyManager.getDeviceId();
		String data = "";
		try {
			data = doGetData(tainted);
		}
		catch (Exception ex) {
			data = tainted;
		}
		ConnectionManager cm = new ConnectionManager();
		cm.publish(data);
	}
	
}
