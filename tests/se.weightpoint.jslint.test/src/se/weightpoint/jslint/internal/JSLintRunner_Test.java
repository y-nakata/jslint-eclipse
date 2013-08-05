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
package se.weightpoint.jslint.internal;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import se.weightpoint.jslint.internal.JSLintRunner;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;


public class JSLintRunner_Test {

  private static final String SYSOUT_ENCODING = "UTF-8";
  private static final String LINE_SEPARATOR = System.getProperty( "line.separator" );
  private PrintStream bufferedSysout;
  private ByteArrayOutputStream sysout;

  @Before
  public void setUp() throws UnsupportedEncodingException {
    bufferedSysout = System.out;
    sysout = new ByteArrayOutputStream();
    System.setOut( new PrintStream( sysout, true, SYSOUT_ENCODING ) );
  }

  @After
  public void tearDown() {
    System.setOut( bufferedSysout );
  }

  @Test
  public void emptyArgs() {
    JSLintRunner runner = new JSLintRunner();

    runner.run();

    assertThat( getSysout(), startsWith( "No input files" + LINE_SEPARATOR ) );
    assertThat( getSysout(), containsString( "Usage:" ) );
  }

  @Test
  public void onlyOptionArgs() {
    JSLintRunner runner = new JSLintRunner();

    runner.run( "--charset" );

    assertThat( getSysout(), startsWith( "No input files" + LINE_SEPARATOR ) );
    assertThat( getSysout(), containsString( "Usage:" ) );
  }

  @Test
  public void emptyFile() throws Exception {
    JSLintRunner runner = new JSLintRunner();
    File file = createTmpFile( "", "UTF-8" );

    runner.run( file.getAbsolutePath() );

    assertEquals( "", getSysout() );
  }

  @Test
  public void validFile() throws Exception {
    JSLintRunner runner = new JSLintRunner();
    File file = createTmpFile( "var a = 23;", "UTF-8" );

    runner.run( file.getAbsolutePath() );

    assertEquals( "", getSysout() );
  }

  @Test
  public void invalidFile() throws Exception {
    JSLintRunner runner = new JSLintRunner();
    File file = createTmpFile( "var a == 23;", "UTF-8" );

    runner.run( file.getAbsolutePath() );

    String fileName = file.getAbsolutePath();
    assertThat( getSysout(), containsString( "Problem in file " + fileName + " at line 1: " ) );
    assertThat( getSysout(), containsString( "\nProblem in file" ) );
  }

  @Test
  public void missingFile() throws Exception {
    JSLintRunner runner = new JSLintRunner();
    File file = createTmpFile( "-- not processed --", "UTF-8" );
    String fileName = file.getAbsolutePath();
    File missingFile = new File( "/nowhere/missing-file.js" );
    String missingFileName = missingFile.getAbsolutePath();

    runner.run( fileName, missingFileName );

    assertThat( getSysout(), startsWith( "No such file: " + missingFileName ) );
  }

  @Test
  public void charsetDefaultsToUtf8() throws Exception {
    JSLintRunner runner = new JSLintRunner();
    File file = createTmpFile( "föhn.foo = 23;", "UTF-8" );

    runner.run( file.getAbsolutePath() );

    assertThat( getSysout(), containsString( "ö" ) );
  }

  @Test
  public void customCharset() throws Exception {
    JSLintRunner runner = new JSLintRunner();
    File file = createTmpFile( "föhn.foo = 23;", "ISO-8859-1" );

    runner.run( "--charset", "ISO-8859-1", file.getAbsolutePath() );

    assertThat( getSysout(), containsString( "ö" ) );
  }

  @Test
  public void illegalCharset() throws Exception {
    JSLintRunner runner = new JSLintRunner();
    File file = createTmpFile( "föhn.foo = 23;", "ISO-8859-1" );

    runner.run( "--charset", "HMPF!", file.getAbsolutePath() );

    assertThat( getSysout(), startsWith( "Unknown or unsupported charset: HMPF!" ) );
  }

  @Test
  public void customLibrary() throws Exception {
    JSLintRunner runner = new JSLintRunner();
    String fakeJsLint = "JSLINT = function() { return false; };"
            + "JSLINT.errors = [ { line: 1, character: 2, reason: 'test' } ]";
    File fakeJSLintFile = createTmpFile( fakeJsLint, "UTF-8" );
    File jsFile = createTmpFile( "-- ignored --", "UTF-8" );
    String fakeJSLintFileName = fakeJSLintFile.getAbsolutePath();
    String jsFileName = jsFile.getAbsolutePath();

    runner.run( "--custom", fakeJSLintFileName, jsFileName );

    assertThat( getSysout(), startsWith( "Problem in file " + jsFileName + " at line 1: test" ) );
  }

  @Test
  public void customLibrary_invalidFile() throws Exception {
    JSLintRunner runner = new JSLintRunner();
    File libraryFile = createTmpFile( "cheese! :D", "UTF-8" );
    File jsFile = createTmpFile( "var föhn = 23;", "UTF-8" );

    runner.run( "--custom", libraryFile.getAbsolutePath(), jsFile.getAbsolutePath() );

    String expected = "Failed to load JSLint library: Could not evaluate JavaScript input";
    assertThat( getSysout(), startsWith( expected ) );
  }

  private String getSysout() {
    try {
      return sysout.toString( SYSOUT_ENCODING );
    } catch( UnsupportedEncodingException exception ) {
      throw new RuntimeException( exception );
    }
  }

  private static File createTmpFile( String content, String charset ) throws IOException {
    File file = File.createTempFile( "jslint-test", ".tmp" );
    FileOutputStream outputStream = new FileOutputStream( file );
    try {
      BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( outputStream, charset ) );
      try {
        writer.write( content );
      } finally {
        writer.close();
      }
    } finally {
      outputStream.close();
    }
    file.deleteOnExit();
    return file;
  }

}
