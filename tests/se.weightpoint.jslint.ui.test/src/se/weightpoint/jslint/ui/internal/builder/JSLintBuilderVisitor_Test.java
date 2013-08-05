/*******************************************************************************
 * Copyright (c) 2013 EclipseSource.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ralf Sternberg - initial implementation and API
 ******************************************************************************/
package se.weightpoint.jslint.ui.internal.builder;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import se.weightpoint.jslint.ui.internal.builder.JSLintBuilderVisitor;
import se.weightpoint.jslint.ui.internal.preferences.EnablementPreferences;
import se.weightpoint.jslint.ui.internal.preferences.PreferencesFactory;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.weightpoint.jslint.ui.test.TestUtil.*;


public class JSLintBuilderVisitor_Test {

  private IProject project;
  private IFile file;
  private IProgressMonitor monitor;

  @Before
  public void setUp() throws BackingStoreException {
    PreferencesFactory.getWorkspacePreferences().clear();
    project = createProject( "test" );
    file = createFile( project, "/test.js", "test content" );
    monitor = new NullProgressMonitor();
  }

  @After
  public void tearDown() {
    deleteProject( project );
  }

  @Test
  public void skipsProjectWithoutIncludePattern() throws CoreException {
    JSLintBuilderVisitor visitor = new JSLintBuilderVisitor( project, monitor );

    assertFalse( visitor.visit( project ) );
  }

  @Test
  public void visitsProjectWhenIncludePatternIsPresent() throws CoreException {
    addIncludePattern( project, "/test.js" );
    JSLintBuilderVisitor visitor = new JSLintBuilderVisitor( project, monitor );

    assertTrue( visitor.visit( project ) );
  }

  @Test
  public void skipsProjectWhenCancelled() throws CoreException {
    addIncludePattern( project, "/test.js" );
    JSLintBuilderVisitor visitor = new JSLintBuilderVisitor( project, monitor );

    monitor.setCanceled( true );

    assertFalse( visitor.visit( project ) );
  }

  @Test
  public void visitsResourceWhenIncluded() throws CoreException {
    addIncludePattern( project, "/test.js" );
    JSLintBuilderVisitor visitor = new JSLintBuilderVisitor( project, monitor );

    assertTrue( visitor.visit( file ) );
  }

  @Test
  public void skipsResourceWhenCancelled() throws CoreException {
    addIncludePattern( project, "/test.js" );
    JSLintBuilderVisitor visitor = new JSLintBuilderVisitor( project, monitor );

    monitor.setCanceled( true );

    assertFalse( visitor.visit( file ) );
  }

  private void addIncludePattern( IProject project, String... pattern ) {
    Preferences projectPrefsNode = PreferencesFactory.getProjectPreferences( project );
    new EnablementPreferences( projectPrefsNode ).setIncludePatterns( list( pattern ) );
  }

}
