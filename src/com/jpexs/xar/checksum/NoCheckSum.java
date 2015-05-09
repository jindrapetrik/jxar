package com.jpexs.xar.checksum;

import com.jpexs.xar.Xar;

/**
 *
 * @author JPEXS
 */
public class NoCheckSum extends CheckSum {

    @Override
    public String getName() {
        return "none";
    }

    @Override
    public int getNum() {
        return Xar.CKSUM_ALG_NUM_NONE;
    }

    @Override
    public byte[] checkSum(byte[] data) {
        return new byte[0];
    }

    @Override
    public int checkSumLength() {
        return 0;
    }
}
