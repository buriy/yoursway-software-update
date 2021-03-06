package com.yoursway.autoupdater.tests;

import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMap;
import static com.yoursway.autoupdater.core.auxiliary.AuxiliaryUtils.createProductVersionDefinition;
import static com.yoursway.autoupdater.tests.internal.FileTestUtils.fileContentsOf;
import static com.yoursway.autoupdater.tests.internal.FileTestUtils.hashOf;
import static com.yoursway.autoupdater.tests.internal.FileTestUtils.lastModifiedOf;
import static com.yoursway.autoupdater.tests.internal.FileTestUtils.sizeOf;
import static com.yoursway.utils.YsFileUtils.readAsString;
import static com.yoursway.utils.YsFileUtils.writeString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.yoursway.autoupdater.core.auxiliary.AuxiliaryUtils;
import com.yoursway.autoupdater.core.auxiliary.ComponentDefinition;
import com.yoursway.autoupdater.core.auxiliary.ComponentFile;
import com.yoursway.autoupdater.core.auxiliary.ComponentStopper;
import com.yoursway.autoupdater.core.auxiliary.ProductDefinition;
import com.yoursway.autoupdater.core.auxiliary.ProductVersionDefinition;
import com.yoursway.autoupdater.core.filelibrary.Request;
import com.yoursway.autoupdater.core.installer.DefaultInstallationCreator;
import com.yoursway.autoupdater.core.installer.Installation;
import com.yoursway.autoupdater.core.installer.InstallationCreator;
import com.yoursway.autoupdater.core.installer.Installer;
import com.yoursway.autoupdater.core.installer.InstallerException;
import com.yoursway.autoupdater.core.installer.InternalInstaller;
import com.yoursway.autoupdater.core.installer.RollbackTestInstallationCreator;
import com.yoursway.autoupdater.core.installer.external.ExternalInstaller;
import com.yoursway.autoupdater.core.installer.external.InstallerCommunication;
import com.yoursway.autoupdater.core.installer.external.UnexpectedMessageException;
import com.yoursway.autoupdater.tests.internal.Pack;
import com.yoursway.utils.YsFileUtils;

public class InstallerTests {
    
    //private final Set<Integer> packIDs = newLinkedHashSet();
    
    private final Collection<File> tempFolders = newLinkedList();
    
    Map<String, File> packs;
    File packsFolder;
    
    @Before
    public void setupEach() throws IOException {
        packs = newHashMap();
        packsFolder = createTempFolder();
    }
    
    @Test
    public void external() throws IOException, InstallerException, UnexpectedMessageException {
        Installer installer = new ExternalInstaller();
        Collection<ComponentDefinition> components = newLinkedList(component(12, 25), component(23, 42));
        File target = createTempFolder();
        
        components.add(createInstallerComponent());
        install(installer, components, components, target);
        
        String result = ExternalInstaller.afterInstall();
        assertEquals(InstallerCommunication.OK, result);
        
        for (int i = 12; i <= 42; i++)
            assertEquals(fileContentsOf(i), readAsString(new File(target, filepath(i))));
    }
    
    private ComponentDefinition createInstallerComponent() throws IOException {
        String root = System.getProperty("user.dir");
        File installerDir = new File(root, "../com.yoursway.autoupdater.installer/build");
        Pack pack = new Pack(installerDir);
        
        Collection<Request> packs = newLinkedList(pack.request());
        Collection<ComponentFile> files = pack.files();
        
        this.packs.put(pack.hash(), pack.packFile());
        return new ComponentDefinition("extinstaller", files, packs);
    }
    
    @Test
    public void internal() throws IOException, InstallerException {
        Installer installer = new InternalInstaller();
        Collection<ComponentDefinition> components = newLinkedList(component(12, 25), component(23, 42));
        File target = createTempFolder();
        
        install(installer, components, components, target);
        
        for (int i = 12; i <= 42; i++)
            assertEquals(fileContentsOf(i), readAsString(new File(target, filepath(i))));
    }
    
    @Test
    public void deleting() throws IOException, InstallerException {
        Installer installer = new InternalInstaller();
        Collection<ComponentDefinition> components1 = newLinkedList(component(8, 15), component(33, 48));
        Collection<ComponentDefinition> components2 = newLinkedList(component(12, 25), component(23, 42));
        File target = createTempFolder();
        
        createFiles(target, components1);
        for (int i = 8; i < 12; i++)
            assertTrue(new File(target, filepath(i)).exists());
        
        install(installer, components1, components2, target);
        
        for (int i = 8; i < 12; i++)
            assertFalse(new File(target, filepath(i)).exists());
        for (int i = 12; i <= 42; i++)
            assertEquals(fileContentsOf(i), readAsString(new File(target, filepath(i))));
    }
    
    @Test
    public void date() throws IOException, InstallerException {
        Installer installer = new InternalInstaller();
        Collection<ComponentDefinition> components = newLinkedList(component(12, 25), component(23, 42));
        File target = createTempFolder();
        
        install(installer, components, components, target);
        
        for (int i = 12; i <= 42; i++)
            assertEquals(lastModifiedOf(i), new File(target, filepath(i)).lastModified());
    }
    
    @Test
    public void rollback() throws IOException, InstallerException {
        Installer installer = new InternalInstaller();
        Collection<ComponentDefinition> components1 = newLinkedList(component(8, 15), component(33, 48));
        Collection<ComponentDefinition> components2 = newLinkedList(component(12, 25), component(23, 42));
        File target = createTempFolder();
        
        createFiles(target, components1);
        for (int i = 8; i < 12; i++)
            assertTrue(new File(target, filepath(i)).exists());
        
        install(installer, components1, components2, target, new RollbackTestInstallationCreator());
        
        //> check file contents replacement
        
        for (int i = 8; i <= 15; i++)
            assertTrue(new File(target, filepath(i)).exists());
        for (int i = 15 + 1; i < 33; i++)
            assertFalse(new File(target, filepath(i)).exists());
    }
    
    @After
    public void cleanEach() {
        for (File folder : tempFolders)
            YsFileUtils.deleteRecursively(folder);
        
        tempFolders.clear();
    }
    
    private void install(Installer installer, Collection<ComponentDefinition> oldComponents,
            Collection<ComponentDefinition> newComponents, File target) throws IOException,
            InstallerException {
        
        InstallationCreator installationCreator = new DefaultInstallationCreator();
        install(installer, oldComponents, newComponents, target, installationCreator);
    }
    
    private void install(Installer installer, Collection<ComponentDefinition> oldComponents,
            Collection<ComponentDefinition> newComponents, File target,
            InstallationCreator installationCreator) throws InstallerException, MalformedURLException {
        
        ProductDefinition productD = AuxiliaryUtils.fakeProductDefinition();
        Collection<Request> p = newLinkedList();
        
        ProductVersionDefinition currentVD = createProductVersionDefinition(productD, "current", "current",
                p, oldComponents, "");
        ProductVersionDefinition newVD = createProductVersionDefinition(productD, "new", "new", p,
                newComponents, "");
        
        ComponentStopper stopper = new ComponentStopper() {
            public boolean stop() {
                return true;
            }
        };
        
        Installation installation = installationCreator.createInstallation(currentVD, newVD, packs, target,
                "");
        installer.install(installation, stopper);
    }
    
    private void createFiles(File target, Collection<ComponentDefinition> components) throws IOException {
        for (ComponentDefinition component : components)
            for (ComponentFile file : component.files())
                new File(target, file.path()).createNewFile();
    }
    
    private File createTempFolder() throws IOException {
        File folder = YsFileUtils.createTempFolder("autoupdater.installer.test", null);
        tempFolders.add(folder);
        return folder;
    }
    
    private ComponentDefinition component(int first, int last) throws IOException {
        return new ComponentDefinition("component-" + first + "-" + last, files(first, last), packs(first,
                last));
    }
    
    private Collection<ComponentFile> files(int first, int last) {
        Collection<ComponentFile> files = newLinkedList();
        for (int i = first; i <= last; i++)
            files.add(new ComponentFile(hashOf(i), sizeOf(i), lastModifiedOf(i), "-", filepath(i)));
        return files;
    }
    
    private String filepath(int i) {
        return "filepath" + i;
    }
    
    private Collection<Request> packs(int first, int last) throws IOException {
        Collection<Request> packs = newLinkedList();
        for (int i = first / 10; i <= last / 10; i++) {
            packs.add(new Request(new URL("http://localhost/packfile" + i + ".zip"), 0, "packhash" + i));
            addPack(i);
        }
        return packs;
    }
    
    private void addPack(int i) throws IOException {
        File pack = new File(packsFolder, "packfile" + i + ".zip");
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(pack));
        for (int j = i * 10; j <= i * 10 + 14; j++) {
            zip.putNextEntry(new ZipEntry(hashOf(j)));
            writeString(zip, fileContentsOf(j));
            zip.closeEntry();
        }
        zip.close();
        packs.put("packhash" + i, pack);
    }
    
}
