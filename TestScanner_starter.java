/*Copyright 2023 by Beverly A Sanders
 * 
 * This code is provided for solely for use of students in COP4020 Programming Language Concepts at the 
 * University of Florida during the spring semester 2023 as part of the course project.  
 * 
 * No other use is authorized. 
 * 
 * This code may not be posted on a public web site either during or after the course.  
 */

package edu.ufl.cise.plcsp23;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import edu.ufl.cise.plcsp23.IToken.Kind;
import edu.ufl.cise.plcsp23.IToken.SourceLocation;

class TestScanner_starter {

	// makes it easy to turn output on and off (and less typing than
	// System.out.println)
	static final boolean VERBOSE = true;

	void show(Object obj) {
		if (VERBOSE) {
			System.out.println(obj);
		}
	}

	// check that this token has the expected kind
	void checkToken(Kind expectedKind, IToken t) {
		assertEquals(expectedKind, t.getKind());
	}
	
	void checkToken(Kind expectedKind, String expectedChars, SourceLocation expectedLocation, IToken t) {
		assertEquals(expectedKind, t.getKind());
		assertEquals(expectedChars, t.getTokenString());
		assertEquals(expectedLocation, t.getSourceLocation());
		;
	}

	void checkIdent(String expectedChars, IToken t) {
		checkToken(Kind.IDENT, t);
		assertEquals(expectedChars.intern(), t.getTokenString().intern());
		;
	}

	void checkString(String expectedValue, IToken t) {
		assertTrue(t instanceof IStringLitToken);
		assertEquals(expectedValue, ((IStringLitToken) t).getValue());
	}

	void checkString(String expectedChars, String expectedValue, SourceLocation expectedLocation, IToken t) {
		assertTrue(t instanceof IStringLitToken);
		assertEquals(expectedValue, ((IStringLitToken) t).getValue());
		assertEquals(expectedChars, t.getTokenString());
		assertEquals(expectedLocation, t.getSourceLocation());
	}

	void checkNUM_LIT(int expectedValue, IToken t) {
		checkToken(Kind.NUM_LIT, t);
		int value = ((INumLitToken) t).getValue();
		assertEquals(expectedValue, value);
	}
	
	void checkNUM_LIT(int expectedValue, SourceLocation expectedLocation, IToken t) {
		checkToken(Kind.NUM_LIT, t);
		int value = ((INumLitToken) t).getValue();
		assertEquals(expectedValue, value);
		assertEquals(expectedLocation, t.getSourceLocation());
	}

	void checkTokens(IScanner s, IToken.Kind... kinds) throws LexicalException {
		for (IToken.Kind kind : kinds) {
			checkToken(kind, s.next());
		}
	}

	void checkTokens(String input, IToken.Kind... kinds) throws LexicalException {
		IScanner s = CompilerComponentFactory.makeScanner(input);
		for (IToken.Kind kind : kinds) {
			checkToken(kind, s.next());
		}
	}

	// check that this token is the EOF token
	void checkEOF(IToken t) {
		checkToken(Kind.EOF, t);
	}


	@Test
	void emptyProg() throws LexicalException {
		String input = "";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkEOF(scanner.next());
	}

	@Test
	void onlyWhiteSpace() throws LexicalException {
		String input = " \t \r\n \f \n";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkEOF(scanner.next());
		checkEOF(scanner.next());  //repeated invocations of next after end reached should return EOF token
	}

	@Test
	void numLits1() throws LexicalException {
		String input = """
				123
				05 240
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkNUM_LIT(123, scanner.next());
		checkNUM_LIT(0, scanner.next());
		checkNUM_LIT(5, scanner.next());
		checkNUM_LIT(240, scanner.next());
		checkEOF(scanner.next());
	}
	
	@Test
	//Too large should still throw LexicalException
	void numLitTooBig() throws LexicalException {
		String input = "999999999999999999999";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		assertThrows(LexicalException.class, () -> {
			scanner.next();
		});
	}


	@Test
	void identsAndReserved() throws LexicalException {
		String input = """
				i0
				  i1  x ~~~2 spaces at beginning and after il
				y Y
				""";

		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkToken(Kind.IDENT,"i0", new SourceLocation(1,1), scanner.next());
		checkToken(Kind.IDENT, "i1",new SourceLocation(2,3), scanner.next());
		checkToken(Kind.RES_x, "x", new SourceLocation(2,7), scanner.next());		
		checkToken(Kind.RES_y, "y", new SourceLocation(3,1), scanner.next());
		checkToken(Kind.RES_Y, "Y", new SourceLocation(3,3), scanner.next());
		checkEOF(scanner.next());
	}
	

	@Test
	void operators0() throws LexicalException {
		String input = """
				==
				+
				/
				====
				=
				===
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkToken(Kind.EQ, scanner.next());
		checkToken(Kind.PLUS, scanner.next());
		checkToken(Kind.DIV, scanner.next());
		checkToken(Kind.EQ, scanner.next());
		checkToken(Kind.EQ, scanner.next());
		checkToken(Kind.ASSIGN, scanner.next());
		checkToken(Kind.EQ, scanner.next());
		checkToken(Kind.ASSIGN, scanner.next());
		checkEOF(scanner.next());
	}


	@Test
	void stringLiterals1() throws LexicalException {
		String input = """
				"hello"
				"\t"
				"\\""
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkString(input.substring(0, 7),"hello", new SourceLocation(1,1), scanner.next());
		checkString(input.substring(8, 11), "\t", new SourceLocation(2,1), scanner.next());
		checkString(input.substring(12, 16), "\"",  new SourceLocation(3,1), scanner.next());
		checkEOF(scanner.next());
	}


	@Test
	void illegalEscape() throws LexicalException {
		String input = """
				"\\t"
				"\\k"
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkString("\"\\t\"","\t", new SourceLocation(1,1), scanner.next());
		assertThrows(LexicalException.class, () -> {
			scanner.next();
		});
	}
	
	@Test
	void illegalLineTermInStringLiteral() throws LexicalException {
		String input = """
				"\\n"  ~ this one passes the escape sequence--it is OK
				"\n"   ~ this on passes the LF, it is illegal.
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkString("\"\\n\"","\n", new SourceLocation(1,1), scanner.next());
		assertThrows(LexicalException.class, () -> {
			scanner.next();
		});
	}

	@Test
	void lessThanGreaterThanExchange() throws LexicalException {
		String input = """
				<->>>>=
				<<=<
				""";
		checkTokens(input, Kind.EXCHANGE, Kind.GT, Kind.GT, Kind.GE, Kind.LT, Kind.LE, Kind.LT, Kind.EOF);
	}
	
	/** The Scanner should not backtrack so this input should throw an exception */
	@Test
	void incompleteExchangeThrowsException() throws LexicalException {
		String input = " <- ";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		assertThrows(LexicalException.class, () -> {
			scanner.next();
		});	
	}

	@Test
	void illegalChar() throws LexicalException {
		String input = """
				abc
				@
				""";
		IScanner scanner = CompilerComponentFactory.makeScanner(input);
		checkIdent("abc", scanner.next());
		assertThrows(LexicalException.class, () -> {
			@SuppressWarnings("unused")
			IToken t = scanner.next();
		});
	}

}
