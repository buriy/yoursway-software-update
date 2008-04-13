package com.yoursway.autoupdate.core.tests;

import static com.google.common.collect.Maps.newHashMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.yoursway.autoupdate.core.tests.internal.SimpleHttpServer;
import com.yoursway.autoupdate.core.tests.internal.SimpleServlet;
import com.yoursway.utils.StringInputStream;

public class WebServer {
    
    private final static int PORT = 8744;
    private SimpleHttpServer server;
    
    private Map<String, String> mountedStrings = newHashMap();
 
    public WebServer() {
       SimpleServlet servlet = new SimpleServlet() {
            
            public void log(String s2) {
                System.out.println(s2);
            }
            
            public InputStream openFile(String path) throws IOException {
                String value = mountedStrings.get(path);
                if (value != null)
                    return new StringInputStream(value);
                throw new IOException("404, blya: " + path);
//                return Activator.openResource("tests/integration/" + path);
            }
            
        };
        server = new SimpleHttpServer(PORT, servlet);
     }
    
    @SuppressWarnings("deprecation")
    public void dispose() {
        server.stop();
    }

    public int getPort() {
        return PORT;
    }

    public void mount(String path, String value) {
        mountedStrings.put(path, value);
    }
    
}
