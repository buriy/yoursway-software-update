package com.yoursway.autoupdater.core.filelibrary.downloader;

import java.net.URL;

import com.yoursway.utils.EventSource;
import com.yoursway.utils.broadcaster.Broadcaster;
import com.yoursway.utils.broadcaster.BroadcasterFactory;

public abstract class AbstractDownloader implements Downloader {
    
    protected final Broadcaster<DownloaderListener> broadcaster = BroadcasterFactory
            .newBroadcaster(DownloaderListener.class);
    
    public EventSource<DownloaderListener> events() {
        return broadcaster;
    }
    
    public boolean cancel(URL url) {
        throw new UnsupportedOperationException();
    }
    
    public boolean loading(URL url) {
        throw new UnsupportedOperationException();
    }
    
}
