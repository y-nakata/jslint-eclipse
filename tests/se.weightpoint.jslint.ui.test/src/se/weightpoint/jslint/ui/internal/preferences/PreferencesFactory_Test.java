/*******************************************************************************
 * Copyright (c) 2012 EclipseSource.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ralf Sternberg - initial implementation and API
 ******************************************************************************/
package se.weightpoint.jslint.ui.internal.preferences;

import org.eclipse.core.resources.IProject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.prefs.Preferences;

import se.weightpoint.jslint.ui.internal.preferences.PreferencesFactory;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static se.weightpoint.jslint.ui.test.TestUtil.createProject;
import static se.weightpoint.jslint.ui.test.TestUtil.deleteProject;


public class PreferencesFactory_Test {
  private IProject project;

  @Before
  public void setUp() {
    project = createProject( "test" );
  }

  @After
  public void tearDown() {
    deleteProject( project );
  }

  @Test
  public void getProjectPreferences() {
    Preferences preferences = PreferencesFactory.getProjectPreferences( project );

    assertNotNull( preferences );
    assertSame( preferences, PreferencesFactory.getProjectPreferences( project ) );
  }

  @Test
  public void getWorkspacePreferences() {
    Preferences preferences = PreferencesFactory.getWorkspacePreferences();

    assertNotNull( preferences );
    assertSame( preferences, PreferencesFactory.getWorkspacePreferences() );
  }

}
