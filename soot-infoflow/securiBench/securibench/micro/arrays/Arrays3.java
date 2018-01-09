/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
 
    $Id: Arrays3.java,v 1.3 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.arrays;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description = "a more complex array test" 
 *  @servlet vuln_count = "1" 
 *  */
public class Arrays3 extends BasicTestCase implements MicroTestCase {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String s1 = req.getParameter("name");
        String[] array1 = new String[10];
        String[] array2 = new String[10];

        array1[0] = s1;
        array2[0] = "abc";
        
        PrintWriter writer = resp.getWriter();
        writer.println(array1[0]);         /* BAD */
        writer.println(array2[0]);         /* OK */
    }
    
    public String getDescription() {
        return "a more complex array test";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}