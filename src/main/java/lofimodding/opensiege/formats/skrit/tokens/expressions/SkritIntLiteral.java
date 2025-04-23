package lofimodding.opensiege.formats.skrit.tokens.expressions;

public class SkritIntLiteral extends SkritExpression {
  public final int value;

  public SkritIntLiteral(final int value) {
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
