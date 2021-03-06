package com.yoursway.autoupdater.tests.internal;

import static com.google.common.collect.Maps.newHashMap;
import static com.yoursway.autoupdater.core.filelibrary.RequestUtils.size;
import static com.yoursway.autoupdater.core.filelibrary.RequestUtils.url;
import static com.yoursway.autoupdater.tests.internal.FileTestUtils.fileContents;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import com.yoursway.autoupdater.core.filelibrary.Request;
import com.yoursway.autoupdater.core.filelibrary.downloader.AbstractDownloader;
import com.yoursway.autoupdater.core.filelibrary.urlfilemapper.URLFileMapping;
import com.yoursway.utils.YsFileUtils;

public class DownloaderMock extends AbstractDownloader {
    
    private final Map<URL, File> files = newHashMap();
    
    public boolean enqueue(URLFileMapping mapping, long loaded) {
        files.put(mapping.url(), mapping.file());
        return true;
    }
    
    public void createFile(Request request) throws IOException {
        URL url = url(request);
        
        File file = files.get(url);
        YsFileUtils.writeString(file, fileContents(size(request)));
        
        broadcaster.fire().someBytesDownloaded(url);
        broadcaster.fire().completed(url);
    }
    
    @Override
    public boolean loading(URL url) {
        return false;
    }
    
}
