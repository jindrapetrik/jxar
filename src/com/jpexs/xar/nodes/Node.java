package com.jpexs.xar.nodes;

import com.jpexs.xar.Xar;
import com.jpexs.xar.checksum.CheckSum;
import com.jpexs.xar.encoding.Encoding;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author JPEXS
 */
public abstract class Node {

    public int id;
    public String name;
    public Map<String, Node> subnodes = new TreeMap<>();
    public long ctime;
    public long mtime;
    public long atime;
    public int gid;
    public int uid;
    public String mode;
    public String user;
    public String group;
    public String type;

    public Node(int id, String name, String type) {
        this.id = id;
        this.ctime = -1;
        this.mtime = -1;
        this.atime = -1;
        this.mode = null;
        this.group = null;
        this.user = null;
        this.gid = -1;
        this.uid = -1;
        this.name = name;
        this.type = type;
    }

    public Node(int id, String name, String type, long ctime, long mtime, long atime, String mode, String group, int gid, String user, int uid) {
        this.id = id;
        this.name = name;
        this.user = user;
        this.group = group;
        this.gid = gid;
        this.uid = uid;
        this.ctime = ctime;
        this.mtime = mtime;
        this.atime = atime;
        this.mode = mode;
        this.type = type;
    }

    protected String basicInfo() {
        return ((ctime > -1) ? "<ctime>" + Xar.DATE_FORMAT.format(new Date(ctime)) + "</ctime>" : "")
                + ((mtime > -1) ? "<mtime>" + Xar.DATE_FORMAT.format(new Date(mtime)) + "</mtime>" : "")
                + ((atime > -1) ? "<atime>" + Xar.DATE_FORMAT.format(new Date(atime)) + "</atime>" : "")
                + ((group != null) ? "<group>" + group + "</group>" : "")
                + ((gid > -1) ? "<gid>" + gid + "</gid>" : "")
                + ((user != null) ? "<user>" + user + "</user>" : "")
                + ((uid > -1) ? "<uid>" + uid + "</uid>" : "")
                + ((mode != null) ? "<mode>" + mode + "</mode>" : "")
                + "<type>" + type + "</type>"
                + "<name>" + name + "</name>";
    }

    public static Node getInstance(String name, File file, CheckSum checksum, Encoding encoding) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        long ctime = attrs.creationTime().toMillis();
        long mtime = attrs.lastModifiedTime().toMillis();
        long atime = attrs.lastAccessTime().toMillis();
        if (attrs.isSymbolicLink()) {
            String linkTarget = Files.readSymbolicLink(file.toPath()).toFile().getAbsolutePath();
            return new SymLinkNode(name, file.isDirectory() ? "directory" : "file", linkTarget, ctime, mtime, atime, null, null, -1, null, -1);
        } else if (file.isDirectory()) {
            return new DirectoryNode(name, ctime, mtime, atime, null, null, -1, null, -1);
        } else {
            return new FileNode(name, new FileInputStream(file), encoding, checksum, ctime, mtime, atime, null, null, -1, null, -1);
        }
    }
}
