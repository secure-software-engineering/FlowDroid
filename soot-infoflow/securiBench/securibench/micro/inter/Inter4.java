/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Inter4.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.inter;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="store stuff in a field" 
 *  @servlet vuln_count = "1" 
 *  */
public class Inter4 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";
    private String name;

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        name = req.getParameter(FIELD_NAME);

        f(resp);
    }
    
    private void f(ServletResponse resp) throws IOException {
        PrintWriter writer = resp.getWriter();
        writer.println(this.name);         /* BAD */        
    }

    public String getDescription() {
        return "store stuff in a field";
    }
    
    public int getVulnerabilityCount() {
        return 2;
    }
}