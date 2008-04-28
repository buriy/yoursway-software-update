package com.yoursway.autoupdater.core.tests.fakeapp.ui;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.yoursway.autoupdate.core.glue.GlueIntegrator;
import com.yoursway.autoupdate.core.glue.GlueIntegratorImpl;
import com.yoursway.autoupdate.core.glue.persister.Storage;
import com.yoursway.autoupdate.core.glue.persister.TransactionalStorage;
import com.yoursway.autoupdate.ui.DialogUtils;
import com.yoursway.autoupdate.ui.UpdatePreferencesComposite;
import com.yoursway.autoupdater.core.tests.fakeapp.ui.internal.Activator;

public class FakeApplication implements IApplication {
    
    public Object start(IApplicationContext context) throws Exception {
        Display display = new Display();
        
        File stateDir = new File(Activator.getDefault().getStateLocation().toFile(), "updater");
        Storage storage = new TransactionalStorage(new File(stateDir, "state.bin"), new File(stateDir,
                "state.upd"));
        
        Executor executor = new ThreadPoolExecutor(0, 1, 10000, TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>());
        
        GlueIntegrator glue = new GlueIntegratorImpl(new SystemClock(), new FakeCheckEngine(),
                new FakeUpdateEngine(), executor, new SwtRelativeScheduler(display), storage);
        
        GlueToPreferences glueToPreferences = new GlueToPreferences(glue, display);
        new GlueToDialog(glue, Activator.getDefault().getDialogSettings());
        
        Shell shell = new Shell(display, SWT.DIALOG_TRIM);
        UpdatePreferencesComposite prefs = new UpdatePreferencesComposite(shell, SWT.NONE);
        glueToPreferences.hook(prefs);
        
        prefs.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        GridLayoutFactory.swtDefaults().generateLayout(shell);
        
        shell.setSize(500, 300);
        DialogUtils.centerWindow(shell);
        shell.open();
        
        while (!shell.isDisposed())
            if (!display.readAndDispatch())
                display.sleep();
        return IApplication.EXIT_OK;
    }
    
    public void stop() {
    }
    
}
