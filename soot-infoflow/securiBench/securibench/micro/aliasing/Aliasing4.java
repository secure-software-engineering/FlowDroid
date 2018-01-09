/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Aliasing4.java,v 1.1 2006/04/21 17:14:27 livshits Exp $
 */
package securibench.micro.aliasing;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="simple aliasing with casts" 
 *  @servlet vuln_count = "1" 
 *  */
public class Aliasing4 extends BasicTestCase implements MicroTestCase {
	private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
       String name = req.getParameter(FIELD_NAME);
       Object o1 = name;
       Object o2 = name.concat("abc");
       Object o3 = "anc";
              
       PrintWriter writer = resp.getWriter();
       writer.println(o1);                              /* BAD */
       writer.println(o2);                              /* BAD */
       writer.println(o3);                              /* OK */
    }
    
    public String getDescription() {
        return "simple aliasing with casts";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}