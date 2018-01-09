package soot.jimple.infoflow.test;

import java.lang.reflect.InvocationTargetException;

import soot.jimple.infoflow.test.android.ConnectionManager;
import soot.jimple.infoflow.test.android.TelephonyManager;

/**
 * Test targets for reflective method calls
 * 
 * @author Steven Arzt
 *
 */
public class ReflectionTestCode {
	
	private static class Container {
		public String data;
		
		public void set(String data) {
			this.data = data;
		}
	}
	
	private static class MetaContainer {
		public Container a;
		public Container b;
	}
	
	public void target(String data) {
		ConnectionManager cm = new ConnectionManager();
		cm.publish(data);
	}
	
	public void transfer(String data, Container container) {
		container.data = data;
	}

	public void doAlias(MetaContainer container1, MetaContainer container2) {
		container2.a = container1.a;
	}

	public void testSimpleMethodCall1() {
		String tainted = TelephonyManager.getDeviceId();
		try {
			this.getClass().getDeclaredMethod("target", String.class).invoke(this, tainted);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			e.printStackTrace();
		}
	}
	
	public void testSimpleMethodCall2() {
		String tainted = TelephonyManager.getDeviceId();
		Class<?> myClass = this.getClass();
		try {
			myClass.getDeclaredMethod("target", String.class).invoke(this, tainted);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			e.printStackTrace();
		}
	}

	public void testSimpleMethodCall3() {
		String tainted = TelephonyManager.getDeviceId();
		try {
			Class<?> myClass = Class.forName("soot.jimple.infoflow.test.ReflectionTestCode");
			myClass.getDeclaredMethod("target", String.class).invoke(this, tainted);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void testSimpleMethodCall4() {
		String tainted = TelephonyManager.getDeviceId();
		try {
			Class<?> myClass = Class.forName("soot.jimple.infoflow.test.ReflectionTestCode");
			Class<?> myClass2 = myClass;
			myClass2.getDeclaredMethod("target", String.class).invoke(this, tainted);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void testConditionalMethodCall1() {
		String tainted = TelephonyManager.getDeviceId();
		try {
			Class<?> myClass;
			if (Math.random() < 0.5)
				myClass = Class.forName("soot.jimple.infoflow.test.ReflectionTestCode");
			else
				myClass = Class.forName("foo");
			myClass.getDeclaredMethod("target", String.class).invoke(this, tainted);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private static class TargetTest {
		
		public void target(String data) {
			ConnectionManager cm = new ConnectionManager();
			cm.publish(Integer.valueOf(data));
		}
		
		@SuppressWarnings("unused")
		public void doLeak() {
			String tainted = TelephonyManager.getDeviceId();
			target(tainted);
		}
		
	}
	
	@SuppressWarnings("unused")
	private static class TargetTestInherited extends TargetTest {
		
	}
	
	@SuppressWarnings("unused")
	private class UnrelatedClass {
		
	}

	public void testConditionalMethodCall2() {
		String tainted = TelephonyManager.getDeviceId();
		try {
			Class<?> myClass;
			if (Math.random() < 0.5)
				myClass = Class.forName("soot.jimple.infoflow.test.ReflectionTestCode");
			else
				myClass = Class.forName("soot.jimple.infoflow.test.ReflectionTestCode$TargetTest");
			Object obj = myClass.newInstance();
			myClass.getDeclaredMethod("target", String.class).invoke(obj, tainted);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException | ClassNotFoundException | InstantiationException e) {
			e.printStackTrace();
		}
	}

	public void testConditionalMethodCall3() {
		String tainted = TelephonyManager.getDeviceId();
		try {
			Class<?> myClass;
			if (Math.random() < 0.5)
				myClass = Class.forName("soot.jimple.infoflow.test.ReflectionTestCode");
			else
				myClass = Class.forName("soot.jimple.infoflow.test.ReflectionTestCode$TargetTestInherited");
			Object obj = myClass.newInstance();
			myClass.getDeclaredMethod("target", String.class).invoke(obj, tainted);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException | ClassNotFoundException | InstantiationException e) {
			e.printStackTrace();
		}
	}

	public void testConditionalMethodCall4() {
		String tainted = TelephonyManager.getDeviceId();
		try {
			Class<?> myClass;
			if (Math.random() < 0.5)
				myClass = Class.forName("soot.jimple.infoflow.test.ReflectionTestCode$IDoNotExist");
			else
				myClass = Class.forName("soot.jimple.infoflow.test.ReflectionTestCode$UnrelatedClass");
			Object obj = myClass.newInstance();
			myClass.getDeclaredMethod("target", String.class).invoke(obj, tainted);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException | ClassNotFoundException | InstantiationException e) {
			e.printStackTrace();
		}
	}

	public void testTransfer1() {
		String tainted = TelephonyManager.getDeviceId();
		Container container = new Container();
		try {
			this.getClass().getDeclaredMethod("transfer", String.class, Container.class).invoke(this, tainted, container);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			e.printStackTrace();
		}
		ConnectionManager cm = new ConnectionManager();
		cm.publish(container.data);
	}
	
	public void testParameterAlias1() {
		MetaContainer metaContainer = new MetaContainer();
		metaContainer.a = new Container();
		metaContainer.b = metaContainer.a;
		String tainted = TelephonyManager.getDeviceId();
		try {
			this.getClass().getDeclaredMethod("transfer", String.class, Container.class).invoke(this, tainted, metaContainer.a);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			e.printStackTrace();
		}
		ConnectionManager cm = new ConnectionManager();
		cm.publish(metaContainer.b.data);
	}
	
	public void testAliasInCallee1() {
		String tainted = TelephonyManager.getDeviceId();

		MetaContainer metaContainer1 = new MetaContainer();
		MetaContainer metaContainer2 = new MetaContainer();
		metaContainer1.a = new Container();
		metaContainer1.a.data = tainted;
		
		try {
			this.getClass().getDeclaredMethod("doAlias", Container.class, Container.class).invoke(this, metaContainer1, metaContainer2);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			e.printStackTrace();
		}
		ConnectionManager cm = new ConnectionManager();
		cm.publish(metaContainer2.a.data);		
	}
	
	public void testReflectiveInstance1() {
		try {
			Container container = (Container) Class.forName("soot.jimple.infoflow.test.ReflectionTestCode$Container").newInstance();
			String tainted = TelephonyManager.getDeviceId();
			
			container.data = tainted;
			
			ConnectionManager cm = new ConnectionManager();
			cm.publish(container.data);		
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void testReflectiveInstance2() {
		try {
			Container container = (Container) Class.forName("soot.jimple.infoflow.test.ReflectionTestCode$Container").newInstance();
			String tainted = TelephonyManager.getDeviceId();
			
			container.set(tainted);
			
			ConnectionManager cm = new ConnectionManager();
			cm.publish(container.data);		
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException
				| IllegalArgumentException | SecurityException e) {
			e.printStackTrace();
		}
	}

	public void testReflectiveInstance3() {
		try {
			Container container = (Container) Class.forName("soot.jimple.infoflow.test.ReflectionTestCode$Container").newInstance();
			String tainted = TelephonyManager.getDeviceId();
			
			container.data = tainted;
			
			ConnectionManager cm = new ConnectionManager();
			cm.publish(container.data);		
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void thisClassTest1() {
		try {
			Class<?> c = this.getClass();
			String tainted = TelephonyManager.getDeviceId();
			c.getDeclaredMethod("target", String.class).invoke(this, tainted);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void thisClassTest2() {
		try {
			Class<?> c = ReflectionTestCode.class;
			String tainted = TelephonyManager.getDeviceId();
			c.getDeclaredMethod("target", String.class).invoke(this, tainted);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void propoagationClassTest1() {
		try {
			String className = "soot.jimple.infoflow.test.ReflectionTestCode";
			Class<?> c = Class.forName(className);
			String tainted = TelephonyManager.getDeviceId();
			c.getDeclaredMethod("target", String.class).invoke(this, tainted);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void propoagationClassTest2() {
		try {
			String className = "soot.jimple.infoflow.test.ReflectionTestCode";
			Class<?> c = Class.forName(className);
			Object obj = c.newInstance();
			c.getDeclaredMethod("target", String.class).invoke(obj, TelephonyManager.getDeviceId());
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void noArgumentTest1() {
		try {
			Class<?> c = Class.forName("soot.jimple.infoflow.test.ReflectionTestCode$TargetTest");
			Object obj = c.newInstance();
			c.getDeclaredMethod("doLeak").invoke(obj, (Object[]) null);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void noArgumentTest2() {
		try {
			Class<?> c = Class.forName("soot.jimple.infoflow.test.ReflectionTestCode$TargetTest");
			Object obj = c.newInstance();
			c.getDeclaredMethod("doLeak").invoke(obj);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void allObjectTest1() {
		try {
			Class<?> c = Class.forName("soot.jimple.infoflow.test.ReflectionTestCode$TargetTest");
			Object obj = c.newInstance();
			c.getDeclaredMethod("target", String.class).invoke(obj, TelephonyManager.getDeviceId());
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
