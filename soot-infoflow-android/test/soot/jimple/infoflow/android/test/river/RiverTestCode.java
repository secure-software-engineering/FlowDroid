package soot.jimple.infoflow.android.test.river;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;

public class RiverTestCode {
    public static String source() {
        return "secret";
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

    public static String userInput() {
        return "untrusted";
    }

    public static byte[] encrypt(String key, String message) {
        return new byte[42];
    }

//    public void riverTest3() {
//        String key = userInput();
//        byte[] encrypted = encrypt(key, "my super secret message");
//        try {
//            URL url = new URL("http://some.url");
//            URLConnection con = url.openConnection();
//
//            OutputStream os = con.getOutputStream();
//            os.write(encrypted);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void riverTest4() {
//        String key = userInput();
//        byte[] encrypted = encrypt(key, "my super secret message");
//        try {
//            OutputStream os = new ByteArrayOutputStream();
//            os.write(encrypted);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
}
