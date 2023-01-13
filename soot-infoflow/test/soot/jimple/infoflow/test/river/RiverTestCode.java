package soot.jimple.infoflow.test.river;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

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

}
