package com.yoursway.autoupdater.localrepository.internal;

import java.net.MalformedURLException;
import java.util.Collection;

import com.yoursway.autoupdater.auxiliary.ProductVersion;
import com.yoursway.autoupdater.filelibrary.LibraryState;
import com.yoursway.autoupdater.filelibrary.Request;
import com.yoursway.autoupdater.protos.LocalRepositoryProtos.ProductVersionStateMemento;

public class ProductVersionStateWrap implements ProductVersionState {
    
    private ProductVersionState state;
    final ProductVersion version;
    final ProductState productState;
    
    public ProductVersionStateWrap(ProductVersion version, ProductState productState) {
        this.productState = productState;
        this.version = version;
        state = new ProductVersionState_Installing(this);
    }
    
    private ProductVersionStateWrap(ProductVersionStateMemento memento, ProductState productState)
            throws MalformedURLException {
        this.productState = productState;
        version = ProductVersion.fromMemento(memento.getVersion());
        state = AbstractProductVersionState.from(memento.getState(), this);
    }
    
    public static ProductVersionStateWrap fromMemento(ProductVersionStateMemento memento,
            ProductState productState) throws MalformedURLException {
        return new ProductVersionStateWrap(memento, productState);
    }
    
    public ProductVersionStateMemento toMemento() {
        return state.toMemento();
    }
    
    void changeState(ProductVersionState newState) {
        state = newState;
        continueWork();
    }
    
    public void startUpdating() {
        state.startUpdating();
    }
    
    public boolean updating() {
        return state.updating();
    }
    
    public void continueWork() {
        state.continueWork();
    }
    
    public Collection<Request> requiredFiles() {
        return state.requiredFiles();
    }
    
    public void libraryChanged(LibraryState s) {
        state.libraryChanged(s);
    }
    
}
