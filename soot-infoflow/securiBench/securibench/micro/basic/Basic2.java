/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Basic2.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="XSS combined with a simple conditional" 
 *  @servlet vuln_count = "1" 
 *  */
public class Basic2 extends BasicTestCase implements MicroTestCase {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String str = req.getParameter("name");
        Random r = new Random();
        boolean choice = r.nextBoolean();
        PrintWriter writer = resp.getWriter();
        
        if(choice) {        
            writer.println(str);    /* BAD */
        }
    }

    public String getDescription() {
        return "XSS combined with a simple conditional";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}