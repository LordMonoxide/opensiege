package lofimodding.opensiege.formats.skrit.tokens;

import lofimodding.opensiege.formats.skrit.tokens.expressions.SkritFunctionCall;

public class SkritFunctionCallStatement extends SkritStatement {
  public final SkritFunctionCall function;

  public SkritFunctionCallStatement(final SkritFunctionCall function) {
    this.function = function;
  }

  @Override
  public String toString() {
    return this.function.toString();
  }
}
