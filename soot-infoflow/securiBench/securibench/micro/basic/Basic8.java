/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Basic8.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.BitSet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="test of complex conditionals" 
 *  @servlet vuln_count = "1" 
 *  */
public class Basic8 extends BasicTestCase implements MicroTestCase {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String str = req.getParameter("name");
        BitSet bs = new BitSet(10);
        
        if(bs.get(0)) {
            if(bs.get(1)) {
                if(bs.get(2)) {
                    if(bs.get(3)) {
                        if(bs.get(4)) {
                            
                        }
                    } else {
                        PrintWriter writer = resp.getWriter();
                        writer.println(str);    /* BAD */
                    }
                }
            }
        }
    }
    
    public String getDescription() {
        return "test of complex conditionals";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}