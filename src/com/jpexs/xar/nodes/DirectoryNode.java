package com.jpexs.xar.nodes;

/**
 *
 * @author JPEXS
 */
public class DirectoryNode extends Node {

    public DirectoryNode(String name) {
        super(-1, name, "directory");
    }

    public DirectoryNode(String name, long ctime, long mtime, long atime, int mode, String group, int gid, String user, int uid) {
        super(-1, name, "directory", ctime, mtime, atime, mode, group, gid, user, uid);
    }

    @Override
    public String toString() {
        String ret = "<file id=\"" + id + "\">"
                + basicInfo();
        for (String k : subnodes.keySet()) {
            ret += subnodes.get(k);
        }
        ret += "</file>";
        return ret;
    }

}
