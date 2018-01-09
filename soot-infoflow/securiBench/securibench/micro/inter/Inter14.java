/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Inter14.java,v 1.1 2006/04/21 17:14:26 livshits Exp $
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
 *  @servlet description="interprocedural loop" 
 *  @servlet vuln_count = "1" 
 *  */
public class Inter14 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String s1 = req.getParameter(FIELD_NAME);
        
        for(int i = 0; i < 1500; i++){
        	if(i > 1000 && i < 1200 && (i % 7 == 3)){
        		f(s1, 1000, resp);		
        	}
        }
    }
    
	private void f(String s1, int i, ServletResponse resp) throws IOException {
		if(i != 0) {
		    PrintWriter writer = resp.getWriter();
	        writer.println(s1);                    /* BAD */
		}		
	}

    public String getDescription() {
        return "interprocedural loop";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}