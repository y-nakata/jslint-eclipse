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
package se.weightpoint.jslint.ui.internal.builder;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.junit.Before;
import org.junit.Test;

import se.weightpoint.jslint.ui.internal.builder.MarkerAdapter;
import se.weightpoint.jslint.ui.internal.builder.MarkerHandler;

import se.weightpoint.jslint.Problem;
import se.weightpoint.jslint.Text;

import static org.junit.Assert.assertEquals;


public class MarkerHandler_Test {

  private MarkerAdapter adapter;
  private List<String> log;

  @Before
  public void setUp() {
    adapter = new TestMarkerAdapter();
    log = new ArrayList<String>();
  }

  @Test
  public void createMarker() throws IOException {
    MarkerHandler handler = new MarkerHandler( adapter, createText( "test" ) );

    handler.handleProblem( new TestProblem( 1, 0, "test" ) );

    assertEquals( 1, log.size() );
    assertEquals( "1,0,0,test", log.get( 0 ) );
  }

  @Test
  public void createMarker_atDocumentWhenLineIsZero() throws IOException {
    MarkerHandler handler = new MarkerHandler( adapter, createText( "test" ) );

    handler.handleProblem( new TestProblem( 0, 1, "test" ) );

    assertEquals( "-1,-1,-1,test", log.get( 0 ) );
  }

  @Test
  public void createMarker_atDocumentWhenLineIsNegative() throws IOException {
    MarkerHandler handler = new MarkerHandler( adapter, createText( "test" ) );

    handler.handleProblem( new TestProblem( -1, 1, "test" ) );

    assertEquals( "-1,-1,-1,test", log.get( 0 ) );
  }

  @Test
  public void createMarker_atDocumentWhenLineExceedsDocument() throws IOException {
    MarkerHandler handler = new MarkerHandler( adapter, createText( "test" ) );

    handler.handleProblem( new TestProblem( 2, 1, "test" ) );

    assertEquals( "-1,-1,-1,test", log.get( 0 ) );
  }

  @Test
  public void createMarker_atLineWhenCharacterIsNegative() throws IOException {
    MarkerHandler handler = new MarkerHandler( adapter, createText( "test" ) );

    handler.handleProblem( new TestProblem( 1, -1, "test" ) );

    assertEquals( "1,-1,-1,test", log.get( 0 ) );
  }

  @Test
  public void createMarker_atLineWhenCharacterExceedsLine() throws IOException {
    MarkerHandler handler = new MarkerHandler( adapter, createText( "line1\nline2\n" ) );

    handler.handleProblem( new TestProblem( 1, 7, "test" ) );

    assertEquals( "1,-1,-1,test", log.get( 0 ) );
  }

  @Test
  public void createMarker_atLineWhenCharacterExceedsDocument() throws IOException {
    MarkerHandler handler = new MarkerHandler( adapter, createText( "line1\nline2\n" ) );

    handler.handleProblem( new TestProblem( 1, 13, "test" ) );

    assertEquals( "1,-1,-1,test", log.get( 0 ) );
  }

  private static Text createText( String string ) throws IOException {
    Reader reader = new StringReader( string );
    Text text = new Text( reader );
    return text;
  }

  private class TestMarkerAdapter extends MarkerAdapter {

    public TestMarkerAdapter() {
      super( null );
    }

    @Override
    public void createMarker( int line, int start, int end, String message ) throws CoreException {
      log.add( line + "," + start + "," + end + "," + message );
    }

  }

  private static class TestProblem implements Problem {

    private final int line;
    private final int start;
    private final String message;

    public TestProblem( int line, int start, String message ) {
      this.line = line;
      this.start = start;
      this.message = message;
    }

    public int getLine() {
      return line;
    }

    public int getCharacter() {
      return start;
    }

    public String getMessage() {
      return message;
    }

  }

}
