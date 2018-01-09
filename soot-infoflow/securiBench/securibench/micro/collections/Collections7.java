/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Collections7.java,v 1.3 2006/04/04 20:00:41 livshits Exp $
 */
package securibench.micro.collections;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description = "test of map iterators" 
 *  @servlet vuln_count = "1" 
 *  */
public class Collections7 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = req.getParameter(FIELD_NAME);
        Map m = new HashMap();
        m.put("a", name);
        for(Iterator iter = m.entrySet().iterator(); iter.hasNext();) {
            Map.Entry e = (Entry) iter.next();
            
            PrintWriter writer = resp.getWriter();
            writer.println(e.getKey());                         /* OK */
            writer.println(e.getValue());                       /* BAD */
        }
    }
    
    public String getDescription() {
        return "test of map iterators";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}