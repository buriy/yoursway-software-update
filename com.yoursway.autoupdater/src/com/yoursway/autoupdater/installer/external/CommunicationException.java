package com.yoursway.autoupdater.installer.external;

import com.yoursway.autoupdater.installer.InstallerException;

public class CommunicationException extends InstallerException {
    private static final long serialVersionUID = 8511744976958167196L;
    
    public CommunicationException(Throwable cause) {
        super("Cannot communicate with an external installer", cause);
    }
    
    public CommunicationException(String message) {
        super(message);
    }
    
}
