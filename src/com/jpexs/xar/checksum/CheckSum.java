package com.jpexs.xar.checksum;

/**
 *
 * @author JPEXS
 */
public abstract class CheckSum {

    public abstract String getName();

    public abstract int getNum();

    public abstract byte[] checkSum(byte data[]);

    public abstract int checkSumLength();
}
