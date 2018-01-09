/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Basic15.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;


/** 
 *  @servlet description="test casts more exhaustively" 
 *  @servlet vuln_count = "1" 
 *  */
public class Basic15 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String s1 = req.getParameter(FIELD_NAME);
        Object o = s1 + ";";
        String s2 = (String) o;
        StringBuffer buf = new StringBuffer(s2);
        Object o2 = buf;
        String s3 = ((StringBuffer) o2).toString();
        PrintWriter writer = resp.getWriter();  
        writer.println(s3);                    /* BAD */
    }
    
    public String getDescription() {
        return "test casts more exhaustively";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}