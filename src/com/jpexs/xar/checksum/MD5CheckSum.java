package com.jpexs.xar.checksum;

import com.jpexs.xar.Xar;

/**
 *
 * @author JPEXS
 */
public class MD5CheckSum extends DigestBasedChecksum {

    public MD5CheckSum() {
        super("md5", "MD5");
    }

    @Override
    public int getNum() {
        return Xar.CKSUM_ALG_NUM_MD5;
    }

    @Override
    public int checkSumLength() {
        return 16;
    }

}
