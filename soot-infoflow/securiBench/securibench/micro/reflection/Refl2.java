/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Refl2.java,v 1.6 2006/04/04 20:00:41 livshits Exp $
 */
package securibench.micro.reflection;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="reflectively access a field" 
 *  @servlet vuln_count = "1" 
 *  */
public class Refl2 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";
    public String name;

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        name = req.getParameter(FIELD_NAME);

        try {
            f(resp);
        } catch(Exception e) {
            System.err.println("An error occurred");
        }
    }
    
    private void f(ServletResponse resp) throws IOException, SecurityException, NoSuchFieldException, ClassNotFoundException, IllegalArgumentException, IllegalAccessException {
        PrintWriter writer = resp.getWriter();
        Field field = Class.forName("securibench.micro.reflection.Refl2").getField("name"); 
        String myName = (String) field.get(this); 
        
        writer.println(myName);         /* BAD */        
    }

    public String getDescription() {
        return "reflectively access a field";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}