package com.jpexs.xar.ant;

import com.jpexs.xar.Xar;
import com.jpexs.xar.nodes.Node;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.TarFileSet;

/**
 * Ant task for creating XAR archives
 *
 * @author JPEXS
 */
public class XarTask {

    private Project project;
    private boolean verbose = false;
    private String compression;
    private String checksum;

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    private String destFile = null;

    public void setDestFile(String destFile) {
        this.destFile = destFile;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    private List<TarFileSet> filesets = new ArrayList<>();

    public void addTarFileset(TarFileSet fileset) {
        filesets.add(fileset);
    }

    protected void validate() {
        if (filesets.isEmpty()) {
            throw new BuildException("fileset not set");
        }
        if (destFile == null) {
            throw new BuildException("destFile not set");
        }
    }

    private String[] getFileNames(FileSet fs) {
        DirectoryScanner ds = fs.getDirectoryScanner(fs.getProject());

        String[] directories = ds.getIncludedDirectories();
        String[] filesPerSe = ds.getIncludedFiles();

        String[] files = new String[directories.length + filesPerSe.length];

        System.arraycopy(directories, 0, files, 0, directories.length);
        System.arraycopy(filesPerSe, 0, files, directories.length, filesPerSe.length);

        return files;
    }

    public void execute() {
        validate();
        System.out.println("Xar: Creating XAR archive to \"" + destFile + "\" ...");
        Set<String> files = new HashSet<>();
        Xar archive = new Xar(compression == null ? "gzip" : compression, checksum == null ? "sha1" : checksum);

        for (TarFileSet fs : filesets) {

            String fullPath = fs.getFullpath(project);
            String prefix = fs.getPrefix(project);
            String fileNames[] = getFileNames(fs);

            for (int i = 0; i < fileNames.length; i++) {
                String targetName;
                String fileName = fileNames[i];

                if (!fullPath.isEmpty()) {
                    targetName = fullPath;
                } else {
                    targetName = prefix + fileName;
                }
                targetName = targetName.replace('\\', '/');
                if (targetName.equals(".")) {
                    targetName = "";
                }
                if (verbose) {
                    System.out.println("Xar: Adding \"" + targetName + "\" ...");
                }
                if (files.contains(targetName)) {
                    continue;
                }
                files.add(targetName);
                File f = new File(fs.getDir(project).getAbsolutePath() + "/" + fileName);
                if (f.isDirectory()) {
                    //no empty directories, sorry...
                    continue;
                }
                try {
                    String baseName = targetName.contains("/") ? targetName.substring(targetName.lastIndexOf("/") + 1) : targetName;
                    String baseDir = targetName.contains("/") ? targetName.substring(0, targetName.lastIndexOf("/")) : "";
                    Node n = archive.add(baseDir, baseName, f);
                    if (fs.hasGroupBeenSet()) {
                        n.group = fs.getGroup();
                    }
                    if (fs.hasGroupIdBeenSet()) {
                        n.gid = fs.getGid();
                    }
                    if (fs.hasUserIdBeenSet()) {
                        n.uid = fs.getUid();
                    }
                    if (fs.hasUserNameBeenSet()) {
                        n.userName = fs.getUserName();
                    }
                    if (f.isDirectory()) {
                        if (fs.hasDirModeBeenSet()) {
                            n.mode = fs.getDirMode(project);
                        }
                    }
                } catch (IOException ex) {
                    throw new BuildException("Xar: Cannot read \"" + f + "\"", ex);
                }
            }

        }

        try {
            archive.save(new File(destFile));
        } catch (IOException ex) {
            throw new BuildException("Xar: Cannot write to \"" + destFile + "\"", ex);
        }
    }

}
