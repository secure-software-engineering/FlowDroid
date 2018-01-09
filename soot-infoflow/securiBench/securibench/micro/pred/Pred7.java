/**
     @author Benjamin Livshits <livshits@cs.stanford.edu>
     
     $Id: Pred7.java,v 1.3 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.pred;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="correlated test with multiple variables" 
 *  @servlet vuln_count = "0" 
 *  */
public class Pred7 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int x = 2;
        int y = 9;
        x++;
        y-= 2;
        
        String name = req.getParameter(FIELD_NAME);
        
        if(x + y == 11) {
            PrintWriter writer = resp.getWriter();
            writer.println(name);              /* OK */     // nothing bad gets here
        }
    }

    public String getDescription() {
        return "correlated test with multiple variables";
    }

    public int getVulnerabilityCount() {
        return 0;
    }    
}