package com.jpexs.xar.encoding;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 *
 * @author JPEXS
 */
public class GZipEncoding extends Encoding {

    @Override
    public String getName() {
        return "application/x-" + getSimpleName();
    }

    @Override
    public String getSimpleName() {
        return "gzip";
    }

    @Override
    public OutputStream encodeOutputStream(OutputStream os) {
        return new DeflaterOutputStream(os, false);
    }

    @Override
    public InputStream decodeInputStream(InputStream is) {
        return new InflaterInputStream(is);
    }

}
