package com.jpexs.xar.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.itadaki.bzip2.BZip2InputStream;
import org.itadaki.bzip2.BZip2OutputStream;

/**
 *
 * @author JPEXS
 */
public class BZip2Encoding extends Encoding {

    @Override
    public String getName() {
        return "application/x-" + getSimpleName();
    }

    @Override
    public String getSimpleName() {
        return "bzip";
    }

    @Override
    public OutputStream encodeOutputStream(OutputStream os) {
        try {
            return new BZip2OutputStream(os);
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public InputStream decodeInputStream(InputStream is) {
        return new BZip2InputStream(is, true);
    }

}
