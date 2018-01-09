/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Basic12.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="a simple conditional; both branches should be taken" 
 *  @servlet vuln_count = "2" 
 *  */
public class Basic12 extends BasicTestCase implements MicroTestCase {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String s1 = req.getParameter("name");
        PrintWriter writer = resp.getWriter();
        boolean choice = new Random().nextBoolean();
        
        if(choice) {
            writer.println(s1 + ":");   /* BAD */
        } else{
            writer.println(s1 + ";");   /* BAD */
        }
        
        writer.println("\n");           /* OK */
    }
    
    public String getDescription() {
        return "a simple conditional; both branches should be taken";
    }
    
    public int getVulnerabilityCount() {
        return 2;
    }
}