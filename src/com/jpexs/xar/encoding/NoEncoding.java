package com.jpexs.xar.encoding;

import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author JPEXS
 */
public class NoEncoding extends Encoding {

    @Override
    public String getName() {
        return "application/octet-stream";
    }

    @Override
    public String getSimpleName() {
        return "none";
    }

    @Override
    public OutputStream encodeOutputStream(OutputStream os) {
        return os;
    }

    @Override
    public InputStream decodeInputStream(InputStream is) {
        return is;
    }

}
