package soot.jimple.infoflow.test;

import soot.jimple.infoflow.test.android.AccountManager;
import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;

/**
 * Test targets for testing the "single join point abstraction" option
 * 
 * @author Steven Arzt
 *
 */
public class SingleJoinPointTestCode {
	
	public void sharedMethodTest1() {
		String secret1 = TelephonyManager.getDeviceId();
		String secret2 = new AccountManager().getPassword();
		
		String data1 = id(secret1);
		String data2 = id(secret2);
		
		ConnectionManager cm = new ConnectionManager();
		cm.publish(data1);
		cm.publish(data2);
	}

	private String id(String secret) {
		return secret;
	}

}
