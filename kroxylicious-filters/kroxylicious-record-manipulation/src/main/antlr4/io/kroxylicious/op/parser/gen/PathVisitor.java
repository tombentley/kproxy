// Generated from /home/tom/Devel/kroxy-dm/kroxylicious-filters/kroxylicious-record-manipulation/src/main/antlr4/io/kroxylicious/op/parser/Path.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link PathParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface PathVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link PathParser#jsonpathQuery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJsonpathQuery(PathParser.JsonpathQueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#segments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSegments(PathParser.SegmentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#selector}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelector(PathParser.SelectorContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#nameSelector}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNameSelector(PathParser.NameSelectorContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#stringLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLiteral(PathParser.StringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#doubleQuoted}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDoubleQuoted(PathParser.DoubleQuotedContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#singleQuoted}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleQuoted(PathParser.SingleQuotedContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#indexSelector}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndexSelector(PathParser.IndexSelectorContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#int}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInt(PathParser.IntContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#sliceSelector}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSliceSelector(PathParser.SliceSelectorContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStart(PathParser.StartContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#end}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnd(PathParser.EndContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#step}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStep(PathParser.StepContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#filterSelector}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilterSelector(PathParser.FilterSelectorContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#logicalExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalExpr(PathParser.LogicalExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#logicalOrExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalOrExpr(PathParser.LogicalOrExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#logicalAndExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalAndExpr(PathParser.LogicalAndExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#basicExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBasicExpr(PathParser.BasicExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#parenExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenExpr(PathParser.ParenExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#logicalNotOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalNotOp(PathParser.LogicalNotOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#testExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTestExpr(PathParser.TestExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#filterQuery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilterQuery(PathParser.FilterQueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#relQuery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelQuery(PathParser.RelQueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#comparisonExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparisonExpr(PathParser.ComparisonExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(PathParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#comparable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparable(PathParser.ComparableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#comparisonOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparisonOp(PathParser.ComparisonOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#singularQuery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingularQuery(PathParser.SingularQueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#relSingularQuery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelSingularQuery(PathParser.RelSingularQueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#absSingularQuery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAbsSingularQuery(PathParser.AbsSingularQueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#singularQuerySegments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingularQuerySegments(PathParser.SingularQuerySegmentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#nameSegment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNameSegment(PathParser.NameSegmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#indexSegment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndexSegment(PathParser.IndexSegmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumber(PathParser.NumberContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#frac}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrac(PathParser.FracContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExp(PathParser.ExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#functionName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionName(PathParser.FunctionNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#functionNameFirst}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionNameFirst(PathParser.FunctionNameFirstContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#functionNameChar}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionNameChar(PathParser.FunctionNameCharContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#functionExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionExpr(PathParser.FunctionExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#functionArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionArgument(PathParser.FunctionArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#segment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSegment(PathParser.SegmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#childSegment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChildSegment(PathParser.ChildSegmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#bracketedSelection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBracketedSelection(PathParser.BracketedSelectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#memberNameShorthand}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMemberNameShorthand(PathParser.MemberNameShorthandContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#nameFirst}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNameFirst(PathParser.NameFirstContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#nameChar}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNameChar(PathParser.NameCharContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#descendantSegment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescendantSegment(PathParser.DescendantSegmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#digit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDigit(PathParser.DigitContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#unescaped}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnescaped(PathParser.UnescapedContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#alpha}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlpha(PathParser.AlphaContext ctx);
	/**
	 * Visit a parse tree produced by {@link PathParser#lower}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLower(PathParser.LowerContext ctx);
}