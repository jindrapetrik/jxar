package com.jpexs.commandline;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 *
 * @author JPEXS
 */
public class CommandlineTest {

    Commandline ap;

    private static final InvalidArgumentsListener EXCEPTION_LISTENER = (String message) -> {
        throw new IllegalArgumentException(message);
    };

    public static class ObjBox {

        public Object[] vals;

        public ObjBox(Object... vals) {
            this.vals = vals;
        }

        @Override
        public String toString() {
            String ret = "[";
            for (int i = 0; i < vals.length; i++) {
                if (i > 0) {
                    ret += ", ";
                }
                if (vals[i] instanceof String) {
                    ret += "\"" + vals[i] + "\"";
                } else {
                    ret += vals[i];
                }
            }
            ret += "]";
            return ret;
        }

    }

    @BeforeClass
    public void before() {
        ap = new Commandline();
        ap.addOption("c", "", "");
        ap.addOption("d", "", "");
        ap.addOption("s", "", "s");
        ap.addOption("i", "", "i");
        ap.addOption("ss", "", "ss");
    }

    @DataProvider(name = "provideCorrectOptions")
    public Object[][] provideCorrectOptions() {
        return new Object[][]{
            new Object[]{"-c"},
            new Object[]{"-c -d"},
            new Object[]{"-cd"},
            new Object[]{"-s str"},
            new Object[]{"-i 59"},
            new Object[]{"--ss str1 str2"},
            new Object[]{"-cd -i 49 -s hi --ss str1 str2"}
        };
    }

    @DataProvider(name = "provideIncorrectOptions")
    public Object[][] provideIncorrectOptions() {
        return new Object[][]{
            new Object[]{"-x"},
            new Object[]{"--invalid"},
            new Object[]{"-cs"},
            new Object[]{"-s"},
            new Object[]{"-i hi"},
            new Object[]{"--ss str1"},
            new Object[]{"fail"}
        };
    }

    @Test(dataProvider = "provideCorrectOptions")
    public void testCorrectOptions(String argsStr) {
        ap.parseArgs(argsStr.split(" "), EXCEPTION_LISTENER);
    }

    @Test(dataProvider = "provideIncorrectOptions")
    public void testIncorrectOptions(String argsStr) {
        try {
            ap.parseArgs(argsStr.split(" "), EXCEPTION_LISTENER);
            Assert.fail("No exception thrown");
        } catch (IllegalArgumentException ex) {
            //okay
        }
    }

    @DataProvider(name = "provideOptionValues")
    public Object[][] provideOptionValues() {
        return new Object[][]{
            new Object[]{"-c", "c", new ObjBox()},
            new Object[]{"-s str", "s", new ObjBox("str")},
            new Object[]{"-i 59", "i", new ObjBox(59)},
            new Object[]{"--ss str1 str2", "ss", new ObjBox("str1", "str2")},};
    }

    @Test(dataProvider = "provideOptionValues")
    public void testOptionValues(String argsStr, String option, ObjBox expectedValues) {
        ap.parseArgs(argsStr.split(" "), EXCEPTION_LISTENER);
        Object[] actualValues = ap.getOptionValues(option);
        Assert.assertEquals(actualValues, expectedValues.vals);
    }

    @Test
    public void testArguments() {
        Commandline ap2 = new Commandline();
        ap2.addOption("a", "", "s");
        ap2.setArgTypes("sssi");
        try {
            ap2.parseArgs("-a x a b c 59 d".split(" "), EXCEPTION_LISTENER);
            Assert.fail("No exception thrown");
        } catch (IllegalArgumentException ex) {
            //okay
        }
        ap2.parseArgs("-a x a b c 59".split(" "), EXCEPTION_LISTENER);
        Assert.assertEquals(ap2.getArgumentValues(), new Object[]{"a", "b", "c", 59});
    }

    @Test
    public void testGroups() {
        Commandline ap2 = new Commandline();
        ap2.addOption("a");
        ap2.addOption("b");
        ap2.addOption("c");
        ap2.disableOptionsTogether("a", "b");
        ap2.parseArgs("-a -c".split(" "), EXCEPTION_LISTENER);
        try {
            ap2.parseArgs("-a -b".split(" "), EXCEPTION_LISTENER);
            Assert.fail("No exception thrown");
        } catch (IllegalArgumentException iex) {
            //okay
        }
    }

    @Test
    public void testArgNames() {
        Commandline ap2 = new Commandline();
        ap2.addOption("f", "Display file", "s<file>");
        ap2.parseArgs("-f file1".split(" "), EXCEPTION_LISTENER);
    }
}
