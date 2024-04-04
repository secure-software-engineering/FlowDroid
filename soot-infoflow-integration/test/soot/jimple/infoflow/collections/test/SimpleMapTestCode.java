package soot.jimple.infoflow.collections.test;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

import java.util.HashMap;
import java.util.Map;

import soot.jimple.infoflow.collections.test.junit.FlowDroidTest;

public class SimpleMapTestCode {
	@FlowDroidTest(expected = 1)
	public void testMapPutGet1() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.put("ConstantKey", tainted);
		String res = map.get("ConstantKey");
		sink(res);
	}

	@FlowDroidTest(expected = 0)
	public void testMapPutGet2() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.put("ConstantKey", tainted);
		String res = map.get("OtherConstantKey");
		sink(res);
	}

	@FlowDroidTest(expected = 1)
	public void testMapPutGetOrDefault1() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.put("ConstantKey", tainted);
		String res = map.getOrDefault("ConstantKey", "Untainted");
		sink(res);
	}

	@FlowDroidTest(expected = 0)
	public void testMapPutGetOrDefault2() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.put("ConstantKey", tainted);
		String res = map.getOrDefault("OtherConstantKey", "Untainted");
		sink(res);
	}

	@FlowDroidTest(expected = 1)
	public void testMapPutGetOrDefault3() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.put("ConstantKey", "Not tainted");
		String res = map.getOrDefault("NonConstantKey", tainted);
		sink(res);
	}

	@FlowDroidTest(expected = 0)
	public void testMapPutRemoveGet1() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.put("ConstantKey", tainted);
		map.remove("ConstantKey");
		String res = map.get("ConstantKey");
		sink(res);
	}

	@FlowDroidTest(expected = 0)
	public void testMapClear1() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.put("ConstantKey", tainted);
		map.clear();
		String res = map.get("ConstantKey");
		sink(res);
	}

	@FlowDroidTest(expected = 1)
	public void testMapKeySet1() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.put("ConstantKey", tainted);
		String res = "";
		for (String e : map.keySet()) {
			if (e.equals("Some Value"))
				res = map.get(e);
		}
		sink(res);
	}

	@FlowDroidTest(expected = 0)
	public void testMapKeySet2() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.put("ConstantKey", tainted);
		for (String e : map.keySet()) {
			if (e.equals("Some Value"))
				sink(e);
		}
	}

	@FlowDroidTest(expected = 1)
	public void testMapValueSet1() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.put("ConstantKey", tainted);
		for (String e : map.values()) {
			if (e.equals("Some Value"))
				sink(e);
		}
	}

	@FlowDroidTest(expected = 1)
	public void testMapPutAll1() {
		Map<String, String> map = new HashMap<>();
		Map<String, String> map2 = new HashMap<>();

		String tainted = source();
		map.put("ConstantKey", tainted);
		map2.putAll(map);
		sink(map2.get("ConstantKey"));
	}

	@FlowDroidTest(expected = 0)
	public void testMapPutAll2() {
		Map<String, String> map = new HashMap<>();
		Map<String, String> map2 = new HashMap<>();

		String tainted = source();
		map.put("ConstantKey", tainted);
		map2.putAll(map);
		sink(map2.get("OtherKey"));
	}

	@FlowDroidTest(expected = 1)
	public void testMapPutIfAbsent1() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.putIfAbsent("ConstantKey", tainted);
		sink(map.get("ConstantKey"));
	}

	@FlowDroidTest(expected = 0)
	public void testMapPutIfAbsent2() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		String out = map.putIfAbsent("ConstantKey", tainted);
		sink(out);
	}

	@FlowDroidTest(expected = 1)
	public void testMapPutIfAbsent3() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.putIfAbsent("ConstantKey", tainted);
		String out = map.putIfAbsent("ConstantKey", "untainted");
		sink(out);
	}

	@FlowDroidTest(expected = 0)
	public void testMapPutIfAbsent4() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.putIfAbsent("ConstantKey", tainted);
		sink(map.get("OtherConstantKey"));
	}

	@FlowDroidTest(expected = 1)
	public void testMapPutIfAbsent5() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.put("ConstantKey", "untainted");
		map.putIfAbsent("ConstantKey", tainted);
		sink(map.get("ConstantKey"));
	}

	@FlowDroidTest(expected = 1)
	public void testMapCompute1() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.put("XXX", tainted);
		map.compute("XXX", (k, v) -> v);
		sink(map.get("XXX"));
	}

	@FlowDroidTest(expected = 0)
	public void testMapCompute2() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.put("XXX", tainted);
		map.computeIfPresent("XXX", (k, v) -> "Overwritten");
		sink(map.get("XXX"));
	}

	@FlowDroidTest(expected = 1)
	public void testMapCompute3() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.computeIfPresent("XXX", (k, v) -> tainted);
		sink(map.get("XXX"));
	}

	public String unusedStringField;

	@FlowDroidTest(expected = 0)
	public void testMapCompute4() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.compute("XXX", (k, v) -> {
			unusedStringField = tainted;
			return "Overwrite";
		});
		sink(map.get("XXX"));
	}

	@FlowDroidTest(expected = 1)
	public void testMapComputeIfAbsent1() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.computeIfAbsent("XXX", (k) -> tainted);
		sink(map.get("XXX"));
	}

	@FlowDroidTest(expected = 0)
	public void testMapComputeIfAbsent2() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.computeIfAbsent("XXX", (k) -> "Whatever");
		sink(map.get("XXX"));
	}

	@FlowDroidTest(expected = 1)
	public void testMapMerge1() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.put("XXX", tainted);
		map.merge("XXX", "Value", (v1, v2) -> new StringBuilder(v1).append(v2).toString());
		sink(map.get("XXX"));
	}

	@FlowDroidTest(expected = 0)
	public void testMapMerge2() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.put("XXX", tainted);
		map.merge("XXX", "Value", (v1, v2) -> "Overwrite");
		sink(map.get("XXX"));
	}

	@FlowDroidTest(expected = 1)
	public void testMapMerge3() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.merge("XXX", tainted, (v1, v2) -> new StringBuilder(v1).append(v2).toString());
		sink(map.get("XXX"));
	}

	@FlowDroidTest(expected = 0)
	public void testMapMerge4() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.merge("XXX", tainted, (v1, v2) -> "Overwrite");
		sink(map.get("XXX"));
	}

	@FlowDroidTest(expected = 0)
	public void testMapMerge5() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.merge("XXX", tainted, (v1, v2) -> v1);
		sink(map.get("XXX"));
	}

	@FlowDroidTest(expected = 1)
	public void testMapMerge6() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.merge("XXX", tainted, (v1, v2) -> v2);
		sink(map.get("XXX"));
	}

	@FlowDroidTest(expected = 1)
	public void testMapMerge7() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.put("XXX", tainted);
		map.merge("XXX", "Value", (v1, v2) -> v1);
		sink(map.get("XXX"));
	}

	@FlowDroidTest(expected = 0)
	public void testMapMerge8() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.put("XXX", tainted);
		map.merge("XXX", "Value", (v1, v2) -> v2);
		sink(map.get("XXX"));
	}

	@FlowDroidTest(expected = 1)
	public void testMapReplaceAll1() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.put("XXX", tainted);
		map.replaceAll((k, v) -> new StringBuilder("Prefix").append(v).toString());
		sink(map.get("XXX"));
	}

	@FlowDroidTest(expected = 0)
	public void testMapReplaceAll2() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.put("XXX", tainted);
		map.replaceAll((k, v) -> "Overwrite");
		sink(map.get("XXX"));
	}

	@FlowDroidTest(expected = 1)
	public void testMapComputeReturn1() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		String returned = map.compute("XXX", (k, v) -> tainted);
		sink(returned);
	}

	@FlowDroidTest(expected = 1)
	public void testMapComputeReturn2() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		String returned = map.computeIfAbsent("XXX", (k) -> tainted);
		sink(returned);
	}

	@FlowDroidTest(expected = 1)
	public void testMapComputeReturn3() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		String returned = map.computeIfPresent("XXX", (k, v) -> tainted);
		sink(returned);
	}

	@FlowDroidTest(expected = 1)
	public void testMapComputeReturn4() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.put("XXX", tainted);
		String returned = map.merge("XXX", "Value", (v1, v2) -> new StringBuilder(v1).append(v2).toString());
		sink(returned);
	}

	@FlowDroidTest(expected = 1)
	public void testMapComputeReturn5() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		String returned = map.merge("XXX", tainted, (v1, v2) -> new StringBuilder(v1).append(v2).toString());
		sink(returned);
	}

	@FlowDroidTest(expected = 0)
	public void testMapComputeReturn6() {
		Map<String, String> map = new HashMap<>();
		String tainted = source();
		map.put("XXX", tainted);
		String returned = map.merge("XXX", "Value", (v1, v2) -> v2);
		sink(returned);
	}

	@FlowDroidTest(expected = 1)
	public void testNestedMap1() {
		Map<String, Map<String, String>> map = new HashMap<>();
		Map<String, String> innerMap = new HashMap<>();
		innerMap.put("Inner", source());
		map.put("Outer", innerMap);

		// The correct leak
		sink(map.get("Outer").get("Inner"));
		// The false negatives
		sink(map.get("Outer").get("Outer"));
		sink(map.get("Inner").get("Outer"));
		sink(map.get("Inner").get("Inner"));
	}

	@FlowDroidTest(expected = 0)
	public void testNoIterativeApply1() {
		Map<String, String> map = new HashMap<>();
		String oldVal = map.put("XXX", source());
		sink(oldVal);
	}

	private final String x = "XXXXX";

	@FlowDroidTest(expected = 1)
	public void testFinalFieldAsKey1() {
		Map<String, String> map = new HashMap<>();
		map.put(x, source());
		sink(map.get(x));
		sink(map.get("XXX"));
	}
}
