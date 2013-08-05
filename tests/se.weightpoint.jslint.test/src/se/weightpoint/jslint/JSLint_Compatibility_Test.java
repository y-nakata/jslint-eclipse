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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import se.weightpoint.jslint.JSLint;
import se.weightpoint.jslint.Problem;
import se.weightpoint.jslint.ProblemHandler;
import se.weightpoint.jslint.json.JsonObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;


@RunWith( value = Parameterized.class )
public class JSLint_Compatibility_Test {

  private ArrayList<Problem> problems;
  private TestHandler handler;
  private final String jsLintResource;
  private JSLint jsLint;

  @Parameters
  public static Collection<Object[]> getParameters() {
    ArrayList<Object[]> parameters = new ArrayList<Object[]>();
    parameters.add( new Object[] { "com/jslint/jslint-2012-02-03.js" } );
    parameters.add( new Object[] { "com/jslint/jslint-2013-07-31.js" } );
    return parameters;
  }

  public JSLint_Compatibility_Test( String jsLintResource ) {
    this.jsLintResource = jsLintResource;
  }

  @Before
  public void setUp() throws IOException {
    problems = new ArrayList<Problem>();
    handler = new TestHandler();
    jsLint = new JSLint();
    loadJsLint();
  }

  @Test
  public void noProblemsForValidCode() {
    jsLint.check( "var a = 23;", handler );

    assertTrue( problems.isEmpty() );
  }

  @Test
  public void problemLineIs_1_Relative() {
    jsLint.check( "#", handler );

    assertEquals( 1, problems.get( 0 ).getLine() );
  }

  @Test
  public void problemCharacterIs_0_Relative() {
    jsLint.check( "#", handler );

    assertEquals( 0, problems.get( 0 ).getCharacter() );
  }

  @Test
  public void cproblemMessageIsNotEmpty() {
    jsLint.check( "#", handler );

    assertTrue( problems.get( 0 ).getMessage().length() > 0 );
  }

  @Test
  public void undefinedVariable_withoutConfig_succeeds() {
    jsLint.check( "foo = {};", handler );

    // seems that the undef option is inverted in jslint
    String expected = isJsLint() ? "1.0:'foo' was used before it was defined" : "";
    assertEquals( expected, getAllProblems() );
  }

  @Test
  public void undefinedVariable_withoutPredefInConfig_fails() {
    jsLint.configure( new JsonObject().add( "undef", true ) );

    jsLint.check( "foo = {};", handler );

    // seems that the undef option is inverted in jslint
    String expected = isJsLint() ? "" : "1.0:'foo' is not defined";
    assertEquals( expected, getAllProblems() );
  }

  @Test
  public void undefinedVariable_withPredefInConfig_succeeds() {
    JsonObject predefined = new JsonObject().add( "foo", true );
    jsLint.configure( new JsonObject().add( "undef", true ).add( "predef", predefined ) );

    jsLint.check( "foo = {};", handler );

    assertEquals( "", getAllProblems() );
  }

  @Test
  public void undefinedVariable_withReadOnlyPredefInConfig_fails() {
    // FIXME [rst] See https://github.com/jshint/jshint/issues/665
    assumeTrue( !isVersion( "r10" ) && !isVersion( "r11" ) && !isVersion( "r12" ) );
    JsonObject predefined = new JsonObject().add( "foo", false );
    jsLint.configure( new JsonObject().add( "undef", true ).add( "predef", predefined ) );

    jsLint.check( "foo = {};", handler );

    assertEquals( "1.0:Read only", getAllProblems() );
  }

  @Test
  public void eqnull_withoutConfig() {
    jsLint.check( "var x = 23 == null;", handler );

    String expected = isJsLint() ? "Expected '===' and instead saw '=='"
                                 : "Use '===' to compare with 'null'";
    assertEquals( "1.11:" + expected, getAllProblems() );
  }

  @Test
  public void eqnull_withEmptyConfig() {
    jsLint.configure( new JsonObject() );

    jsLint.check( "var x = 23 == null;", handler );

    String expected = isJsLint() ? "Expected '===' and instead saw '=='"
                                   : "Use '===' to compare with 'null'";
    assertEquals( "1.11:" + expected, getAllProblems() );
  }

  @Test
  public void eqnull_withEqnullInConfig() {
    // JSLint doesn't get this right
    assumeTrue( !isJsLint() );
    jsLint.configure( new JsonObject().add( "eqnull", true ) );

    jsLint.check( "var f = x == null ? null : x + 1;", handler );

    assertEquals( "", getAllProblems() );
  }

  @Test
  public void positionIsCorrect() {
    jsLint.check( "var x = 23 == null;", handler );

    assertEquals( "1.11", getPositionFromProblem( 0 ) );
  }

  @Test
  public void positionIsCorrectWithLeadingSpace() {
    assumeTrue( !isJsLint() );
    jsLint.configure( new JsonObject().add( "white", false ) );
    jsLint.check( " var x = 23 == null;", handler );

    assertEquals( "1.12", getPositionFromProblem( 0 ) );
  }

  @Test
  public void positionIsCorrectWithLeadingTab() {
    assumeTrue( !isJsLint() );
    jsLint.configure( new JsonObject().add( "white", false ) );
    jsLint.check( "\tvar x = 23 == null;", handler );

    assertEquals( "1.12", getPositionFromProblem( 0 ) );
  }

  @Test
  public void positionIsCorrectWithMultipleTabs() {
    assumeTrue( !isJsLint() );
    jsLint.configure( new JsonObject().add( "white", false ) );
    jsLint.check( "\tvar x\t= 23 == null;", handler );

    assertEquals( "1.12", getPositionFromProblem( 0 ) );
  }

  @Test
  public void toleratesWindowsLineBreaks() {
    jsLint.configure( new JsonObject().add( "white", false ) );
    jsLint.check( "var x = 1;\r\nvar y = 2;\r\nvar z = 23 == null;", handler );

    assertEquals( "3.11", getPositionFromProblem( 0 ) );
  }

  private void loadJsLint() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream stream = classLoader.getResourceAsStream( jsLintResource );
    try {
      jsLint.load( stream );
    } finally {
      stream.close();
    }
  }

  private boolean isVersion( String version ) {
    return jsLintResource.contains( version );
  }

  private boolean isJsLint() {
    return jsLintResource.contains( "jslint" );
  }

  private String getPositionFromProblem( int n ) {
    Problem problem = problems.get( n );
    return problem.getLine() + "." + problem.getCharacter();
  }

  private String getAllProblems() {
    StringBuilder builder = new StringBuilder();
    for( Problem problem : problems ) {
      if( builder.length() > 0 ) {
        builder.append( ", " );
      }
      builder.append( problem.getLine() );
      builder.append( '.' );
      builder.append( problem.getCharacter() );
      builder.append( ':' );
      builder.append( problem.getMessage() );
    }
    return builder.toString();
  }

  private class TestHandler implements ProblemHandler {

    public void handleProblem( Problem problem ) {
      problems.add( problem );
    }
  }

}
