/**
   $Id: BasicTestCase.java,v 1.5 2006/04/21 17:14:24 livshits Exp $
*/
package securibench.micro;

import java.io.IOException;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import soot.jimple.infoflow.test.securibench.supportClasses.DummyServletConfig;

public abstract class BasicTestCase extends HttpServlet {
	protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// do nothing
	}

	protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// do nothing
	}

	protected void doPost(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
		// do nothing
	}

	protected void doDelete(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
		// do nothing
	}

	protected void doPut(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
		// do nothing
	}

	// added method for testing:

	@Override
	public ServletConfig getServletConfig() {
		return new DummyServletConfig();
	}

}