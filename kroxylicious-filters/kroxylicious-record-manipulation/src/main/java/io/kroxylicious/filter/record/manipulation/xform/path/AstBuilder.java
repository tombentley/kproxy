/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.ArrayList;
import java.util.List;

import io.kroxylicious.filter.record.manipulation.expr.ExprBaseVisitor;
import io.kroxylicious.filter.record.manipulation.expr.ExprParser;

/**
 * Visitor for the parse tree which constructs the AST.
 */
class AstBuilder extends ExprBaseVisitor<Ast> {

    @Override
    public Ast visitExpr(ExprParser.ExprContext ctx) {
        return new Ast.Expr(super.visitExpr(ctx)).setTree(ctx);
    }

    @Override
    public Ast visitJsonpathQuery(ExprParser.JsonpathQueryContext ctx) {
        return buildPath(Identifier.ROOT, ctx.segments()).setTree(ctx);
    }

    /**
     * The relative query {@code @<segments>} used inside a filter selector; identical to an absolute
     * {@link #visitJsonpathQuery(ExprParser.JsonpathQueryContext) jsonpath query} but rooted at the filter's
     * current node ({@link Identifier#CURRENT}) rather than the document root.
     */
    @Override
    public Ast visitRelQuery(ExprParser.RelQueryContext ctx) {
        return buildPath(Identifier.CURRENT, ctx.segments()).setTree(ctx);
    }

    /** Builds an {@link Ast.Path} rooted at {@code identifier} from a {@code segments} parse subtree. */
    private Ast buildPath(Identifier identifier, ExprParser.SegmentsContext segments) {
        List<Ast> list = segments.segment().stream().map(s -> s.accept(this)).toList();
        return new Ast.Path(identifier, list, null);
    }

    @Override
    public Ast visitStringLiteral(ExprParser.StringLiteralContext ctx) {
        if (ctx.STRING() == null) {
            throw new IllegalStateException("Expected a string literal");
        }

        String str = ctx.STRING().getText();
        return new Ast.Str(unescape(str)).setTree(ctx);
    }

    static String unescape(String raw) {
        // Our grammar allows stuff that the RFC ABNF does not, so we need to reject that
        if (raw == null || raw.length() < 2) {
            return "";
        }
        // Remove enclosing quotes (' or ")
        String content = raw.substring(1, raw.length() - 1);
        StringBuilder sb = new StringBuilder(content.length());

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\\' && i + 1 < content.length()) {
                char next = content.charAt(++i);
                switch (next) {
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '/' -> sb.append('/');
                    case '\\' -> sb.append('\\');
                    case '\'' -> sb.append('\'');
                    case '"' -> sb.append('"');
                    case 'u' -> {
                        // Decode \\uXXXX and handle UTF-16 surrogate pairs
                        String hex = content.substring(i + 1, i + 5);
                        int codePoint = Integer.parseInt(hex, 16);
                        if (!Character.isValidCodePoint(codePoint)) {
                            throw new IllegalArgumentException("Invalid unicode code point: \\u" + hex);
                        }
                        sb.append((char) codePoint);
                        i += 4;
                    }
                    default -> throw new IllegalArgumentException("Invalid escape sequence: \\" + next);
                }
            }
            else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public Ast visitIntLiteral(ExprParser.IntLiteralContext ctx) {
        return new Ast.Int(Long.parseLong(ctx.getText())).setTree(ctx);
    }

    @Override
    public Ast visitNumberLiteral(ExprParser.NumberLiteralContext ctx) {
        // A numberLiteral is a decimal (it may have a fraction/exponent), so parse as a double.
        return new Ast.Num(Double.parseDouble(ctx.getText())).setTree(ctx);
    }

    @Override
    public Ast visitBooleanLiteral(ExprParser.BooleanLiteralContext ctx) {
        return new Ast.Bool("true".equals(ctx.getText())).setTree(ctx);
    }

    @Override
    public Ast visitLiteral(ExprParser.LiteralContext ctx) {
        if (ctx.NULL() != null) {
            return new Ast.Null().setTree(ctx);
        }
        else {
            return super.visitLiteral(ctx);
        }
    }

    @Override
    public Ast visitChildSegment(ExprParser.ChildSegmentContext ctx) {
        // childSegment:
        // bracketedSelection
        // | ( DOT ( STAR | memberNameShorthand ) )
        // ;
        if (ctx.bracketedSelection() != null) {
            return new Ast.Segment.Child<>((List) ctx.bracketedSelection().selector().stream()
                    .map(super::visit)
                    .map(Ast.Selector.class::cast).toList()).setTree(ctx);
        }
        else if (ctx.STAR() != null) {
            return new Ast.Segment.Child<>(List.of(new Ast.Selector.Children<>())).setTree(ctx);
        }
        else if (ctx.memberNameShorthand() != null) {
            Ast.Selector.Name<Object> e1 = new Ast.Selector.Name<>(
                    ctx.memberNameShorthand().getText());
            e1.setTree(ctx.memberNameShorthand());
            return new Ast.Segment.Child<>(List.of(e1)).setTree(ctx);
        }
        else {
            throw new IllegalStateException("Unexpected child segment: " + ctx);
        }
    }

    @Override
    public Ast visitDescendantSegment(ExprParser.DescendantSegmentContext ctx) {
        // descendantSegment:
        // DOTDOT (bracketedSelection
        // | STAR
        // | memberNameShorthand)
        // ;
        if (ctx.bracketedSelection() != null) {
            return new Ast.Segment.Descendant<>((List) ctx.bracketedSelection().selector().stream().map(super::visit).toList()).setTree(ctx);
        }
        else if (ctx.STAR() != null) {
            return new Ast.Segment.Descendant<>(List.of(new Ast.Selector.Children<>())).setTree(ctx);
        }
        else if (ctx.memberNameShorthand() != null) {
            return new Ast.Segment.Descendant<>(List.of(new Ast.Selector.Name<>(
                    ctx.memberNameShorthand().getText()))).setTree(ctx);
        }
        else {
            throw new IllegalStateException("Unexpected child segment: " + ctx);
        }
    }

    @Override
    public Ast visitSelector(ExprParser.SelectorContext ctx) {
        if (ctx.STAR() != null) {
            return new Ast.Selector.Children<>().setTree(ctx);
        }
        else {
            return super.visitSelector(ctx);
        }
    }

    @Override
    public Ast visitNameSelector(ExprParser.NameSelectorContext ctx) {
        return new Ast.Selector.Name<>(unescape(ctx.getText())).setTree(ctx);
    }

    @Override
    public Ast visitIndexSelector(ExprParser.IndexSelectorContext ctx) {
        Ast.Int visit = (Ast.Int) visit(ctx.intLiteral());
        return new Ast.Selector.Index<>(Math.toIntExact(visit.value())).setTree(ctx);
    }

    @Override
    public Ast visitSliceSelector(ExprParser.SliceSelectorContext ctx) {
        // start, end and step are all optional (e.g. `[1:3]`, `[5:]`, `[::2]`); omitted bounds are null and
        // an omitted step defaults to 1.
        Integer start = ctx.start() == null ? null : Math.toIntExact(((Ast.Int) visit(ctx.start())).value());
        Integer end = ctx.end() == null ? null : Math.toIntExact(((Ast.Int) visit(ctx.end())).value());
        int step = ctx.step() == null ? 1 : Math.toIntExact(((Ast.Int) visit(ctx.step())).value());
        return new Ast.Selector.Slice<>(start, end, step).setTree(ctx);
    }

    @Override
    public Ast visitFilterSelector(ExprParser.FilterSelectorContext ctx) {
        return new Ast.Selector.Filter<>(visit(ctx.logicalExpr())).setTree(ctx);
    }

    /**
     * A test expression inside a filter: an existence {@code filterQuery} or a {@code functionExpr}, optionally
     * negated. The pass-through (non-negated) case is left to the default single-child aggregation.
     */
    @Override
    public Ast visitTestExpr(ExprParser.TestExprContext ctx) {
        Ast inner = ctx.filterQuery() != null ? visit(ctx.filterQuery()) : visit(ctx.functionExpr());
        if (ctx.logicalNotOp() != null) {
            return new Ast.Not(inner).setTree(ctx);
        }
        return inner;
    }

    // A singular query (used as a comparison operand) navigates a single value via name/index segments only.

    @Override
    public Ast visitRelSingularQuery(ExprParser.RelSingularQueryContext ctx) {
        return buildSingularPath(Identifier.CURRENT, ctx.singularQuerySegments()).setTree(ctx);
    }

    @Override
    public Ast visitAbsSingularQuery(ExprParser.AbsSingularQueryContext ctx) {
        return buildSingularPath(Identifier.ROOT, ctx.singularQuerySegments()).setTree(ctx);
    }

    /** Builds an {@link Ast.Path} from {@code singularQuerySegments}, preserving the interleaved name/index order. */
    private Ast buildSingularPath(Identifier identifier, ExprParser.SingularQuerySegmentsContext ctx) {
        List<Ast> segments = new ArrayList<>();
        if (ctx.children != null) {
            for (var child : ctx.children) {
                if (child instanceof ExprParser.NameSegmentContext || child instanceof ExprParser.IndexSegmentContext) {
                    segments.add(visit(child));
                }
            }
        }
        return new Ast.Path(identifier, segments, null);
    }

    @Override
    public Ast visitNameSegment(ExprParser.NameSegmentContext ctx) {
        Ast.Selector.Name<Object> name = ctx.nameSelector() != null
                ? new Ast.Selector.Name<>(unescape(ctx.nameSelector().getText()))
                : new Ast.Selector.Name<>(ctx.memberNameShorthand().getText());
        return new Ast.Segment.Child<>(List.of(name)).setTree(ctx);
    }

    @Override
    public Ast visitIndexSegment(ExprParser.IndexSegmentContext ctx) {
        Ast.Int idx = (Ast.Int) visit(ctx.indexSelector().intLiteral());
        Ast.Selector.Index<Object> index = new Ast.Selector.Index<>(Math.toIntExact(idx.value()));
        return new Ast.Segment.Child<>(List.of(index)).setTree(ctx);
    }

    @Override
    public Ast visitLogicalOrExpr(ExprParser.LogicalOrExprContext ctx) {
        if (ctx.logicalAndExpr().size() == 1) {
            return visit(ctx.logicalAndExpr().getFirst());
        }
        return new Ast.Disjunction(
                ctx.logicalAndExpr().stream().map(this::visit).toList()).setTree(ctx);
    }

    @Override
    public Ast visitLogicalAndExpr(ExprParser.LogicalAndExprContext ctx) {
        if (ctx.basicExpr().size() == 1) {
            return visit(ctx.basicExpr().getFirst());
        }
        return new Ast.Conjunction(
                ctx.basicExpr().stream().map(this::visit).toList()).setTree(ctx);
    }

    @Override
    public Ast visitParenExpr(ExprParser.ParenExprContext ctx) {
        Ast visit = visit(ctx.logicalExpr());
        if (ctx.logicalNotOp() != null) {
            visit = new Ast.Not(visit).setTree(ctx);
        }
        return visit;
    }

    @Override
    public Ast visitComparisonExpr(ExprParser.ComparisonExprContext ctx) {
        final Ast left = visit(ctx.comparable(0));
        final Ast right = visit(ctx.comparable(1));
        var op = switch (ctx.comparisonOp().getText()) {
            case "==" -> Ast.BinOp.EQ;
            case "!=" -> Ast.BinOp.NE;
            case "<=" -> Ast.BinOp.LE;
            case ">=" -> Ast.BinOp.GE;
            case "<" -> Ast.BinOp.LT;
            case ">" -> Ast.BinOp.GT;
            default -> throw new IllegalStateException();
        };
        return new Ast.BinaryOp(left, op, right).setTree(ctx);
    }

    @Override
    public Ast visitFunctionExpr(ExprParser.FunctionExprContext ctx) {
        var functionName = ctx.functionName().getText();
        return new Ast.FnCall(functionName, ctx.functionArgument().stream().map(this::visit).map(Ast.class::cast).toList()).setTree(ctx);
    }

}
