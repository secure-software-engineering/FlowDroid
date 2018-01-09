/**
   @author Benjamin Livshits <livshits@cs.stanford.edu>
   
   $Id: Pred8.java,v 1.3 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.pred;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="using an array element as in a predicate" 
 *  @servlet vuln_count = "1" 
 *  */
public class Pred8 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {       
        String name = req.getParameter(FIELD_NAME);
        String array[] = new String[] {"abc", name};
        
        if(array[1].equals(name)) {
            PrintWriter writer = resp.getWriter();
            writer.println(name);              /* BAD */
        }
    }

    public String getDescription() {
        return "using an array element as in a predicate";
    }

    public int getVulnerabilityCount() {
        return 1;
    }    
}