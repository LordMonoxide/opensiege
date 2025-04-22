package lofimodding.opensiege.formats.skrit.tokens.expressions;

import java.util.List;

public class SkritReadVariable extends SkritExpression {
  public final List<String> names;

  public SkritReadVariable(final List<String> names) {
    this.names = names;
  }

  @Override
  public boolean isConstant() {
    return false;
  }
}
