/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Arrays5.java,v 1.3 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.arrays;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description = "strong updates to array elements" 
 *  @servlet vuln_count = "0" 
 *  */
public class Arrays5 extends BasicTestCase implements MicroTestCase {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = req.getParameter("name");
        String[] array = new String[10];
        
        array[0] = name;
        array[0] = "abc";
        
        PrintWriter writer = resp.getWriter();
        writer.println(array[0]);         /* OK */
    }
    
    public String getDescription() {
        return "strong updates to array elements";
    }
    
    public int getVulnerabilityCount() {
        return 0;
    }
}