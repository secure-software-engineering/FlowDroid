/**
   @author Benjamin Livshits <livshits@cs.stanford.edu>
   
   $Id: Basic36.java,v 1.2 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.basic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="values obtained from HttpServletRequest input stream"
 * @servlet vuln_count = "1"
 */
public class Basic36 extends BasicTestCase implements MicroTestCase {
      protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ServletInputStream in = req.getInputStream();
        BufferedReader r = new BufferedReader(new InputStreamReader(in));
        String line = r.readLine();
        
        PrintWriter writer = resp.getWriter();        
        writer.println(line);              /* BAD */
    }

    public String getDescription() {
        return "values obtained from HttpServletRequest input stream";
    }

    public int getVulnerabilityCount() {
        return 1;
    }
}