/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Collections6.java,v 1.4 2006/04/04 20:00:41 livshits Exp $
 */
package securibench.micro.collections;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description = "test of maps" 
 *  @servlet vuln_count = "1" 
 *  */
public class Collections6 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = req.getParameter(FIELD_NAME);
        Map m = new HashMap();
        m.put("a", name);
        String s1 = (String) m.get("b");
        String s2 = (String) m.get("a");
        
        PrintWriter writer = resp.getWriter();
        writer.println(s1);                    /* OK */
        writer.println(s2);                    /* BAD */
    }
    
    public String getDescription() {
        return "test of maps";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}