/**
   @author Benjamin Livshits <livshits@cs.stanford.edu>
   
   $Id: Basic33.java,v 1.2 2006/04/04 20:00:40 livshits Exp $
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
 * @servlet description="values obtained from headers"
 * @servlet vuln_count = "1"
 */
public class Basic33 extends BasicTestCase implements MicroTestCase {
      protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Enumeration e = req.getHeaders("Accept-Language");
        while(e.hasMoreElements()) {
            String header = (String) e.nextElement();
         
            PrintWriter writer = resp.getWriter();            
            writer.println(header);                      /* BAD */            
        }        
    }

    public String getDescription() {
        return "values obtained from headers";
    }

    public int getVulnerabilityCount() {
        return 1;
    }
}