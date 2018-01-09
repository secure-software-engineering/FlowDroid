/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
   
    $Id: Session3.java,v 1.3 2006/04/04 20:00:41 livshits Exp $
 */
package securibench.micro.session;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="test of session enumeration"
 *  @servlet vuln_count = "1" 
 *  */
public class Session3 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
       String name = req.getParameter(FIELD_NAME);
       HttpSession session = req.getSession();
       session.setAttribute("name", name);
       Enumeration e = session.getAttributeNames();
       while(e.hasMoreElements()) {
           String attrName = (String) e.nextElement();
           String attrValue = (String) session.getAttribute(attrName);

           PrintWriter writer = resp.getWriter();
           writer.println(attrValue);                      /* BAD */
       }
    }
    
    public String getDescription() {
        return "test of session enumeration";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}