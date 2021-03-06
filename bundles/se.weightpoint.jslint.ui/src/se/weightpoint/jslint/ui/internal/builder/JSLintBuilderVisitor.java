/*******************************************************************************
 * Copyright (c) 2012, 2013 EclipseSource.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ralf Sternberg - initial implementation and API
 ******************************************************************************/
package se.weightpoint.jslint.ui.internal.builder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.service.prefs.Preferences;

import se.weightpoint.jslint.ui.internal.Activator;
import se.weightpoint.jslint.ui.internal.builder.JSLintBuilder.CoreExceptionWrapper;
import se.weightpoint.jslint.ui.internal.preferences.EnablementPreferences;
import se.weightpoint.jslint.ui.internal.preferences.JSLintPreferences;
import se.weightpoint.jslint.ui.internal.preferences.OptionsPreferences;
import se.weightpoint.jslint.ui.internal.preferences.PreferencesFactory;
import se.weightpoint.jslint.ui.internal.preferences.ResourceSelector;

import se.weightpoint.jslint.JSLint;
import se.weightpoint.jslint.ProblemHandler;
import se.weightpoint.jslint.Text;
import se.weightpoint.jslint.json.JsonObject;


class JSLintBuilderVisitor implements IResourceVisitor, IResourceDeltaVisitor {

  private final JSLint checker;
  private final ResourceSelector selector;
  private IProgressMonitor monitor;

  public JSLintBuilderVisitor( IProject project, IProgressMonitor monitor ) throws CoreException {
    Preferences node = PreferencesFactory.getProjectPreferences( project );
    new EnablementPreferences( node );
    selector = new ResourceSelector( project );
    checker = selector.allowVisitProject() ? createJSLint( getConfiguration( project ) ) : null;
    this.monitor = monitor;
  }

  public boolean visit( IResourceDelta delta ) throws CoreException {
    IResource resource = delta.getResource();
    return visit( resource );
  }

  public boolean visit( IResource resource ) throws CoreException {
    boolean descend = false;
    if( resource.exists() && selector.allowVisitProject() && !monitor.isCanceled() ) {
      if( resource.getType() != IResource.FILE ) {
        descend = selector.allowVisitFolder( resource );
      } else {
        clean( resource );
        if( selector.allowVisitFile( resource ) ) {
          check( (IFile)resource );
        }
        descend = true;
      }
    }
    return descend;
  }

  private JSLint createJSLint( JsonObject configuration ) throws CoreException {
      JSLint jslint = new JSLint();
    try {
      InputStream inputStream = getCustomLib();
      if( inputStream != null ) {
        try {
          jslint.load( inputStream );
        } finally {
          inputStream.close();
        }
      } else {
        jslint.load();
      }
      jslint.configure( configuration );
    } catch( IOException exception ) {
      String message = "Failed to intialize JSLint";
      throw new CoreException( new Status( IStatus.ERROR, Activator.PLUGIN_ID, message, exception ) );
    }
    return jslint;
  }

  private void check( IFile file ) throws CoreException {
    Text code = readContent( file );
    ProblemHandler handler = new MarkerHandler( new MarkerAdapter( file ), code );
    try {
      checker.check( code, handler );
    } catch( CoreExceptionWrapper wrapper ) {
      throw (CoreException)wrapper.getCause();
    } catch( RuntimeException exception ) {
      String message = "Failed checking file " + file.getFullPath().toPortableString();
      throw new RuntimeException( message, exception );
    }
  }

  private static JsonObject getConfiguration( IProject project ) {
    JsonObject configuration;
    Preferences projectNode = PreferencesFactory.getProjectPreferences( project );
    OptionsPreferences projectPreferences = new OptionsPreferences( projectNode );
    if( projectPreferences.getProjectSpecific() ) {
      configuration = projectPreferences.getConfiguration();
    } else {
      Preferences workspaceNode = PreferencesFactory.getWorkspacePreferences();
      OptionsPreferences workspacePreferences = new OptionsPreferences( workspaceNode );
      configuration = workspacePreferences.getConfiguration();
    }
    return configuration;
  }

  private static void clean( IResource resource ) throws CoreException {
    new MarkerAdapter( resource ).removeMarkers();
  }

  private static InputStream getCustomLib() throws FileNotFoundException {
    JSLintPreferences globalPrefs = new JSLintPreferences();
    if( globalPrefs.getUseCustomLib() ) {
      File file = new File( globalPrefs.getCustomLibPath() );
      return new FileInputStream( file );
    }
    return null;
  }

  private static Text readContent( IFile file ) throws CoreException {
    try {
      InputStream inputStream = file.getContents();
      String charset = file.getCharset();
      return readContent( inputStream, charset );
    } catch( IOException exception ) {
      String message = "Failed to read resource";
      throw new CoreException( new Status( IStatus.ERROR, Activator.PLUGIN_ID, message, exception ) );
    }
  }

  private static Text readContent( InputStream inputStream, String charset )
      throws UnsupportedEncodingException, IOException
  {
    Text result;
    BufferedReader reader = new BufferedReader( new InputStreamReader( inputStream, charset ) );
    try {
      result = new Text( reader );
    } finally {
      reader.close();
    }
    return result;
  }

}
