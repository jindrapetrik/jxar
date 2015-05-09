package com.jpexs.xar.checksum;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author JPEXS
 */
public abstract class DigestBasedChecksum extends CheckSum {

    protected String name;
    protected String digestAlgoName;

    public DigestBasedChecksum(String name, String digestAlgoName) {
        this.name = name;
        this.digestAlgoName = digestAlgoName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public byte[] checkSum(byte[] data) {
        try {
            MessageDigest crypt = MessageDigest.getInstance(digestAlgoName);
            crypt.reset();
            crypt.update(data);
            return crypt.digest();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

}
