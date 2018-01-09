package soot.jimple.infoflow.test;

import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;

/**
 * Tests for exceptional data- and control flows
 * 
 * @author Steven Arzt
 */
public class ExceptionTestCode {
	
	public void exceptionControlFlowTest1() {
		String tainted = TelephonyManager.getDeviceId();
		try {
			doThrowException();
		}
		catch (RuntimeException ex) {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(tainted);
			System.out.println(ex);
		}
	}

	private void doThrowException() {
		throw new RuntimeException("foo");
	}
	
	public void exceptionControlFlowTest2() {
		try {
			String s = getConstantStringAndThrow();
			System.out.println(s);
		}
		catch (Exception ex) {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(TelephonyManager.getDeviceId());
			System.out.println(ex);
		}
	}
	
	private String getConstantStringAndThrow() {
		throw new RuntimeException("foo");
	}
	
	public void exceptionControlFlowTest3() {
		String tainted = TelephonyManager.getDeviceId();
		try {
			tainted = doThrowImplicitException();
		}
		catch (ArrayIndexOutOfBoundsException ex) {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(tainted);
			System.out.println(ex);
		}
		System.out.println(tainted);
	}
	
	private String doThrowImplicitException() {
		String[] foo = new String[2];
		foo[10] = "Hello World";
		return "foo";
	}
	
	public void exceptionDataFlowTest1() {
		String tainted = TelephonyManager.getDeviceId();
		try {
			throwData(tainted);
		}
		catch (Exception ex) {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(ex.getMessage());
		}
	}

	private void throwData(String tainted) {
		throw new RuntimeException(tainted);
	}
	
	public void exceptionDataFlowTest2() {
		String tainted = TelephonyManager.getDeviceId();
		try {
			throw new RuntimeException(tainted);
		}
		catch (Exception ex) {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(ex.getMessage());
		}
	}
	
	public void disabledExceptionTest() {
		String imei = TelephonyManager.getDeviceId();
		try {
			throw new RuntimeException();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		ConnectionManager cm = new ConnectionManager();
		cm.publish(imei);		
	}
	
	private class Data {
		public String imei;
	}
	
	public void callMethodParamReturnTest1() {
		Data data = new Data();
		data.imei = TelephonyManager.getDeviceId();
		data = setAndReturn1(data);
		ConnectionManager cm = new ConnectionManager();
		cm.publish(data.imei);
	}

	private Data setAndReturn1(Data data) {
		String s = data.imei;
		data.imei = "";
		
		Data d = new Data();
		d.imei = s;
		return d;
	}
	
	public void callMethodParamReturnTest2() {
		Data data = new Data();
		data.imei = TelephonyManager.getDeviceId();
		try {
			data = setAndReturn2(data);
		}
		finally {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(data.imei);
		}
	}

	private Data setAndReturn2(Data data) {
		String s = data.imei;
		data.imei = "";
		
		// cause an exception
		data.imei.substring(-10, -1);
		
		Data d = new Data();
		d.imei = s;
		return d;
	}

	public void callMethodParamReturnTest2b() {
		Data data = new Data();
		data.imei = "";
		try {
			data = setAndReturn2b(data);
		}
		finally {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(data.imei);
		}
	}

	private Data setAndReturn2b(Data data) {
		String s = data.imei;
		data.imei = TelephonyManager.getDeviceId();
		
		// cause an exception
		data.imei.substring(-10, -1);
		
		Data d = new Data();
		d.imei = s;
		return d;
	}

	public void callMethodParamReturnTest3() {
		Data data = new Data();
		data.imei = TelephonyManager.getDeviceId();
		try {
			data = setAndReturn3(data);
		}
		finally {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(data.imei);
		}
	}

	private Data setAndReturn3(Data data) {
		String s = data.imei;
		
		// cause an exception
		data.imei.substring(-10, -1);

		data.imei = "";
				
		Data d = new Data();
		d.imei = s;
		return d;
	}

}
