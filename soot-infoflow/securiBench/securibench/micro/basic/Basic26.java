/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Basic26.java,v 1.3 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="test getParameterMap" 
 *  @servlet vuln_count = "1" 
 *  */
public class Basic26 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
       Map m = req.getParameterMap();
       for(Iterator iter = m.entrySet().iterator(); iter.hasNext();) {
           Map.Entry e = (Entry) iter.next();
           if(e.getKey().equals(FIELD_NAME)) {
               PrintWriter writer = resp.getWriter();
               writer.println(e.getValue());        
           }           
       }
    }
    
    public String getDescription() {
        return "test getParameterMap";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}