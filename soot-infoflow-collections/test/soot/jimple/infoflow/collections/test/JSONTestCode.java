package soot.jimple.infoflow.collections.test;

import org.json.JSONObject;
import soot.jimple.infoflow.collections.test.junit.FlowDroidTest;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

public class JSONTestCode {
    @FlowDroidTest(expected = 1)
    public void testJSON1() {
        JSONObject jobj = new JSONObject();
        jobj.append("Key", source());
        sink(jobj.get("Key"));
    }

    @FlowDroidTest(expected = 0)
    public void testJSON2() {
        JSONObject jobj = new JSONObject();
        jobj.append("Key", source());
        sink(jobj.get("Key2"));
    }

    @FlowDroidTest(expected = 0)
    public void testJSON3() {
        JSONObject jobj = new JSONObject();
        jobj.put("Key", source());
        JSONObject alias = jobj.put("Key", "Overwritten");
        sink(alias.get("Key"));
    }
}
