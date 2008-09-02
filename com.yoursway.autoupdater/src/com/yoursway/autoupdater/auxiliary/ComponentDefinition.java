package com.yoursway.autoupdater.auxiliary;

import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.yoursway.autoupdater.filelibrary.Request;
import com.yoursway.autoupdater.protos.LocalRepositoryProtos.ComponentDefinitionMemento;
import com.yoursway.autoupdater.protos.LocalRepositoryProtos.ComponentFileMemento;
import com.yoursway.autoupdater.protos.LocalRepositoryProtos.RequestMemento;
import com.yoursway.autoupdater.protos.LocalRepositoryProtos.ComponentDefinitionMemento.Builder;

public class ComponentDefinition {
    
    private static final String COMPONENTS_PATH = "components/";
    private static final String PACKS_PATH = "packs/";
    
    private final Map<String, ComponentFile> files = newHashMap();
    private final Collection<Request> packs;
    
    private String name;
    private final Set<String> tags;
    
    public ComponentDefinition(Collection<ComponentFile> files, Collection<Request> packs, String[] tags) {
        if (packs == null)
            throw new NullPointerException("packs is null");
        for (Request packRequest : packs)
            if (!packRequest.url().toString().endsWith(".zip"))
                throw new IllegalArgumentException("packs: A pack filename must ends with .zip");
        
        this.packs = packs;
        this.tags = newHashSet(tags);
        
        for (ComponentFile file : files)
            addFile(file);
    }
    
    private void addFile(ComponentFile file) {
        files.put(file.hash, file);
    }
    
    public ComponentDefinition(URL updateSite, String name, String[] tags) throws IOException,
            InvalidFileFormatException {
        
        this.name = name;
        this.tags = newHashSet(tags);
        packs = newLinkedList();
        
        URL url = new URL(updateSite + COMPONENTS_PATH + filename());
        InputStream stream = url.openStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;
            
            String[] fields = line.split("\t");
            String type = fields[0];
            String hash = fields[1];
            long size = Long.parseLong(fields[2]);
            if (type.equals("P")) {
                URL packUrl = new URL(updateSite + PACKS_PATH + hash + ".zip");
                Request request = new Request(packUrl, size, hash);
                packs.add(request);
            } else if (type.equals("F")) {
                long modified = Long.parseLong(fields[3]);
                String[] fileTags = fields[5].split(",");
                addFile(new ComponentFile(hash, size, modified, fields[4], fileTags, fields[6]));
            } else
                throw new InvalidFileFormatException(url);
        }
    }
    
    private String filename() {
        return name.replaceAll("/", "_") + ".txt";
    }
    
    public Collection<ComponentFile> files() {
        return files.values();
    }
    
    static ComponentDefinition fromMemento(ComponentDefinitionMemento memento) throws MalformedURLException {
        Collection<ComponentFile> files = newLinkedList();
        Collection<Request> packs = newLinkedList();
        for (RequestMemento m : memento.getPackList())
            packs.add(Request.fromMemento(m));
        for (ComponentFileMemento m : memento.getFileList())
            files.add(ComponentFile.fromMemento(m));
        String[] tags = memento.getTagList().toArray(new String[memento.getTagCount()]);
        return new ComponentDefinition(files, packs, tags);
    }
    
    ComponentDefinitionMemento toMemento() {
        Builder b = ComponentDefinitionMemento.newBuilder();
        for (Request pack : packs)
            b.addPack(pack.toMemento());
        for (ComponentFile file : files.values())
            b.addFile(file.toMemento());
        b.addAllTag(tags);
        return b.build();
    }
    
    public Collection<Request> packs() {
        return packs;
    }
    
    private boolean hasTag(String tag) {
        return tags.contains(tag);
    }
    
    public boolean isInstaller() {
        return hasTag("installer");
    }
    
    public ComponentFile runJar() throws Exception {
        for (ComponentFile file : files.values())
            if (file.isRunJar())
                return file;
        throw new Exception("The component has not runjar file"); //!
    }
    
    public ComponentFile getExecIfExists() {
        for (ComponentFile file : files.values())
            if (file.isAppExec())
                return file;
        return null;
    }
}
