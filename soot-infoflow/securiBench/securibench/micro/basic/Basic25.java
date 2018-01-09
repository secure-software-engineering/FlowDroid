/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Basic25.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="test getParameterValues" 
 *  @servlet vuln_count = "1" 
 *  */
public class Basic25 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
       String[] s = req.getParameterValues(FIELD_NAME);
       String name = s[0].toLowerCase(Locale.UK);
       
       PrintWriter writer = resp.getWriter();
       writer.println(name);                    /* BAD */
    }
    
    public String getDescription() {
        return "test getParameterValues";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}