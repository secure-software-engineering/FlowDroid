/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Basic23.java,v 1.6 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.basic;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="path traversal" 
 *  @servlet vuln_count = "3" 
 *  */
public class Basic23 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String s = req.getParameter(FIELD_NAME);
        String name = s.toLowerCase(Locale.UK);

        try {
            /* FileWriter fw = */ new FileWriter(name);                        /* BAD */
            /* FileWriter fr = */ new FileWriter(name);                        /* BAD */
            /* FileInputStream fis = */ new FileInputStream(name);             /* BAD */
        } catch(Throwable e) {
            System.err.println("An error occurred");
        }
    }
    
    public String getDescription() {
        return "path traversal";
    }
    
    public int getVulnerabilityCount() {
        return 3;
    }
}