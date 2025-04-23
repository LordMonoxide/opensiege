package lofimodding.opensiege.formats.skrit.tokens;

import java.util.List;
import java.util.stream.Collectors;

public class SkritMethod extends SkritToken {
  public final String name;
  public final List<SkritVariable> params;
  public final List<SkritToken> tokens;

  public SkritMethod(final String name, final List<SkritVariable> params, final List<SkritToken> tokens) {
    this.name = name;
    this.params = params;
    this.tokens = tokens;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append(this.name).append('(').append(this.params.stream().map(String::valueOf).collect(Collectors.joining(", "))).append(") {\n");

    for(final SkritToken token : this.tokens) {
      builder.append(token.toString().lines().map(str -> "  " + str).collect(Collectors.joining("\n"))).append('\n');
    }

    builder.append("}\n");
    return builder.toString();
  }
}
