package lofimodding.opensiege.formats.skrit.tokens.expressions;

import java.util.List;
import java.util.stream.Collectors;

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

  @Override
  public String toString() {
    return String.join(".", this.names) + '(' + this.params.stream().map(String::valueOf).collect(Collectors.joining(", ")) + ')';
  }
}
