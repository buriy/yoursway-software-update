package com.yoursway.autoupdater.core.installer;

import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.yoursway.utils.YsFileUtils.saveToFile;
import static com.yoursway.utils.os.YsOSUtils.setExecAttribute;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.yoursway.autoupdater.core.auxiliary.ComponentDefinition;
import com.yoursway.autoupdater.core.auxiliary.ComponentFile;
import com.yoursway.autoupdater.core.auxiliary.ProductVersionDefinition;
import com.yoursway.autoupdater.core.filelibrary.Request;
import com.yoursway.autoupdater.core.installer.external.ExternalInstaller;
import com.yoursway.autoupdater.core.installer.log.InstallerLog;
import com.yoursway.autoupdater.core.localrepository.internal.DefinitionException;
import com.yoursway.autoupdater.core.localrepository.internal.LocalProduct;
import com.yoursway.autoupdater.core.localrepository.internal.LocalProductVersion;
import com.yoursway.autoupdater.core.protos.InstallationProtos.InstallationMemento;
import com.yoursway.autoupdater.core.protos.InstallationProtos.PackMemento;
import com.yoursway.autoupdater.core.protos.InstallationProtos.InstallationMemento.Builder;
import com.yoursway.utils.YsDigest;
import com.yoursway.utils.YsFileUtils;
import com.yoursway.utils.os.YsOSUtils;

public class Installation {
    
    private final ProductVersionDefinition currentVD;
    private final ProductVersionDefinition newVD;
    final Map<String, File> packs;
    private final File target;
    private final String executablePath;
    
    private final List<RollbackAction> rollbackActions = newLinkedList();
    private File backupDir;
    
    Installation(ProductVersionDefinition currentVD, ProductVersionDefinition newVD, Map<String, File> packs,
            File target, String executablePath) {
        if (currentVD == null)
            throw new NullPointerException("current is null");
        if (newVD == null)
            throw new NullPointerException("version is null");
        if (packs == null)
            throw new NullPointerException("packs is null");
        if (target == null)
            throw new NullPointerException("target is null");
        if (executablePath == null)
            throw new NullPointerException("executablePath is null");
        
        this.currentVD = currentVD;
        this.newVD = newVD;
        this.packs = packs;
        this.target = target;
        this.executablePath = executablePath;
    }
    
    public Installation(LocalProductVersion version, Map<String, File> packs) throws InstallerException {
        LocalProduct product = version.product();
        
        try {
            currentVD = product.currentVersion();
        } catch (DefinitionException e) {
            throw new InstallerException("Cannot get current version definition", e);
        }
        newVD = version.definition();
        
        if (!currentVD.product().equals(newVD.product()))
            throw new AssertionError("Must not update one product to another.");
        
        try {
            target = product.rootFolder();
        } catch (IOException e) {
            throw new InstallerException("Cannot get application root folder", e);
        }
        
        this.packs = packs;
        
        executablePath = product.executablePath();
    }
    
    public void perform(InstallerLog log) throws InstallerException {
        
        try {
            backupDir = YsFileUtils.createTempFolder("autoupdater-installation-backup-", null);
        } catch (IOException e) {
            throw new InstallerException("Cannot create installation backup directory", e);
        }
        
        try {
            Set<String> newVersionFilePaths = newHashSet();
            
            for (ComponentDefinition component : newVD.components()) {
                if (component.isInstaller())
                    continue;
                
                for (ComponentFile file : component.files()) {
                    newVersionFilePaths.add(file.path());
                    setupFile(file, component.packs(), log, target);
                }
            }
            
            for (ComponentDefinition component : currentVD.components()) {
                if (component.isInstaller())
                    continue;
                
                for (ComponentFile file : component.files()) {
                    String path = file.path();
                    if (!newVersionFilePaths.contains(path)) {
                        removeOldFile(path, log);
                    }
                }
            }
            
        } catch (Throwable e) {
            throw new InstallerException("Cannot perform installation", e);
        }
    }
    
    public void rollback() throws RollbackException {
        for (RollbackAction action : rollbackActions) {
            boolean ok = action._do();
            if (!ok)
                throw new RollbackException();
        }
        rollbackActions.clear();
    }
    
    public void deleteBackupFiles() {
        YsFileUtils.deleteRecursively(backupDir);
    }
    
    protected void setupFile(ComponentFile file, Iterable<Request> packs, InstallerLog log, File target)
            throws IOException {
        log.debug("Setting up file " + file.path());
        
        ZipFile pack = null;
        ZipEntry entry = null;
        
        for (Request request : packs) {
            String packHash = request.hash();
            pack = new ZipFile(this.packs.get(packHash));
            entry = pack.getEntry(file.hash());
            if (entry != null)
                break;
        }
        
        if (entry == null)
            throw new FileNotFoundException(); //?
            
        MessageDigest digest = YsDigest.createSha1();
        InputStream in = new DigestInputStream(pack.getInputStream(entry), digest);
        final File targetFile = new File(target, file.path());
        
        if (targetFile.exists()) {
            final File rollbackFile = new File(backupDir, file.path());
            targetFile.renameTo(rollbackFile);
            rollbackActions.add(0, new RollbackAction() {
                public boolean _do() {
                    return rollbackFile.renameTo(targetFile);
                }
            });
        }
        
        targetFile.getParentFile().mkdirs();
        saveToFile(in, targetFile);
        
        String hash = YsDigest.asHex(digest.digest());
        
        if (!hash.equals(file.hash()))
            throw new RuntimeException("Incorrect hash"); //!
            
        rollbackActions.add(0, new RollbackAction() {
            public boolean _do() {
                return targetFile.delete();
            }
        });
        
        boolean ok = targetFile.setLastModified(file.modified());
        if (!ok)
            log.error("Cannot set lastmodified property of file " + targetFile);
        
        if (file.hasExecAttribute() || file.path().equals(executablePath))
            setExecAttribute(targetFile);
    }
    
    protected void removeOldFile(String path, InstallerLog log) {
        log.debug("Removing old file " + path);
        final File localFile = new File(target, path);
        
        final File rollbackFile = new File(backupDir, path);
        boolean renamed = localFile.renameTo(rollbackFile);
        rollbackActions.add(0, new RollbackAction() {
            public boolean _do() {
                return rollbackFile.renameTo(localFile);
            }
        });
        
        if (!renamed)
            log.error("Cannot remove old file " + localFile);
    }
    
    public InstallationMemento toMemento() {
        Builder b = InstallationMemento.newBuilder().setCurrent(currentVD.toMemento()).setVersion(
                newVD.toMemento()).setTarget(target.getAbsolutePath()).setExecutable(executablePath);
        for (Map.Entry<String, File> entry : packs.entrySet()) {
            String hash = entry.getKey();
            File file = entry.getValue();
            b.addPack(PackMemento.newBuilder().setHash(hash).setPath(file.getPath()));
        }
        return b.build();
    }
    
    public static Installation fromMemento(InstallationMemento memento) throws MalformedURLException {
        ProductVersionDefinition current = ProductVersionDefinition.fromMemento(memento.getCurrent());
        ProductVersionDefinition version = ProductVersionDefinition.fromMemento(memento.getVersion());
        Map<String, File> packs = newHashMap();
        for (PackMemento m : memento.getPackList())
            packs.put(m.getHash(), new File(m.getPath()));
        File target = new File(memento.getTarget());
        return new Installation(current, version, packs, target, memento.getExecutable());
    }
    
    public void startVersionExecutable(InstallerLog log, int port) throws Exception {
        log.debug("target folder: " + target.toString());
        log.debug("exepath: " + executablePath);
        
        if (executablePath.length() == 0)
            return;
        File executable = new File(target, executablePath);
        //File executable = new File(target, newVD.executable().path());
        
        List<String> cmd = newLinkedList();
        if (executable.getName().endsWith(".jar")) {
            cmd.add(YsOSUtils.javaPath());
            cmd.add("-jar");
        }
        cmd.add(executable.getCanonicalPath());
        
        ExternalInstaller.startProcess(cmd, target, port);
    }
    
    public ComponentDefinition getInstallerComponent() throws Exception {
        return newVD.installer();
    }
    
    public void setupExternalInstaller(File dir) throws Exception {
        ComponentDefinition externalInstaller = newVD.installer();
        
        for (ComponentFile file : externalInstaller.files())
            setupFile(file, externalInstaller.packs(), InstallerLog.NOP, dir);
    }
    
}
