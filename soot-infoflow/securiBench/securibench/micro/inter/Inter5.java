/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Inter5.java,v 1.3 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.inter;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="store stuff in a field" 
 *  @servlet vuln_count = "1" 
 *  */
public class Inter5 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = req.getParameter(FIELD_NAME);

        String s1 = id(name);
        String s2 = id("abc");
        
        PrintWriter writer = resp.getWriter();
        writer.println(s1);         /* BAD */
        writer.println(s2);         /* OK */
    }
    
    private String id(String in) throws IOException {
        return in.toLowerCase();
    }

    public String getDescription() {
        return "store stuff in a field";
    }
    
    public int getVulnerabilityCount() {
        return 2;
    }
}