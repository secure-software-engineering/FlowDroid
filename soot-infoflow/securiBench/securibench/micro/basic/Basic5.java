/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Basic5.java,v 1.5 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="moderately complex test of derived strings" 
 *  @servlet vuln_count = "3" 
 *  */
public class Basic5 extends BasicTestCase implements MicroTestCase {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String s1 = req.getParameter("name");
        String s2 = s1.toUpperCase();
        String s3 = s2.concat(";");
        String s4 = s3.replace(';', '.');
        String s5 = s4.trim();
        PrintWriter writer = resp.getWriter();
        
        writer.println(s3);    /* BAD */
        writer.println(s4);    /* BAD */
        writer.println(s5);    /* BAD */
    }
    
    public String getDescription() {
        return "moderately complex test of derived strings";
    }
    
    public int getVulnerabilityCount() {
        return 3;
    }
}