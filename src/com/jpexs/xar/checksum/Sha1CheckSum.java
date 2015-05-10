package com.jpexs.xar.checksum;

import com.jpexs.xar.Xar;

/**
 *
 * @author JPEXS
 */
public class Sha1CheckSum extends DigestBasedChecksum {

    public Sha1CheckSum() {
        super("sha1", "SHA-1");
    }

    @Override
    public int getNum() {
        return Xar.CKSUM_ALG_NUM_SHA1;
    }

    @Override
    public int checkSumLength() {
        return 20;
    }

}
