package com.jpexs.xar.checksum.errorhandlers;

/**
 *
 * @author JPEXS
 */
public class ExceptionCheckSumErrorHandler implements CheckSumErrorHandler {

    @Override
    public void handleCheckSumError(String path) {
        throw new CheckSumException("Invalid checksum: \"" + path + "\"");
    }

}
