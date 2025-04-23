package lofimodding.opensiege.formats.skrit.tokens.expressions;

public class SkritStringLiteral extends SkritExpression {
  public final String value;

  public SkritStringLiteral(final String value) {
    this.value = value;
  }

  @Override
  public boolean isConstant() {
    return true;
  }

  @Override
  public String toString() {
    return this.value;
  }
}
