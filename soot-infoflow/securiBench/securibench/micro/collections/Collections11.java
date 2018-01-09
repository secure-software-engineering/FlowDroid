/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Collections11.java,v 1.1 2006/04/21 17:14:26 livshits Exp $
 */
package securibench.micro.collections;

import java.io.IOException;
import java.util.LinkedList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description = "interprocedural collection passing" 
 *  @servlet vuln_count = "1" 
 *  */
public class Collections11 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String s1 = req.getParameter(FIELD_NAME);
        LinkedList c1 = new LinkedList();
        c1.addLast(s1);
        Collections11b c11b = new Collections11b();
        c11b.foo(c1, resp);
    }
    
    public String getDescription() {
        return "interprocedural collection passing";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}