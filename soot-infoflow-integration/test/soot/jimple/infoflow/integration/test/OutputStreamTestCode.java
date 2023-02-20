package soot.jimple.infoflow.integration.test;

import com.esotericsoftware.kryo.io.ByteBufferOutput;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

public class OutputStreamTestCode {
    public static String source() {
        return "secret";
    }

    public void testBufferedOutputStream1() {
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

    public void testBufferedOutputStream2() {
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

    public void testObjectOutputStream1() {
        try {
            String src = source();

            URL url = new URL("http://some.url");
            URLConnection con = url.openConnection();

            OutputStream os = con.getOutputStream();

            OutputStream osb = new ObjectOutputStream(os);
            osb.write(src.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void testObjectOutputStream2() {
        try {
            String src = source();

            URL url = new URL("http://some.url");
            URLConnection con = url.openConnection();

            OutputStream os = con.getOutputStream();

            ObjectOutputStream osb = new ObjectOutputStream(os);
            osb.write(src.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void testDataOutputStream1() {
        try {
            String src = source();

            URL url = new URL("http://some.url");
            URLConnection con = url.openConnection();

            OutputStream os = con.getOutputStream();

            OutputStream osb = new DataOutputStream(os);
            osb.write(src.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void testDataOutputStream2() {
        try {
            String src = source();

            URL url = new URL("http://some.url");
            URLConnection con = url.openConnection();

            OutputStream os = con.getOutputStream();

            DataOutputStream osb = new DataOutputStream(os);
            osb.write(src.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void testOutputStreamWriter1() {
        try {
            String src = source();

            URL url = new URL("http://some.url");
            URLConnection con = url.openConnection();

            OutputStream os = con.getOutputStream();

            OutputStreamWriter osWriter = new OutputStreamWriter(os);
            osWriter.write(src);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void testOutputStreamWriter2() {
        try {
            String src = source();

            URL url = new URL("http://some.url");
            URLConnection con = url.openConnection();

            OutputStream os = con.getOutputStream();

            Writer osWriter = new OutputStreamWriter(os);
            osWriter.write(src);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void testPrintWriter1() {
        try {
            String src = source();

            URL url = new URL("http://some.url");
            URLConnection con = url.openConnection();

            OutputStream os = con.getOutputStream();

            PrintWriter writer = new PrintWriter(os);
            writer.write(src);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void testPrintWriter2() {
        try {
            String src = source();

            URL url = new URL("http://some.url");
            URLConnection con = url.openConnection();

            OutputStream os = con.getOutputStream();

            Writer writer = new PrintWriter(os);
            writer.write(src);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void testByteBufferOutput1() {
        try {
            String src = source();

            URL url = new URL("http://some.url");
            URLConnection con = url.openConnection();

            OutputStream os = con.getOutputStream();

            OutputStream bbo = (OutputStream) new ByteBufferOutput(os);
            bbo.write(src.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void testByteBufferOutput2() {
        try {
            String src = source();

            URL url = new URL("http://some.url");
            URLConnection con = url.openConnection();

            OutputStream os = con.getOutputStream();

            ByteBufferOutput bbo = new ByteBufferOutput(os);
            bbo.write(src.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void testByteBufferOutput3() {
        try {
            String src = source();

            URL url = new URL("http://some.url");
            URLConnection con = url.openConnection();

            OutputStream os = con.getOutputStream();

            ByteBufferOutput bbo = new ByteBufferOutput(os);
            bbo.writeString(src);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void testByteBufferOutput4() {
        try {
            String src = source();

            URL url = new URL("http://some.url");
            URLConnection con = url.openConnection();

            OutputStream os = con.getOutputStream();

            ByteBufferOutput bbo = new ByteBufferOutput(os);
            bbo.writeAscii(src);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void testByteBufferOutput5() {
        try {
            String src = source();

            URL url = new URL("http://some.url");
            URLConnection con = url.openConnection();

            OutputStream os = con.getOutputStream();

            OutputStream bbo = new ByteBufferOutput(os);
            BufferedOutputStream bos = new BufferedOutputStream(bbo);
            bos.write(src.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
