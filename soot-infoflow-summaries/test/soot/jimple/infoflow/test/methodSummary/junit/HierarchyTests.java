package soot.jimple.infoflow.test.methodSummary.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import soot.jimple.infoflow.methodSummary.data.provider.EagerSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.provider.IMethodSummaryProvider;

public class HierarchyTests {

	protected final IMethodSummaryProvider testProvider;
	protected final IMethodSummaryProvider manualProvider;

	public HierarchyTests() throws IOException {
		File testRoot = BaseSummaryTaintWrapperTests.getTestRoot();
		testProvider = new EagerSummaryProvider(new File(testRoot, "testSummaries"));
		manualProvider = new EagerSummaryProvider(new File(testRoot, "summariesManual"));
	}

	@Test
	public void superClassesTest() {
		List<String> superclasses = manualProvider.getSuperclassesOf("java.util.ArrayList$ListItr");
		assertEquals(2, superclasses.size());
		assertEquals("java.util.ArrayList$Itr", superclasses.get(0));
		assertEquals("java.lang.Object", superclasses.get(1));
	}

	@Test
	public void superInterfacesTest() {
		Collection<String> superInterfaces = manualProvider.getSuperinterfacesOf("java.io.InputStream");
		assertEquals(2, superInterfaces.size());
		assertTrue(superInterfaces.contains("java.io.Closeable"));
		assertTrue(superInterfaces.contains("java.lang.AutoCloseable"));
	}

	@Test
	public void subclassesTest() {
		Collection<String> subclasses = manualProvider.getSubclassesOf("org.apache.http.message.AbstractHttpMessage");
		assertEquals(13, subclasses.size());
		assertTrue(subclasses.contains("org.apache.http.client.methods.AbstractExecutionAwareRequest"));
		assertTrue(subclasses.contains("org.apache.http.message.BasicHttpRequest"));
		assertTrue(subclasses.contains("org.apache.http.client.methods.HttpRequestBase"));
		assertTrue(subclasses.contains("org.apache.http.message.BasicHttpEntityEnclosingRequest"));
		assertTrue(subclasses.contains("org.apache.http.client.methods.HttpDelete"));
		assertTrue(subclasses.contains("org.apache.http.client.methods.HttpTrace"));
		assertTrue(subclasses.contains("org.apache.http.client.methods.HttpEntityEnclosingRequestBase"));
		assertTrue(subclasses.contains("org.apache.http.client.methods.HttpHead"));
		assertTrue(subclasses.contains("org.apache.http.client.methods.HttpGet"));
		assertTrue(subclasses.contains("org.apache.http.client.methods.HttpOptions"));
		assertTrue(subclasses.contains("org.apache.http.client.methods.HttpPatch"));
		assertTrue(subclasses.contains("org.apache.http.client.methods.HttpPost"));
		assertTrue(subclasses.contains("org.apache.http.client.methods.HttpPut"));
	}

	@Test
	public void implementersOfInterfaceTest() {
		List<String> implementers = manualProvider.getImplementersOfInterface("java.util.Iterator");
		assertEquals(1, implementers.size());
		assertTrue(implementers.contains("java.util.Scanner"));
	}

	@Test
	public void subInterfacesOfTest() {
		Set<String> subinterfaces = manualProvider.getSubInterfacesOf("java.util.Collection");
		assertEquals(4, subinterfaces.size());
		assertTrue(subinterfaces.contains("java.util.Set"));
		assertTrue(subinterfaces.contains("java.util.List"));
		assertTrue(subinterfaces.contains("java.util.Queue"));
		assertTrue(subinterfaces.contains("java.util.Deque"));
	}

}
