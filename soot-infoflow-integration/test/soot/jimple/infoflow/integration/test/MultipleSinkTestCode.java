package soot.jimple.infoflow.integration.test;

public class MultipleSinkTestCode {
    public static String source() {
        return "secret";
    }

    static class ReturnObject {
        String fieldA;
        String fieldB;
    }

    public static ReturnObject objectSource() {
        return new ReturnObject();
    }

    public static ReturnObject objectSourceA() {
        return new ReturnObject();
    }

    public static ReturnObject objectSourceB() {
        return new ReturnObject();
    }

    static class MyClass {
        private String str;

        // Has different additional flow conditions in different categories
        void conditionalSink(String str) {
            this.str = str;
        }
        void contextOne() {
            System.out.println(str);
        }
        void contextTwo() {
            System.out.println(str);
        }
    }

    // Each parameter is in a different category
    public static void sink(String param1, String param2) {
        System.out.println(param1 + param2);
    }

    public static void objectSink(ReturnObject obj) {
        System.out.println(obj.fieldA + obj.fieldB);
    }

    public void testNoCondition() {
        String secret = source();
        MyClass c = new MyClass();
        c.conditionalSink(secret);
    }

    public void testOneCondition() {
        String secret = source();
        MyClass c = new MyClass();
        c.contextOne();
        c.conditionalSink(secret);
    }

    public void testBothConditions() {
        String secret = source();
        MyClass c = new MyClass();
        c.contextOne();
        c.contextTwo();
        c.conditionalSink(secret);
    }

    public void testParam1() {
        sink(source(), "not secret");
    }

    public void testParam2() {
        sink("not secret", source());
    }

    public void testBothParams() {
        sink(source(), source());
    }

    public void testReturn1() {
        objectSink(objectSourceA());
    }

    public void testReturn2() {
        objectSink(objectSourceB());
    }

    public void testBothReturns() {
        objectSink(objectSource());
    }

    ReturnObject source;
    public void testParamAsSource() {
        objectSink(source);
    }

    ReturnObject sink;
    public void testParamAsSink() {
        sink = objectSource();
    }
}
