/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Basic7.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="complex test of derived strings involving a string buffer" 
 *  @servlet vuln_count = "1" 
 *  */
public class Basic7 extends BasicTestCase implements MicroTestCase {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String s1 = req.getParameter("name");
        String s2 = s1.toUpperCase();
        StringBuffer buf = new StringBuffer(s2);
        buf.append("abcdefgh");
        buf.insert(3, 's');
        String s3 = buf.toString();
        
        PrintWriter writer = resp.getWriter();
        
        writer.println(s3);    /* BAD */
    }
    
    public String getDescription() {
        return "complex test of derived strings involving a string buffer";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}