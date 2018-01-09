/*
    $Id: MicroTestCase.java,v 1.4 2005/11/26 22:18:19 livshits Exp $
 */
package securibench.micro;

/**
 * An interface all test cases are supposed to implement this interface.
 * 
 * At the top of you case, place the following two keywords:
 * 
 * \@servlet description="..." 
 * \@servlet vuln_count = "1" 
 * 
 * These values will be used by the test harness.
 * */
public interface MicroTestCase {
    public String CONNECTION_STRING = "jdbc:dtF:E. coli;USR=dtfadm;PWD=dtfadm;Create=always;APPL=GIVE;DType=FILE";

    /**
     * A brief textual description of the test case.
     * */
    public String getDescription();
    
    /**
     * Expected number of vulnerabilities in the test case.
     * */
    public int getVulnerabilityCount();
}
