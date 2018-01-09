/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Inter13.java,v 1.1 2006/04/21 17:14:26 livshits Exp $
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
 *  @servlet description="recursive case" 
 *  @servlet vuln_count = "1" 
 *  */
public class Inter13 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String s1 = req.getParameter(FIELD_NAME);
        
        f(s1, 1000, resp);
    }
    
	private void f(String s1, int i, ServletResponse resp) throws IOException {
		if(i != 0) {
			f(s1, i-1, resp);
		} else {
	        PrintWriter writer = resp.getWriter();
	        writer.println(s1);                    /* BAD */
		}

		
	}

    public String getDescription() {
        return "recursive case";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}