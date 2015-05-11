package com.jpexs.xar.nodes;

/**
 *
 * @author JPEXS
 */
public class SymLinkNode extends Node {

    public String link;
    public String targetType;

    public SymLinkNode(String name, String targetType, String link) {
        this(name, targetType, link, -1, -1, -1, null, null, -1, null, -1);
    }

    public SymLinkNode(String name, String targetType, String link, long ctime, long mtime, long atime, String mode, String group, int gid, String user, int uid) {
        super(-1, name, "symlink", ctime, mtime, atime, mode, group, gid, user, uid);
        this.link = link;
        this.targetType = targetType;
    }

    @Override
    public String toString() {
        String ret = "<file id=\"" + id + "\">"
                + "<link type=\"" + targetType + "\">" + link + "</link>"
                + basicInfo();
        ret += "</file>";
        return ret;
    }

}
