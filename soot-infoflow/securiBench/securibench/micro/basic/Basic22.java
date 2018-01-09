/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Basic22.java,v 1.5 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.basic;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="basic path traversal" 
 *  @servlet vuln_count = "1" 
 *  */
public class Basic22 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String s = req.getParameter(FIELD_NAME);
        String name = s.toLowerCase(Locale.UK);

        // this is not a problem just yet: it's perhaps okay to create a file file 
        // a tained filename, but not use it in any way
        File f = new File(name);                       
        // this is definitely bad; an error should be flagged either on this or the 
        // previous line
        f.createNewFile();                              /* BAD */
    }
    
    public String getDescription() {
        return "basic path traversal";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}