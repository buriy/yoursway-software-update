package com.yoursway.autoupdater.gui.controller;

import com.yoursway.autoupdater.auxiliary.AutoupdaterException;
import com.yoursway.autoupdater.auxiliary.SuiteDefinition;
import com.yoursway.autoupdater.auxiliary.UpdatableApplication;
import com.yoursway.autoupdater.gui.view.VersionsView;
import com.yoursway.autoupdater.gui.view.VersionsViewFactory;
import com.yoursway.autoupdater.installer.external.ExternalInstaller;
import com.yoursway.autoupdater.localrepository.LocalRepository;
import com.yoursway.utils.annotations.Nullable;

public class UpdaterController {
    
    private final UpdatableApplication app;
    private final VersionsViewFactory viewFactory;
    
    public UpdaterController(UpdatableApplication app) {
        this(app, null);
    }
    
    public UpdaterController(UpdatableApplication app, @Nullable VersionsViewFactory viewFactory) {
        if (app == null)
            throw new NullPointerException("app is null");
        this.app = app;
        
        this.viewFactory = viewFactory != null ? viewFactory : VersionsView.factory();
    }
    
    public void onStart() {
        if (!app.inInstallingState())
            return;
        
        try {
            ExternalInstaller.afterInstall();
            app.setInstallingState(false);
        } catch (AutoupdaterException e) {
            // cannot to communicate with external installer
            app.view().displayAutoupdaterErrorMessage(e); //?
        }
    }
    
    public void updateApplication() {
        try {
            SuiteDefinition suite = SuiteDefinition.load(app.updateSite(), app.suiteName());
            LocalRepository repo = LocalRepository.createForGUI(app);
            
            viewFactory.createView(app.view(), suite, repo).show();
        } catch (AutoupdaterException e) {
            app.view().displayAutoupdaterErrorMessage(e);
        }
    }
    
}
