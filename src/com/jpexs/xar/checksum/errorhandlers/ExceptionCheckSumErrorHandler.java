package com.jpexs.xar.checksum.errorhandlers;

/**
 *
 * @author JPEXS
 */
public class ExceptionCheckSumErrorHandler implements CheckSumErrorHandler {

    @Override
    public void handleCheckSumError(String path) {
        throw new RuntimeException("Invalid checksum: \"" + path + "\"");
    }

}
