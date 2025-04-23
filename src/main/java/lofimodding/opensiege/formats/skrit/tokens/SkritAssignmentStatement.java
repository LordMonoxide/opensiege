package lofimodding.opensiege.formats.skrit.tokens;

import lofimodding.opensiege.formats.skrit.tokens.expressions.SkritExpression;

import java.util.List;

public class SkritAssignmentStatement extends SkritStatement {
  public final List<String> names;
  public final SkritExpression expression;

  public SkritAssignmentStatement(final List<String> names, final SkritExpression expression) {
    this.names = names;
    this.expression = expression;
  }

  @Override
  public String toString() {
    return String.join(".", this.names) + " = " + this.expression;
  }
}
