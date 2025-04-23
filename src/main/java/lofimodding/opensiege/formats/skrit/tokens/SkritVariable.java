package lofimodding.opensiege.formats.skrit.tokens;

import lofimodding.opensiege.formats.skrit.tokens.expressions.SkritExpression;
import lofimodding.opensiege.formats.skrit.types.SkritType;

import javax.annotation.Nullable;

public class SkritVariable extends SkritToken {
  public final SkritType type;
  public final String name;
  @Nullable
  public final SkritExpression expression;

  public SkritVariable(final SkritType type, final String name, @Nullable final SkritExpression expression) {
    this.type = type;
    this.name = name;
    this.expression = expression;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append(this.type).append(' ').append(this.name);

    if(this.expression != null) {
      builder.append(" = ").append(this.expression);
    }

    return builder.toString();
  }
}
