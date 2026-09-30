// Generated from /home/tom/Devel/kroxy-dm/kroxylicious-transform-providers/kroxylicious-transform-provider-neo/src/main/antlr4/io/kroxylicious/transform/neo/Path.g4 by ANTLR 4.13.2
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
		DIGIT=1, INT=2, NUMBER=3, LOWER=4, STRING=5, NOT=6, UCALPHA=7, HI_ASCII=8, 
		NON_ASCII=9, UNDERSCORE=10, MINUS=11, PLUS=12, DOT=13, DOTDOT=14, COMMA=15, 
		OR=16, AND=17, EQ=18, NE=19, LT=20, LE=21, GT=22, GE=23, QUEST=24, COLON=25, 
		RPAR=26, LPAR=27, LSQ=28, RSQ=29, DOLLAR=30, AT=31, STAR=32, TRUE=33, 
		FALSE=34, NULL=35, B=36, SQUOT=37, DQUOT=38;
	public static final int
		RULE_jsonpathQuery = 0, RULE_segments = 1, RULE_segment = 2, RULE_childSegment = 3, 
		RULE_descendantSegment = 4, RULE_bracketedSelection = 5, RULE_selector = 6, 
		RULE_nameSelector = 7, RULE_indexSelector = 8, RULE_sliceSelector = 9, 
		RULE_start = 10, RULE_end = 11, RULE_step = 12, RULE_filterSelector = 13, 
		RULE_logicalExpr = 14, RULE_logicalOrExpr = 15, RULE_logicalAndExpr = 16, 
		RULE_basicExpr = 17, RULE_parenExpr = 18, RULE_logicalNotOp = 19, RULE_testExpr = 20, 
		RULE_filterQuery = 21, RULE_relQuery = 22, RULE_comparisonExpr = 23, RULE_literal = 24, 
		RULE_comparable = 25, RULE_comparisonOp = 26, RULE_singularQuery = 27, 
		RULE_relSingularQuery = 28, RULE_absSingularQuery = 29, RULE_singularQuerySegments = 30, 
		RULE_nameSegment = 31, RULE_indexSegment = 32, RULE_functionExpr = 33, 
		RULE_functionArgument = 34, RULE_stringLiteral = 35, RULE_booleanLiteral = 36, 
		RULE_numberLiteral = 37, RULE_intLiteral = 38, RULE_alpha = 39, RULE_s = 40, 
		RULE_memberNameShorthand = 41, RULE_nameFirst = 42, RULE_nameChar = 43, 
		RULE_functionName = 44, RULE_functionNameFirst = 45, RULE_functionNameChar = 46;
	private static String[] makeRuleNames() {
		return new String[] {
			"jsonpathQuery", "segments", "segment", "childSegment", "descendantSegment", 
			"bracketedSelection", "selector", "nameSelector", "indexSelector", "sliceSelector", 
			"start", "end", "step", "filterSelector", "logicalExpr", "logicalOrExpr", 
			"logicalAndExpr", "basicExpr", "parenExpr", "logicalNotOp", "testExpr", 
			"filterQuery", "relQuery", "comparisonExpr", "literal", "comparable", 
			"comparisonOp", "singularQuery", "relSingularQuery", "absSingularQuery", 
			"singularQuerySegments", "nameSegment", "indexSegment", "functionExpr", 
			"functionArgument", "stringLiteral", "booleanLiteral", "numberLiteral", 
			"intLiteral", "alpha", "s", "memberNameShorthand", "nameFirst", "nameChar", 
			"functionName", "functionNameFirst", "functionNameChar"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, null, "'!'", null, null, null, "'_'", "'-'", 
			"'+'", "'.'", "'..'", "','", "'||'", "'&&'", "'=='", "'!='", "'<'", "'<='", 
			"'>'", "'>='", "'?'", "':'", "')'", "'('", "'['", "']'", "'$'", "'@'", 
			"'*'", "'true'", "'false'", "'null'", null, "'''", "'\"'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "DIGIT", "INT", "NUMBER", "LOWER", "STRING", "NOT", "UCALPHA", 
			"HI_ASCII", "NON_ASCII", "UNDERSCORE", "MINUS", "PLUS", "DOT", "DOTDOT", 
			"COMMA", "OR", "AND", "EQ", "NE", "LT", "LE", "GT", "GE", "QUEST", "COLON", 
			"RPAR", "LPAR", "LSQ", "RSQ", "DOLLAR", "AT", "STAR", "TRUE", "FALSE", 
			"NULL", "B", "SQUOT", "DQUOT"
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
			setState(94);
			match(DOLLAR);
			setState(95);
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
		public List<SContext> s() {
			return getRuleContexts(SContext.class);
		}
		public SContext s(int i) {
			return getRuleContext(SContext.class,i);
		}
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
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(102);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(97);
					s();
					setState(98);
					segment();
					}
					} 
				}
				setState(104);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
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
		enterRule(_localctx, 4, RULE_segment);
		try {
			setState(107);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DOT:
			case LSQ:
				enterOuterAlt(_localctx, 1);
				{
				setState(105);
				childSegment();
				}
				break;
			case DOTDOT:
				enterOuterAlt(_localctx, 2);
				{
				setState(106);
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
		enterRule(_localctx, 6, RULE_childSegment);
		try {
			setState(115);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LSQ:
				enterOuterAlt(_localctx, 1);
				{
				setState(109);
				bracketedSelection();
				}
				break;
			case DOT:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(110);
				match(DOT);
				setState(113);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case STAR:
					{
					setState(111);
					match(STAR);
					}
					break;
				case LOWER:
				case UCALPHA:
				case NON_ASCII:
				case UNDERSCORE:
					{
					setState(112);
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
		enterRule(_localctx, 8, RULE_descendantSegment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(117);
			match(DOTDOT);
			setState(121);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LSQ:
				{
				setState(118);
				bracketedSelection();
				}
				break;
			case STAR:
				{
				setState(119);
				match(STAR);
				}
				break;
			case LOWER:
			case UCALPHA:
			case NON_ASCII:
			case UNDERSCORE:
				{
				setState(120);
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
	public static class BracketedSelectionContext extends ParserRuleContext {
		public TerminalNode LSQ() { return getToken(PathParser.LSQ, 0); }
		public List<SContext> s() {
			return getRuleContexts(SContext.class);
		}
		public SContext s(int i) {
			return getRuleContext(SContext.class,i);
		}
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
		enterRule(_localctx, 10, RULE_bracketedSelection);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(123);
			match(LSQ);
			setState(124);
			s();
			setState(125);
			selector();
			setState(133);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(126);
					s();
					setState(127);
					match(COMMA);
					setState(128);
					s();
					setState(129);
					selector();
					}
					} 
				}
				setState(135);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
			}
			setState(136);
			s();
			setState(137);
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
		enterRule(_localctx, 12, RULE_selector);
		try {
			setState(144);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(139);
				nameSelector();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(140);
				match(STAR);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(141);
				sliceSelector();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(142);
				indexSelector();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(143);
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
		enterRule(_localctx, 14, RULE_nameSelector);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(146);
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
	public static class IndexSelectorContext extends ParserRuleContext {
		public IntLiteralContext intLiteral() {
			return getRuleContext(IntLiteralContext.class,0);
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
		enterRule(_localctx, 16, RULE_indexSelector);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(148);
			intLiteral();
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
		public List<SContext> s() {
			return getRuleContexts(SContext.class);
		}
		public SContext s(int i) {
			return getRuleContext(SContext.class,i);
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
			setState(153);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DIGIT || _la==INT) {
				{
				setState(150);
				start();
				setState(151);
				s();
				}
			}

			setState(155);
			match(COLON);
			setState(156);
			s();
			setState(160);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DIGIT || _la==INT) {
				{
				setState(157);
				end();
				setState(158);
				s();
				}
			}

			setState(168);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLON) {
				{
				setState(162);
				match(COLON);
				setState(166);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
				case 1:
					{
					setState(163);
					s();
					setState(164);
					step();
					}
					break;
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
		public IntLiteralContext intLiteral() {
			return getRuleContext(IntLiteralContext.class,0);
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
			setState(170);
			intLiteral();
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
		public IntLiteralContext intLiteral() {
			return getRuleContext(IntLiteralContext.class,0);
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
			setState(172);
			intLiteral();
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
		public IntLiteralContext intLiteral() {
			return getRuleContext(IntLiteralContext.class,0);
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
			setState(174);
			intLiteral();
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
		public SContext s() {
			return getRuleContext(SContext.class,0);
		}
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
			setState(176);
			match(QUEST);
			setState(177);
			s();
			setState(178);
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
			setState(180);
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
		public List<SContext> s() {
			return getRuleContexts(SContext.class);
		}
		public SContext s(int i) {
			return getRuleContext(SContext.class,i);
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
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(182);
			logicalAndExpr();
			setState(190);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,11,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(183);
					s();
					setState(184);
					match(OR);
					setState(185);
					s();
					setState(186);
					logicalAndExpr();
					}
					} 
				}
				setState(192);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,11,_ctx);
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
		public List<SContext> s() {
			return getRuleContexts(SContext.class);
		}
		public SContext s(int i) {
			return getRuleContext(SContext.class,i);
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
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(193);
			basicExpr();
			setState(201);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(194);
					s();
					setState(195);
					match(AND);
					setState(196);
					s();
					setState(197);
					basicExpr();
					}
					} 
				}
				setState(203);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
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
			setState(207);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(204);
				parenExpr();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(205);
				comparisonExpr();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(206);
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
			setState(210);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NOT) {
				{
				setState(209);
				logicalNotOp();
				}
			}

			setState(212);
			match(LPAR);
			setState(213);
			logicalExpr();
			setState(214);
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
		public TerminalNode NOT() { return getToken(PathParser.NOT, 0); }
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
			setState(216);
			match(NOT);
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
		public SContext s() {
			return getRuleContext(SContext.class,0);
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
			setState(221);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NOT) {
				{
				setState(218);
				logicalNotOp();
				setState(219);
				s();
				}
			}

			setState(225);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DOLLAR:
			case AT:
				{
				setState(223);
				filterQuery();
				}
				break;
			case LOWER:
				{
				setState(224);
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
			setState(229);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case AT:
				enterOuterAlt(_localctx, 1);
				{
				setState(227);
				relQuery();
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(228);
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
			setState(231);
			match(AT);
			setState(232);
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
		public List<SContext> s() {
			return getRuleContexts(SContext.class);
		}
		public SContext s(int i) {
			return getRuleContext(SContext.class,i);
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
			setState(234);
			comparable();
			setState(235);
			s();
			setState(236);
			comparisonOp();
			setState(237);
			s();
			setState(238);
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
		public IntLiteralContext intLiteral() {
			return getRuleContext(IntLiteralContext.class,0);
		}
		public NumberLiteralContext numberLiteral() {
			return getRuleContext(NumberLiteralContext.class,0);
		}
		public StringLiteralContext stringLiteral() {
			return getRuleContext(StringLiteralContext.class,0);
		}
		public BooleanLiteralContext booleanLiteral() {
			return getRuleContext(BooleanLiteralContext.class,0);
		}
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
			setState(245);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DIGIT:
			case INT:
				enterOuterAlt(_localctx, 1);
				{
				setState(240);
				intLiteral();
				}
				break;
			case NUMBER:
				enterOuterAlt(_localctx, 2);
				{
				setState(241);
				numberLiteral();
				}
				break;
			case STRING:
				enterOuterAlt(_localctx, 3);
				{
				setState(242);
				stringLiteral();
				}
				break;
			case TRUE:
			case FALSE:
				enterOuterAlt(_localctx, 4);
				{
				setState(243);
				booleanLiteral();
				}
				break;
			case NULL:
				enterOuterAlt(_localctx, 5);
				{
				setState(244);
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
			setState(250);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DIGIT:
			case INT:
			case NUMBER:
			case STRING:
			case TRUE:
			case FALSE:
			case NULL:
				enterOuterAlt(_localctx, 1);
				{
				setState(247);
				literal();
				}
				break;
			case DOLLAR:
			case AT:
				enterOuterAlt(_localctx, 2);
				{
				setState(248);
				singularQuery();
				}
				break;
			case LOWER:
				enterOuterAlt(_localctx, 3);
				{
				setState(249);
				functionExpr();
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
			setState(252);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 16515072L) != 0)) ) {
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
			setState(256);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case AT:
				enterOuterAlt(_localctx, 1);
				{
				setState(254);
				relSingularQuery();
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(255);
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
			setState(258);
			match(AT);
			setState(259);
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
			setState(261);
			match(DOLLAR);
			setState(262);
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
		public List<SContext> s() {
			return getRuleContexts(SContext.class);
		}
		public SContext s(int i) {
			return getRuleContext(SContext.class,i);
		}
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
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(271);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(264);
					s();
					setState(267);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
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
					} 
				}
				setState(273);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
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
			setState(280);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LSQ:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(274);
				match(LSQ);
				setState(275);
				nameSelector();
				setState(276);
				match(RSQ);
				}
				}
				break;
			case DOT:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(278);
				match(DOT);
				setState(279);
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
			setState(282);
			match(LSQ);
			setState(283);
			indexSelector();
			setState(284);
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
	public static class FunctionExprContext extends ParserRuleContext {
		public FunctionNameContext functionName() {
			return getRuleContext(FunctionNameContext.class,0);
		}
		public TerminalNode LPAR() { return getToken(PathParser.LPAR, 0); }
		public List<SContext> s() {
			return getRuleContexts(SContext.class);
		}
		public SContext s(int i) {
			return getRuleContext(SContext.class,i);
		}
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
		enterRule(_localctx, 66, RULE_functionExpr);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(286);
			functionName();
			setState(287);
			match(LPAR);
			setState(300);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
			case 1:
				{
				setState(288);
				s();
				setState(289);
				functionArgument();
				setState(297);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,24,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(290);
						s();
						setState(291);
						match(COMMA);
						setState(292);
						s();
						setState(293);
						functionArgument();
						}
						} 
					}
					setState(299);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,24,_ctx);
				}
				}
				break;
			}
			setState(302);
			s();
			setState(303);
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
		enterRule(_localctx, 68, RULE_functionArgument);
		try {
			setState(309);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,26,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(305);
				literal();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(306);
				filterQuery();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(307);
				logicalExpr();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(308);
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
	public static class StringLiteralContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(PathParser.STRING, 0); }
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
		enterRule(_localctx, 70, RULE_stringLiteral);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(311);
			match(STRING);
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
	public static class BooleanLiteralContext extends ParserRuleContext {
		public TerminalNode TRUE() { return getToken(PathParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(PathParser.FALSE, 0); }
		public BooleanLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_booleanLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterBooleanLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitBooleanLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitBooleanLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BooleanLiteralContext booleanLiteral() throws RecognitionException {
		BooleanLiteralContext _localctx = new BooleanLiteralContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_booleanLiteral);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(313);
			_la = _input.LA(1);
			if ( !(_la==TRUE || _la==FALSE) ) {
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
	public static class NumberLiteralContext extends ParserRuleContext {
		public TerminalNode NUMBER() { return getToken(PathParser.NUMBER, 0); }
		public NumberLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_numberLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterNumberLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitNumberLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitNumberLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NumberLiteralContext numberLiteral() throws RecognitionException {
		NumberLiteralContext _localctx = new NumberLiteralContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_numberLiteral);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(315);
			match(NUMBER);
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
	public static class IntLiteralContext extends ParserRuleContext {
		public TerminalNode DIGIT() { return getToken(PathParser.DIGIT, 0); }
		public TerminalNode INT() { return getToken(PathParser.INT, 0); }
		public IntLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_intLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterIntLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitIntLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitIntLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IntLiteralContext intLiteral() throws RecognitionException {
		IntLiteralContext _localctx = new IntLiteralContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_intLiteral);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(317);
			_la = _input.LA(1);
			if ( !(_la==DIGIT || _la==INT) ) {
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
	public static class AlphaContext extends ParserRuleContext {
		public TerminalNode UCALPHA() { return getToken(PathParser.UCALPHA, 0); }
		public TerminalNode LOWER() { return getToken(PathParser.LOWER, 0); }
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
		enterRule(_localctx, 78, RULE_alpha);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(319);
			_la = _input.LA(1);
			if ( !(_la==LOWER || _la==UCALPHA) ) {
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
	public static class SContext extends ParserRuleContext {
		public List<TerminalNode> B() { return getTokens(PathParser.B); }
		public TerminalNode B(int i) {
			return getToken(PathParser.B, i);
		}
		public SContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_s; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).enterS(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PathListener ) ((PathListener)listener).exitS(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof PathVisitor ) return ((PathVisitor<? extends T>)visitor).visitS(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SContext s() throws RecognitionException {
		SContext _localctx = new SContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_s);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(324);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,27,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(321);
					match(B);
					}
					} 
				}
				setState(326);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,27,_ctx);
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
	public static class MemberNameShorthandContext extends ParserRuleContext {
		public NameFirstContext nameFirst() {
			return getRuleContext(NameFirstContext.class,0);
		}
		public List<NameCharContext> nameChar() {
			return getRuleContexts(NameCharContext.class);
		}
		public NameCharContext nameChar(int i) {
			return getRuleContext(NameCharContext.class,i);
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
		enterRule(_localctx, 82, RULE_memberNameShorthand);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(327);
			nameFirst();
			setState(331);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 1682L) != 0)) {
				{
				{
				setState(328);
				nameChar();
				}
				}
				setState(333);
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
		enterRule(_localctx, 84, RULE_nameFirst);
		try {
			setState(337);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LOWER:
			case UCALPHA:
				enterOuterAlt(_localctx, 1);
				{
				setState(334);
				alpha();
				}
				break;
			case UNDERSCORE:
				enterOuterAlt(_localctx, 2);
				{
				setState(335);
				match(UNDERSCORE);
				}
				break;
			case NON_ASCII:
				enterOuterAlt(_localctx, 3);
				{
				setState(336);
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
		public TerminalNode DIGIT() { return getToken(PathParser.DIGIT, 0); }
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
		enterRule(_localctx, 86, RULE_nameChar);
		try {
			setState(341);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LOWER:
			case UCALPHA:
			case NON_ASCII:
			case UNDERSCORE:
				enterOuterAlt(_localctx, 1);
				{
				setState(339);
				nameFirst();
				}
				break;
			case DIGIT:
				enterOuterAlt(_localctx, 2);
				{
				setState(340);
				match(DIGIT);
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
	public static class FunctionNameContext extends ParserRuleContext {
		public FunctionNameFirstContext functionNameFirst() {
			return getRuleContext(FunctionNameFirstContext.class,0);
		}
		public List<FunctionNameCharContext> functionNameChar() {
			return getRuleContexts(FunctionNameCharContext.class);
		}
		public FunctionNameCharContext functionNameChar(int i) {
			return getRuleContext(FunctionNameCharContext.class,i);
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
		enterRule(_localctx, 88, RULE_functionName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(343);
			functionNameFirst();
			setState(347);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 1042L) != 0)) {
				{
				{
				setState(344);
				functionNameChar();
				}
				}
				setState(349);
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
	public static class FunctionNameFirstContext extends ParserRuleContext {
		public TerminalNode LOWER() { return getToken(PathParser.LOWER, 0); }
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
		enterRule(_localctx, 90, RULE_functionNameFirst);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(350);
			match(LOWER);
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
		public TerminalNode DIGIT() { return getToken(PathParser.DIGIT, 0); }
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
		enterRule(_localctx, 92, RULE_functionNameChar);
		try {
			setState(355);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LOWER:
				enterOuterAlt(_localctx, 1);
				{
				setState(352);
				functionNameFirst();
				}
				break;
			case UNDERSCORE:
				enterOuterAlt(_localctx, 2);
				{
				setState(353);
				match(UNDERSCORE);
				}
				break;
			case DIGIT:
				enterOuterAlt(_localctx, 3);
				{
				setState(354);
				match(DIGIT);
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

	public static final String _serializedATN =
		"\u0004\u0001&\u0166\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
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
		"-\u0007-\u0002.\u0007.\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0005\u0001e\b\u0001\n\u0001\f\u0001h\t\u0001"+
		"\u0001\u0002\u0001\u0002\u0003\u0002l\b\u0002\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0003\u0003r\b\u0003\u0003\u0003t\b\u0003\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0003\u0004z\b\u0004\u0001"+
		"\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0005\u0005\u0084\b\u0005\n\u0005\f\u0005\u0087\t\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0003\u0006\u0091\b\u0006\u0001\u0007\u0001\u0007"+
		"\u0001\b\u0001\b\u0001\t\u0001\t\u0001\t\u0003\t\u009a\b\t\u0001\t\u0001"+
		"\t\u0001\t\u0001\t\u0001\t\u0003\t\u00a1\b\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0003\t\u00a7\b\t\u0003\t\u00a9\b\t\u0001\n\u0001\n\u0001\u000b\u0001"+
		"\u000b\u0001\f\u0001\f\u0001\r\u0001\r\u0001\r\u0001\r\u0001\u000e\u0001"+
		"\u000e\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001"+
		"\u000f\u0005\u000f\u00bd\b\u000f\n\u000f\f\u000f\u00c0\t\u000f\u0001\u0010"+
		"\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0005\u0010"+
		"\u00c8\b\u0010\n\u0010\f\u0010\u00cb\t\u0010\u0001\u0011\u0001\u0011\u0001"+
		"\u0011\u0003\u0011\u00d0\b\u0011\u0001\u0012\u0003\u0012\u00d3\b\u0012"+
		"\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0013\u0001\u0013"+
		"\u0001\u0014\u0001\u0014\u0001\u0014\u0003\u0014\u00de\b\u0014\u0001\u0014"+
		"\u0001\u0014\u0003\u0014\u00e2\b\u0014\u0001\u0015\u0001\u0015\u0003\u0015"+
		"\u00e6\b\u0015\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0017\u0001\u0017"+
		"\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0018\u0001\u0018"+
		"\u0001\u0018\u0001\u0018\u0001\u0018\u0003\u0018\u00f6\b\u0018\u0001\u0019"+
		"\u0001\u0019\u0001\u0019\u0003\u0019\u00fb\b\u0019\u0001\u001a\u0001\u001a"+
		"\u0001\u001b\u0001\u001b\u0003\u001b\u0101\b\u001b\u0001\u001c\u0001\u001c"+
		"\u0001\u001c\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001e\u0001\u001e"+
		"\u0001\u001e\u0003\u001e\u010c\b\u001e\u0005\u001e\u010e\b\u001e\n\u001e"+
		"\f\u001e\u0111\t\u001e\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f"+
		"\u0001\u001f\u0001\u001f\u0003\u001f\u0119\b\u001f\u0001 \u0001 \u0001"+
		" \u0001 \u0001!\u0001!\u0001!\u0001!\u0001!\u0001!\u0001!\u0001!\u0001"+
		"!\u0005!\u0128\b!\n!\f!\u012b\t!\u0003!\u012d\b!\u0001!\u0001!\u0001!"+
		"\u0001\"\u0001\"\u0001\"\u0001\"\u0003\"\u0136\b\"\u0001#\u0001#\u0001"+
		"$\u0001$\u0001%\u0001%\u0001&\u0001&\u0001\'\u0001\'\u0001(\u0005(\u0143"+
		"\b(\n(\f(\u0146\t(\u0001)\u0001)\u0005)\u014a\b)\n)\f)\u014d\t)\u0001"+
		"*\u0001*\u0001*\u0003*\u0152\b*\u0001+\u0001+\u0003+\u0156\b+\u0001,\u0001"+
		",\u0005,\u015a\b,\n,\f,\u015d\t,\u0001-\u0001-\u0001.\u0001.\u0001.\u0003"+
		".\u0164\b.\u0001.\u0000\u0000/\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010"+
		"\u0012\u0014\u0016\u0018\u001a\u001c\u001e \"$&(*,.02468:<>@BDFHJLNPR"+
		"TVXZ\\\u0000\u0004\u0001\u0000\u0012\u0017\u0001\u0000!\"\u0001\u0000"+
		"\u0001\u0002\u0002\u0000\u0004\u0004\u0007\u0007\u0164\u0000^\u0001\u0000"+
		"\u0000\u0000\u0002f\u0001\u0000\u0000\u0000\u0004k\u0001\u0000\u0000\u0000"+
		"\u0006s\u0001\u0000\u0000\u0000\bu\u0001\u0000\u0000\u0000\n{\u0001\u0000"+
		"\u0000\u0000\f\u0090\u0001\u0000\u0000\u0000\u000e\u0092\u0001\u0000\u0000"+
		"\u0000\u0010\u0094\u0001\u0000\u0000\u0000\u0012\u0099\u0001\u0000\u0000"+
		"\u0000\u0014\u00aa\u0001\u0000\u0000\u0000\u0016\u00ac\u0001\u0000\u0000"+
		"\u0000\u0018\u00ae\u0001\u0000\u0000\u0000\u001a\u00b0\u0001\u0000\u0000"+
		"\u0000\u001c\u00b4\u0001\u0000\u0000\u0000\u001e\u00b6\u0001\u0000\u0000"+
		"\u0000 \u00c1\u0001\u0000\u0000\u0000\"\u00cf\u0001\u0000\u0000\u0000"+
		"$\u00d2\u0001\u0000\u0000\u0000&\u00d8\u0001\u0000\u0000\u0000(\u00dd"+
		"\u0001\u0000\u0000\u0000*\u00e5\u0001\u0000\u0000\u0000,\u00e7\u0001\u0000"+
		"\u0000\u0000.\u00ea\u0001\u0000\u0000\u00000\u00f5\u0001\u0000\u0000\u0000"+
		"2\u00fa\u0001\u0000\u0000\u00004\u00fc\u0001\u0000\u0000\u00006\u0100"+
		"\u0001\u0000\u0000\u00008\u0102\u0001\u0000\u0000\u0000:\u0105\u0001\u0000"+
		"\u0000\u0000<\u010f\u0001\u0000\u0000\u0000>\u0118\u0001\u0000\u0000\u0000"+
		"@\u011a\u0001\u0000\u0000\u0000B\u011e\u0001\u0000\u0000\u0000D\u0135"+
		"\u0001\u0000\u0000\u0000F\u0137\u0001\u0000\u0000\u0000H\u0139\u0001\u0000"+
		"\u0000\u0000J\u013b\u0001\u0000\u0000\u0000L\u013d\u0001\u0000\u0000\u0000"+
		"N\u013f\u0001\u0000\u0000\u0000P\u0144\u0001\u0000\u0000\u0000R\u0147"+
		"\u0001\u0000\u0000\u0000T\u0151\u0001\u0000\u0000\u0000V\u0155\u0001\u0000"+
		"\u0000\u0000X\u0157\u0001\u0000\u0000\u0000Z\u015e\u0001\u0000\u0000\u0000"+
		"\\\u0163\u0001\u0000\u0000\u0000^_\u0005\u001e\u0000\u0000_`\u0003\u0002"+
		"\u0001\u0000`\u0001\u0001\u0000\u0000\u0000ab\u0003P(\u0000bc\u0003\u0004"+
		"\u0002\u0000ce\u0001\u0000\u0000\u0000da\u0001\u0000\u0000\u0000eh\u0001"+
		"\u0000\u0000\u0000fd\u0001\u0000\u0000\u0000fg\u0001\u0000\u0000\u0000"+
		"g\u0003\u0001\u0000\u0000\u0000hf\u0001\u0000\u0000\u0000il\u0003\u0006"+
		"\u0003\u0000jl\u0003\b\u0004\u0000ki\u0001\u0000\u0000\u0000kj\u0001\u0000"+
		"\u0000\u0000l\u0005\u0001\u0000\u0000\u0000mt\u0003\n\u0005\u0000nq\u0005"+
		"\r\u0000\u0000or\u0005 \u0000\u0000pr\u0003R)\u0000qo\u0001\u0000\u0000"+
		"\u0000qp\u0001\u0000\u0000\u0000rt\u0001\u0000\u0000\u0000sm\u0001\u0000"+
		"\u0000\u0000sn\u0001\u0000\u0000\u0000t\u0007\u0001\u0000\u0000\u0000"+
		"uy\u0005\u000e\u0000\u0000vz\u0003\n\u0005\u0000wz\u0005 \u0000\u0000"+
		"xz\u0003R)\u0000yv\u0001\u0000\u0000\u0000yw\u0001\u0000\u0000\u0000y"+
		"x\u0001\u0000\u0000\u0000z\t\u0001\u0000\u0000\u0000{|\u0005\u001c\u0000"+
		"\u0000|}\u0003P(\u0000}\u0085\u0003\f\u0006\u0000~\u007f\u0003P(\u0000"+
		"\u007f\u0080\u0005\u000f\u0000\u0000\u0080\u0081\u0003P(\u0000\u0081\u0082"+
		"\u0003\f\u0006\u0000\u0082\u0084\u0001\u0000\u0000\u0000\u0083~\u0001"+
		"\u0000\u0000\u0000\u0084\u0087\u0001\u0000\u0000\u0000\u0085\u0083\u0001"+
		"\u0000\u0000\u0000\u0085\u0086\u0001\u0000\u0000\u0000\u0086\u0088\u0001"+
		"\u0000\u0000\u0000\u0087\u0085\u0001\u0000\u0000\u0000\u0088\u0089\u0003"+
		"P(\u0000\u0089\u008a\u0005\u001d\u0000\u0000\u008a\u000b\u0001\u0000\u0000"+
		"\u0000\u008b\u0091\u0003\u000e\u0007\u0000\u008c\u0091\u0005 \u0000\u0000"+
		"\u008d\u0091\u0003\u0012\t\u0000\u008e\u0091\u0003\u0010\b\u0000\u008f"+
		"\u0091\u0003\u001a\r\u0000\u0090\u008b\u0001\u0000\u0000\u0000\u0090\u008c"+
		"\u0001\u0000\u0000\u0000\u0090\u008d\u0001\u0000\u0000\u0000\u0090\u008e"+
		"\u0001\u0000\u0000\u0000\u0090\u008f\u0001\u0000\u0000\u0000\u0091\r\u0001"+
		"\u0000\u0000\u0000\u0092\u0093\u0003F#\u0000\u0093\u000f\u0001\u0000\u0000"+
		"\u0000\u0094\u0095\u0003L&\u0000\u0095\u0011\u0001\u0000\u0000\u0000\u0096"+
		"\u0097\u0003\u0014\n\u0000\u0097\u0098\u0003P(\u0000\u0098\u009a\u0001"+
		"\u0000\u0000\u0000\u0099\u0096\u0001\u0000\u0000\u0000\u0099\u009a\u0001"+
		"\u0000\u0000\u0000\u009a\u009b\u0001\u0000\u0000\u0000\u009b\u009c\u0005"+
		"\u0019\u0000\u0000\u009c\u00a0\u0003P(\u0000\u009d\u009e\u0003\u0016\u000b"+
		"\u0000\u009e\u009f\u0003P(\u0000\u009f\u00a1\u0001\u0000\u0000\u0000\u00a0"+
		"\u009d\u0001\u0000\u0000\u0000\u00a0\u00a1\u0001\u0000\u0000\u0000\u00a1"+
		"\u00a8\u0001\u0000\u0000\u0000\u00a2\u00a6\u0005\u0019\u0000\u0000\u00a3"+
		"\u00a4\u0003P(\u0000\u00a4\u00a5\u0003\u0018\f\u0000\u00a5\u00a7\u0001"+
		"\u0000\u0000\u0000\u00a6\u00a3\u0001\u0000\u0000\u0000\u00a6\u00a7\u0001"+
		"\u0000\u0000\u0000\u00a7\u00a9\u0001\u0000\u0000\u0000\u00a8\u00a2\u0001"+
		"\u0000\u0000\u0000\u00a8\u00a9\u0001\u0000\u0000\u0000\u00a9\u0013\u0001"+
		"\u0000\u0000\u0000\u00aa\u00ab\u0003L&\u0000\u00ab\u0015\u0001\u0000\u0000"+
		"\u0000\u00ac\u00ad\u0003L&\u0000\u00ad\u0017\u0001\u0000\u0000\u0000\u00ae"+
		"\u00af\u0003L&\u0000\u00af\u0019\u0001\u0000\u0000\u0000\u00b0\u00b1\u0005"+
		"\u0018\u0000\u0000\u00b1\u00b2\u0003P(\u0000\u00b2\u00b3\u0003\u001c\u000e"+
		"\u0000\u00b3\u001b\u0001\u0000\u0000\u0000\u00b4\u00b5\u0003\u001e\u000f"+
		"\u0000\u00b5\u001d\u0001\u0000\u0000\u0000\u00b6\u00be\u0003 \u0010\u0000"+
		"\u00b7\u00b8\u0003P(\u0000\u00b8\u00b9\u0005\u0010\u0000\u0000\u00b9\u00ba"+
		"\u0003P(\u0000\u00ba\u00bb\u0003 \u0010\u0000\u00bb\u00bd\u0001\u0000"+
		"\u0000\u0000\u00bc\u00b7\u0001\u0000\u0000\u0000\u00bd\u00c0\u0001\u0000"+
		"\u0000\u0000\u00be\u00bc\u0001\u0000\u0000\u0000\u00be\u00bf\u0001\u0000"+
		"\u0000\u0000\u00bf\u001f\u0001\u0000\u0000\u0000\u00c0\u00be\u0001\u0000"+
		"\u0000\u0000\u00c1\u00c9\u0003\"\u0011\u0000\u00c2\u00c3\u0003P(\u0000"+
		"\u00c3\u00c4\u0005\u0011\u0000\u0000\u00c4\u00c5\u0003P(\u0000\u00c5\u00c6"+
		"\u0003\"\u0011\u0000\u00c6\u00c8\u0001\u0000\u0000\u0000\u00c7\u00c2\u0001"+
		"\u0000\u0000\u0000\u00c8\u00cb\u0001\u0000\u0000\u0000\u00c9\u00c7\u0001"+
		"\u0000\u0000\u0000\u00c9\u00ca\u0001\u0000\u0000\u0000\u00ca!\u0001\u0000"+
		"\u0000\u0000\u00cb\u00c9\u0001\u0000\u0000\u0000\u00cc\u00d0\u0003$\u0012"+
		"\u0000\u00cd\u00d0\u0003.\u0017\u0000\u00ce\u00d0\u0003(\u0014\u0000\u00cf"+
		"\u00cc\u0001\u0000\u0000\u0000\u00cf\u00cd\u0001\u0000\u0000\u0000\u00cf"+
		"\u00ce\u0001\u0000\u0000\u0000\u00d0#\u0001\u0000\u0000\u0000\u00d1\u00d3"+
		"\u0003&\u0013\u0000\u00d2\u00d1\u0001\u0000\u0000\u0000\u00d2\u00d3\u0001"+
		"\u0000\u0000\u0000\u00d3\u00d4\u0001\u0000\u0000\u0000\u00d4\u00d5\u0005"+
		"\u001b\u0000\u0000\u00d5\u00d6\u0003\u001c\u000e\u0000\u00d6\u00d7\u0005"+
		"\u001a\u0000\u0000\u00d7%\u0001\u0000\u0000\u0000\u00d8\u00d9\u0005\u0006"+
		"\u0000\u0000\u00d9\'\u0001\u0000\u0000\u0000\u00da\u00db\u0003&\u0013"+
		"\u0000\u00db\u00dc\u0003P(\u0000\u00dc\u00de\u0001\u0000\u0000\u0000\u00dd"+
		"\u00da\u0001\u0000\u0000\u0000\u00dd\u00de\u0001\u0000\u0000\u0000\u00de"+
		"\u00e1\u0001\u0000\u0000\u0000\u00df\u00e2\u0003*\u0015\u0000\u00e0\u00e2"+
		"\u0003B!\u0000\u00e1\u00df\u0001\u0000\u0000\u0000\u00e1\u00e0\u0001\u0000"+
		"\u0000\u0000\u00e2)\u0001\u0000\u0000\u0000\u00e3\u00e6\u0003,\u0016\u0000"+
		"\u00e4\u00e6\u0003\u0000\u0000\u0000\u00e5\u00e3\u0001\u0000\u0000\u0000"+
		"\u00e5\u00e4\u0001\u0000\u0000\u0000\u00e6+\u0001\u0000\u0000\u0000\u00e7"+
		"\u00e8\u0005\u001f\u0000\u0000\u00e8\u00e9\u0003\u0002\u0001\u0000\u00e9"+
		"-\u0001\u0000\u0000\u0000\u00ea\u00eb\u00032\u0019\u0000\u00eb\u00ec\u0003"+
		"P(\u0000\u00ec\u00ed\u00034\u001a\u0000\u00ed\u00ee\u0003P(\u0000\u00ee"+
		"\u00ef\u00032\u0019\u0000\u00ef/\u0001\u0000\u0000\u0000\u00f0\u00f6\u0003"+
		"L&\u0000\u00f1\u00f6\u0003J%\u0000\u00f2\u00f6\u0003F#\u0000\u00f3\u00f6"+
		"\u0003H$\u0000\u00f4\u00f6\u0005#\u0000\u0000\u00f5\u00f0\u0001\u0000"+
		"\u0000\u0000\u00f5\u00f1\u0001\u0000\u0000\u0000\u00f5\u00f2\u0001\u0000"+
		"\u0000\u0000\u00f5\u00f3\u0001\u0000\u0000\u0000\u00f5\u00f4\u0001\u0000"+
		"\u0000\u0000\u00f61\u0001\u0000\u0000\u0000\u00f7\u00fb\u00030\u0018\u0000"+
		"\u00f8\u00fb\u00036\u001b\u0000\u00f9\u00fb\u0003B!\u0000\u00fa\u00f7"+
		"\u0001\u0000\u0000\u0000\u00fa\u00f8\u0001\u0000\u0000\u0000\u00fa\u00f9"+
		"\u0001\u0000\u0000\u0000\u00fb3\u0001\u0000\u0000\u0000\u00fc\u00fd\u0007"+
		"\u0000\u0000\u0000\u00fd5\u0001\u0000\u0000\u0000\u00fe\u0101\u00038\u001c"+
		"\u0000\u00ff\u0101\u0003:\u001d\u0000\u0100\u00fe\u0001\u0000\u0000\u0000"+
		"\u0100\u00ff\u0001\u0000\u0000\u0000\u01017\u0001\u0000\u0000\u0000\u0102"+
		"\u0103\u0005\u001f\u0000\u0000\u0103\u0104\u0003<\u001e\u0000\u01049\u0001"+
		"\u0000\u0000\u0000\u0105\u0106\u0005\u001e\u0000\u0000\u0106\u0107\u0003"+
		"<\u001e\u0000\u0107;\u0001\u0000\u0000\u0000\u0108\u010b\u0003P(\u0000"+
		"\u0109\u010c\u0003>\u001f\u0000\u010a\u010c\u0003@ \u0000\u010b\u0109"+
		"\u0001\u0000\u0000\u0000\u010b\u010a\u0001\u0000\u0000\u0000\u010c\u010e"+
		"\u0001\u0000\u0000\u0000\u010d\u0108\u0001\u0000\u0000\u0000\u010e\u0111"+
		"\u0001\u0000\u0000\u0000\u010f\u010d\u0001\u0000\u0000\u0000\u010f\u0110"+
		"\u0001\u0000\u0000\u0000\u0110=\u0001\u0000\u0000\u0000\u0111\u010f\u0001"+
		"\u0000\u0000\u0000\u0112\u0113\u0005\u001c\u0000\u0000\u0113\u0114\u0003"+
		"\u000e\u0007\u0000\u0114\u0115\u0005\u001d\u0000\u0000\u0115\u0119\u0001"+
		"\u0000\u0000\u0000\u0116\u0117\u0005\r\u0000\u0000\u0117\u0119\u0003R"+
		")\u0000\u0118\u0112\u0001\u0000\u0000\u0000\u0118\u0116\u0001\u0000\u0000"+
		"\u0000\u0119?\u0001\u0000\u0000\u0000\u011a\u011b\u0005\u001c\u0000\u0000"+
		"\u011b\u011c\u0003\u0010\b\u0000\u011c\u011d\u0005\u001d\u0000\u0000\u011d"+
		"A\u0001\u0000\u0000\u0000\u011e\u011f\u0003X,\u0000\u011f\u012c\u0005"+
		"\u001b\u0000\u0000\u0120\u0121\u0003P(\u0000\u0121\u0129\u0003D\"\u0000"+
		"\u0122\u0123\u0003P(\u0000\u0123\u0124\u0005\u000f\u0000\u0000\u0124\u0125"+
		"\u0003P(\u0000\u0125\u0126\u0003D\"\u0000\u0126\u0128\u0001\u0000\u0000"+
		"\u0000\u0127\u0122\u0001\u0000\u0000\u0000\u0128\u012b\u0001\u0000\u0000"+
		"\u0000\u0129\u0127\u0001\u0000\u0000\u0000\u0129\u012a\u0001\u0000\u0000"+
		"\u0000\u012a\u012d\u0001\u0000\u0000\u0000\u012b\u0129\u0001\u0000\u0000"+
		"\u0000\u012c\u0120\u0001\u0000\u0000\u0000\u012c\u012d\u0001\u0000\u0000"+
		"\u0000\u012d\u012e\u0001\u0000\u0000\u0000\u012e\u012f\u0003P(\u0000\u012f"+
		"\u0130\u0005\u001a\u0000\u0000\u0130C\u0001\u0000\u0000\u0000\u0131\u0136"+
		"\u00030\u0018\u0000\u0132\u0136\u0003*\u0015\u0000\u0133\u0136\u0003\u001c"+
		"\u000e\u0000\u0134\u0136\u0003B!\u0000\u0135\u0131\u0001\u0000\u0000\u0000"+
		"\u0135\u0132\u0001\u0000\u0000\u0000\u0135\u0133\u0001\u0000\u0000\u0000"+
		"\u0135\u0134\u0001\u0000\u0000\u0000\u0136E\u0001\u0000\u0000\u0000\u0137"+
		"\u0138\u0005\u0005\u0000\u0000\u0138G\u0001\u0000\u0000\u0000\u0139\u013a"+
		"\u0007\u0001\u0000\u0000\u013aI\u0001\u0000\u0000\u0000\u013b\u013c\u0005"+
		"\u0003\u0000\u0000\u013cK\u0001\u0000\u0000\u0000\u013d\u013e\u0007\u0002"+
		"\u0000\u0000\u013eM\u0001\u0000\u0000\u0000\u013f\u0140\u0007\u0003\u0000"+
		"\u0000\u0140O\u0001\u0000\u0000\u0000\u0141\u0143\u0005$\u0000\u0000\u0142"+
		"\u0141\u0001\u0000\u0000\u0000\u0143\u0146\u0001\u0000\u0000\u0000\u0144"+
		"\u0142\u0001\u0000\u0000\u0000\u0144\u0145\u0001\u0000\u0000\u0000\u0145"+
		"Q\u0001\u0000\u0000\u0000\u0146\u0144\u0001\u0000\u0000\u0000\u0147\u014b"+
		"\u0003T*\u0000\u0148\u014a\u0003V+\u0000\u0149\u0148\u0001\u0000\u0000"+
		"\u0000\u014a\u014d\u0001\u0000\u0000\u0000\u014b\u0149\u0001\u0000\u0000"+
		"\u0000\u014b\u014c\u0001\u0000\u0000\u0000\u014cS\u0001\u0000\u0000\u0000"+
		"\u014d\u014b\u0001\u0000\u0000\u0000\u014e\u0152\u0003N\'\u0000\u014f"+
		"\u0152\u0005\n\u0000\u0000\u0150\u0152\u0005\t\u0000\u0000\u0151\u014e"+
		"\u0001\u0000\u0000\u0000\u0151\u014f\u0001\u0000\u0000\u0000\u0151\u0150"+
		"\u0001\u0000\u0000\u0000\u0152U\u0001\u0000\u0000\u0000\u0153\u0156\u0003"+
		"T*\u0000\u0154\u0156\u0005\u0001\u0000\u0000\u0155\u0153\u0001\u0000\u0000"+
		"\u0000\u0155\u0154\u0001\u0000\u0000\u0000\u0156W\u0001\u0000\u0000\u0000"+
		"\u0157\u015b\u0003Z-\u0000\u0158\u015a\u0003\\.\u0000\u0159\u0158\u0001"+
		"\u0000\u0000\u0000\u015a\u015d\u0001\u0000\u0000\u0000\u015b\u0159\u0001"+
		"\u0000\u0000\u0000\u015b\u015c\u0001\u0000\u0000\u0000\u015cY\u0001\u0000"+
		"\u0000\u0000\u015d\u015b\u0001\u0000\u0000\u0000\u015e\u015f\u0005\u0004"+
		"\u0000\u0000\u015f[\u0001\u0000\u0000\u0000\u0160\u0164\u0003Z-\u0000"+
		"\u0161\u0164\u0005\n\u0000\u0000\u0162\u0164\u0005\u0001\u0000\u0000\u0163"+
		"\u0160\u0001\u0000\u0000\u0000\u0163\u0161\u0001\u0000\u0000\u0000\u0163"+
		"\u0162\u0001\u0000\u0000\u0000\u0164]\u0001\u0000\u0000\u0000!fkqsy\u0085"+
		"\u0090\u0099\u00a0\u00a6\u00a8\u00be\u00c9\u00cf\u00d2\u00dd\u00e1\u00e5"+
		"\u00f5\u00fa\u0100\u010b\u010f\u0118\u0129\u012c\u0135\u0144\u014b\u0151"+
		"\u0155\u015b\u0163";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}