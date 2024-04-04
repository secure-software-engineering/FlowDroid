package soot.jimple.infoflow.integration.test;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

public class RiverTestCode {
    public static String source() {
        return "secret";
    }
    public static int intSource() {
        return 1337;
    }

    public void riverTest1() {
        try {
            String src = source();

            URL url = new URL("http://some.url");
            URLConnection con = url.openConnection();

            OutputStream os = con.getOutputStream();

            OutputStream osb = new BufferedOutputStream(os);
            osb.write(src.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void riverTest2() {
        try {
            String src = source();

            OutputStream os = new ByteArrayOutputStream();

            OutputStream osb = new BufferedOutputStream(os);
            osb.write(src.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void riverTest3() {
        try {
            String src = source();

            URL url = new URL("http://some.url");
            URLConnection con = url.openConnection();

            OutputStream os = con.getOutputStream();

            BufferedOutputStream osb = new BufferedOutputStream(os);
            osb.write(src.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void riverTest4() {
        try {
            String src = source();

            ByteArrayOutputStream os = new ByteArrayOutputStream();

            BufferedOutputStream osb = new BufferedOutputStream(os);
            osb.write(src.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void riverTest5() {
        try {
            String src = source();

            URL url = new URL("http://some.url");
            URLConnection con = url.openConnection();

            OutputStream os = con.getOutputStream();

            new BufferedWriter(new OutputStreamWriter(os)).write(src);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void riverTest6() {
        try {
            String src = source();

            OutputStream os = new ByteArrayOutputStream();

            new BufferedWriter(new OutputStreamWriter(os)).write(src);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void riverTest7() {
        try {
            String src1 = source();
            String src2 = source();

            URL url = new URL("http://some.url");
            URLConnection con = url.openConnection();

            OutputStream os1 = con.getOutputStream();
            OutputStream os2 = new ByteArrayOutputStream();

            OutputStream os1b = new BufferedOutputStream(os1);
            OutputStream os2b = new BufferedOutputStream(os2);

            os1b.write(src1.getBytes());
            os2.write(src2.getBytes());

            new BufferedWriter(new OutputStreamWriter(os1b)).write(src1);
            new BufferedWriter(new OutputStreamWriter(os2b)).write(src2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void sendToUrl(URL url, String data) {
        System.out.println("leak");
    }

    public void riverTest8() {
        try {
            URL url = new URL("http://some.url");

            String source = source();
            sendToUrl(url, source);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void unconditionalSink(String str) {
        System.out.println("leak");
    }

    public void riverTest9() {
        unconditionalSink(source());
    }


    void classConditionalSink(String data) {
        System.out.println("leak");
    }

    static class T {
        void injectSensitiveData(OutputStream os) {
            //
        }
    }

    public void riverTest10() throws IOException {
        int secret = intSource();
        OutputStream os = new ByteArrayOutputStream();
        os.write(secret);
    }

    public void riverTest11() throws IOException {
        int secret = intSource();
        OutputStream os = new ByteArrayOutputStream();
        T t = new T();
        t.injectSensitiveData(os);
        os.write(secret);
    }

}
