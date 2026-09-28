/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

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
        List<Ast> list = ctx.segments().segment().stream().map(s -> s.accept(this)).toList();
        return new Ast.Path(Identifier.ROOT, list, null).setTree(ctx);
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
        return new Ast.Num(Long.parseLong(ctx.getText())).setTree(ctx);
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
        Ast.Int start = (Ast.Int) visit(ctx.start());
        Ast.Int end = (Ast.Int) visit(ctx.end());
        Ast.Int step = (Ast.Int) visit(ctx.step());
        return new Ast.Selector.Slice<>(
                Math.toIntExact(start.value()),
                Math.toIntExact(end.value()),
                Math.toIntExact(step.value())).setTree(ctx);
    }

    @Override
    public Ast visitFilterSelector(ExprParser.FilterSelectorContext ctx) {
        var filt = visit(ctx.logicalExpr());
        // TODO I can't currently implement filter property, because
        // (unlike the other selectors) is not take a literal. It should take an Ast
        // but currently accepts a predicate. Fixing that will require that path evaluation is
        // moved out of PathEvaluator and into the HandlerBuilder doodah
        // Ast.Selector.Filter
        return new Ast.Selector.FilterFilter<>(null);
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
            case ">" -> Ast.BinOp.GE;
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
