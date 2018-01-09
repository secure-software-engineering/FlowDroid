/**
   @author Benjamin Livshits <livshits@cs.stanford.edu>
   
   $Id: Basic30.java,v 1.3 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="field sensitivity"
 * @servlet vuln_count = "1"
 */
public class Basic30 extends BasicTestCase implements MicroTestCase {
    class Data {
        String value1;
        String value2;
    }
    
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = req.getParameter("name");
        Data d = new Data();
        d.value2 = name;
        d.value1 = "abc";
        
        PrintWriter writer = resp.getWriter();
        writer.println(d.value1);                   /* OK */
        writer.println(d.value2);                   /* BAD */
    }

    public String getDescription() {
        return "field sensitivity";
    }

    public int getVulnerabilityCount() {
        return 1;
    }
}