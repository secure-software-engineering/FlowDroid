/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Factories1.java,v 1.3 2006/04/04 20:00:41 livshits Exp $
 */
package securibench.micro.factories;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="simple factory problem with toLowerCase" 
 *  @servlet vuln_count = "1" 
 *  */
public class Factories1 extends BasicTestCase implements MicroTestCase {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String s1 = req.getParameter("name");
        String s2 = s1.toLowerCase();
        String s3 = "abc".toLowerCase();
            
        PrintWriter writer = resp.getWriter();
        
        writer.println(s2);    /* BAD */
        writer.println(s3);    /* OK */
    }
    
    public String getDescription() {
        return "simple factory problem with toLowerCase";
    }

    public int getVulnerabilityCount() {
        return 1;
    }
}