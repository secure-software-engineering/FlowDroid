/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Session2.java,v 1.3 2006/04/04 20:00:41 livshits Exp $
 */
package securibench.micro.session;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="test of session false positives" 
 *  @servlet vuln_count = "1" 
 *  */
public class Session2 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
       String name = req.getParameter(FIELD_NAME);
       HttpSession session = req.getSession();
       session.setAttribute("name", name);
       String s2 = (String) session.getAttribute("name");
       String s3 = (String) session.getAttribute("nonsense");
       PrintWriter writer = resp.getWriter();
       
       writer.println(s2);                      /* BAD */
       writer.println(s3);                      /* OK */
    }
    
    public String getDescription() {
        return "test of session false positives";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}