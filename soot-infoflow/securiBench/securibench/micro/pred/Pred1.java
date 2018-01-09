/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Pred1.java,v 1.5 2006/04/21 17:14:26 livshits Exp $
*/
package securibench.micro.pred;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="simple if(false) test" 
 *  @servlet vuln_count = "0" 
 *  */
public class Pred1 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = req.getParameter(FIELD_NAME);
        if(false) {
            PrintWriter writer = resp.getWriter();
            writer.println(name);              					/* OK */
        }
    }

    public String getDescription() {
        return "simple if(false) test";
    }

    public int getVulnerabilityCount() {
        return 0;
    }    
}