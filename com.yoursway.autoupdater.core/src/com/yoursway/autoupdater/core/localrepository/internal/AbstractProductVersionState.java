package com.yoursway.autoupdater.core.localrepository.internal;

import static com.google.common.collect.Lists.newLinkedList;

import java.util.Collection;

import com.yoursway.autoupdater.core.auxiliary.AutoupdaterException;
import com.yoursway.autoupdater.core.auxiliary.ComponentStopper;
import com.yoursway.autoupdater.core.auxiliary.ProductVersionDefinition;
import com.yoursway.autoupdater.core.filelibrary.LibraryState;
import com.yoursway.autoupdater.core.filelibrary.OrderManager;
import com.yoursway.autoupdater.core.filelibrary.Request;
import com.yoursway.autoupdater.core.installer.Installer;
import com.yoursway.autoupdater.core.localrepository.UpdatingListener;
import com.yoursway.autoupdater.core.protos.LocalRepositoryProtos.LocalProductVersionMemento.State;

abstract class AbstractProductVersionState implements ProductVersionState {
    
    protected final LocalProductVersion version;
    
    public AbstractProductVersionState(LocalProductVersion version) {
        this.version = version;
    }
    
    public static ProductVersionState from(State s, LocalProductVersion v) {
        if (s == State.Installing)
            return new ProductVersionState_Installing(v);
        if (s == State.Idle)
            return new ProductVersionState_Idle(v);
        if (s == State.InstallingExternal)
            return new ProductVersionState_InstallingExternal(v);
        if (s == State.InternalError)
            return new ProductVersionState_InternalError(v);
        throw new IllegalArgumentException("State s == " + s.toString());
    }
    
    protected final void changeState(ProductVersionState newState) {
        version.changeState(newState);
    }
    
    protected ProductVersionDefinition versionDefinition() {
        return version.definition;
    }
    
    protected Installer installer() {
        return version.product().installer;
    }
    
    protected OrderManager orderManager() {
        return version.product().orderManager;
    }
    
    protected ComponentStopper componentStopper() {
        return version.product().componentStopper();
    }
    
    protected UpdatingListener fire() {
        return version.broadcaster.fire();
    }
    
    public void startUpdating() {
        throw new IllegalStateException();
    }
    
    public boolean updating() {
        return false;
    }
    
    public void continueWork() {
        // nothing to do
    }
    
    public Collection<Request> libraryRequests() {
        return newLinkedList();
    }
    
    public void libraryChanged(LibraryState state) {
        // nothing to do
    }
    
    public void atStartup() {
        continueWork();
    }
    
    public boolean failed() {
        return false;
    }
    
    protected void errorOccured(AutoupdaterException e) {
        version.errors.errorOccured(e);
    }
    
}
