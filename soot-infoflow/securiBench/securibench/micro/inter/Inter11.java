/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Inter11.java,v 1.1 2006/04/21 17:14:26 livshits Exp $
 */
package securibench.micro.inter;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="object sensitivity and context sensitivity combined" 
 *  @servlet vuln_count = "1" 
 *  */
public class Inter11 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String s1 = req.getParameter(FIELD_NAME);
        
        String s2 = id(foo(s1));
        String s3 = id(foo("abc"));
        
        PrintWriter writer = resp.getWriter();  
        writer.println(s2);                    /* BAD */
        writer.println(s3);                    /* OK */
    }
    
    private String id(String string) {
		return string;
	}

	private String foo(String s1) {
		return s1.toUpperCase();
	}
    
    public String getDescription() {
        return "object sensitivity and context sensitivity combined";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}