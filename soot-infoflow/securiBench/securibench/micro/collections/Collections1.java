/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Collections1.java,v 1.5 2006/04/04 20:00:41 livshits Exp $
 */
package securibench.micro.collections;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description = "simple collection deposit/retrieve" 
 *  @servlet vuln_count = "1" 
 *  */
public class Collections1 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String s1 = req.getParameter(FIELD_NAME);
        LinkedList ll = new LinkedList();
        ll.addLast(s1);
        String s2 = (String) ll.getLast();
        
        PrintWriter writer = resp.getWriter();  
        writer.println(s2);                    /* BAD */
    }
    
    public String getDescription() {
        return "simple collection deposit/retrieve";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}