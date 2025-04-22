package lofimodding.opensiege.formats.skrit.tokens.expressions;

import java.util.List;

public class SkritFunctionCall extends SkritExpression {
  public final List<String> names;
  public final List<SkritExpression> params;

  public SkritFunctionCall(final List<String> names, final List<SkritExpression> params) {
    this.names = names;
    this.params = params;
  }

  @Override
  public boolean isConstant() {
    return false;
  }
}
