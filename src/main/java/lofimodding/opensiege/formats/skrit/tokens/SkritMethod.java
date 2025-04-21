package lofimodding.opensiege.formats.skrit.tokens;

import java.util.List;

public class SkritMethod extends SkritToken {
  public final String name;
  public final List<SkritVariable> params;
  public final List<SkritToken> tokens;

  public SkritMethod(final String name, final List<SkritVariable> params, final List<SkritToken> tokens) {
    this.name = name;
    this.params = params;
    this.tokens = tokens;
  }
}
