package hudson.cli;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * {@link OutputStream} that records what's being written but also where the boundaries are.
 *
 * Used to spy traffic of {@link InputStream} or {@link OutputStream}
 *
 * @author Kohsuke Kawaguchi
 */
class TraceOutputStream extends OutputStream {
    private PrintWriter s;

    public static OutputStream wrap(OutputStream base, File f) throws IOException {
        return new TeeOutputStream(base,new TraceOutputStream(f));
    }

    public static InputStream wrap(InputStream base, File f) throws IOException {
        return new TeeInputStream(base,new TraceOutputStream(f));
    }

    public TraceOutputStream(File f) throws IOException {
        s = new PrintWriter(new FileWriter(f),true);
    }

    @Override
    public void write(int b) throws IOException {
        s.println(toHex(new StringBuilder(),(byte)b));
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        s.println(toHex(b, off, len));
    }

    @Override
    public void flush() throws IOException {
        s.flush();
    }

    @Override
    public void close() throws IOException {
        s.close();
    }

    private static final String CODE = "0123456789abcdef";

    static String toHex(byte[] buf, int start, int len) {
        StringBuilder r = new StringBuilder(len*2);
        for (int i=0; i<len; i++) {
            byte b = buf[start+i];
            toHex(r, b);
        }
        return r.toString();
    }

    static StringBuilder toHex(StringBuilder r, byte b) {
        r.append(CODE.charAt((b>>4)&15));
        r.append(CODE.charAt(b&15));
        return r;
    }
}
