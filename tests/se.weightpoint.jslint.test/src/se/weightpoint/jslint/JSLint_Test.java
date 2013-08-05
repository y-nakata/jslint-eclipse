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
package se.weightpoint.jslint;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.JavaScriptException;

import se.weightpoint.jslint.JSLint;
import se.weightpoint.jslint.Problem;
import se.weightpoint.jslint.ProblemHandler;
import se.weightpoint.jslint.Text;
import se.weightpoint.jslint.json.JsonObject;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.*;


public class JSLint_Test {

  private List<Problem> problems;
  private TestHandler handler;
  private JSLint jsLint;

  @Before
  public void setUp() throws IOException {
    problems = new ArrayList<Problem>();
    handler = new TestHandler( problems );
    jsLint = new JSLint();
    jsLint.load();
  }

  @Test
  public void getDefaultVersion() {
    String version = JSLint.getDefaultLibraryVersion();

    assertTrue( version.matches( "\\d+\\.\\d+\\.\\d+" ) );
  }

  @Test( expected = NullPointerException.class )
  public void configureWithNull() {
    jsLint.configure( null );
  }

  @Test
  public void configureBeforeLoad() throws Exception {
    JsonObject configuration = new JsonObject().add( "undef", true );

    JSLint jsLint = new JSLint();
    jsLint.configure( configuration );
    jsLint.load();
    jsLint.check( "x = 23;", handler );

    assertEquals( "'x' is not defined", problems.get( 0 ).getMessage() );
  }

  @Test
    public void loadBeforeConfigure() throws Exception {
    JsonObject configuration = new JsonObject().add( "undef", true );

    JSLint jsLint = new JSLint();
    jsLint.load();
    jsLint.configure( configuration );
    jsLint.check( "x = 23;", handler );

    assertEquals( "'x' is not defined", problems.get( 0 ).getMessage() );
  }

  @Test( expected = IllegalStateException.class )
  public void checkWithoutLoad() {
    JSLint jsLint = new JSLint();
    jsLint.check( "code", handler );
  }

  @Test( expected = NullPointerException.class )
  public void checkWithNullCode() {
    jsLint.check( (String)null, handler );
  }

  @Test( expected = NullPointerException.class )
  public void checkWithNullText() {
    jsLint.check( (Text)null, handler );
  }

  @Test
  public void checkWithNullHandler() {
    assertTrue( jsLint.check( "var a = 23;", null ) );
    assertFalse( jsLint.check( "HMPF!", null ) );
  }

  @Test( expected = NullPointerException.class )
  public void loadCustomWithNullParameter() throws Exception {
    JSLint jsLint = new JSLint();
    jsLint.load( null );
  }

  @Test
  public void loadCustomWithEmptyFile() throws Exception {
    JSLint jsLint = new JSLint();
    try {
      jsLint.load( new ByteArrayInputStream( "".getBytes() ) );
      fail();
    } catch( IllegalArgumentException exception ) {
      assertEquals( "Global JSLINT function missing in input", exception.getMessage() );
    }
  }

  @Test
  public void loadCustomWithWrongJsFile() throws Exception {
    JSLint jsLint = new JSLint();
    try {
      jsLint.load( new ByteArrayInputStream( "var a = 23;".getBytes() ) );
      fail();
    } catch( IllegalArgumentException exception ) {
      assertEquals( "Global JSLINT function missing in input", exception.getMessage() );
    }
  }

  @Test
  public void loadCustomWithFakeJsLintFile() throws Exception {
    JSLint jsLint = new JSLint();
    try {
      jsLint.load( new ByteArrayInputStream( "JSLINT = {};".getBytes() ) );
      fail();
    } catch( IllegalArgumentException exception ) {
      assertEquals( "Global JSLINT is not a function", exception.getMessage() );
    }
  }

  @Test
  public void loadCustomWithEcmaException() throws Exception {
    JSLint jsLint = new JSLint();
    try {
      jsLint.load( new ByteArrayInputStream( "JSLINT = foo;".getBytes() ) );
      fail();
    } catch( IllegalArgumentException exception ) {
      assertEquals( "Could not evaluate JavaScript input", exception.getMessage() );
    }
  }

  @Test
  public void loadCustomWithGarbage() throws Exception {
    JSLint jsLint = new JSLint();
    try {
      jsLint.load( new ByteArrayInputStream( "cheese! :D".getBytes() ) );
      fail();
    } catch( IllegalArgumentException exception ) {
      assertEquals( "Could not evaluate JavaScript input", exception.getMessage() );
    }
  }

  @Test
  public void loadCustom() throws Exception {
    JSLint jsLint = new JSLint();
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream stream = classLoader.getResourceAsStream( "com/jshint/jshint-r03.js" );
    try {
      jsLint.load( stream );
    } finally {
      stream.close();
    }

    jsLint.check( "cheese! :D", handler );

    assertFalse( problems.isEmpty() );
  }

  @Test
  public void checkWithEmptyCode() {
    boolean result = jsLint.check( "", handler );

    assertTrue( result );
    assertTrue( problems.isEmpty() );
  }

  @Test
  public void checkWithOnlyWhitespace() {
    boolean result = jsLint.check( " ", handler );

    assertTrue( result );
    assertTrue( problems.isEmpty() );
  }

  @Test
  public void checkWithValidCode() {
    boolean result = jsLint.check( "var foo = 23;", handler );

    assertTrue( result );
    assertTrue( problems.isEmpty() );
  }

  @Test
  public void checkWithFaultyCode() {
    boolean result = jsLint.check( "cheese!", handler );

    assertFalse( result );
    assertFalse( problems.isEmpty() );
  }

  @Test
  public void checkWithJavaScriptException() throws Exception {
    JSLint jsLint = new JSLint();
    jsLint.load( new ByteArrayInputStream( "JSLINT = function() { throw 'ERROR'; };".getBytes() ) );

    try {
      jsLint.check( "var a = 1;", handler );
      fail();
    } catch( RuntimeException exception ) {

      String expected = "JavaScript exception thrown by JSLint: ERROR";
      assertThat( exception.getMessage(), startsWith( expected ) );
      assertSame( JavaScriptException.class, exception.getCause().getClass() );
    }
  }

  @Test
  public void checkWithRhinoException() throws Exception {
    JSLint jsLint = new JSLint();
    jsLint.load( new ByteArrayInputStream( "JSLINT = function() { throw x[ 0 ]; };".getBytes() ) );

    try {
      jsLint.check( "var a = 1;", handler );
      fail();
    } catch( RuntimeException exception ) {

      String expected = "JavaScript exception caused by JSLint: ReferenceError";
      assertThat( exception.getMessage(), startsWith( expected ) );
      assertSame( EcmaError.class, exception.getCause().getClass() );
    }
  }

  @Test
  public void noErrorsWithoutConfig() {
    // undefined variable is only reported with 'undef' in config
    jsLint.check( "var f = function () { v = {}; };", handler );

    assertTrue( problems.isEmpty() );
  }

  @Test
  public void noErrorsWithEmptyConfig() {
    // undefined variable is only reported with 'undef' in config
    jsLint.configure( new JsonObject() );

    jsLint.check( "var f = function () { v = {}; };", handler );

    assertTrue( problems.isEmpty() );
  }

  @Test
  public void errorWithUndefInConfig() {
    jsLint.configure( new JsonObject().add( "undef", true ) );

    jsLint.check( "var f = function () { v = {}; };", handler );

    assertThat( problems.get( 0 ).getMessage(), containsString( "'v' is not defined" ) );
  }

  @Test
  public void errorAfterTabHasCorrectPosition() {
    jsLint.configure( new JsonObject().add( "undef", true ) );

    jsLint.check( "var x = 1,\t# y = 2;", handler );

    assertEquals( 11, problems.get( 0 ).getCharacter() );
  }

  @Test
  public void errorAtEndDoesNotThrowException() {
    jsLint.configure( new JsonObject().add( "undef", true ) );

    // Must not throw SIOOBE
    // See https://github.com/eclipsesource/jshint-eclipse/issues/34
    jsLint.check( "var x = 1;\t#", handler );
  }

  @Test
  public void checkSameInputTwice() {
    jsLint.configure( new JsonObject().add( "undef", true ) );
    LoggingHandler handler1 = new LoggingHandler();
    LoggingHandler handler2 = new LoggingHandler();

    jsLint.check( "var x = 1;\t#", handler1 );
    jsLint.check( "var x = 1;\t#", handler2 );

    assertTrue( handler1.toString().length() > 0 );
    assertEquals( handler1.toString(), handler2.toString() );
  }

  @Test
  public void checkMultipleFiles() {
    // see https://github.com/jshint/jshint/issues/931
    jsLint.configure( new JsonObject().add( "undef", true ) );

    jsLint.check( "var x = 1;\t#", handler );
    jsLint.check( "var x = 1;\t#", handler );
    jsLint.check( "var x = 1;\t#", handler );
    jsLint.check( "var x = 1;\t#", handler );
    jsLint.check( "var x = 1;\t#", handler );
  }

  private static class LoggingHandler implements ProblemHandler {

    StringBuilder log = new StringBuilder();

    public void handleProblem( Problem problem ) {
      log.append( problem.getLine() );
      log.append( ':' );
      log.append( problem.getCharacter() );
      log.append( ':' );
      log.append( problem.getMessage() );
      log.append( '\n' );
    }

    @Override
    public String toString() {
      return log.toString();
    }

  }

  private static class TestHandler implements ProblemHandler {

    private final List<Problem> problems;

    public TestHandler( List<Problem> problems ) {
      this.problems = problems;
    }

    public void handleProblem( Problem problem ) {
      problems.add( problem );
    }

  }

}
