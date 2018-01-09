/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Basic11.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="a simple false positive because of two calls to String.toLowerCase" 
 *  @servlet vuln_count = "2" 
 *  */
public class Basic11 extends BasicTestCase implements MicroTestCase {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String s1 = req.getParameter("name");
        String s2 = "abc";
        String s3 = s1.toUpperCase();
        String s4 = s2.toUpperCase();
        
        PrintWriter writer = resp.getWriter();
        writer.println(s3);         /* BAD */
        writer.println(s1 + ";");   /* BAD */
        writer.println(s4);         /* OK */
    }
    
    public String getDescription() {
        return "a simple false positive because of two calls to String.toLowerCase";
    }
    
    public int getVulnerabilityCount() {
        return 2;
    }
}