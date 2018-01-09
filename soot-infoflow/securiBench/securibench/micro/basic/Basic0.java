/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Basic1.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="very simple XSS" 
 *  @servlet vuln_count = "1" 
 *  */
public class Basic0 extends BasicTestCase implements MicroTestCase {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String str = req.getParameter("name");
        PrintWriter writer = resp.getWriter();
        String str2 = str.toString();
        writer.println(str2);    /* BAD */
    }
    
    public String getDescription() {
        return "very simple XSS";
    }

    public int getVulnerabilityCount() {
        return 1;
    }
}