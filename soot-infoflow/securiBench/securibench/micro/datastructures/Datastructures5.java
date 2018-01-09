/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Datastructures5.java,v 1.1 2006/04/21 17:14:24 livshits Exp $
 */
package securibench.micro.datastructures;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="nested data in a loop" 
 *  @servlet vuln_count = "1" 
 *  */
public class Datastructures5 extends BasicTestCase implements MicroTestCase {
    public class C {
    	private String str;
    	private C next;
    	
    	public String getData(){return this.str;}
    	public void setData(String str){this.str = str;}
    	public void setNext(C next){this.next = next;}
	}

	private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
       String name = req.getParameter(FIELD_NAME);
       C c1 = new C();
       c1.setData("abc");
       
       C c2 = new C();
       c2.setData("def");
       c1.setNext(c2);
       
       C c3 = new C();
       c3.setData(name.toUpperCase());
       c2.setNext(c3);
       
       C c = c1;
       while (c != null) {
    	   String str = c.getData();
           PrintWriter writer = resp.getWriter();
           writer.println(str);                              /* BAD */
           c = c.next;
       }
    }
    
    public String getDescription() {
        return "nested data in a loop";
    }
    
    public int getVulnerabilityCount() {
        return 1;
    }
}