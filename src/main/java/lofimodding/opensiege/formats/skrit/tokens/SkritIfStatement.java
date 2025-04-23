package lofimodding.opensiege.formats.skrit.tokens;

import lofimodding.opensiege.formats.skrit.tokens.expressions.SkritExpression;

import java.util.List;
import java.util.stream.Collectors;

public class SkritIfStatement extends SkritStatement {
  public final SkritExpression expression;
  public final List<SkritToken> block;

  public SkritIfStatement(final SkritExpression expression, final List<SkritToken> block) {
    this.expression = expression;
    this.block = block;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append("if(").append(this.expression).append(") {\n");

    for(final SkritToken skritToken : this.block) {
      builder.append(skritToken.toString().lines().map(str -> "  " + str).collect(Collectors.joining("\n"))).append('\n');
    }

    builder.append("}\n");
    return builder.toString();
  }
}
