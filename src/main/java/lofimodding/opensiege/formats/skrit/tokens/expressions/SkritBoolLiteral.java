package lofimodding.opensiege.formats.skrit.tokens.expressions;

public class SkritBoolLiteral extends SkritExpression {
  public final boolean value;

  public SkritBoolLiteral(final boolean value) {
    this.value = value;
  }

  @Override
  public boolean isConstant() {
    return true;
  }

  @Override
  public String toString() {
    return String.valueOf(this.value);
  }
}
