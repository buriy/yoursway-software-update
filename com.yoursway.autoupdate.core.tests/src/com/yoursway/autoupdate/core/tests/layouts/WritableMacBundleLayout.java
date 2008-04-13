package com.yoursway.autoupdate.core.tests.layouts;

import static com.google.common.collect.Iterators.forEnumeration;
import static com.google.common.collect.Lists.newArrayList;
import static com.yoursway.utils.YsFileUtils.saveToFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.osgi.framework.Bundle;

import com.yoursway.autoupdate.core.app.layout.MacBundlePlatformLayout;
import com.yoursway.autoupdate.core.tests.internal.Activator;

public class WritableMacBundleLayout {
    
    private File plugins;
    private final PluginSource source;
    
    public WritableMacBundleLayout(File root, PluginSource source) throws IOException {
        this.source = source;
        Bundle bundle = Activator.getDefault().getBundle();
        copyFromBundleTo(bundle, "osgi_templates/mac_bundle", root);
        plugins = new File(root, "Contents/Resources/Java/plugins");
        plugins.mkdirs();
        
        Process proc = Runtime.getRuntime().exec(
                new String[] { "/bin/chmod", "+x", new File(root, "Contents/MacOS/eclipse").toString() },
                null, null);
        try {
            proc.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
    }
    
    public void copyJar(String id) throws IOException {
        source.putJar(id, plugins);
    }
    
    public void copyPlugin(String id) throws IOException {
        source.putPlugin(id, plugins);
    }
    
    public MacBundlePlatformLayout toLayout() {
        MacBundlePlatformLayout result = MacBundlePlatformLayout.explore(plugins.getParentFile());
        if (result == null)
            throw new AssertionError("Incorrect platform built by WritableMacBundleLayout");
        return result;
    }
    
    public static void copyFromBundleTo(Bundle bundle, String path, File root) throws IOException {
        if (!path.endsWith("/"))
            path = path + "/";
        copyFromBundleTo_(bundle, path, root);
    }
    
    @SuppressWarnings("unchecked")
    private static void copyFromBundleTo_(Bundle bundle, String path, File destination)
            throws FileNotFoundException, IOException {
        destination.mkdirs();
        List<String> list = newArrayList(forEnumeration(bundle.getEntryPaths(path)));
        for (String elem : list) {
            File newDest = new File(destination, elementName(elem));
            if (elem.endsWith("/"))
                copyFromBundleTo_(bundle, elem, newDest);
            else
                saveToFile(bundle.getEntry(elem).openStream(), newDest);
        }
    }
    
    private static String elementName(String elem) {
        if (elem.endsWith("/"))
            elem = elem.substring(0, elem.length() - 1);
        int pos = elem.lastIndexOf('/');
        if (pos < 0)
            return elem;
        return elem.substring(pos + 1);
    }
    
}
