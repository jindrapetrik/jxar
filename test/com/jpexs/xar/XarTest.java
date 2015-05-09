package com.jpexs.xar;

import java.io.File;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author JPEXS
 */
public class XarTest {

    @Test
    public void testCompressDecompress() throws IOException {
        Xar x = new Xar();
        x.addFile("src/test/one/first.xml", new File("build.xml"));
        x.addFile("other/second.properties", new File("build.properties"));
        x.addDirectory("third");
        File f = new File("test1.xar");
        try {
            x.save(f);
        } catch (Exception ex) {
            if (f.exists()) {
                f.delete();
            }
            throw ex;
        }
        Xar x2 = new Xar(f);

        Assert.assertEquals(x.getToc(), x2.getToc());
    }
}
