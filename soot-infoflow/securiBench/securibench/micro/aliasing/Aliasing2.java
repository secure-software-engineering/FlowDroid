/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Aliasing2.java,v 1.1 2006/04/21 17:14:27 livshits Exp $
 */
package securibench.micro.aliasing;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="simple aliasing false positive" 
 *  @servlet vuln_count = "1" 
 *  */
public class Aliasing2 extends BasicTestCase implements MicroTestCase {
	private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
       String name = req.getParameter(FIELD_NAME);
       String str = "abc";
       name = str;
              
       PrintWriter writer = resp.getWriter();
       writer.println(str);                              /* OK */
    }
    
    public String getDescription() {
        return "simple aliasing false positive";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}