// Generated from /home/tom/Devel/kroxy-dm/kroxylicious-transform-providers/kroxylicious-transform-provider-neo/src/main/antlr4/io/kroxylicious/transform/neo/Path.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link PathParser}.
 */
public interface PathListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link PathParser#jsonpathQuery}.
	 * @param ctx the parse tree
	 */
	void enterJsonpathQuery(PathParser.JsonpathQueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#jsonpathQuery}.
	 * @param ctx the parse tree
	 */
	void exitJsonpathQuery(PathParser.JsonpathQueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#segments}.
	 * @param ctx the parse tree
	 */
	void enterSegments(PathParser.SegmentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#segments}.
	 * @param ctx the parse tree
	 */
	void exitSegments(PathParser.SegmentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#segment}.
	 * @param ctx the parse tree
	 */
	void enterSegment(PathParser.SegmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#segment}.
	 * @param ctx the parse tree
	 */
	void exitSegment(PathParser.SegmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#childSegment}.
	 * @param ctx the parse tree
	 */
	void enterChildSegment(PathParser.ChildSegmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#childSegment}.
	 * @param ctx the parse tree
	 */
	void exitChildSegment(PathParser.ChildSegmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#descendantSegment}.
	 * @param ctx the parse tree
	 */
	void enterDescendantSegment(PathParser.DescendantSegmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#descendantSegment}.
	 * @param ctx the parse tree
	 */
	void exitDescendantSegment(PathParser.DescendantSegmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#bracketedSelection}.
	 * @param ctx the parse tree
	 */
	void enterBracketedSelection(PathParser.BracketedSelectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#bracketedSelection}.
	 * @param ctx the parse tree
	 */
	void exitBracketedSelection(PathParser.BracketedSelectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#selector}.
	 * @param ctx the parse tree
	 */
	void enterSelector(PathParser.SelectorContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#selector}.
	 * @param ctx the parse tree
	 */
	void exitSelector(PathParser.SelectorContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#nameSelector}.
	 * @param ctx the parse tree
	 */
	void enterNameSelector(PathParser.NameSelectorContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#nameSelector}.
	 * @param ctx the parse tree
	 */
	void exitNameSelector(PathParser.NameSelectorContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#indexSelector}.
	 * @param ctx the parse tree
	 */
	void enterIndexSelector(PathParser.IndexSelectorContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#indexSelector}.
	 * @param ctx the parse tree
	 */
	void exitIndexSelector(PathParser.IndexSelectorContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#sliceSelector}.
	 * @param ctx the parse tree
	 */
	void enterSliceSelector(PathParser.SliceSelectorContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#sliceSelector}.
	 * @param ctx the parse tree
	 */
	void exitSliceSelector(PathParser.SliceSelectorContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#start}.
	 * @param ctx the parse tree
	 */
	void enterStart(PathParser.StartContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#start}.
	 * @param ctx the parse tree
	 */
	void exitStart(PathParser.StartContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#end}.
	 * @param ctx the parse tree
	 */
	void enterEnd(PathParser.EndContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#end}.
	 * @param ctx the parse tree
	 */
	void exitEnd(PathParser.EndContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#step}.
	 * @param ctx the parse tree
	 */
	void enterStep(PathParser.StepContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#step}.
	 * @param ctx the parse tree
	 */
	void exitStep(PathParser.StepContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#filterSelector}.
	 * @param ctx the parse tree
	 */
	void enterFilterSelector(PathParser.FilterSelectorContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#filterSelector}.
	 * @param ctx the parse tree
	 */
	void exitFilterSelector(PathParser.FilterSelectorContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#logicalExpr}.
	 * @param ctx the parse tree
	 */
	void enterLogicalExpr(PathParser.LogicalExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#logicalExpr}.
	 * @param ctx the parse tree
	 */
	void exitLogicalExpr(PathParser.LogicalExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#logicalOrExpr}.
	 * @param ctx the parse tree
	 */
	void enterLogicalOrExpr(PathParser.LogicalOrExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#logicalOrExpr}.
	 * @param ctx the parse tree
	 */
	void exitLogicalOrExpr(PathParser.LogicalOrExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#logicalAndExpr}.
	 * @param ctx the parse tree
	 */
	void enterLogicalAndExpr(PathParser.LogicalAndExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#logicalAndExpr}.
	 * @param ctx the parse tree
	 */
	void exitLogicalAndExpr(PathParser.LogicalAndExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#basicExpr}.
	 * @param ctx the parse tree
	 */
	void enterBasicExpr(PathParser.BasicExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#basicExpr}.
	 * @param ctx the parse tree
	 */
	void exitBasicExpr(PathParser.BasicExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#parenExpr}.
	 * @param ctx the parse tree
	 */
	void enterParenExpr(PathParser.ParenExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#parenExpr}.
	 * @param ctx the parse tree
	 */
	void exitParenExpr(PathParser.ParenExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#logicalNotOp}.
	 * @param ctx the parse tree
	 */
	void enterLogicalNotOp(PathParser.LogicalNotOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#logicalNotOp}.
	 * @param ctx the parse tree
	 */
	void exitLogicalNotOp(PathParser.LogicalNotOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#testExpr}.
	 * @param ctx the parse tree
	 */
	void enterTestExpr(PathParser.TestExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#testExpr}.
	 * @param ctx the parse tree
	 */
	void exitTestExpr(PathParser.TestExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#filterQuery}.
	 * @param ctx the parse tree
	 */
	void enterFilterQuery(PathParser.FilterQueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#filterQuery}.
	 * @param ctx the parse tree
	 */
	void exitFilterQuery(PathParser.FilterQueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#relQuery}.
	 * @param ctx the parse tree
	 */
	void enterRelQuery(PathParser.RelQueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#relQuery}.
	 * @param ctx the parse tree
	 */
	void exitRelQuery(PathParser.RelQueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#comparisonExpr}.
	 * @param ctx the parse tree
	 */
	void enterComparisonExpr(PathParser.ComparisonExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#comparisonExpr}.
	 * @param ctx the parse tree
	 */
	void exitComparisonExpr(PathParser.ComparisonExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(PathParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(PathParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#comparable}.
	 * @param ctx the parse tree
	 */
	void enterComparable(PathParser.ComparableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#comparable}.
	 * @param ctx the parse tree
	 */
	void exitComparable(PathParser.ComparableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#comparisonOp}.
	 * @param ctx the parse tree
	 */
	void enterComparisonOp(PathParser.ComparisonOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#comparisonOp}.
	 * @param ctx the parse tree
	 */
	void exitComparisonOp(PathParser.ComparisonOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#singularQuery}.
	 * @param ctx the parse tree
	 */
	void enterSingularQuery(PathParser.SingularQueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#singularQuery}.
	 * @param ctx the parse tree
	 */
	void exitSingularQuery(PathParser.SingularQueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#relSingularQuery}.
	 * @param ctx the parse tree
	 */
	void enterRelSingularQuery(PathParser.RelSingularQueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#relSingularQuery}.
	 * @param ctx the parse tree
	 */
	void exitRelSingularQuery(PathParser.RelSingularQueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#absSingularQuery}.
	 * @param ctx the parse tree
	 */
	void enterAbsSingularQuery(PathParser.AbsSingularQueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#absSingularQuery}.
	 * @param ctx the parse tree
	 */
	void exitAbsSingularQuery(PathParser.AbsSingularQueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#singularQuerySegments}.
	 * @param ctx the parse tree
	 */
	void enterSingularQuerySegments(PathParser.SingularQuerySegmentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#singularQuerySegments}.
	 * @param ctx the parse tree
	 */
	void exitSingularQuerySegments(PathParser.SingularQuerySegmentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#nameSegment}.
	 * @param ctx the parse tree
	 */
	void enterNameSegment(PathParser.NameSegmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#nameSegment}.
	 * @param ctx the parse tree
	 */
	void exitNameSegment(PathParser.NameSegmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#indexSegment}.
	 * @param ctx the parse tree
	 */
	void enterIndexSegment(PathParser.IndexSegmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#indexSegment}.
	 * @param ctx the parse tree
	 */
	void exitIndexSegment(PathParser.IndexSegmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#functionExpr}.
	 * @param ctx the parse tree
	 */
	void enterFunctionExpr(PathParser.FunctionExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#functionExpr}.
	 * @param ctx the parse tree
	 */
	void exitFunctionExpr(PathParser.FunctionExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#functionArgument}.
	 * @param ctx the parse tree
	 */
	void enterFunctionArgument(PathParser.FunctionArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#functionArgument}.
	 * @param ctx the parse tree
	 */
	void exitFunctionArgument(PathParser.FunctionArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#stringLiteral}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(PathParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#stringLiteral}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(PathParser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#booleanLiteral}.
	 * @param ctx the parse tree
	 */
	void enterBooleanLiteral(PathParser.BooleanLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#booleanLiteral}.
	 * @param ctx the parse tree
	 */
	void exitBooleanLiteral(PathParser.BooleanLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#numberLiteral}.
	 * @param ctx the parse tree
	 */
	void enterNumberLiteral(PathParser.NumberLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#numberLiteral}.
	 * @param ctx the parse tree
	 */
	void exitNumberLiteral(PathParser.NumberLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#intLiteral}.
	 * @param ctx the parse tree
	 */
	void enterIntLiteral(PathParser.IntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#intLiteral}.
	 * @param ctx the parse tree
	 */
	void exitIntLiteral(PathParser.IntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#alpha}.
	 * @param ctx the parse tree
	 */
	void enterAlpha(PathParser.AlphaContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#alpha}.
	 * @param ctx the parse tree
	 */
	void exitAlpha(PathParser.AlphaContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#s}.
	 * @param ctx the parse tree
	 */
	void enterS(PathParser.SContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#s}.
	 * @param ctx the parse tree
	 */
	void exitS(PathParser.SContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#memberNameShorthand}.
	 * @param ctx the parse tree
	 */
	void enterMemberNameShorthand(PathParser.MemberNameShorthandContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#memberNameShorthand}.
	 * @param ctx the parse tree
	 */
	void exitMemberNameShorthand(PathParser.MemberNameShorthandContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#nameFirst}.
	 * @param ctx the parse tree
	 */
	void enterNameFirst(PathParser.NameFirstContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#nameFirst}.
	 * @param ctx the parse tree
	 */
	void exitNameFirst(PathParser.NameFirstContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#nameChar}.
	 * @param ctx the parse tree
	 */
	void enterNameChar(PathParser.NameCharContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#nameChar}.
	 * @param ctx the parse tree
	 */
	void exitNameChar(PathParser.NameCharContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#functionName}.
	 * @param ctx the parse tree
	 */
	void enterFunctionName(PathParser.FunctionNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#functionName}.
	 * @param ctx the parse tree
	 */
	void exitFunctionName(PathParser.FunctionNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#functionNameFirst}.
	 * @param ctx the parse tree
	 */
	void enterFunctionNameFirst(PathParser.FunctionNameFirstContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#functionNameFirst}.
	 * @param ctx the parse tree
	 */
	void exitFunctionNameFirst(PathParser.FunctionNameFirstContext ctx);
	/**
	 * Enter a parse tree produced by {@link PathParser#functionNameChar}.
	 * @param ctx the parse tree
	 */
	void enterFunctionNameChar(PathParser.FunctionNameCharContext ctx);
	/**
	 * Exit a parse tree produced by {@link PathParser#functionNameChar}.
	 * @param ctx the parse tree
	 */
	void exitFunctionNameChar(PathParser.FunctionNameCharContext ctx);
}