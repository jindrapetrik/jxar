package com.jpexs.xar.encoding;

import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author JPEXS
 */
public abstract class Encoding {

    public abstract String getName();

    public abstract String getSimpleName();

    public abstract OutputStream encodeOutputStream(OutputStream os);

    public abstract InputStream decodeInputStream(InputStream is);
}
