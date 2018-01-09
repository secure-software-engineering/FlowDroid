/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Arrays10.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.arrays;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description = "comple multidimentional array test" 
 *  @servlet vuln_count = "1" 
 *  */
public class Arrays10 extends BasicTestCase implements MicroTestCase {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = req.getParameter("name");
        String[][] array = new String[3][5];
        array[0] = new String[] {name, "abc"};
        
        PrintWriter writer = resp.getWriter();
        writer.println(array[0][0]);         /* BAD */
        writer.println(array[0][2]);         /* OK */
    }
    
    public String getDescription() {
        return "comple multidimentional array test";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}