package soot.jimple.infoflow.collections.test;

public class Helper {
    public static String source() {
        return "secret";
    }

    public static void sink(Object str) {
        System.out.println(str);
    }
}
