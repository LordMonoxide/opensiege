package lofimodding.opensiege.formats.skrit.tokens;

import lofimodding.opensiege.formats.skrit.tokens.expressions.SkritExpression;

import java.util.List;

public class SkritStatement extends SkritToken {
  public final List<String> names;
  public final SkritExpression expression;

  public SkritStatement(final List<String> names, final SkritExpression expression) {
    this.names = names;
    this.expression = expression;
  }
}
