package com.jpexs.xar.nodes;

import com.jpexs.xar.Xar;
import com.jpexs.xar.checksum.CheckSum;
import com.jpexs.xar.encoding.Encoding;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author JPEXS
 */
public class FileNode extends Node {

    public byte[] data;
    public byte[] compressedData;
    public int length;
    public int size;
    public String encodingStyle;
    public long offset;
    public String archivedChecksum = "";
    public String extractedChecksum = "";
    public String cksum_alg;

    @Override
    public String toString() {

        return "<file id=\"" + id + "\">"
                + "<data>"
                + "<length>" + length + "</length>"
                + "<encoding style=\"" + encodingStyle + "\" />"
                + "<offset>" + offset + "</offset>"
                + "<size>" + size + "</size>"
                + "<archived-checksum>" + archivedChecksum + "</archived-checksum>"
                + "<extracted-checksum>" + extractedChecksum + "</extracted-checksum>"
                + "</data>"
                + basicInfo()
                + "</file>";
    }

    public FileNode(String name, InputStream is, Encoding encoding, CheckSum checksum, long ctime, long mtime, long atime, String mode, String group, int gid, String user, int uid) throws IOException {
        super(-1, name, "file", ctime, mtime, atime, mode, group, gid, user, uid);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int cnt;
        while ((cnt = is.read(buf)) > 0) {
            baos.write(buf, 0, cnt);
        }
        is.close();
        init(baos.toByteArray(), null, encoding, checksum, 0);
    }

    public FileNode(String name, byte[] data, byte compressedData[], Encoding encoding, CheckSum checksum, long offset) {
        this(name, data, compressedData, encoding, checksum, offset, -1, -1, -1, null, null, -1, null, -1);
    }

    private void init(byte[] data, byte compressedData[], Encoding encoding, CheckSum checksum, long offset) {
        if (data == null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream is = new ByteArrayInputStream(compressedData);
            try {
                is = encoding.decodeInputStream(is);
                int cnt;
                byte buf[] = new byte[1024];
                while ((cnt = is.read(buf)) > 0) {
                    baos.write(buf, 0, cnt);
                }
            } catch (IOException ex) {
                //ignore
            }
            data = baos.toByteArray();
        }
        this.data = data;
        if (compressedData == null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStream os = baos;
            try {
                os = encoding.encodeOutputStream(os);
                os.write(data);
                os.close();
            } catch (IOException ex) {
                //ignore
            }
            compressedData = baos.toByteArray();
        }
        this.encodingStyle = encoding.getName();
        this.compressedData = compressedData;

        this.cksum_alg = checksum.getName();
        this.offset = offset;
        length = compressedData.length;
        size = data.length;
        archivedChecksum = Xar.byteToHex(checksum.checkSum(compressedData));
        extractedChecksum = Xar.byteToHex(checksum.checkSum(data));
    }

    public FileNode(String name, byte[] data, byte compressedData[], Encoding encoding, CheckSum checksum, long offset, long ctime, long mtime, long atime, String mode, String group, int gid, String user, int uid) {
        super(-1, name, "file", ctime, mtime, atime, mode, group, gid, user, uid);
        init(data, compressedData, encoding, checksum, offset);
    }

}
