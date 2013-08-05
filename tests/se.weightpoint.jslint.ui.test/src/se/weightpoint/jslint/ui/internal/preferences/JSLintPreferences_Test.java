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

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import se.weightpoint.jslint.ui.internal.preferences.JSLintPreferences;
import se.weightpoint.jslint.ui.internal.preferences.PreferencesFactory;
import static org.junit.Assert.*;


public class JSLintPreferences_Test {

  @Before
  public void setUp() throws BackingStoreException {
    Preferences node = PreferencesFactory.getWorkspacePreferences();
    node.clear();
  }

  @Test
  public void defaultPrefsForEmptyProject() {
    JSLintPreferences prefs = new JSLintPreferences();

    assertFalse( prefs.getUseCustomLib() );
    assertEquals( "", prefs.getCustomLibPath() );
  }

  @Test
  public void resetToDefaults() throws Exception {
    JSLintPreferences prefs = new JSLintPreferences();
    prefs.setUseCustomLib( true );
    prefs.setCustomLibPath( "foo" );
    prefs.save();

    prefs.resetToDefaults();

    assertTrue( prefs.hasChanged() );
    assertFalse( prefs.getUseCustomLib() );
    assertEquals( "", prefs.getCustomLibPath() );
  }

  @Test
  public void setUseCustomLib() {
    JSLintPreferences prefs = new JSLintPreferences();

    prefs.setUseCustomLib( true );

    assertTrue( prefs.hasChanged() );
    assertTrue( prefs.getUseCustomLib() );
    assertFalse( new JSLintPreferences().getUseCustomLib() );
  }

  @Test
  public void setUseCustomLib_unchanged() {
    JSLintPreferences prefs = new JSLintPreferences();

    prefs.setUseCustomLib( prefs.getUseCustomLib() );

    assertFalse( prefs.hasChanged() );
  }

  @Test
  public void setCustomLibPath() {
    JSLintPreferences prefs = new JSLintPreferences();

    prefs.setCustomLibPath( "foo" );

    assertTrue( prefs.hasChanged() );
    assertEquals( "foo", prefs.getCustomLibPath() );
    assertEquals( "", new JSLintPreferences().getCustomLibPath() );
  }

  @Test
  public void setCustomLibPath_unchanged() {
    JSLintPreferences prefs = new JSLintPreferences();

    prefs.setCustomLibPath( prefs.getCustomLibPath() );

    assertFalse( prefs.hasChanged() );
  }

  @Test
  public void save() throws Exception {
    JSLintPreferences prefs = new JSLintPreferences();
    prefs.setCustomLibPath( "foo" );

    prefs.save();

    assertFalse( prefs.hasChanged() );
    assertEquals( "foo", new JSLintPreferences().getCustomLibPath() );
  }

}
