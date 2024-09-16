/**
   @author Benjamin Livshits <livshits@cs.stanford.edu>
   
   $Id: Basic40.java,v 1.3 2006/04/21 17:14:26 livshits Exp $
 */
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;

import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="MultipartRequest test"
 * @servlet vuln_count = "1"
 */
public class Basic40 extends BasicTestCase implements MicroTestCase {
	private static final String FIELD_NAME = "name";

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		MultipartHttpServletRequest mreq = new DefaultMultipartHttpServletRequest(req);
		String name = mreq.getParameter(FIELD_NAME);

		PrintWriter writer = resp.getWriter();
		writer.println(name); /* BAD */
	}

	public String getDescription() {
		return "MultipartRequest test";
	}

	public int getVulnerabilityCount() {
		return 1;
	}
}