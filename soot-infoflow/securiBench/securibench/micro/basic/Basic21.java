/**
    @author Benjamin Livshits <livshits@cs.stanford.edu>
    
    $Id: Basic21.java,v 1.7 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.basic;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/** 
 *  @servlet description="SQL injection with less commonly used methods" 
 *  @servlet vuln_count = "4" 
 *  */
public class Basic21 extends BasicTestCase implements MicroTestCase {
    private static final String FIELD_NAME = "name";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String s = req.getParameter(FIELD_NAME);
        String name = s.toLowerCase(Locale.UK);

        Connection con = null;
        try {
            con = DriverManager.getConnection(MicroTestCase.CONNECTION_STRING);
            Statement stmt = con.createStatement();
            stmt.executeUpdate("select * from Users where name=" + name);       /* BAD */
            stmt.executeUpdate("select * from Users where name=" + name, 0);    /* BAD */
            stmt.executeUpdate("select * from Users where name=" + name,        /* BAD */ 
                new String[] {});     
            stmt.executeQuery("select * from Users where name=" + name);        /* BAD */
        } catch (SQLException e) {
            System.err.println("An error occurred");
        } finally {
            try {
                if(con != null) con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    public String getDescription() {
        return "SQL injection with less commonly used methods";
    }
    
    public int getVulnerabilityCount() {
        return 4;
    }
}