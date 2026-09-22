// Generated from /home/tom/Devel/kroxy-dm/kroxylicious-filters/kroxylicious-record-manipulation/src/main/antlr4/io/kroxylicious/op/parser/Path.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class PathParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		EXPONENT=10, LCALPHA=11, UCALPHA=12, HI_ASCII=13, NON_ASCII=14, SPACE=15, 
		UNDERSCORE=16, MINUS=17, MINUS_ZERO=18, PLUS=19, DOT=20, DOTDOT=21, COMMA=22, 
		OR=23, AND=24, EQ=25, NE=26, LT=27, LE=28, GT=29, GE=30, QUEST=31, COLON=32, 
		RPAR=33, LPAR=34, LSQ=35, RSQ=36, ZERO=37, NON_0_DIGIT=38, DOLLAR=39, 
		AT=40, STAR=41, TRUE=42, FALSE=43, NULL=44, WS=45, ESC=46, ESCAPED=47, 
		SQUOT=48, DQUOT=49;
	public static final int
		RULE_jsonpathQuery = 0, RULE_segments = 1, RULE_selector = 2, RULE_nameSelector = 3, 
		RULE_stringLiteral = 4, RULE_doubleQuoted = 5, RULE_singleQuoted = 6, 
		RULE_indexSelector = 7, RULE_int = 8, RULE_sliceSelector = 9, RULE_start = 10, 
		RULE_end = 11, RULE_step = 12, RULE_filterSelector = 13, RULE_logicalExpr = 14, 
		RULE_logicalOrExpr = 15, RULE_logicalAndExpr = 16, RULE_basicExpr = 17, 
		RULE_parenExpr = 18, RULE_logicalNotOp = 19, RULE_testExpr = 20, RULE_filterQuery = 21, 
		RULE_relQuery = 22, RULE_comparisonExpr = 23, RULE_literal = 24, RULE_comparable = 25, 
		RULE_comparisonOp = 26, RULE_singularQuery = 27, RULE_relSingularQuery = 28, 
		RULE_absSingularQuery = 29, RULE_singularQuerySegments = 30, RULE_nameSegment = 31, 
		RULE_indexSegment = 32, RULE_number = 33, RULE_frac = 34, RULE_exp = 35, 
		RULE_functionName = 36, RULE_functionNameFirst = 37, RULE_functionNameChar = 38, 
		RULE_functionExpr = 39, RULE_functionArgument = 40, RULE_segment = 41, 
		RULE_childSegment = 42, RULE_bracketedSelection = 43, RULE_memberNameShorthand = 44, 
		RULE_nameFirst = 45, RULE_nameChar = 46, RULE_descendantSegment = 47, 
		RULE_digit = 48, RULE_unescaped = 49, RULE_alpha = 50, RULE_lower = 51;
	private static String[] makeRuleNames() {
		return new String[] {
			"jsonpathQuery", "segments", "selector", "nameSelector", "stringLiteral", 
			"doubleQuoted", "singleQuoted", "indexSelector", "int", "sliceSelector", 
			"start", "end", "step", "filterSelector", "logicalExpr", "logicalOrExpr", 
			"logicalAndExpr", "basicExpr", "parenExpr", "logicalNotOp", "testExpr", 
			"filterQuery", "relQuery", "comparisonExpr", "literal", "comparable", 
			"comparisonOp", "singularQuery", "relSingularQuery", "absSingularQuery", 
			"singularQuerySegments", "nameSegment", "indexSegment", "number", "frac", 
			"exp", "functionName", "functionNameFirst", "functionNameChar", "functionExpr", 
			"functionArgument", "segment", "childSegment", "bracketedSelection", 
			"memberNameShorthand", "nameFirst", "nameChar", "descendantSegment", 
			"digit", "unescaped", "alpha", "lower"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'!'", "'#'", "'%'", "'&'", "'/'", "';'", "'='", "'^'", "'`'", 
			"'e'", null, null, null, null, "' '", "'_'", "'-'", "'-0'", "'+'", "'.'", 
			"'..'", "','", "'||'", "'&&'", "'::'", "'!:'", "'<'", "'<:'", "'>'", 
			"'>:'", "'?'", "':'", "')'", "'('", "'['", "']'", "'0'", null, "'$'", 
			"'@'", "'*'", "'true'", "'false'", "'null'", null, "'\\'", null, "'''", 
			"'\"'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, "EXPONENT", 
			"LCALPHA", "UCALPHA", "HI_ASCII", "NON_ASCII", "SPACE", "UNDERSCORE", 
			"MINUS", "MINUS_ZERO", "PLUS", "DOT", "DOTDOT", "COMMA", "OR", "AND", 
			"EQ", "NE", "LT", "LE", "GT", "GE", "QUEST", "COLON", "RPAR", "LPAR", 
			"LSQ", "RSQ", "ZERO", "NON_0_DIGIT", "DOLLAR", "AT", "STAR", "TRUE", 
			"FALSE", "NULL", "WS", "ESC", "ESCAPED", "SQUOT", "DQUOT"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "Path.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public PathParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class JsonpathQueryContext extends ParserRuleContext {
		public TerminalNode DOLLAR() { return getToken(PathParser.DOLLAR, 0); }
		public SegmentsContext segments() {
			return getRuleContext(SegmentsContext.class,0);
		}
		public JsonpathQueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_jsonpathQuery; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterJsonpathQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitJsonpathQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitJsonpathQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JsonpathQueryContext jsonpathQuery() throws RecognitionException {
		JsonpathQueryContext _localctx = new JsonpathQueryContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_jsonpathQuery);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(104);
			match(DOLLAR);
			setState(105);
			segments();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SegmentsContext extends ParserRuleContext {
		public List<SegmentContext> segment() {
			return getRuleContexts(SegmentContext.class);
		}
		public SegmentContext segment(int i) {
			return getRuleContext(SegmentContext.class,i);
		}
		public SegmentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_segments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterSegments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitSegments(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitSegments(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SegmentsContext segments() throws RecognitionException {
		SegmentsContext _localctx = new SegmentsContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_segments);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(110);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 34362884096L) != 0)) {
				{
				{
				setState(107);
				segment();
				}
				}
				setState(112);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SelectorContext extends ParserRuleContext {
		public NameSelectorContext nameSelector() {
			return getRuleContext(NameSelectorContext.class,0);
		}
		public TerminalNode STAR() { return getToken(PathParser.STAR, 0); }
		public SliceSelectorContext sliceSelector() {
			return getRuleContext(SliceSelectorContext.class,0);
		}
		public IndexSelectorContext indexSelector() {
			return getRuleContext(IndexSelectorContext.class,0);
		}
		public FilterSelectorContext filterSelector() {
			return getRuleContext(FilterSelectorContext.class,0);
		}
		public SelectorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selector; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterSelector(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitSelector(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitSelector(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SelectorContext selector() throws RecognitionException {
		SelectorContext _localctx = new SelectorContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_selector);
		try {
			setState(118);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(113);
				nameSelector();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(114);
				match(STAR);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(115);
				sliceSelector();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(116);
				indexSelector();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(117);
				filterSelector();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NameSelectorContext extends ParserRuleContext {
		public StringLiteralContext stringLiteral() {
			return getRuleContext(StringLiteralContext.class,0);
		}
		public NameSelectorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nameSelector; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterNameSelector(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitNameSelector(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitNameSelector(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NameSelectorContext nameSelector() throws RecognitionException {
		NameSelectorContext _localctx = new NameSelectorContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_nameSelector);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(120);
			stringLiteral();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StringLiteralContext extends ParserRuleContext {
		public List<TerminalNode> DQUOT() { return getTokens(PathParser.DQUOT); }
		public TerminalNode DQUOT(int i) {
			return getToken(PathParser.DQUOT, i);
		}
		public List<DoubleQuotedContext> doubleQuoted() {
			return getRuleContexts(DoubleQuotedContext.class);
		}
		public DoubleQuotedContext doubleQuoted(int i) {
			return getRuleContext(DoubleQuotedContext.class,i);
		}
		public List<TerminalNode> SQUOT() { return getTokens(PathParser.SQUOT); }
		public TerminalNode SQUOT(int i) {
			return getToken(PathParser.SQUOT, i);
		}
		public List<SingleQuotedContext> singleQuoted() {
			return getRuleContexts(SingleQuotedContext.class);
		}
		public SingleQuotedContext singleQuoted(int i) {
			return getRuleContext(SingleQuotedContext.class,i);
		}
		public StringLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stringLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterStringLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitStringLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitStringLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StringLiteralContext stringLiteral() throws RecognitionException {
		StringLiteralContext _localctx = new StringLiteralContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_stringLiteral);
		int _la;
		try {
			setState(138);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DQUOT:
				enterOuterAlt(_localctx, 1);
				{
				setState(122);
				match(DQUOT);
				setState(126);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 496977785651198L) != 0)) {
					{
					{
					setState(123);
					doubleQuoted();
					}
					}
					setState(128);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(129);
				match(DQUOT);
				}
				break;
			case SQUOT:
				enterOuterAlt(_localctx, 2);
				{
				setState(130);
				match(SQUOT);
				setState(134);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 778452762361854L) != 0)) {
					{
					{
					setState(131);
					singleQuoted();
					}
					}
					setState(136);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(137);
				match(SQUOT);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DoubleQuotedContext extends ParserRuleContext {
		public UnescapedContext unescaped() {
			return getRuleContext(UnescapedContext.class,0);
		}
		public TerminalNode SQUOT() { return getToken(PathParser.SQUOT, 0); }
		public TerminalNode ESC() { return getToken(PathParser.ESC, 0); }
		public TerminalNode DQUOT() { return getToken(PathParser.DQUOT, 0); }
		public TerminalNode ESCAPED() { return getToken(PathParser.ESCAPED, 0); }
		public DoubleQuotedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_doubleQuoted; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterDoubleQuoted(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitDoubleQuoted(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitDoubleQuoted(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DoubleQuotedContext doubleQuoted() throws RecognitionException {
		DoubleQuotedContext _localctx = new DoubleQuotedContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_doubleQuoted);
		try {
			setState(145);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(140);
				unescaped();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(141);
				match(SQUOT);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(142);
				match(ESC);
				setState(143);
				match(DQUOT);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(144);
				match(ESCAPED);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SingleQuotedContext extends ParserRuleContext {
		public UnescapedContext unescaped() {
			return getRuleContext(UnescapedContext.class,0);
		}
		public TerminalNode DQUOT() { return getToken(PathParser.DQUOT, 0); }
		public TerminalNode ESC() { return getToken(PathParser.ESC, 0); }
		public TerminalNode SQUOT() { return getToken(PathParser.SQUOT, 0); }
		public TerminalNode ESCAPED() { return getToken(PathParser.ESCAPED, 0); }
		public SingleQuotedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleQuoted; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterSingleQuoted(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitSingleQuoted(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitSingleQuoted(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingleQuotedContext singleQuoted() throws RecognitionException {
		SingleQuotedContext _localctx = new SingleQuotedContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_singleQuoted);
		try {
			setState(152);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(147);
				unescaped();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(148);
				match(DQUOT);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(149);
				match(ESC);
				setState(150);
				match(SQUOT);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(151);
				match(ESCAPED);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class IndexSelectorContext extends ParserRuleContext {
		public IntContext int_() {
			return getRuleContext(IntContext.class,0);
		}
		public IndexSelectorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_indexSelector; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterIndexSelector(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitIndexSelector(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitIndexSelector(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IndexSelectorContext indexSelector() throws RecognitionException {
		IndexSelectorContext _localctx = new IndexSelectorContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_indexSelector);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(154);
			int_();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class IntContext extends ParserRuleContext {
		public TerminalNode ZERO() { return getToken(PathParser.ZERO, 0); }
		public TerminalNode NON_0_DIGIT() { return getToken(PathParser.NON_0_DIGIT, 0); }
		public TerminalNode MINUS() { return getToken(PathParser.MINUS, 0); }
		public List<DigitContext> digit() {
			return getRuleContexts(DigitContext.class);
		}
		public DigitContext digit(int i) {
			return getRuleContext(DigitContext.class,i);
		}
		public IntContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_int; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterInt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitInt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitInt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IntContext int_() throws RecognitionException {
		IntContext _localctx = new IntContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_int);
		int _la;
		try {
			setState(167);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ZERO:
				enterOuterAlt(_localctx, 1);
				{
				setState(156);
				match(ZERO);
				}
				break;
			case MINUS:
			case NON_0_DIGIT:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(158);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(157);
					match(MINUS);
					}
				}

				setState(160);
				match(NON_0_DIGIT);
				setState(164);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==ZERO || _la==NON_0_DIGIT) {
					{
					{
					setState(161);
					digit();
					}
					}
					setState(166);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SliceSelectorContext extends ParserRuleContext {
		public List<TerminalNode> COLON() { return getTokens(PathParser.COLON); }
		public TerminalNode COLON(int i) {
			return getToken(PathParser.COLON, i);
		}
		public StartContext start() {
			return getRuleContext(StartContext.class,0);
		}
		public EndContext end() {
			return getRuleContext(EndContext.class,0);
		}
		public StepContext step() {
			return getRuleContext(StepContext.class,0);
		}
		public SliceSelectorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sliceSelector; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterSliceSelector(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitSliceSelector(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitSliceSelector(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SliceSelectorContext sliceSelector() throws RecognitionException {
		SliceSelectorContext _localctx = new SliceSelectorContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_sliceSelector);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(170);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 412316991488L) != 0)) {
				{
				setState(169);
				start();
				}
			}

			setState(172);
			match(COLON);
			setState(174);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 412316991488L) != 0)) {
				{
				setState(173);
				end();
				}
			}

			setState(180);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLON) {
				{
				setState(176);
				match(COLON);
				setState(178);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 412316991488L) != 0)) {
					{
					setState(177);
					step();
					}
				}

				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StartContext extends ParserRuleContext {
		public IntContext int_() {
			return getRuleContext(IntContext.class,0);
		}
		public StartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_start; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterStart(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitStart(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitStart(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StartContext start() throws RecognitionException {
		StartContext _localctx = new StartContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_start);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(182);
			int_();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class EndContext extends ParserRuleContext {
		public IntContext int_() {
			return getRuleContext(IntContext.class,0);
		}
		public EndContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_end; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterEnd(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitEnd(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitEnd(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EndContext end() throws RecognitionException {
		EndContext _localctx = new EndContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_end);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(184);
			int_();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StepContext extends ParserRuleContext {
		public IntContext int_() {
			return getRuleContext(IntContext.class,0);
		}
		public StepContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_step; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterStep(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitStep(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitStep(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StepContext step() throws RecognitionException {
		StepContext _localctx = new StepContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_step);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(186);
			int_();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FilterSelectorContext extends ParserRuleContext {
		public TerminalNode QUEST() { return getToken(PathParser.QUEST, 0); }
		public LogicalExprContext logicalExpr() {
			return getRuleContext(LogicalExprContext.class,0);
		}
		public FilterSelectorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filterSelector; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterFilterSelector(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitFilterSelector(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitFilterSelector(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterSelectorContext filterSelector() throws RecognitionException {
		FilterSelectorContext _localctx = new FilterSelectorContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_filterSelector);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(188);
			match(QUEST);
			setState(189);
			logicalExpr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LogicalExprContext extends ParserRuleContext {
		public LogicalOrExprContext logicalOrExpr() {
			return getRuleContext(LogicalOrExprContext.class,0);
		}
		public LogicalExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_logicalExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterLogicalExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitLogicalExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitLogicalExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LogicalExprContext logicalExpr() throws RecognitionException {
		LogicalExprContext _localctx = new LogicalExprContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_logicalExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(191);
			logicalOrExpr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LogicalOrExprContext extends ParserRuleContext {
		public List<LogicalAndExprContext> logicalAndExpr() {
			return getRuleContexts(LogicalAndExprContext.class);
		}
		public LogicalAndExprContext logicalAndExpr(int i) {
			return getRuleContext(LogicalAndExprContext.class,i);
		}
		public List<TerminalNode> OR() { return getTokens(PathParser.OR); }
		public TerminalNode OR(int i) {
			return getToken(PathParser.OR, i);
		}
		public LogicalOrExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_logicalOrExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterLogicalOrExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitLogicalOrExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitLogicalOrExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LogicalOrExprContext logicalOrExpr() throws RecognitionException {
		LogicalOrExprContext _localctx = new LogicalOrExprContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_logicalOrExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(193);
			logicalAndExpr();
			setState(198);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==OR) {
				{
				{
				setState(194);
				match(OR);
				setState(195);
				logicalAndExpr();
				}
				}
				setState(200);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LogicalAndExprContext extends ParserRuleContext {
		public List<BasicExprContext> basicExpr() {
			return getRuleContexts(BasicExprContext.class);
		}
		public BasicExprContext basicExpr(int i) {
			return getRuleContext(BasicExprContext.class,i);
		}
		public List<TerminalNode> AND() { return getTokens(PathParser.AND); }
		public TerminalNode AND(int i) {
			return getToken(PathParser.AND, i);
		}
		public LogicalAndExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_logicalAndExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterLogicalAndExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitLogicalAndExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitLogicalAndExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LogicalAndExprContext logicalAndExpr() throws RecognitionException {
		LogicalAndExprContext _localctx = new LogicalAndExprContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_logicalAndExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(201);
			basicExpr();
			setState(206);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AND) {
				{
				{
				setState(202);
				match(AND);
				setState(203);
				basicExpr();
				}
				}
				setState(208);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class BasicExprContext extends ParserRuleContext {
		public ParenExprContext parenExpr() {
			return getRuleContext(ParenExprContext.class,0);
		}
		public ComparisonExprContext comparisonExpr() {
			return getRuleContext(ComparisonExprContext.class,0);
		}
		public TestExprContext testExpr() {
			return getRuleContext(TestExprContext.class,0);
		}
		public BasicExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_basicExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterBasicExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitBasicExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitBasicExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BasicExprContext basicExpr() throws RecognitionException {
		BasicExprContext _localctx = new BasicExprContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_basicExpr);
		try {
			setState(212);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(209);
				parenExpr();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(210);
				comparisonExpr();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(211);
				testExpr();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ParenExprContext extends ParserRuleContext {
		public TerminalNode LPAR() { return getToken(PathParser.LPAR, 0); }
		public LogicalExprContext logicalExpr() {
			return getRuleContext(LogicalExprContext.class,0);
		}
		public TerminalNode RPAR() { return getToken(PathParser.RPAR, 0); }
		public LogicalNotOpContext logicalNotOp() {
			return getRuleContext(LogicalNotOpContext.class,0);
		}
		public ParenExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parenExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterParenExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitParenExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitParenExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ParenExprContext parenExpr() throws RecognitionException {
		ParenExprContext _localctx = new ParenExprContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_parenExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(215);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__0) {
				{
				setState(214);
				logicalNotOp();
				}
			}

			setState(217);
			match(LPAR);
			setState(218);
			logicalExpr();
			setState(219);
			match(RPAR);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LogicalNotOpContext extends ParserRuleContext {
		public LogicalNotOpContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_logicalNotOp; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterLogicalNotOp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitLogicalNotOp(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitLogicalNotOp(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LogicalNotOpContext logicalNotOp() throws RecognitionException {
		LogicalNotOpContext _localctx = new LogicalNotOpContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_logicalNotOp);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(221);
			match(T__0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TestExprContext extends ParserRuleContext {
		public FilterQueryContext filterQuery() {
			return getRuleContext(FilterQueryContext.class,0);
		}
		public FunctionExprContext functionExpr() {
			return getRuleContext(FunctionExprContext.class,0);
		}
		public LogicalNotOpContext logicalNotOp() {
			return getRuleContext(LogicalNotOpContext.class,0);
		}
		public TestExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_testExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterTestExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitTestExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitTestExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TestExprContext testExpr() throws RecognitionException {
		TestExprContext _localctx = new TestExprContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_testExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(224);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__0) {
				{
				setState(223);
				logicalNotOp();
				}
			}

			setState(228);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DOLLAR:
			case AT:
				{
				setState(226);
				filterQuery();
				}
				break;
			case EXPONENT:
			case LCALPHA:
			case UNDERSCORE:
			case ZERO:
			case NON_0_DIGIT:
				{
				setState(227);
				functionExpr();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FilterQueryContext extends ParserRuleContext {
		public RelQueryContext relQuery() {
			return getRuleContext(RelQueryContext.class,0);
		}
		public JsonpathQueryContext jsonpathQuery() {
			return getRuleContext(JsonpathQueryContext.class,0);
		}
		public FilterQueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filterQuery; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterFilterQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitFilterQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitFilterQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterQueryContext filterQuery() throws RecognitionException {
		FilterQueryContext _localctx = new FilterQueryContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_filterQuery);
		try {
			setState(232);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case AT:
				enterOuterAlt(_localctx, 1);
				{
				setState(230);
				relQuery();
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(231);
				jsonpathQuery();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RelQueryContext extends ParserRuleContext {
		public TerminalNode AT() { return getToken(PathParser.AT, 0); }
		public SegmentsContext segments() {
			return getRuleContext(SegmentsContext.class,0);
		}
		public RelQueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_relQuery; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterRelQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitRelQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitRelQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RelQueryContext relQuery() throws RecognitionException {
		RelQueryContext _localctx = new RelQueryContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_relQuery);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(234);
			match(AT);
			setState(235);
			segments();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ComparisonExprContext extends ParserRuleContext {
		public List<ComparableContext> comparable() {
			return getRuleContexts(ComparableContext.class);
		}
		public ComparableContext comparable(int i) {
			return getRuleContext(ComparableContext.class,i);
		}
		public ComparisonOpContext comparisonOp() {
			return getRuleContext(ComparisonOpContext.class,0);
		}
		public ComparisonExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comparisonExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterComparisonExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitComparisonExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitComparisonExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComparisonExprContext comparisonExpr() throws RecognitionException {
		ComparisonExprContext _localctx = new ComparisonExprContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_comparisonExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(237);
			comparable();
			setState(238);
			comparisonOp();
			setState(239);
			comparable();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LiteralContext extends ParserRuleContext {
		public NumberContext number() {
			return getRuleContext(NumberContext.class,0);
		}
		public StringLiteralContext stringLiteral() {
			return getRuleContext(StringLiteralContext.class,0);
		}
		public TerminalNode TRUE() { return getToken(PathParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(PathParser.FALSE, 0); }
		public TerminalNode NULL() { return getToken(PathParser.NULL, 0); }
		public LiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LiteralContext literal() throws RecognitionException {
		LiteralContext _localctx = new LiteralContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_literal);
		try {
			setState(246);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case MINUS:
			case MINUS_ZERO:
			case ZERO:
			case NON_0_DIGIT:
				enterOuterAlt(_localctx, 1);
				{
				setState(241);
				number();
				}
				break;
			case SQUOT:
			case DQUOT:
				enterOuterAlt(_localctx, 2);
				{
				setState(242);
				stringLiteral();
				}
				break;
			case TRUE:
				enterOuterAlt(_localctx, 3);
				{
				setState(243);
				match(TRUE);
				}
				break;
			case FALSE:
				enterOuterAlt(_localctx, 4);
				{
				setState(244);
				match(FALSE);
				}
				break;
			case NULL:
				enterOuterAlt(_localctx, 5);
				{
				setState(245);
				match(NULL);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ComparableContext extends ParserRuleContext {
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public SingularQueryContext singularQuery() {
			return getRuleContext(SingularQueryContext.class,0);
		}
		public FunctionExprContext functionExpr() {
			return getRuleContext(FunctionExprContext.class,0);
		}
		public ComparableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comparable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterComparable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitComparable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitComparable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComparableContext comparable() throws RecognitionException {
		ComparableContext _localctx = new ComparableContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_comparable);
		try {
			setState(251);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,22,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(248);
				literal();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(249);
				singularQuery();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(250);
				functionExpr();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ComparisonOpContext extends ParserRuleContext {
		public TerminalNode EQ() { return getToken(PathParser.EQ, 0); }
		public TerminalNode NE() { return getToken(PathParser.NE, 0); }
		public TerminalNode LE() { return getToken(PathParser.LE, 0); }
		public TerminalNode GE() { return getToken(PathParser.GE, 0); }
		public TerminalNode LT() { return getToken(PathParser.LT, 0); }
		public TerminalNode GT() { return getToken(PathParser.GT, 0); }
		public ComparisonOpContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comparisonOp; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterComparisonOp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitComparisonOp(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitComparisonOp(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComparisonOpContext comparisonOp() throws RecognitionException {
		ComparisonOpContext _localctx = new ComparisonOpContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_comparisonOp);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(253);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 2113929216L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SingularQueryContext extends ParserRuleContext {
		public RelSingularQueryContext relSingularQuery() {
			return getRuleContext(RelSingularQueryContext.class,0);
		}
		public AbsSingularQueryContext absSingularQuery() {
			return getRuleContext(AbsSingularQueryContext.class,0);
		}
		public SingularQueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singularQuery; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterSingularQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitSingularQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitSingularQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingularQueryContext singularQuery() throws RecognitionException {
		SingularQueryContext _localctx = new SingularQueryContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_singularQuery);
		try {
			setState(257);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case AT:
				enterOuterAlt(_localctx, 1);
				{
				setState(255);
				relSingularQuery();
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(256);
				absSingularQuery();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RelSingularQueryContext extends ParserRuleContext {
		public TerminalNode AT() { return getToken(PathParser.AT, 0); }
		public SingularQuerySegmentsContext singularQuerySegments() {
			return getRuleContext(SingularQuerySegmentsContext.class,0);
		}
		public RelSingularQueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_relSingularQuery; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterRelSingularQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitRelSingularQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitRelSingularQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RelSingularQueryContext relSingularQuery() throws RecognitionException {
		RelSingularQueryContext _localctx = new RelSingularQueryContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_relSingularQuery);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(259);
			match(AT);
			setState(260);
			singularQuerySegments();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AbsSingularQueryContext extends ParserRuleContext {
		public TerminalNode DOLLAR() { return getToken(PathParser.DOLLAR, 0); }
		public SingularQuerySegmentsContext singularQuerySegments() {
			return getRuleContext(SingularQuerySegmentsContext.class,0);
		}
		public AbsSingularQueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_absSingularQuery; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterAbsSingularQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitAbsSingularQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitAbsSingularQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AbsSingularQueryContext absSingularQuery() throws RecognitionException {
		AbsSingularQueryContext _localctx = new AbsSingularQueryContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_absSingularQuery);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(262);
			match(DOLLAR);
			setState(263);
			singularQuerySegments();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SingularQuerySegmentsContext extends ParserRuleContext {
		public List<NameSegmentContext> nameSegment() {
			return getRuleContexts(NameSegmentContext.class);
		}
		public NameSegmentContext nameSegment(int i) {
			return getRuleContext(NameSegmentContext.class,i);
		}
		public List<IndexSegmentContext> indexSegment() {
			return getRuleContexts(IndexSegmentContext.class);
		}
		public IndexSegmentContext indexSegment(int i) {
			return getRuleContext(IndexSegmentContext.class,i);
		}
		public SingularQuerySegmentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singularQuerySegments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterSingularQuerySegments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitSingularQuerySegments(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitSingularQuerySegments(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SingularQuerySegmentsContext singularQuerySegments() throws RecognitionException {
		SingularQuerySegmentsContext _localctx = new SingularQuerySegmentsContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_singularQuerySegments);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(269);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT || _la==LSQ) {
				{
				setState(267);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
				case 1:
					{
					setState(265);
					nameSegment();
					}
					break;
				case 2:
					{
					setState(266);
					indexSegment();
					}
					break;
				}
				}
				setState(271);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NameSegmentContext extends ParserRuleContext {
		public TerminalNode LSQ() { return getToken(PathParser.LSQ, 0); }
		public NameSelectorContext nameSelector() {
			return getRuleContext(NameSelectorContext.class,0);
		}
		public TerminalNode RSQ() { return getToken(PathParser.RSQ, 0); }
		public TerminalNode DOT() { return getToken(PathParser.DOT, 0); }
		public MemberNameShorthandContext memberNameShorthand() {
			return getRuleContext(MemberNameShorthandContext.class,0);
		}
		public NameSegmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nameSegment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterNameSegment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitNameSegment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitNameSegment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NameSegmentContext nameSegment() throws RecognitionException {
		NameSegmentContext _localctx = new NameSegmentContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_nameSegment);
		try {
			setState(278);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LSQ:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(272);
				match(LSQ);
				setState(273);
				nameSelector();
				setState(274);
				match(RSQ);
				}
				}
				break;
			case DOT:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(276);
				match(DOT);
				setState(277);
				memberNameShorthand();
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class IndexSegmentContext extends ParserRuleContext {
		public TerminalNode LSQ() { return getToken(PathParser.LSQ, 0); }
		public IndexSelectorContext indexSelector() {
			return getRuleContext(IndexSelectorContext.class,0);
		}
		public TerminalNode RSQ() { return getToken(PathParser.RSQ, 0); }
		public IndexSegmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_indexSegment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterIndexSegment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitIndexSegment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitIndexSegment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IndexSegmentContext indexSegment() throws RecognitionException {
		IndexSegmentContext _localctx = new IndexSegmentContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_indexSegment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(280);
			match(LSQ);
			setState(281);
			indexSelector();
			setState(282);
			match(RSQ);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NumberContext extends ParserRuleContext {
		public IntContext int_() {
			return getRuleContext(IntContext.class,0);
		}
		public TerminalNode MINUS_ZERO() { return getToken(PathParser.MINUS_ZERO, 0); }
		public FracContext frac() {
			return getRuleContext(FracContext.class,0);
		}
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public NumberContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_number; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterNumber(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitNumber(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitNumber(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NumberContext number() throws RecognitionException {
		NumberContext _localctx = new NumberContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_number);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(286);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case MINUS:
			case ZERO:
			case NON_0_DIGIT:
				{
				setState(284);
				int_();
				}
				break;
			case MINUS_ZERO:
				{
				setState(285);
				match(MINUS_ZERO);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(289);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(288);
				frac();
				}
			}

			setState(292);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EXPONENT) {
				{
				setState(291);
				exp();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FracContext extends ParserRuleContext {
		public TerminalNode DOT() { return getToken(PathParser.DOT, 0); }
		public List<DigitContext> digit() {
			return getRuleContexts(DigitContext.class);
		}
		public DigitContext digit(int i) {
			return getRuleContext(DigitContext.class,i);
		}
		public FracContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_frac; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterFrac(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitFrac(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitFrac(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FracContext frac() throws RecognitionException {
		FracContext _localctx = new FracContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_frac);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(294);
			match(DOT);
			setState(296); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(295);
				digit();
				}
				}
				setState(298); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==ZERO || _la==NON_0_DIGIT );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExpContext extends ParserRuleContext {
		public TerminalNode EXPONENT() { return getToken(PathParser.EXPONENT, 0); }
		public List<DigitContext> digit() {
			return getRuleContexts(DigitContext.class);
		}
		public DigitContext digit(int i) {
			return getRuleContext(DigitContext.class,i);
		}
		public TerminalNode MINUS() { return getToken(PathParser.MINUS, 0); }
		public TerminalNode PLUS() { return getToken(PathParser.PLUS, 0); }
		public ExpContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exp; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterExp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitExp(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitExp(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpContext exp() throws RecognitionException {
		ExpContext _localctx = new ExpContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_exp);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(300);
			match(EXPONENT);
			setState(302);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==MINUS || _la==PLUS) {
				{
				setState(301);
				_la = _input.LA(1);
				if ( !(_la==MINUS || _la==PLUS) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			setState(305); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(304);
				digit();
				}
				}
				setState(307); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==ZERO || _la==NON_0_DIGIT );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionNameContext extends ParserRuleContext {
		public FunctionNameCharContext functionNameChar() {
			return getRuleContext(FunctionNameCharContext.class,0);
		}
		public List<FunctionNameFirstContext> functionNameFirst() {
			return getRuleContexts(FunctionNameFirstContext.class);
		}
		public FunctionNameFirstContext functionNameFirst(int i) {
			return getRuleContext(FunctionNameFirstContext.class,i);
		}
		public FunctionNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterFunctionName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitFunctionName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitFunctionName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionNameContext functionName() throws RecognitionException {
		FunctionNameContext _localctx = new FunctionNameContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_functionName);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(312);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,33,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(309);
					functionNameFirst();
					}
					} 
				}
				setState(314);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,33,_ctx);
			}
			setState(315);
			functionNameChar();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionNameFirstContext extends ParserRuleContext {
		public LowerContext lower() {
			return getRuleContext(LowerContext.class,0);
		}
		public FunctionNameFirstContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionNameFirst; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterFunctionNameFirst(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitFunctionNameFirst(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitFunctionNameFirst(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionNameFirstContext functionNameFirst() throws RecognitionException {
		FunctionNameFirstContext _localctx = new FunctionNameFirstContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_functionNameFirst);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(317);
			lower();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionNameCharContext extends ParserRuleContext {
		public FunctionNameFirstContext functionNameFirst() {
			return getRuleContext(FunctionNameFirstContext.class,0);
		}
		public TerminalNode UNDERSCORE() { return getToken(PathParser.UNDERSCORE, 0); }
		public DigitContext digit() {
			return getRuleContext(DigitContext.class,0);
		}
		public FunctionNameCharContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionNameChar; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterFunctionNameChar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitFunctionNameChar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitFunctionNameChar(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionNameCharContext functionNameChar() throws RecognitionException {
		FunctionNameCharContext _localctx = new FunctionNameCharContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_functionNameChar);
		try {
			setState(322);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EXPONENT:
			case LCALPHA:
				enterOuterAlt(_localctx, 1);
				{
				setState(319);
				functionNameFirst();
				}
				break;
			case UNDERSCORE:
				enterOuterAlt(_localctx, 2);
				{
				setState(320);
				match(UNDERSCORE);
				}
				break;
			case ZERO:
			case NON_0_DIGIT:
				enterOuterAlt(_localctx, 3);
				{
				setState(321);
				digit();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionExprContext extends ParserRuleContext {
		public FunctionNameContext functionName() {
			return getRuleContext(FunctionNameContext.class,0);
		}
		public TerminalNode LPAR() { return getToken(PathParser.LPAR, 0); }
		public TerminalNode RPAR() { return getToken(PathParser.RPAR, 0); }
		public List<FunctionArgumentContext> functionArgument() {
			return getRuleContexts(FunctionArgumentContext.class);
		}
		public FunctionArgumentContext functionArgument(int i) {
			return getRuleContext(FunctionArgumentContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(PathParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(PathParser.COMMA, i);
		}
		public FunctionExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterFunctionExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitFunctionExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitFunctionExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionExprContext functionExpr() throws RecognitionException {
		FunctionExprContext _localctx = new FunctionExprContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_functionExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(324);
			functionName();
			setState(325);
			match(LPAR);
			setState(334);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 877290020342786L) != 0)) {
				{
				setState(326);
				functionArgument();
				setState(331);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(327);
					match(COMMA);
					setState(328);
					functionArgument();
					}
					}
					setState(333);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(336);
			match(RPAR);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionArgumentContext extends ParserRuleContext {
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public FilterQueryContext filterQuery() {
			return getRuleContext(FilterQueryContext.class,0);
		}
		public LogicalExprContext logicalExpr() {
			return getRuleContext(LogicalExprContext.class,0);
		}
		public FunctionExprContext functionExpr() {
			return getRuleContext(FunctionExprContext.class,0);
		}
		public FunctionArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionArgument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterFunctionArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitFunctionArgument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitFunctionArgument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionArgumentContext functionArgument() throws RecognitionException {
		FunctionArgumentContext _localctx = new FunctionArgumentContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_functionArgument);
		try {
			setState(342);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,37,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(338);
				literal();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(339);
				filterQuery();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(340);
				logicalExpr();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(341);
				functionExpr();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SegmentContext extends ParserRuleContext {
		public ChildSegmentContext childSegment() {
			return getRuleContext(ChildSegmentContext.class,0);
		}
		public DescendantSegmentContext descendantSegment() {
			return getRuleContext(DescendantSegmentContext.class,0);
		}
		public SegmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_segment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterSegment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitSegment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitSegment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SegmentContext segment() throws RecognitionException {
		SegmentContext _localctx = new SegmentContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_segment);
		try {
			setState(346);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DOT:
			case LSQ:
				enterOuterAlt(_localctx, 1);
				{
				setState(344);
				childSegment();
				}
				break;
			case DOTDOT:
				enterOuterAlt(_localctx, 2);
				{
				setState(345);
				descendantSegment();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ChildSegmentContext extends ParserRuleContext {
		public BracketedSelectionContext bracketedSelection() {
			return getRuleContext(BracketedSelectionContext.class,0);
		}
		public TerminalNode DOT() { return getToken(PathParser.DOT, 0); }
		public TerminalNode STAR() { return getToken(PathParser.STAR, 0); }
		public MemberNameShorthandContext memberNameShorthand() {
			return getRuleContext(MemberNameShorthandContext.class,0);
		}
		public ChildSegmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_childSegment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterChildSegment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitChildSegment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitChildSegment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ChildSegmentContext childSegment() throws RecognitionException {
		ChildSegmentContext _localctx = new ChildSegmentContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_childSegment);
		try {
			setState(354);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LSQ:
				enterOuterAlt(_localctx, 1);
				{
				setState(348);
				bracketedSelection();
				}
				break;
			case DOT:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(349);
				match(DOT);
				setState(352);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case STAR:
					{
					setState(350);
					match(STAR);
					}
					break;
				case EXPONENT:
				case LCALPHA:
				case UCALPHA:
				case NON_ASCII:
				case UNDERSCORE:
				case ZERO:
				case NON_0_DIGIT:
					{
					setState(351);
					memberNameShorthand();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class BracketedSelectionContext extends ParserRuleContext {
		public TerminalNode LSQ() { return getToken(PathParser.LSQ, 0); }
		public List<SelectorContext> selector() {
			return getRuleContexts(SelectorContext.class);
		}
		public SelectorContext selector(int i) {
			return getRuleContext(SelectorContext.class,i);
		}
		public TerminalNode RSQ() { return getToken(PathParser.RSQ, 0); }
		public List<TerminalNode> COMMA() { return getTokens(PathParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(PathParser.COMMA, i);
		}
		public BracketedSelectionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bracketedSelection; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterBracketedSelection(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitBracketedSelection(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitBracketedSelection(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BracketedSelectionContext bracketedSelection() throws RecognitionException {
		BracketedSelectionContext _localctx = new BracketedSelectionContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_bracketedSelection);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(356);
			match(LSQ);
			setState(357);
			selector();
			setState(362);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(358);
				match(COMMA);
				setState(359);
				selector();
				}
				}
				setState(364);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(365);
			match(RSQ);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MemberNameShorthandContext extends ParserRuleContext {
		public NameCharContext nameChar() {
			return getRuleContext(NameCharContext.class,0);
		}
		public List<NameFirstContext> nameFirst() {
			return getRuleContexts(NameFirstContext.class);
		}
		public NameFirstContext nameFirst(int i) {
			return getRuleContext(NameFirstContext.class,i);
		}
		public MemberNameShorthandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_memberNameShorthand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterMemberNameShorthand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitMemberNameShorthand(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitMemberNameShorthand(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MemberNameShorthandContext memberNameShorthand() throws RecognitionException {
		MemberNameShorthandContext _localctx = new MemberNameShorthandContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_memberNameShorthand);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(370);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,42,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(367);
					nameFirst();
					}
					} 
				}
				setState(372);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,42,_ctx);
			}
			setState(373);
			nameChar();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NameFirstContext extends ParserRuleContext {
		public AlphaContext alpha() {
			return getRuleContext(AlphaContext.class,0);
		}
		public TerminalNode UNDERSCORE() { return getToken(PathParser.UNDERSCORE, 0); }
		public TerminalNode NON_ASCII() { return getToken(PathParser.NON_ASCII, 0); }
		public NameFirstContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nameFirst; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterNameFirst(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitNameFirst(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitNameFirst(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NameFirstContext nameFirst() throws RecognitionException {
		NameFirstContext _localctx = new NameFirstContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_nameFirst);
		try {
			setState(378);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EXPONENT:
			case LCALPHA:
			case UCALPHA:
				enterOuterAlt(_localctx, 1);
				{
				setState(375);
				alpha();
				}
				break;
			case UNDERSCORE:
				enterOuterAlt(_localctx, 2);
				{
				setState(376);
				match(UNDERSCORE);
				}
				break;
			case NON_ASCII:
				enterOuterAlt(_localctx, 3);
				{
				setState(377);
				match(NON_ASCII);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NameCharContext extends ParserRuleContext {
		public NameFirstContext nameFirst() {
			return getRuleContext(NameFirstContext.class,0);
		}
		public DigitContext digit() {
			return getRuleContext(DigitContext.class,0);
		}
		public NameCharContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nameChar; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterNameChar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitNameChar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitNameChar(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NameCharContext nameChar() throws RecognitionException {
		NameCharContext _localctx = new NameCharContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_nameChar);
		try {
			setState(382);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EXPONENT:
			case LCALPHA:
			case UCALPHA:
			case NON_ASCII:
			case UNDERSCORE:
				enterOuterAlt(_localctx, 1);
				{
				setState(380);
				nameFirst();
				}
				break;
			case ZERO:
			case NON_0_DIGIT:
				enterOuterAlt(_localctx, 2);
				{
				setState(381);
				digit();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DescendantSegmentContext extends ParserRuleContext {
		public TerminalNode DOTDOT() { return getToken(PathParser.DOTDOT, 0); }
		public BracketedSelectionContext bracketedSelection() {
			return getRuleContext(BracketedSelectionContext.class,0);
		}
		public TerminalNode STAR() { return getToken(PathParser.STAR, 0); }
		public MemberNameShorthandContext memberNameShorthand() {
			return getRuleContext(MemberNameShorthandContext.class,0);
		}
		public DescendantSegmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_descendantSegment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterDescendantSegment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitDescendantSegment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitDescendantSegment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DescendantSegmentContext descendantSegment() throws RecognitionException {
		DescendantSegmentContext _localctx = new DescendantSegmentContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_descendantSegment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(384);
			match(DOTDOT);
			setState(388);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LSQ:
				{
				setState(385);
				bracketedSelection();
				}
				break;
			case STAR:
				{
				setState(386);
				match(STAR);
				}
				break;
			case EXPONENT:
			case LCALPHA:
			case UCALPHA:
			case NON_ASCII:
			case UNDERSCORE:
			case ZERO:
			case NON_0_DIGIT:
				{
				setState(387);
				memberNameShorthand();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DigitContext extends ParserRuleContext {
		public TerminalNode ZERO() { return getToken(PathParser.ZERO, 0); }
		public TerminalNode NON_0_DIGIT() { return getToken(PathParser.NON_0_DIGIT, 0); }
		public DigitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_digit; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterDigit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitDigit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitDigit(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DigitContext digit() throws RecognitionException {
		DigitContext _localctx = new DigitContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_digit);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(390);
			_la = _input.LA(1);
			if ( !(_la==ZERO || _la==NON_0_DIGIT) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UnescapedContext extends ParserRuleContext {
		public TerminalNode SPACE() { return getToken(PathParser.SPACE, 0); }
		public TerminalNode DOLLAR() { return getToken(PathParser.DOLLAR, 0); }
		public TerminalNode LPAR() { return getToken(PathParser.LPAR, 0); }
		public TerminalNode RPAR() { return getToken(PathParser.RPAR, 0); }
		public TerminalNode STAR() { return getToken(PathParser.STAR, 0); }
		public TerminalNode PLUS() { return getToken(PathParser.PLUS, 0); }
		public TerminalNode COMMA() { return getToken(PathParser.COMMA, 0); }
		public TerminalNode MINUS() { return getToken(PathParser.MINUS, 0); }
		public TerminalNode DOT() { return getToken(PathParser.DOT, 0); }
		public DigitContext digit() {
			return getRuleContext(DigitContext.class,0);
		}
		public TerminalNode COLON() { return getToken(PathParser.COLON, 0); }
		public TerminalNode LT() { return getToken(PathParser.LT, 0); }
		public TerminalNode GT() { return getToken(PathParser.GT, 0); }
		public TerminalNode QUEST() { return getToken(PathParser.QUEST, 0); }
		public TerminalNode AT() { return getToken(PathParser.AT, 0); }
		public TerminalNode UCALPHA() { return getToken(PathParser.UCALPHA, 0); }
		public TerminalNode LSQ() { return getToken(PathParser.LSQ, 0); }
		public TerminalNode ESC() { return getToken(PathParser.ESC, 0); }
		public TerminalNode RSQ() { return getToken(PathParser.RSQ, 0); }
		public TerminalNode UNDERSCORE() { return getToken(PathParser.UNDERSCORE, 0); }
		public LowerContext lower() {
			return getRuleContext(LowerContext.class,0);
		}
		public TerminalNode HI_ASCII() { return getToken(PathParser.HI_ASCII, 0); }
		public TerminalNode NON_ASCII() { return getToken(PathParser.NON_ASCII, 0); }
		public TerminalNode MINUS_ZERO() { return getToken(PathParser.MINUS_ZERO, 0); }
		public UnescapedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unescaped; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterUnescaped(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitUnescaped(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitUnescaped(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnescapedContext unescaped() throws RecognitionException {
		UnescapedContext _localctx = new UnescapedContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_unescaped);
		try {
			setState(425);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SPACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(392);
				match(SPACE);
				}
				break;
			case T__0:
				enterOuterAlt(_localctx, 2);
				{
				setState(393);
				match(T__0);
				}
				break;
			case T__1:
				enterOuterAlt(_localctx, 3);
				{
				setState(394);
				match(T__1);
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 4);
				{
				setState(395);
				match(DOLLAR);
				}
				break;
			case T__2:
				enterOuterAlt(_localctx, 5);
				{
				setState(396);
				match(T__2);
				}
				break;
			case T__3:
				enterOuterAlt(_localctx, 6);
				{
				setState(397);
				match(T__3);
				}
				break;
			case LPAR:
				enterOuterAlt(_localctx, 7);
				{
				setState(398);
				match(LPAR);
				}
				break;
			case RPAR:
				enterOuterAlt(_localctx, 8);
				{
				setState(399);
				match(RPAR);
				}
				break;
			case STAR:
				enterOuterAlt(_localctx, 9);
				{
				setState(400);
				match(STAR);
				}
				break;
			case PLUS:
				enterOuterAlt(_localctx, 10);
				{
				setState(401);
				match(PLUS);
				}
				break;
			case COMMA:
				enterOuterAlt(_localctx, 11);
				{
				setState(402);
				match(COMMA);
				}
				break;
			case MINUS:
				enterOuterAlt(_localctx, 12);
				{
				setState(403);
				match(MINUS);
				}
				break;
			case DOT:
				enterOuterAlt(_localctx, 13);
				{
				setState(404);
				match(DOT);
				}
				break;
			case T__4:
				enterOuterAlt(_localctx, 14);
				{
				setState(405);
				match(T__4);
				}
				break;
			case ZERO:
			case NON_0_DIGIT:
				enterOuterAlt(_localctx, 15);
				{
				setState(406);
				digit();
				}
				break;
			case COLON:
				enterOuterAlt(_localctx, 16);
				{
				setState(407);
				match(COLON);
				}
				break;
			case T__5:
				enterOuterAlt(_localctx, 17);
				{
				setState(408);
				match(T__5);
				}
				break;
			case LT:
				enterOuterAlt(_localctx, 18);
				{
				setState(409);
				match(LT);
				}
				break;
			case T__6:
				enterOuterAlt(_localctx, 19);
				{
				setState(410);
				match(T__6);
				}
				break;
			case GT:
				enterOuterAlt(_localctx, 20);
				{
				setState(411);
				match(GT);
				}
				break;
			case QUEST:
				enterOuterAlt(_localctx, 21);
				{
				setState(412);
				match(QUEST);
				}
				break;
			case AT:
				enterOuterAlt(_localctx, 22);
				{
				setState(413);
				match(AT);
				}
				break;
			case UCALPHA:
				enterOuterAlt(_localctx, 23);
				{
				setState(414);
				match(UCALPHA);
				}
				break;
			case LSQ:
				enterOuterAlt(_localctx, 24);
				{
				setState(415);
				match(LSQ);
				}
				break;
			case ESC:
				enterOuterAlt(_localctx, 25);
				{
				setState(416);
				match(ESC);
				}
				break;
			case RSQ:
				enterOuterAlt(_localctx, 26);
				{
				setState(417);
				match(RSQ);
				}
				break;
			case T__7:
				enterOuterAlt(_localctx, 27);
				{
				setState(418);
				match(T__7);
				}
				break;
			case UNDERSCORE:
				enterOuterAlt(_localctx, 28);
				{
				setState(419);
				match(UNDERSCORE);
				}
				break;
			case T__8:
				enterOuterAlt(_localctx, 29);
				{
				setState(420);
				match(T__8);
				}
				break;
			case EXPONENT:
			case LCALPHA:
				enterOuterAlt(_localctx, 30);
				{
				setState(421);
				lower();
				}
				break;
			case HI_ASCII:
				enterOuterAlt(_localctx, 31);
				{
				setState(422);
				match(HI_ASCII);
				}
				break;
			case NON_ASCII:
				enterOuterAlt(_localctx, 32);
				{
				setState(423);
				match(NON_ASCII);
				}
				break;
			case MINUS_ZERO:
				enterOuterAlt(_localctx, 33);
				{
				setState(424);
				match(MINUS_ZERO);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AlphaContext extends ParserRuleContext {
		public TerminalNode UCALPHA() { return getToken(PathParser.UCALPHA, 0); }
		public LowerContext lower() {
			return getRuleContext(LowerContext.class,0);
		}
		public AlphaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alpha; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterAlpha(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitAlpha(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitAlpha(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AlphaContext alpha() throws RecognitionException {
		AlphaContext _localctx = new AlphaContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_alpha);
		try {
			setState(429);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case UCALPHA:
				enterOuterAlt(_localctx, 1);
				{
				setState(427);
				match(UCALPHA);
				}
				break;
			case EXPONENT:
			case LCALPHA:
				enterOuterAlt(_localctx, 2);
				{
				setState(428);
				lower();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LowerContext extends ParserRuleContext {
		public TerminalNode LCALPHA() { return getToken(PathParser.LCALPHA, 0); }
		public TerminalNode EXPONENT() { return getToken(PathParser.EXPONENT, 0); }
		public LowerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lower; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterLower(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitLower(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitLower(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LowerContext lower() throws RecognitionException {
		LowerContext _localctx = new LowerContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_lower);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(431);
			_la = _input.LA(1);
			if ( !(_la==EXPONENT || _la==LCALPHA) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\u0004\u00011\u01b2\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015"+
		"\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007\u0018"+
		"\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007\u001b"+
		"\u0002\u001c\u0007\u001c\u0002\u001d\u0007\u001d\u0002\u001e\u0007\u001e"+
		"\u0002\u001f\u0007\u001f\u0002 \u0007 \u0002!\u0007!\u0002\"\u0007\"\u0002"+
		"#\u0007#\u0002$\u0007$\u0002%\u0007%\u0002&\u0007&\u0002\'\u0007\'\u0002"+
		"(\u0007(\u0002)\u0007)\u0002*\u0007*\u0002+\u0007+\u0002,\u0007,\u0002"+
		"-\u0007-\u0002.\u0007.\u0002/\u0007/\u00020\u00070\u00021\u00071\u0002"+
		"2\u00072\u00023\u00073\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0001"+
		"\u0005\u0001m\b\u0001\n\u0001\f\u0001p\t\u0001\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u0002w\b\u0002\u0001\u0003"+
		"\u0001\u0003\u0001\u0004\u0001\u0004\u0005\u0004}\b\u0004\n\u0004\f\u0004"+
		"\u0080\t\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0005\u0004\u0085\b"+
		"\u0004\n\u0004\f\u0004\u0088\t\u0004\u0001\u0004\u0003\u0004\u008b\b\u0004"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0003\u0005"+
		"\u0092\b\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0003\u0006\u0099\b\u0006\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0003"+
		"\b\u009f\b\b\u0001\b\u0001\b\u0005\b\u00a3\b\b\n\b\f\b\u00a6\t\b\u0003"+
		"\b\u00a8\b\b\u0001\t\u0003\t\u00ab\b\t\u0001\t\u0001\t\u0003\t\u00af\b"+
		"\t\u0001\t\u0001\t\u0003\t\u00b3\b\t\u0003\t\u00b5\b\t\u0001\n\u0001\n"+
		"\u0001\u000b\u0001\u000b\u0001\f\u0001\f\u0001\r\u0001\r\u0001\r\u0001"+
		"\u000e\u0001\u000e\u0001\u000f\u0001\u000f\u0001\u000f\u0005\u000f\u00c5"+
		"\b\u000f\n\u000f\f\u000f\u00c8\t\u000f\u0001\u0010\u0001\u0010\u0001\u0010"+
		"\u0005\u0010\u00cd\b\u0010\n\u0010\f\u0010\u00d0\t\u0010\u0001\u0011\u0001"+
		"\u0011\u0001\u0011\u0003\u0011\u00d5\b\u0011\u0001\u0012\u0003\u0012\u00d8"+
		"\b\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0013\u0001"+
		"\u0013\u0001\u0014\u0003\u0014\u00e1\b\u0014\u0001\u0014\u0001\u0014\u0003"+
		"\u0014\u00e5\b\u0014\u0001\u0015\u0001\u0015\u0003\u0015\u00e9\b\u0015"+
		"\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0017\u0001\u0017\u0001\u0017"+
		"\u0001\u0017\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018"+
		"\u0003\u0018\u00f7\b\u0018\u0001\u0019\u0001\u0019\u0001\u0019\u0003\u0019"+
		"\u00fc\b\u0019\u0001\u001a\u0001\u001a\u0001\u001b\u0001\u001b\u0003\u001b"+
		"\u0102\b\u001b\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001d\u0001\u001d"+
		"\u0001\u001d\u0001\u001e\u0001\u001e\u0005\u001e\u010c\b\u001e\n\u001e"+
		"\f\u001e\u010f\t\u001e\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f"+
		"\u0001\u001f\u0001\u001f\u0003\u001f\u0117\b\u001f\u0001 \u0001 \u0001"+
		" \u0001 \u0001!\u0001!\u0003!\u011f\b!\u0001!\u0003!\u0122\b!\u0001!\u0003"+
		"!\u0125\b!\u0001\"\u0001\"\u0004\"\u0129\b\"\u000b\"\f\"\u012a\u0001#"+
		"\u0001#\u0003#\u012f\b#\u0001#\u0004#\u0132\b#\u000b#\f#\u0133\u0001$"+
		"\u0005$\u0137\b$\n$\f$\u013a\t$\u0001$\u0001$\u0001%\u0001%\u0001&\u0001"+
		"&\u0001&\u0003&\u0143\b&\u0001\'\u0001\'\u0001\'\u0001\'\u0001\'\u0005"+
		"\'\u014a\b\'\n\'\f\'\u014d\t\'\u0003\'\u014f\b\'\u0001\'\u0001\'\u0001"+
		"(\u0001(\u0001(\u0001(\u0003(\u0157\b(\u0001)\u0001)\u0003)\u015b\b)\u0001"+
		"*\u0001*\u0001*\u0001*\u0003*\u0161\b*\u0003*\u0163\b*\u0001+\u0001+\u0001"+
		"+\u0001+\u0005+\u0169\b+\n+\f+\u016c\t+\u0001+\u0001+\u0001,\u0005,\u0171"+
		"\b,\n,\f,\u0174\t,\u0001,\u0001,\u0001-\u0001-\u0001-\u0003-\u017b\b-"+
		"\u0001.\u0001.\u0003.\u017f\b.\u0001/\u0001/\u0001/\u0001/\u0003/\u0185"+
		"\b/\u00010\u00010\u00011\u00011\u00011\u00011\u00011\u00011\u00011\u0001"+
		"1\u00011\u00011\u00011\u00011\u00011\u00011\u00011\u00011\u00011\u0001"+
		"1\u00011\u00011\u00011\u00011\u00011\u00011\u00011\u00011\u00011\u0001"+
		"1\u00011\u00011\u00011\u00011\u00011\u00031\u01aa\b1\u00012\u00012\u0003"+
		"2\u01ae\b2\u00013\u00013\u00013\u0000\u00004\u0000\u0002\u0004\u0006\b"+
		"\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e \"$&(*,.02"+
		"468:<>@BDFHJLNPRTVXZ\\^`bdf\u0000\u0004\u0001\u0000\u0019\u001e\u0002"+
		"\u0000\u0011\u0011\u0013\u0013\u0001\u0000%&\u0001\u0000\n\u000b\u01dd"+
		"\u0000h\u0001\u0000\u0000\u0000\u0002n\u0001\u0000\u0000\u0000\u0004v"+
		"\u0001\u0000\u0000\u0000\u0006x\u0001\u0000\u0000\u0000\b\u008a\u0001"+
		"\u0000\u0000\u0000\n\u0091\u0001\u0000\u0000\u0000\f\u0098\u0001\u0000"+
		"\u0000\u0000\u000e\u009a\u0001\u0000\u0000\u0000\u0010\u00a7\u0001\u0000"+
		"\u0000\u0000\u0012\u00aa\u0001\u0000\u0000\u0000\u0014\u00b6\u0001\u0000"+
		"\u0000\u0000\u0016\u00b8\u0001\u0000\u0000\u0000\u0018\u00ba\u0001\u0000"+
		"\u0000\u0000\u001a\u00bc\u0001\u0000\u0000\u0000\u001c\u00bf\u0001\u0000"+
		"\u0000\u0000\u001e\u00c1\u0001\u0000\u0000\u0000 \u00c9\u0001\u0000\u0000"+
		"\u0000\"\u00d4\u0001\u0000\u0000\u0000$\u00d7\u0001\u0000\u0000\u0000"+
		"&\u00dd\u0001\u0000\u0000\u0000(\u00e0\u0001\u0000\u0000\u0000*\u00e8"+
		"\u0001\u0000\u0000\u0000,\u00ea\u0001\u0000\u0000\u0000.\u00ed\u0001\u0000"+
		"\u0000\u00000\u00f6\u0001\u0000\u0000\u00002\u00fb\u0001\u0000\u0000\u0000"+
		"4\u00fd\u0001\u0000\u0000\u00006\u0101\u0001\u0000\u0000\u00008\u0103"+
		"\u0001\u0000\u0000\u0000:\u0106\u0001\u0000\u0000\u0000<\u010d\u0001\u0000"+
		"\u0000\u0000>\u0116\u0001\u0000\u0000\u0000@\u0118\u0001\u0000\u0000\u0000"+
		"B\u011e\u0001\u0000\u0000\u0000D\u0126\u0001\u0000\u0000\u0000F\u012c"+
		"\u0001\u0000\u0000\u0000H\u0138\u0001\u0000\u0000\u0000J\u013d\u0001\u0000"+
		"\u0000\u0000L\u0142\u0001\u0000\u0000\u0000N\u0144\u0001\u0000\u0000\u0000"+
		"P\u0156\u0001\u0000\u0000\u0000R\u015a\u0001\u0000\u0000\u0000T\u0162"+
		"\u0001\u0000\u0000\u0000V\u0164\u0001\u0000\u0000\u0000X\u0172\u0001\u0000"+
		"\u0000\u0000Z\u017a\u0001\u0000\u0000\u0000\\\u017e\u0001\u0000\u0000"+
		"\u0000^\u0180\u0001\u0000\u0000\u0000`\u0186\u0001\u0000\u0000\u0000b"+
		"\u01a9\u0001\u0000\u0000\u0000d\u01ad\u0001\u0000\u0000\u0000f\u01af\u0001"+
		"\u0000\u0000\u0000hi\u0005\'\u0000\u0000ij\u0003\u0002\u0001\u0000j\u0001"+
		"\u0001\u0000\u0000\u0000km\u0003R)\u0000lk\u0001\u0000\u0000\u0000mp\u0001"+
		"\u0000\u0000\u0000nl\u0001\u0000\u0000\u0000no\u0001\u0000\u0000\u0000"+
		"o\u0003\u0001\u0000\u0000\u0000pn\u0001\u0000\u0000\u0000qw\u0003\u0006"+
		"\u0003\u0000rw\u0005)\u0000\u0000sw\u0003\u0012\t\u0000tw\u0003\u000e"+
		"\u0007\u0000uw\u0003\u001a\r\u0000vq\u0001\u0000\u0000\u0000vr\u0001\u0000"+
		"\u0000\u0000vs\u0001\u0000\u0000\u0000vt\u0001\u0000\u0000\u0000vu\u0001"+
		"\u0000\u0000\u0000w\u0005\u0001\u0000\u0000\u0000xy\u0003\b\u0004\u0000"+
		"y\u0007\u0001\u0000\u0000\u0000z~\u00051\u0000\u0000{}\u0003\n\u0005\u0000"+
		"|{\u0001\u0000\u0000\u0000}\u0080\u0001\u0000\u0000\u0000~|\u0001\u0000"+
		"\u0000\u0000~\u007f\u0001\u0000\u0000\u0000\u007f\u0081\u0001\u0000\u0000"+
		"\u0000\u0080~\u0001\u0000\u0000\u0000\u0081\u008b\u00051\u0000\u0000\u0082"+
		"\u0086\u00050\u0000\u0000\u0083\u0085\u0003\f\u0006\u0000\u0084\u0083"+
		"\u0001\u0000\u0000\u0000\u0085\u0088\u0001\u0000\u0000\u0000\u0086\u0084"+
		"\u0001\u0000\u0000\u0000\u0086\u0087\u0001\u0000\u0000\u0000\u0087\u0089"+
		"\u0001\u0000\u0000\u0000\u0088\u0086\u0001\u0000\u0000\u0000\u0089\u008b"+
		"\u00050\u0000\u0000\u008az\u0001\u0000\u0000\u0000\u008a\u0082\u0001\u0000"+
		"\u0000\u0000\u008b\t\u0001\u0000\u0000\u0000\u008c\u0092\u0003b1\u0000"+
		"\u008d\u0092\u00050\u0000\u0000\u008e\u008f\u0005.\u0000\u0000\u008f\u0092"+
		"\u00051\u0000\u0000\u0090\u0092\u0005/\u0000\u0000\u0091\u008c\u0001\u0000"+
		"\u0000\u0000\u0091\u008d\u0001\u0000\u0000\u0000\u0091\u008e\u0001\u0000"+
		"\u0000\u0000\u0091\u0090\u0001\u0000\u0000\u0000\u0092\u000b\u0001\u0000"+
		"\u0000\u0000\u0093\u0099\u0003b1\u0000\u0094\u0099\u00051\u0000\u0000"+
		"\u0095\u0096\u0005.\u0000\u0000\u0096\u0099\u00050\u0000\u0000\u0097\u0099"+
		"\u0005/\u0000\u0000\u0098\u0093\u0001\u0000\u0000\u0000\u0098\u0094\u0001"+
		"\u0000\u0000\u0000\u0098\u0095\u0001\u0000\u0000\u0000\u0098\u0097\u0001"+
		"\u0000\u0000\u0000\u0099\r\u0001\u0000\u0000\u0000\u009a\u009b\u0003\u0010"+
		"\b\u0000\u009b\u000f\u0001\u0000\u0000\u0000\u009c\u00a8\u0005%\u0000"+
		"\u0000\u009d\u009f\u0005\u0011\u0000\u0000\u009e\u009d\u0001\u0000\u0000"+
		"\u0000\u009e\u009f\u0001\u0000\u0000\u0000\u009f\u00a0\u0001\u0000\u0000"+
		"\u0000\u00a0\u00a4\u0005&\u0000\u0000\u00a1\u00a3\u0003`0\u0000\u00a2"+
		"\u00a1\u0001\u0000\u0000\u0000\u00a3\u00a6\u0001\u0000\u0000\u0000\u00a4"+
		"\u00a2\u0001\u0000\u0000\u0000\u00a4\u00a5\u0001\u0000\u0000\u0000\u00a5"+
		"\u00a8\u0001\u0000\u0000\u0000\u00a6\u00a4\u0001\u0000\u0000\u0000\u00a7"+
		"\u009c\u0001\u0000\u0000\u0000\u00a7\u009e\u0001\u0000\u0000\u0000\u00a8"+
		"\u0011\u0001\u0000\u0000\u0000\u00a9\u00ab\u0003\u0014\n\u0000\u00aa\u00a9"+
		"\u0001\u0000\u0000\u0000\u00aa\u00ab\u0001\u0000\u0000\u0000\u00ab\u00ac"+
		"\u0001\u0000\u0000\u0000\u00ac\u00ae\u0005 \u0000\u0000\u00ad\u00af\u0003"+
		"\u0016\u000b\u0000\u00ae\u00ad\u0001\u0000\u0000\u0000\u00ae\u00af\u0001"+
		"\u0000\u0000\u0000\u00af\u00b4\u0001\u0000\u0000\u0000\u00b0\u00b2\u0005"+
		" \u0000\u0000\u00b1\u00b3\u0003\u0018\f\u0000\u00b2\u00b1\u0001\u0000"+
		"\u0000\u0000\u00b2\u00b3\u0001\u0000\u0000\u0000\u00b3\u00b5\u0001\u0000"+
		"\u0000\u0000\u00b4\u00b0\u0001\u0000\u0000\u0000\u00b4\u00b5\u0001\u0000"+
		"\u0000\u0000\u00b5\u0013\u0001\u0000\u0000\u0000\u00b6\u00b7\u0003\u0010"+
		"\b\u0000\u00b7\u0015\u0001\u0000\u0000\u0000\u00b8\u00b9\u0003\u0010\b"+
		"\u0000\u00b9\u0017\u0001\u0000\u0000\u0000\u00ba\u00bb\u0003\u0010\b\u0000"+
		"\u00bb\u0019\u0001\u0000\u0000\u0000\u00bc\u00bd\u0005\u001f\u0000\u0000"+
		"\u00bd\u00be\u0003\u001c\u000e\u0000\u00be\u001b\u0001\u0000\u0000\u0000"+
		"\u00bf\u00c0\u0003\u001e\u000f\u0000\u00c0\u001d\u0001\u0000\u0000\u0000"+
		"\u00c1\u00c6\u0003 \u0010\u0000\u00c2\u00c3\u0005\u0017\u0000\u0000\u00c3"+
		"\u00c5\u0003 \u0010\u0000\u00c4\u00c2\u0001\u0000\u0000\u0000\u00c5\u00c8"+
		"\u0001\u0000\u0000\u0000\u00c6\u00c4\u0001\u0000\u0000\u0000\u00c6\u00c7"+
		"\u0001\u0000\u0000\u0000\u00c7\u001f\u0001\u0000\u0000\u0000\u00c8\u00c6"+
		"\u0001\u0000\u0000\u0000\u00c9\u00ce\u0003\"\u0011\u0000\u00ca\u00cb\u0005"+
		"\u0018\u0000\u0000\u00cb\u00cd\u0003\"\u0011\u0000\u00cc\u00ca\u0001\u0000"+
		"\u0000\u0000\u00cd\u00d0\u0001\u0000\u0000\u0000\u00ce\u00cc\u0001\u0000"+
		"\u0000\u0000\u00ce\u00cf\u0001\u0000\u0000\u0000\u00cf!\u0001\u0000\u0000"+
		"\u0000\u00d0\u00ce\u0001\u0000\u0000\u0000\u00d1\u00d5\u0003$\u0012\u0000"+
		"\u00d2\u00d5\u0003.\u0017\u0000\u00d3\u00d5\u0003(\u0014\u0000\u00d4\u00d1"+
		"\u0001\u0000\u0000\u0000\u00d4\u00d2\u0001\u0000\u0000\u0000\u00d4\u00d3"+
		"\u0001\u0000\u0000\u0000\u00d5#\u0001\u0000\u0000\u0000\u00d6\u00d8\u0003"+
		"&\u0013\u0000\u00d7\u00d6\u0001\u0000\u0000\u0000\u00d7\u00d8\u0001\u0000"+
		"\u0000\u0000\u00d8\u00d9\u0001\u0000\u0000\u0000\u00d9\u00da\u0005\"\u0000"+
		"\u0000\u00da\u00db\u0003\u001c\u000e\u0000\u00db\u00dc\u0005!\u0000\u0000"+
		"\u00dc%\u0001\u0000\u0000\u0000\u00dd\u00de\u0005\u0001\u0000\u0000\u00de"+
		"\'\u0001\u0000\u0000\u0000\u00df\u00e1\u0003&\u0013\u0000\u00e0\u00df"+
		"\u0001\u0000\u0000\u0000\u00e0\u00e1\u0001\u0000\u0000\u0000\u00e1\u00e4"+
		"\u0001\u0000\u0000\u0000\u00e2\u00e5\u0003*\u0015\u0000\u00e3\u00e5\u0003"+
		"N\'\u0000\u00e4\u00e2\u0001\u0000\u0000\u0000\u00e4\u00e3\u0001\u0000"+
		"\u0000\u0000\u00e5)\u0001\u0000\u0000\u0000\u00e6\u00e9\u0003,\u0016\u0000"+
		"\u00e7\u00e9\u0003\u0000\u0000\u0000\u00e8\u00e6\u0001\u0000\u0000\u0000"+
		"\u00e8\u00e7\u0001\u0000\u0000\u0000\u00e9+\u0001\u0000\u0000\u0000\u00ea"+
		"\u00eb\u0005(\u0000\u0000\u00eb\u00ec\u0003\u0002\u0001\u0000\u00ec-\u0001"+
		"\u0000\u0000\u0000\u00ed\u00ee\u00032\u0019\u0000\u00ee\u00ef\u00034\u001a"+
		"\u0000\u00ef\u00f0\u00032\u0019\u0000\u00f0/\u0001\u0000\u0000\u0000\u00f1"+
		"\u00f7\u0003B!\u0000\u00f2\u00f7\u0003\b\u0004\u0000\u00f3\u00f7\u0005"+
		"*\u0000\u0000\u00f4\u00f7\u0005+\u0000\u0000\u00f5\u00f7\u0005,\u0000"+
		"\u0000\u00f6\u00f1\u0001\u0000\u0000\u0000\u00f6\u00f2\u0001\u0000\u0000"+
		"\u0000\u00f6\u00f3\u0001\u0000\u0000\u0000\u00f6\u00f4\u0001\u0000\u0000"+
		"\u0000\u00f6\u00f5\u0001\u0000\u0000\u0000\u00f71\u0001\u0000\u0000\u0000"+
		"\u00f8\u00fc\u00030\u0018\u0000\u00f9\u00fc\u00036\u001b\u0000\u00fa\u00fc"+
		"\u0003N\'\u0000\u00fb\u00f8\u0001\u0000\u0000\u0000\u00fb\u00f9\u0001"+
		"\u0000\u0000\u0000\u00fb\u00fa\u0001\u0000\u0000\u0000\u00fc3\u0001\u0000"+
		"\u0000\u0000\u00fd\u00fe\u0007\u0000\u0000\u0000\u00fe5\u0001\u0000\u0000"+
		"\u0000\u00ff\u0102\u00038\u001c\u0000\u0100\u0102\u0003:\u001d\u0000\u0101"+
		"\u00ff\u0001\u0000\u0000\u0000\u0101\u0100\u0001\u0000\u0000\u0000\u0102"+
		"7\u0001\u0000\u0000\u0000\u0103\u0104\u0005(\u0000\u0000\u0104\u0105\u0003"+
		"<\u001e\u0000\u01059\u0001\u0000\u0000\u0000\u0106\u0107\u0005\'\u0000"+
		"\u0000\u0107\u0108\u0003<\u001e\u0000\u0108;\u0001\u0000\u0000\u0000\u0109"+
		"\u010c\u0003>\u001f\u0000\u010a\u010c\u0003@ \u0000\u010b\u0109\u0001"+
		"\u0000\u0000\u0000\u010b\u010a\u0001\u0000\u0000\u0000\u010c\u010f\u0001"+
		"\u0000\u0000\u0000\u010d\u010b\u0001\u0000\u0000\u0000\u010d\u010e\u0001"+
		"\u0000\u0000\u0000\u010e=\u0001\u0000\u0000\u0000\u010f\u010d\u0001\u0000"+
		"\u0000\u0000\u0110\u0111\u0005#\u0000\u0000\u0111\u0112\u0003\u0006\u0003"+
		"\u0000\u0112\u0113\u0005$\u0000\u0000\u0113\u0117\u0001\u0000\u0000\u0000"+
		"\u0114\u0115\u0005\u0014\u0000\u0000\u0115\u0117\u0003X,\u0000\u0116\u0110"+
		"\u0001\u0000\u0000\u0000\u0116\u0114\u0001\u0000\u0000\u0000\u0117?\u0001"+
		"\u0000\u0000\u0000\u0118\u0119\u0005#\u0000\u0000\u0119\u011a\u0003\u000e"+
		"\u0007\u0000\u011a\u011b\u0005$\u0000\u0000\u011bA\u0001\u0000\u0000\u0000"+
		"\u011c\u011f\u0003\u0010\b\u0000\u011d\u011f\u0005\u0012\u0000\u0000\u011e"+
		"\u011c\u0001\u0000\u0000\u0000\u011e\u011d\u0001\u0000\u0000\u0000\u011f"+
		"\u0121\u0001\u0000\u0000\u0000\u0120\u0122\u0003D\"\u0000\u0121\u0120"+
		"\u0001\u0000\u0000\u0000\u0121\u0122\u0001\u0000\u0000\u0000\u0122\u0124"+
		"\u0001\u0000\u0000\u0000\u0123\u0125\u0003F#\u0000\u0124\u0123\u0001\u0000"+
		"\u0000\u0000\u0124\u0125\u0001\u0000\u0000\u0000\u0125C\u0001\u0000\u0000"+
		"\u0000\u0126\u0128\u0005\u0014\u0000\u0000\u0127\u0129\u0003`0\u0000\u0128"+
		"\u0127\u0001\u0000\u0000\u0000\u0129\u012a\u0001\u0000\u0000\u0000\u012a"+
		"\u0128\u0001\u0000\u0000\u0000\u012a\u012b\u0001\u0000\u0000\u0000\u012b"+
		"E\u0001\u0000\u0000\u0000\u012c\u012e\u0005\n\u0000\u0000\u012d\u012f"+
		"\u0007\u0001\u0000\u0000\u012e\u012d\u0001\u0000\u0000\u0000\u012e\u012f"+
		"\u0001\u0000\u0000\u0000\u012f\u0131\u0001\u0000\u0000\u0000\u0130\u0132"+
		"\u0003`0\u0000\u0131\u0130\u0001\u0000\u0000\u0000\u0132\u0133\u0001\u0000"+
		"\u0000\u0000\u0133\u0131\u0001\u0000\u0000\u0000\u0133\u0134\u0001\u0000"+
		"\u0000\u0000\u0134G\u0001\u0000\u0000\u0000\u0135\u0137\u0003J%\u0000"+
		"\u0136\u0135\u0001\u0000\u0000\u0000\u0137\u013a\u0001\u0000\u0000\u0000"+
		"\u0138\u0136\u0001\u0000\u0000\u0000\u0138\u0139\u0001\u0000\u0000\u0000"+
		"\u0139\u013b\u0001\u0000\u0000\u0000\u013a\u0138\u0001\u0000\u0000\u0000"+
		"\u013b\u013c\u0003L&\u0000\u013cI\u0001\u0000\u0000\u0000\u013d\u013e"+
		"\u0003f3\u0000\u013eK\u0001\u0000\u0000\u0000\u013f\u0143\u0003J%\u0000"+
		"\u0140\u0143\u0005\u0010\u0000\u0000\u0141\u0143\u0003`0\u0000\u0142\u013f"+
		"\u0001\u0000\u0000\u0000\u0142\u0140\u0001\u0000\u0000\u0000\u0142\u0141"+
		"\u0001\u0000\u0000\u0000\u0143M\u0001\u0000\u0000\u0000\u0144\u0145\u0003"+
		"H$\u0000\u0145\u014e\u0005\"\u0000\u0000\u0146\u014b\u0003P(\u0000\u0147"+
		"\u0148\u0005\u0016\u0000\u0000\u0148\u014a\u0003P(\u0000\u0149\u0147\u0001"+
		"\u0000\u0000\u0000\u014a\u014d\u0001\u0000\u0000\u0000\u014b\u0149\u0001"+
		"\u0000\u0000\u0000\u014b\u014c\u0001\u0000\u0000\u0000\u014c\u014f\u0001"+
		"\u0000\u0000\u0000\u014d\u014b\u0001\u0000\u0000\u0000\u014e\u0146\u0001"+
		"\u0000\u0000\u0000\u014e\u014f\u0001\u0000\u0000\u0000\u014f\u0150\u0001"+
		"\u0000\u0000\u0000\u0150\u0151\u0005!\u0000\u0000\u0151O\u0001\u0000\u0000"+
		"\u0000\u0152\u0157\u00030\u0018\u0000\u0153\u0157\u0003*\u0015\u0000\u0154"+
		"\u0157\u0003\u001c\u000e\u0000\u0155\u0157\u0003N\'\u0000\u0156\u0152"+
		"\u0001\u0000\u0000\u0000\u0156\u0153\u0001\u0000\u0000\u0000\u0156\u0154"+
		"\u0001\u0000\u0000\u0000\u0156\u0155\u0001\u0000\u0000\u0000\u0157Q\u0001"+
		"\u0000\u0000\u0000\u0158\u015b\u0003T*\u0000\u0159\u015b\u0003^/\u0000"+
		"\u015a\u0158\u0001\u0000\u0000\u0000\u015a\u0159\u0001\u0000\u0000\u0000"+
		"\u015bS\u0001\u0000\u0000\u0000\u015c\u0163\u0003V+\u0000\u015d\u0160"+
		"\u0005\u0014\u0000\u0000\u015e\u0161\u0005)\u0000\u0000\u015f\u0161\u0003"+
		"X,\u0000\u0160\u015e\u0001\u0000\u0000\u0000\u0160\u015f\u0001\u0000\u0000"+
		"\u0000\u0161\u0163\u0001\u0000\u0000\u0000\u0162\u015c\u0001\u0000\u0000"+
		"\u0000\u0162\u015d\u0001\u0000\u0000\u0000\u0163U\u0001\u0000\u0000\u0000"+
		"\u0164\u0165\u0005#\u0000\u0000\u0165\u016a\u0003\u0004\u0002\u0000\u0166"+
		"\u0167\u0005\u0016\u0000\u0000\u0167\u0169\u0003\u0004\u0002\u0000\u0168"+
		"\u0166\u0001\u0000\u0000\u0000\u0169\u016c\u0001\u0000\u0000\u0000\u016a"+
		"\u0168\u0001\u0000\u0000\u0000\u016a\u016b\u0001\u0000\u0000\u0000\u016b"+
		"\u016d\u0001\u0000\u0000\u0000\u016c\u016a\u0001\u0000\u0000\u0000\u016d"+
		"\u016e\u0005$\u0000\u0000\u016eW\u0001\u0000\u0000\u0000\u016f\u0171\u0003"+
		"Z-\u0000\u0170\u016f\u0001\u0000\u0000\u0000\u0171\u0174\u0001\u0000\u0000"+
		"\u0000\u0172\u0170\u0001\u0000\u0000\u0000\u0172\u0173\u0001\u0000\u0000"+
		"\u0000\u0173\u0175\u0001\u0000\u0000\u0000\u0174\u0172\u0001\u0000\u0000"+
		"\u0000\u0175\u0176\u0003\\.\u0000\u0176Y\u0001\u0000\u0000\u0000\u0177"+
		"\u017b\u0003d2\u0000\u0178\u017b\u0005\u0010\u0000\u0000\u0179\u017b\u0005"+
		"\u000e\u0000\u0000\u017a\u0177\u0001\u0000\u0000\u0000\u017a\u0178\u0001"+
		"\u0000\u0000\u0000\u017a\u0179\u0001\u0000\u0000\u0000\u017b[\u0001\u0000"+
		"\u0000\u0000\u017c\u017f\u0003Z-\u0000\u017d\u017f\u0003`0\u0000\u017e"+
		"\u017c\u0001\u0000\u0000\u0000\u017e\u017d\u0001\u0000\u0000\u0000\u017f"+
		"]\u0001\u0000\u0000\u0000\u0180\u0184\u0005\u0015\u0000\u0000\u0181\u0185"+
		"\u0003V+\u0000\u0182\u0185\u0005)\u0000\u0000\u0183\u0185\u0003X,\u0000"+
		"\u0184\u0181\u0001\u0000\u0000\u0000\u0184\u0182\u0001\u0000\u0000\u0000"+
		"\u0184\u0183\u0001\u0000\u0000\u0000\u0185_\u0001\u0000\u0000\u0000\u0186"+
		"\u0187\u0007\u0002\u0000\u0000\u0187a\u0001\u0000\u0000\u0000\u0188\u01aa"+
		"\u0005\u000f\u0000\u0000\u0189\u01aa\u0005\u0001\u0000\u0000\u018a\u01aa"+
		"\u0005\u0002\u0000\u0000\u018b\u01aa\u0005\'\u0000\u0000\u018c\u01aa\u0005"+
		"\u0003\u0000\u0000\u018d\u01aa\u0005\u0004\u0000\u0000\u018e\u01aa\u0005"+
		"\"\u0000\u0000\u018f\u01aa\u0005!\u0000\u0000\u0190\u01aa\u0005)\u0000"+
		"\u0000\u0191\u01aa\u0005\u0013\u0000\u0000\u0192\u01aa\u0005\u0016\u0000"+
		"\u0000\u0193\u01aa\u0005\u0011\u0000\u0000\u0194\u01aa\u0005\u0014\u0000"+
		"\u0000\u0195\u01aa\u0005\u0005\u0000\u0000\u0196\u01aa\u0003`0\u0000\u0197"+
		"\u01aa\u0005 \u0000\u0000\u0198\u01aa\u0005\u0006\u0000\u0000\u0199\u01aa"+
		"\u0005\u001b\u0000\u0000\u019a\u01aa\u0005\u0007\u0000\u0000\u019b\u01aa"+
		"\u0005\u001d\u0000\u0000\u019c\u01aa\u0005\u001f\u0000\u0000\u019d\u01aa"+
		"\u0005(\u0000\u0000\u019e\u01aa\u0005\f\u0000\u0000\u019f\u01aa\u0005"+
		"#\u0000\u0000\u01a0\u01aa\u0005.\u0000\u0000\u01a1\u01aa\u0005$\u0000"+
		"\u0000\u01a2\u01aa\u0005\b\u0000\u0000\u01a3\u01aa\u0005\u0010\u0000\u0000"+
		"\u01a4\u01aa\u0005\t\u0000\u0000\u01a5\u01aa\u0003f3\u0000\u01a6\u01aa"+
		"\u0005\r\u0000\u0000\u01a7\u01aa\u0005\u000e\u0000\u0000\u01a8\u01aa\u0005"+
		"\u0012\u0000\u0000\u01a9\u0188\u0001\u0000\u0000\u0000\u01a9\u0189\u0001"+
		"\u0000\u0000\u0000\u01a9\u018a\u0001\u0000\u0000\u0000\u01a9\u018b\u0001"+
		"\u0000\u0000\u0000\u01a9\u018c\u0001\u0000\u0000\u0000\u01a9\u018d\u0001"+
		"\u0000\u0000\u0000\u01a9\u018e\u0001\u0000\u0000\u0000\u01a9\u018f\u0001"+
		"\u0000\u0000\u0000\u01a9\u0190\u0001\u0000\u0000\u0000\u01a9\u0191\u0001"+
		"\u0000\u0000\u0000\u01a9\u0192\u0001\u0000\u0000\u0000\u01a9\u0193\u0001"+
		"\u0000\u0000\u0000\u01a9\u0194\u0001\u0000\u0000\u0000\u01a9\u0195\u0001"+
		"\u0000\u0000\u0000\u01a9\u0196\u0001\u0000\u0000\u0000\u01a9\u0197\u0001"+
		"\u0000\u0000\u0000\u01a9\u0198\u0001\u0000\u0000\u0000\u01a9\u0199\u0001"+
		"\u0000\u0000\u0000\u01a9\u019a\u0001\u0000\u0000\u0000\u01a9\u019b\u0001"+
		"\u0000\u0000\u0000\u01a9\u019c\u0001\u0000\u0000\u0000\u01a9\u019d\u0001"+
		"\u0000\u0000\u0000\u01a9\u019e\u0001\u0000\u0000\u0000\u01a9\u019f\u0001"+
		"\u0000\u0000\u0000\u01a9\u01a0\u0001\u0000\u0000\u0000\u01a9\u01a1\u0001"+
		"\u0000\u0000\u0000\u01a9\u01a2\u0001\u0000\u0000\u0000\u01a9\u01a3\u0001"+
		"\u0000\u0000\u0000\u01a9\u01a4\u0001\u0000\u0000\u0000\u01a9\u01a5\u0001"+
		"\u0000\u0000\u0000\u01a9\u01a6\u0001\u0000\u0000\u0000\u01a9\u01a7\u0001"+
		"\u0000\u0000\u0000\u01a9\u01a8\u0001\u0000\u0000\u0000\u01aac\u0001\u0000"+
		"\u0000\u0000\u01ab\u01ae\u0005\f\u0000\u0000\u01ac\u01ae\u0003f3\u0000"+
		"\u01ad\u01ab\u0001\u0000\u0000\u0000\u01ad\u01ac\u0001\u0000\u0000\u0000"+
		"\u01aee\u0001\u0000\u0000\u0000\u01af\u01b0\u0007\u0003\u0000\u0000\u01b0"+
		"g\u0001\u0000\u0000\u00000nv~\u0086\u008a\u0091\u0098\u009e\u00a4\u00a7"+
		"\u00aa\u00ae\u00b2\u00b4\u00c6\u00ce\u00d4\u00d7\u00e0\u00e4\u00e8\u00f6"+
		"\u00fb\u0101\u010b\u010d\u0116\u011e\u0121\u0124\u012a\u012e\u0133\u0138"+
		"\u0142\u014b\u014e\u0156\u015a\u0160\u0162\u016a\u0172\u017a\u017e\u0184"+
		"\u01a9\u01ad";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}