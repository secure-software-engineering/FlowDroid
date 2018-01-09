/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Basic24.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.basic;

import java.io.IOException;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="unsafe redirect" 
 *  @servlet vuln_count = "1" 
 *  */
public class Basic24 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String s = req.getParameter(FIELD_NAME);
        String name = s.toLowerCase(Locale.UK);

       resp.sendRedirect("/user/" + name);          /* BAD */
    }
    
    public String getDescription() {
        return "unsafe redirect";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}