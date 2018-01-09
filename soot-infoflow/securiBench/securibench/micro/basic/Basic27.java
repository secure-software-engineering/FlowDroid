/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Basic27.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
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
 *  @servlet description="test getParameterMap" 
 *  @servlet vuln_count = "1" 
 *  */
public class Basic27 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
       Enumeration e = req.getParameterNames();
       while(e.hasMoreElements()) {
           String name = (String) e.nextElement();
           if(name.equals(FIELD_NAME)) {
               PrintWriter writer = resp.getWriter();
               String value = req.getParameter(name);
               writer.println("value: " + value);           /* BAD */
           }
       }
    }
    
    public String getDescription() {
        return "test getParameterMap";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}