/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Datastructures2.java,v 1.1 2006/04/21 17:14:24 livshits Exp $
 */
package securibench.micro.datastructures;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="multiple objects of the same type" 
 *  @servlet vuln_count = "1" 
 *  */
public class Datastructures2 extends BasicTestCase implements MicroTestCase {
    public class C {
    	private String str;
    	public String getData(){return this.str;}
    	public String getTag(){return this.str;}
    	public void setData(String str){this.str = str;}
	}

	private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
       String name = req.getParameter(FIELD_NAME);
       C c1 = new C();
       c1.setData("def");
       
       C c2 = new C();
       c2.setData(name);
       
       String str1 = c1.getData();
       String str2 = c2.getData();
       
       PrintWriter writer = resp.getWriter();
       writer.println(str1);                              /* OK */
       writer.println(str2);                              /* BAD */
    }
    
    public String getDescription() {
        return "simple test of field assignment";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}