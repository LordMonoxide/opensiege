package lofimodding.opensiege.formats.skrit.tokens.expressions;

import lofimodding.opensiege.formats.skrit.tokens.SkritToken;

public abstract class SkritExpression extends SkritToken {
  public abstract boolean isConstant();
}
