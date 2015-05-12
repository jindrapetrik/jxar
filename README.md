# jxar
Java port of eXtensible ARchiver (XAR)

Original source: https://code.google.com/p/xar/

## Commandline usage
Is is similar to original xar, but some options are not yet implemented
```
Usage: java -jar jxar.jar -[cxt] -f <archive> [<file>] ...
	--help	Prints usage
	-c	Creates an archive
	-x	Extracts an archive
	-t	Lists an archive
	-f <archive>	Specifies an archive to operate on [REQUIRED!]
	--toc-cksum <algorithm>	Specifies the hashing algorithm to use for xml header verification.
			Valid values: none, sha1, and md5
			Default: sha1
	--dump-toc <filename>	Has xar dump the xml header into the specified file.
	--compression <type>	Specifies the compression type to use.
			Valid values: none, gzip, bzip2
			Default: gzip
	--version	Print xar's version number
```

## Java usage
### Reading
```java
import com.jpexs.xar.Xar;
...
Xar x = new Xar(new File("archive.xar"));
x.extract(new File("out/")); //extract all
System.out.println(x.getToc()); //print TOC
String[] dirs = x.listDirs(); //list all directories
String[] files = x.listFiles(); //list all files
byte[] data = x.getFileData("path/in/archive/test.txt"); //Get uncompressed data of file
```

### Writing
```java
import com.jpexs.xar.Xar;
import com.jpexs.xar.nodes.*;
...
Xar x = new Xar(); //Use constructor parameter to change compression and/or checksum type
x.add("dir1/first","file.txt",new File("localfile.txt"));  //Add text file
x.add("dir1/first","file2.png",new File("localimage.png")); //Second file
x.addDirectory("dir2/mydir"); //Empty directory

//Add file and modify permissions:
Node node = x.add("dir2/special","script.sh",new File("script.sh")); 
node.mode = 0777;
node.group = "root";
node.uid = 59;

//Add symlink
x.add("dir3",new SymLinkNode("mylink","directory","../dir1"));

//Finally:
x.save(new File("archive.xar"));  //Save to file

```
## Ant usage
XAR archives creation only, using xar task:
```ant
    <taskdef name="xar" classname="com.jpexs.xar.ant.XarTask" classpath="somepath/jxar.jar" />
    <xar destfile="test.xar">
      <tarfileset dir="somedir" />
    </xar>
```

##Bzip2 compression
To enable Bzip2 compression, add JBZip2 to your class path. 
You can download the library here:
https://code.google.com/p/jbzip2/

##LZMA compression
LZMA compression is currently unsupported. Maybe in the future.

## What else is missing
Symbolic links and other node types than directory/file.
