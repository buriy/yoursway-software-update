package com.yoursway.autoupdater.tests;

import static com.yoursway.utils.YsFileUtils.readAsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import com.yoursway.autoupdater.filelibrary.downloader.Downloader;
import com.yoursway.autoupdater.filelibrary.downloader.DownloaderListener;
import com.yoursway.autoupdater.tests.internal.server.WebServer;

public class DownloaderTests {
    
    boolean completed;
    
    @Test
    public void connection() throws IOException, InterruptedException {
        String remotePath = "test";
        String text = "Hello world!\nOK\n";
        completed = false;
        
        WebServer server = null;
        File file = null;
        try {
            server = new WebServer();
            server.mount(remotePath, text);
            
            Downloader downloader = new Downloader();
            DownloaderListener listener = new DownloaderListener() {
                
                public void someBytesDownloaded(URL url) {
                    // nothing
                }
                
                public void completed(URL url) {
                    synchronized (DownloaderTests.this) {
                        completed = true;
                        DownloaderTests.this.notify();
                    }
                }
            };
            downloader.events().addListener(listener);
            
            URL url = new URL("http://localhost:" + server.getPort() + "/" + remotePath);
            file = File.createTempFile("autoupdater.test", null);
            System.out.println(file.getAbsolutePath());
            
            synchronized (this) {
                downloader.enqueue(url, file);
                wait(1000);
            }
            
            assertEquals(text, readAsString(file));
            assertTrue(completed);
            
        } finally {
            try {
                file.delete();
            } finally {
                server.dispose();
            }
        }
    }
}