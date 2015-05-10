package com.jpexs.commandline;

/**
 *
 * @author JPEXS
 */
public interface OptionActionListener {

    public void handleOption(String option, Object[] values, String[] valuesStr);
}
