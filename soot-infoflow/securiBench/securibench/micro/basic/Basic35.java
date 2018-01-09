/**
   @author Benjamin Livshits <livshits@cs.stanford.edu>
   
   $Id: Basic35.java,v 1.2 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="values obtained from HttpServletRequest"
 * @servlet vuln_count = "6"
 */
public class Basic35 extends BasicTestCase implements MicroTestCase {
      protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Enumeration e = req.getHeaderNames();
        while(e.hasMoreElements()) {
            PrintWriter writer = resp.getWriter();
            // I believe these can be forged also
            // TODO: double-check this
            writer.println(req.getProtocol());                /* BAD */
            writer.println(req.getScheme());                  /* BAD */
            writer.println(req.getAuthType());                /* BAD */
            writer.println(req.getQueryString());             /* BAD */
            writer.println(req.getRemoteUser());              /* BAD */
            writer.println(req.getRequestURL());              /* BAD */
        }        
    }

    public String getDescription() {
        return "values obtained from headers";
    }

    public int getVulnerabilityCount() {
        return 6;
    }
}