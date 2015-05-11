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
        x.add("src/test/one", "first.xml", new File("build.xml"));
        x.add("other", "second.properties", new File("build.properties"));
        x.addDirectory("third");
        File f = new File("test1.xar");
        try {
            x.save(f);
            Xar x2 = new Xar(f);
            Assert.assertEquals(x.getToc(), x2.getToc());
        } finally {
            if (f.exists()) {
                f.delete();
            }
        }
    }

}
