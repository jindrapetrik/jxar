package com.jpexs.xar.checksum.errorhandlers;

/**
 *
 * @author JPEXS
 */
public class WarningCheckSumErrorHandler implements CheckSumErrorHandler {

    @Override
    public void handleCheckSumError(String path) {
        System.err.println("WARNING: Invalid checksum: \"" + path + "\"");
    }

}
