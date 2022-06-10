package soot.jimple.infoflow.test;

import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.Location;
import soot.jimple.infoflow.test.android.LocationManager;
import soot.jimple.infoflow.test.android.TelephonyManager;

/**
 * Simple test targets for very basic functions
 * 
 * @author Steven Arzt
 *
 */
public class BasicTestCode {

	public void overwriteInCalleeTest1() {
		Location loc = new Location();
		calleeOverwrite(loc);

		ConnectionManager cm = new ConnectionManager();
		cm.publish(loc.getLatitude());
	}

	private void calleeOverwrite(Location loc) {
		System.out.println(loc);
		loc = LocationManager.getLastKnownLocation();
		System.out.println(loc);
	}

	public void overwriteInCalleeTest2() {
		Location loc = LocationManager.getLastKnownLocation();
		calleeOverwriteNull(loc);

		ConnectionManager cm = new ConnectionManager();
		cm.publish(loc.getLatitude());
	}

	private void calleeOverwriteNull(Location loc) {
		System.out.println(loc);
		loc = null;
	}

	public void overwriteBaseObjectTest1() {
		Location loc = new Location(LocationManager.getLongitude(), 0.0d);
		loc = loc.clear();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(loc.getLongitude());
	}

	public void overwriteBaseObjectTest2() {
		Location loc = new Location(LocationManager.getLongitude(), LocationManager.getLongitude());
		loc = loc.clearLongitude();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(loc.getLongitude());
	}

	public void simpleArithmeticTest1() {
		int i = TelephonyManager.getIMEI();
		i++;
		ConnectionManager cm = new ConnectionManager();
		cm.publish(i);
	}

	public void arithmeticLoopTest1() {
		int i = TelephonyManager.getIMEI();
		while (i < 100)
			i = increment(i);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(i);
	}

	public void arithmeticLoopTest2() {
		int j = 0;
		int i = TelephonyManager.getIMEI();
		while (i < 100) {
			i = j;
			j = increment(i);
		}
		ConnectionManager cm = new ConnectionManager();
		cm.publish(i);
	}

	private int increment(int i) {
		return i + 1;
	}

	class IntegerRef {
		int value;
	}

	public void basicAliasTest() {
		IntegerRef i = new IntegerRef();
		IntegerRef j = i;
		j.value = TelephonyManager.getIMEI();

		ConnectionManager cm = new ConnectionManager();
		cm.publish(i.value);
	}

	public void simpleTest() {
		Location loc = LocationManager.getLastKnownLocation();
		ConnectionManager cm = new ConnectionManager();
		cm.publish(loc.getLatitude());
	}

}
