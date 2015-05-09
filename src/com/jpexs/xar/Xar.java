package com.jpexs.xar;

import com.jpexs.xar.checksum.CheckSum;
import com.jpexs.xar.checksum.errorhandlers.CheckSumErrorHandler;
import com.jpexs.xar.checksum.MD5CheckSum;
import com.jpexs.xar.checksum.NoCheckSum;
import com.jpexs.xar.checksum.SHA1CheckSum;
import com.jpexs.xar.checksum.errorhandlers.WarningCheckSumErrorHandler;
import com.jpexs.xar.encoding.BZip2Encoding;
import com.jpexs.xar.encoding.Encoding;
import com.jpexs.xar.encoding.GZipEncoding;
import com.jpexs.xar.encoding.NoEncoding;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XAR - eXtensible ARchiver
 *
 * @author JPEXS
 */
public class Xar {

    public static final byte[] MAGIC = new byte[]{0x78, 0x61, 0x72, 0x21}; //"xar!"
    public static final int VERSION = 1;
    public static final int HEADER_SIZE = 4 + 2 + 2 + 8 + 8 + 4;

    public static final int CKSUM_ALG_NUM_NONE = 0;
    public static final int CKSUM_ALG_NUM_SHA1 = 1;
    public static final int CKSUM_ALG_NUM_MD5 = 2;
    public static final int CKSUM_ALG_NUM_OTHER = 3;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private long currentOffset = 0;

    private Map<String, Node> allNodes = new TreeMap<>();
    private List<FileNode> files = new ArrayList<>();

    private int last_file_id = 0;
    private CheckSum checksum;
    private Encoding encoding;
    private String user = "root";
    private String group = "root";
    private int gid = 80;
    private int uid = 0;
    private String mode = "0755";
    private long creationTime;

    private CheckSumErrorHandler checkSumErrorHandler;

    private final static Map<String, CheckSum> supportedChecksums = new HashMap<>();

    private final static Map<String, Encoding> supportedEncodings = new HashMap<>();

    static {
        CheckSum c;
        c = new NoCheckSum();
        supportedChecksums.put(c.getName(), c);
        c = new MD5CheckSum();
        supportedChecksums.put(c.getName(), c);
        c = new SHA1CheckSum();
        supportedChecksums.put(c.getName(), c);

        Encoding e;
        e = new GZipEncoding();
        supportedEncodings.put(e.getSimpleName(), e);
        try {
            Class.forName("org.itadaki.bzip2.BZip2InputStream");
            e = new BZip2Encoding();
            supportedEncodings.put(e.getSimpleName(), e);
        } catch (ClassNotFoundException ex) {
            //my class isn't there!
        }
        e = new NoEncoding();
        supportedEncodings.put(e.getSimpleName(), e);

    }

    public static void addEncodingType(Encoding e) {
        supportedEncodings.put(e.getSimpleName(), e);
    }

    public static void addCheckSumType(CheckSum c) {
        supportedChecksums.put(c.getName(), c);
    }

    public static Set<String> getSupportedEncodings() {
        return supportedEncodings.keySet();
    }

    public static Set<String> getSupportedCheckSums() {
        return supportedChecksums.keySet();
    }

    public void setGid(int gid) {
        this.gid = gid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    private static String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    private static class TocHandler extends DefaultHandler {

        protected Xar xar;
        private String textData = "";
        long ctime = 0;
        long mtime = 0;
        long atime = 0;
        int gid = 0;
        int uid = 0;
        String group = "";
        String user = "";
        String mode = "";
        String lastType = "";
        String lastName = "";
        int id = 0;
        String encodingStyle = "";
        int length;
        int size;
        boolean inData;
        boolean inCheckSum = false;
        long offset;
        String archivedCheckSum = "";
        String extractedCheckSum = "";
        String archivedCheckSumStyle = "";
        String extractedCheckSumStyle = "";
        String checkSumStyle = "";
        CheckSum checksum = supportedChecksums.get("none");
        long heap_offset;

        //List<DirectoryNode> path = new ArrayList<>();
        protected File file;
        Stack<String> path = new Stack<>();
        Stack<String> typeStack = new Stack<>();
        Stack<String> nameStack = new Stack<>();
        protected RandomAccessFile raf;

        protected String getPathString() {
            String ret = "";
            for (String d : path) {
                if (!ret.equals("")) {
                    ret += "/";
                }
                ret += d;
            }
            return ret;
        }

        public TocHandler(File file, Xar xar, long heap_offset) throws IOException {
            this.xar = xar;
            this.heap_offset = heap_offset;
            raf = new RandomAccessFile(file, "r");

        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            switch (localName) {
                case "checksum":
                    inCheckSum = true;
                    checkSumStyle = attributes.getValue("style");
                    if (checkSumStyle.equals("")) {
                        checksum = supportedChecksums.get("none");
                    } else {
                        for (CheckSum c : supportedChecksums.values()) {
                            if (c.getName().toLowerCase().equals(checkSumStyle.toLowerCase())) {
                                checksum = c;
                                break;
                            }
                        }
                    }
                    xar.checksum = checksum;
                    break;
                case "archived-checksum":
                    archivedCheckSumStyle = attributes.getValue("style");
                    if (archivedCheckSumStyle == null) {
                        archivedCheckSumStyle = "SHA1";
                    }
                    break;
                case "extracted-checksum":
                    extractedCheckSumStyle = attributes.getValue("style");
                    if (extractedCheckSumStyle == null) {
                        extractedCheckSumStyle = "SHA1";
                    }
                    break;
                case "data":
                    inData = true;
                    break;
                case "file":

                    ctime = 0;
                    mtime = 0;
                    atime = 0;
                    lastType = "";
                    lastName = "";
                    encodingStyle = "";
                    group = "";
                    user = "";
                    uid = 0;
                    gid = 0;
                    length = 0;
                    size = 0;
                    offset = 0;
                    archivedCheckSum = "";
                    extractedCheckSum = "";
                    id = Integer.parseInt(attributes.getValue("id"));
                    if (id > xar.last_file_id) {
                        xar.last_file_id = id;
                    }
                    break;

                case "encoding":
                    encodingStyle = attributes.getValue("style");
                    break;
            }
            textData = "";
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            textData += new String(ch, start, length);
        }

        private void addDir() {
            String baseDir = getPathString();
            path.push(nameStack.peek());
            DirectoryNode dnode = new DirectoryNode(id, nameStack.peek());
            xar.allNodes.get(baseDir).subnodes.put(nameStack.peek(), dnode);
            xar.allNodes.put(getPathString(), dnode);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (inCheckSum) {

            }
            switch (localName) {
                case "creation-time": {
                    try {
                        xar.creationTime = DATE_FORMAT.parse(textData).getTime();
                    } catch (ParseException ex) {
                        xar.creationTime = 0L;
                    }
                }
                break;
                case "checksum":
                    inCheckSum = false;
                    break;
                case "data":
                    inData = false;
                    break;

                case "file":
                    Node node = null;
                    String type = typeStack.peek();
                    String name = nameStack.peek();

                    if (type.equals("directory")) {
                        path.pop();
                    } else if (type.equals("file")) {

                        Encoding encoding = null;
                        if (encodingStyle.equals("")) {
                            encoding = supportedEncodings.get("none");
                        } else {
                            for (Encoding e : supportedEncodings.values()) {
                                if (e.getName().equals(encodingStyle)) {
                                    encoding = e;
                                    break;
                                }
                            }
                        }
                        if (encoding == null) {
                            throw new RuntimeException("Unknown encoding: " + encodingStyle);
                        }

                        //Assuming extracted is same as archived
                        if (extractedCheckSumStyle.equals("")) {
                            checksum = supportedChecksums.get("none");
                        } else {
                            for (CheckSum c : supportedChecksums.values()) {
                                if (c.getName().toLowerCase().equals(extractedCheckSumStyle.toLowerCase())) {
                                    checksum = c;
                                    break;
                                }
                            }
                        }

                        if (checksum == null) {
                            throw new RuntimeException("Unknown checksum: " + checkSumStyle);
                        }
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte compressedData[] = new byte[length];
                        try {
                            raf.seek(heap_offset + offset);
                            raf.readFully(compressedData);
                        } catch (IOException ex) {
                            //ignore
                        }
                        FileNode fnode = new FileNode(id, name, null, compressedData, encoding, ctime, mtime, atime, mode, group, gid, user, uid, checksum, offset);
                        if (!fnode.archivedChecksum.equals(archivedCheckSum) || !fnode.extractedChecksum.equals(extractedCheckSum)) {
                            path.push(name);
                            if (xar.checkSumErrorHandler != null) {
                                xar.checkSumErrorHandler.handleCheckSumError(getPathString());
                            }
                            path.pop();
                        } else {
                            String baseDir = getPathString();
                            path.push(name);
                            xar.allNodes.put(getPathString(), fnode);
                            path.pop();
                            xar.allNodes.get(baseDir).subnodes.put(name, fnode);
                            xar.files.add(fnode);
                            if (xar.currentOffset < offset + length) {
                                xar.currentOffset = offset + length;
                            }
                        }
                    } else {
                        //unknown node type
                    }
                    typeStack.pop();
                    nameStack.pop();
                    break;
                case "mode":
                    mode = textData;
                    break;
                case "name":
                    nameStack.push(lastName = textData);
                    if (lastType.equals("directory")) {
                        addDir();
                    }
                    break;
                case "type":
                    typeStack.push(lastType = textData);
                    if (lastType.equals("directory") && !lastName.equals("")) {
                        addDir();
                    }
                    break;
                case "user":
                    user = textData;
                    break;
                case "group":
                    group = textData;
                    break;
                case "uid":
                    uid = Integer.parseInt(textData);
                    break;
                case "gid":
                    gid = Integer.parseInt(textData);
                    break;
                case "ctime":
                    try {
                        ctime = DATE_FORMAT.parse(textData).getTime();
                    } catch (ParseException ex) {
                        //ignore
                    }
                    break;
                case "mtime":
                    try {
                        mtime = DATE_FORMAT.parse(textData).getTime();
                    } catch (ParseException ex) {
                        //ignore
                    }
                    break;
                case "atime":
                    try {
                        atime = DATE_FORMAT.parse(textData).getTime();
                    } catch (ParseException ex) {
                        //ignore
                    }
                    break;
            }
            if (inData) {
                switch (localName) {
                    case "length":
                        length = Integer.parseInt(textData);
                        break;
                    case "size":
                        size = Integer.parseInt(textData);
                        break;
                    case "offset":
                        offset = Integer.parseInt(textData);
                        break;
                    case "archived-checksum":
                        archivedCheckSum = textData;

                        break;
                    case "extracted-checksum":
                        extractedCheckSum = textData;
                        break;
                }
            }
            textData = "";
        }

    }

    private abstract static class Node {

        public int id;
        public String name;
        Map<String, Node> subnodes = new TreeMap<>();

        public Node(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static class DirectoryNode extends Node {

        public DirectoryNode(int id, String name) {
            super(id, name);
        }

        @Override
        public String toString() {
            String ret = "<file id=\"" + id + "\">"
                    + "<type>directory</type>"
                    + "<name>" + name + "</name>";
            for (String k : subnodes.keySet()) {
                ret += subnodes.get(k);
            }
            ret += "</file>";
            return ret;
        }

    }

    private static class FileNode extends Node {

        public byte[] data;
        public byte[] compressedData;
        public int length;
        public int size;
        public String encodingStyle;
        public long offset;
        public String archivedChecksum = "";
        public String extractedChecksum = "";
        public String cksum_alg;
        public long ctime;
        public long mtime;
        public long atime;
        public int gid;
        public int uid;
        public String mode;
        public String user;
        public String group;

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
                    + "<ctime>" + DATE_FORMAT.format(new Date(ctime)) + "</ctime>"
                    + "<mtime>" + DATE_FORMAT.format(new Date(mtime)) + "</mtime>"
                    + "<atime>" + DATE_FORMAT.format(new Date(atime)) + "</atime>"
                    + "<group>" + group + "</group>"
                    + "<gid>" + gid + "</gid>"
                    + "<user>" + user + "</user>"
                    + "<uid>" + uid + "</uid>"
                    + "<mode>" + mode + "</mode>"
                    + "<type>file</type>"
                    + "<name>" + name + "</name>"
                    + "</file>";
        }

        public FileNode(int id, String name, byte[] data, byte compressedData[], Encoding encoding, long ctime, long mtime, long atime, String mode, String group, int gid, String user, int uid, CheckSum checksum, long offset) {
            super(id, name);
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

            this.uid = uid;
            this.gid = gid;
            this.user = user;
            this.group = group;
            this.mode = mode;
            this.cksum_alg = checksum.getName();
            this.offset = offset;
            this.ctime = ctime;
            this.atime = atime;
            this.mtime = mtime;
            length = compressedData.length;
            size = data.length;
            archivedChecksum = byteToHex(checksum.checkSum(compressedData));
            extractedChecksum = byteToHex(checksum.checkSum(data));
        }

    }

    public Xar() {
        this("gzip");
    }

    public Xar(String enc_alg) {
        this(enc_alg, "sha1");
    }

    public Xar(String enc_alg, String cksum_alg) {

        if (cksum_alg == null || cksum_alg.equals("")) {
            cksum_alg = "none";
        }

        if (!supportedEncodings.containsKey(enc_alg)) {
            throw new RuntimeException("Uknown encoding type: " + enc_alg);
        }

        if (!supportedChecksums.containsKey(cksum_alg)) {
            throw new RuntimeException("Uknown checksum type: " + cksum_alg);
        }

        this.creationTime = new Date().getTime();
        this.encoding = supportedEncodings.get(enc_alg);
        this.checksum = supportedChecksums.get(cksum_alg);

        currentOffset = checksum.checkSumLength();
        putRoot();
    }

    private void putRoot() {
        allNodes.put("", new DirectoryNode(last_file_id, ""));
    }

    public Xar(File file) throws IOException {
        this(file, new WarningCheckSumErrorHandler());
    }

    public Xar(File file, CheckSumErrorHandler checkSumErrorHandler) throws IOException {
        this.checkSumErrorHandler = checkSumErrorHandler;
        int header_size = 0;
        long toc_length_compressed = 0;
        String toc = "";
        try (FileInputStream fis = new FileInputStream(file)) {
            DataInputStream dis = new DataInputStream(fis);
            byte[] magic = new byte[MAGIC.length];
            dis.readFully(magic);
            if (!new String(magic).equals(new String(MAGIC))) {
                throw new IOException("No XAR file");
            }
            header_size = dis.readUnsignedShort();
            int version = dis.readUnsignedShort();
            if (version != VERSION) {
                throw new IOException("Unknown XAR version: " + version);
            }
            toc_length_compressed = dis.readLong();
            //long toc_length_uncompressed = ;
            dis.readLong();
            int cksum_alg_n = dis.readInt();
            int current_header_size = 4 + 2 + 2 + 8 + 8 + 4;
            if (cksum_alg_n == CKSUM_ALG_NUM_OTHER) {
                ByteArrayOutputStream chksum_baos = new ByteArrayOutputStream();
                int c;
                while ((c = dis.read()) > 0) {
                    chksum_baos.write(c);
                    current_header_size++;
                }
                current_header_size++; //termination zero                                                
            }
            while (current_header_size < header_size) {
                dis.read();
                current_header_size++;
            }
            byte[] toc_compressed = new byte[(int) toc_length_compressed];
            dis.readFully(toc_compressed);
            InflaterInputStream iis = new InflaterInputStream(new ByteArrayInputStream(toc_compressed));
            ByteArrayOutputStream toc_baos = new ByteArrayOutputStream();
            int cnt;
            byte buf[] = new byte[1024];
            while ((cnt = iis.read(buf)) > 0) {
                toc_baos.write(buf, 0, cnt);
            }
            byte[] toc_bytes = toc_baos.toByteArray();
            toc = new String(toc_bytes, "UTF-8");
        }

        putRoot();
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            SAXParser saxParser = spf.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            xmlReader.setContentHandler(new TocHandler(file, this, header_size + toc_length_compressed));
            xmlReader.parse(new InputSource(new StringReader(toc)));
        } catch (ParserConfigurationException | SAXException ex) {

        }
    }

    public void addFile(String name, String data, long ctime, long mtime, long atime, String mode, String group, int gid, String user, int uid) {
        try {
            addFile(name, data.getBytes("UTF-8"), ctime, mtime, atime, mode, group, gid, user, uid);
        } catch (UnsupportedEncodingException ex) {
            //ignored
        }
    }

    public void addDirectory(String name) {
        if (name.equals("")) {
            return;
        }
        name = name.replace("\\", "/");
        if (name.startsWith("/")) {
            name = name.substring(1);
        }

        String baseName = name.contains("/") ? name.substring(name.lastIndexOf("/") + 1) : name;
        String baseDir = name.contains("/") ? name.substring(0, name.lastIndexOf("/")) : "";

        addDirectory(baseDir);
        if (!allNodes.get(baseDir).subnodes.containsKey(baseName)) {
            last_file_id++;
            DirectoryNode dnode = new DirectoryNode(last_file_id, baseName);
            allNodes.get(baseDir).subnodes.put(baseName, dnode);
            allNodes.put(name, dnode);
        }

    }

    public void addFile(String name, byte[] data, long ctime, long mtime, long atime, String mode, String group, int gid, String user, int uid) {
        name = name.replace("\\", "/");
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        String baseName = name.contains("/") ? name.substring(name.lastIndexOf("/") + 1) : name;
        String baseDir = name.contains("/") ? name.substring(0, name.lastIndexOf("/")) : "";

        addDirectory(baseDir);
        if (!allNodes.get(baseDir).subnodes.containsKey(baseName)) {
            last_file_id++;
            FileNode fnode = new FileNode(last_file_id, baseName, data, null, encoding, ctime, mtime, atime, mode, group, gid, user, uid, checksum, currentOffset);
            currentOffset += fnode.length;
            allNodes.put(name, fnode);
            allNodes.get(baseDir).subnodes.put(baseName, fnode);
            files.add(fnode);
        }
    }

    public void addFile(String name, File file) throws IOException {
        addFile(name, file, group, gid, mode, user, uid);
    }

    public void addFile(String name, File file, String group, int gid, String mode, String user, int uid) throws IOException {
        if (file.isDirectory()) {
            throw new IOException("It's a directory!");
        }
        BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        addFile(name, file, attrs.creationTime().toMillis(), attrs.lastModifiedTime().toMillis(), attrs.lastAccessTime().toMillis(), mode, group, gid, user, uid);
    }

    public void addFile(String name, File file, long ctime, long mtime, long atime) throws IOException {
        addFile(name, file, ctime, mtime, atime, mode, group, gid, user, uid);
    }

    public void addFile(String name, File file, long ctime, long mtime, long atime, String mode, String group, int gid, String user, int uid) throws IOException {
        if (file.isDirectory()) {
            throw new IOException("It's a directory!");
        }

        addFile(name, new FileInputStream(file), ctime, mtime, atime, mode, group, gid, user, uid);
    }

    public void addFile(String name, InputStream is, long ctime, long mtime, long atime, String mode, String group, int gid, String user, int uid) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int cnt;
        while ((cnt = is.read(buf)) > 0) {
            baos.write(buf, 0, cnt);
        }
        is.close();
        addFile(name, baos.toByteArray(), ctime, mtime, atime, mode, group, gid, user, uid);
    }

    public void save(File file) throws IOException {

        int cksum_alg_n = checksum.getNum();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            DataOutputStream daos = new DataOutputStream(fos);
            //Note: DataOutputStream writes in network byte order (big-endian), which is what we need

            String toc = getToc();

            ByteArrayOutputStream toc_baos = new ByteArrayOutputStream();
            DeflaterOutputStream toc_deos = new DeflaterOutputStream(toc_baos);
            byte[] toc_bytes = toc.getBytes("UTF-8");
            toc_deos.write(toc_bytes);
            toc_deos.close();
            byte[] toc_compressed_bytes = toc_baos.toByteArray();
            int toc_length_compressed = toc_compressed_bytes.length;
            int toc_length_uncompressed = toc_bytes.length;

            daos.write(MAGIC);
            daos.writeShort(HEADER_SIZE);
            daos.writeShort(VERSION);
            daos.writeLong(toc_length_compressed);
            daos.writeLong(toc_length_uncompressed);
            daos.writeInt(cksum_alg_n);
            daos.write(toc_compressed_bytes);
            byte cksum[] = checksum.checkSum(toc_compressed_bytes);
            daos.write(cksum);
            for (FileNode f : files) {
                daos.write(f.compressedData);
            }
        }
    }

    public String getToc() {
        String toc = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<xar>"
                + "<toc>"
                + "<creation-time>" + DATE_FORMAT.format(creationTime) + "</creation-time>"
                + (checksum.getName().equals("none") ? "" : ("<checksum style=\"" + checksum.getName() + "\">"
                        + "<offset>0</offset>"
                        + "<size>" + checksum.checkSumLength() + "</size>"
                        + "</checksum>"));
        for (String k : allNodes.get("").subnodes.keySet()) {
            toc += allNodes.get("").subnodes.get(k);
        }
        toc += "</toc>";
        toc += "</xar>";

        return prettyFormat(toc, 2);
    }

    public static String prettyFormat(String input, int indent) {
        try {
            Source xmlInput = new StreamSource(new StringReader(input));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (IllegalArgumentException | TransformerException e) {
            return input;
        }
    }

    public static void main(String[] args) throws IOException {

    }

    public String[] listDirs() {
        String ret[] = new String[allNodes.size() - files.size()];
        int pos = 0;
        for (String path : allNodes.keySet()) {
            if (allNodes.get(path) instanceof DirectoryNode) {
                ret[pos] = path;
                pos++;
            }
        }
        return ret;
    }

    public String[] listFiles() {
        String ret[] = new String[files.size()];
        int pos = 0;
        for (String path : allNodes.keySet()) {
            if (allNodes.get(path) instanceof FileNode) {
                ret[pos] = path;
                pos++;
            }
        }
        return ret;
    }

    public void extract(File outdir) throws IOException {
        for (String path : allNodes.keySet()) {
            Node n = allNodes.get(path);
            File f = new File(outdir.getAbsolutePath() + "/" + path);
            if (n instanceof FileNode) {
                try (FileOutputStream fos = new FileOutputStream(f)) {
                    fos.write(((FileNode) n).data);
                }
            } else {
                f.mkdirs();
            }
        }
    }

    public byte[] getFileData(String name, OutputStream out) {
        if (!allNodes.containsKey(name)) {
            return null;
        }
        Node n = allNodes.get(name);
        if (n instanceof FileNode) {
            FileNode fn = (FileNode) n;
            return fn.data;
        } else {
            return null;
        }
    }

    protected void printNode(PrintStream out, Node d, int level) {
        for (int i = 0; i < level; i++) {
            System.out.print("--");
        }
        System.out.print("" + d.name);
        if (d instanceof DirectoryNode) {
            System.out.println("/");
        } else {
            System.out.println();
        }
        for (String k : d.subnodes.keySet()) {
            printNode(out, d.subnodes.get(k), level + 1);
        }
    }

    public void printTree(PrintStream out) {
        printNode(out, allNodes.get(""), 0);
    }

    public void printTree() {
        printTree(System.out);
    }
}
