package com.yoursway.autoupdater.gui.controller;

import com.yoursway.autoupdater.auxiliary.AutoupdaterException;
import com.yoursway.autoupdater.auxiliary.SuiteDefinition;
import com.yoursway.autoupdater.auxiliary.UpdatableApplication;
import com.yoursway.autoupdater.gui.view.UpdaterView;
import com.yoursway.autoupdater.gui.view.UpdaterViewFactory;
import com.yoursway.autoupdater.localrepository.LocalRepository;
import com.yoursway.utils.annotations.Nullable;

public class UpdaterController {
    
    private final UpdatableApplication app;
    private final UpdaterViewFactory viewFactory;
    private LocalRepository repo;
    
    public UpdaterController(UpdatableApplication app) {
        this(app, null);
    }
    
    public UpdaterController(UpdatableApplication app, @Nullable UpdaterViewFactory viewFactory) {
        if (app == null)
            throw new NullPointerException("app is null");
        this.app = app;
        
        this.viewFactory = viewFactory != null ? viewFactory : UpdaterView.factory();
    }
    
    public void onStart() {
        try {
            repo = LocalRepository.createForGUI(app);
            repo.atStartup();
        } catch (AutoupdaterException e) {
            app.view().displayAutoupdaterErrorMessage(e); //?
        }
    }
    
    public void updateApplication() {
        try {
            SuiteDefinition suite = SuiteDefinition.load(app.updateSite(), app.suiteName());
            if (repo == null)
                throw new AutoupdaterException("LocalRepository has not yet been created");
            
            viewFactory.createView(app.view(), suite, repo).show();
        } catch (AutoupdaterException e) {
            app.view().displayAutoupdaterErrorMessage(e);
        }
    }
    
}