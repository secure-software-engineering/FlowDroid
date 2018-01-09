/**
   @author Benjamin Livshits <livshits@cs.stanford.edu>
   
   $Id: Basic31.java,v 1.2 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="values obtained from cookies"
 * @servlet vuln_count = "2"
 */
public class Basic31 extends BasicTestCase implements MicroTestCase {
    class Data {
        String value1;
        String value2;
    }
    
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Cookie[] cookies = req.getCookies();
        
        String name     = cookies[0].getName();
        String value    = cookies[0].getValue();
        String comment  = cookies[0].getComment();
        
        PrintWriter writer = resp.getWriter();
        
        if(name != null) {
            writer.println(name);                      /* BAD */
        }
        if(value != null) {
            writer.println(value);                     /* BAD */
        }
        if(comment != null) {
            writer.println(comment);                   /* BAD */
        }
    }

    public String getDescription() {
        return "values obtained from cookies";
    }

    public int getVulnerabilityCount() {
        return 2;
    }
}