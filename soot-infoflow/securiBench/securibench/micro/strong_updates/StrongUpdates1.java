/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: StrongUpdates1.java,v 1.5 2006/04/04 20:00:41 livshits Exp $
 */
package securibench.micro.strong_updates;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="simple test of strong updates" 
 *  @servlet vuln_count = "0" 
 *  */
public class StrongUpdates1 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = req.getParameter(FIELD_NAME);
        name = "abc";

        PrintWriter writer = resp.getWriter();
        writer.println(name);              /* OK */
    }

    public String getDescription() {
        return "simple test of strong updates";
    }

    public int getVulnerabilityCount() {
        return 0;
    }    
}