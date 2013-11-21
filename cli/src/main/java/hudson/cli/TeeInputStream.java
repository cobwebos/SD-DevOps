package hudson.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Kohsuke Kawaguchi
 */
class TeeInputStream extends InputStream {
    private final InputStream core;
    private final OutputStream side;

    TeeInputStream(InputStream core, OutputStream side) {
        this.side = side;
        this.core = core;
    }

    @Override
    public int read() throws IOException {
        int b = core.read();
        if (b<0)    side.close();
        else        side.write(b);
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int r = core.read(b,off,len);
        if (r<0)    side.close();
        else        side.write(b,off,r);
        return r;
    }

    @Override
    public long skip(long n) throws IOException {
        // this is incorrect implementation but for our purpose this would do
        return core.skip(n);
    }

    @Override
    public void close() throws IOException {
        core.close();
        side.close();
    }

    /**
     * Mark not supported to avoid getting the side writer confused.
     */
    @Override
    public boolean markSupported() {
        return false;
    }
}
