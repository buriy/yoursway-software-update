package com.yoursway.autoupdater.internal;

import com.yoursway.autoupdater.Downloader;
import com.yoursway.autoupdater.auxiliary.Packs;
import com.yoursway.autoupdater.protos.LocalRepositoryProtos.ProductVersionStateMemento;
import com.yoursway.autoupdater.protos.LocalRepositoryProtos.ProductVersionStateMemento.State;

public class ProductVersionState_Downloading extends AbstractProductVersionState {
    
    public ProductVersionState_Downloading(ProductVersionStateWrap wrap) {
        super(wrap);
    }
    
    @Override
    public boolean updating() {
        return true;
    }
    
    @Override
    public void continueWork() {
        Packs packs = version().packs();
        DownloadProgress progress = Downloader.instance().startDownloading(packs);
        
        progress.waitCompletion();
        if (progress.successful())
            changeState(new ProductVersionState_Installing(wrap));
        else {
            
        }
    }
    
    public ProductVersionStateMemento toMemento() {
        return ProductVersionStateMemento.newBuilder().setState(State.Downloading).setVersion(
                version().toMemento()).build();
    }
    
}
