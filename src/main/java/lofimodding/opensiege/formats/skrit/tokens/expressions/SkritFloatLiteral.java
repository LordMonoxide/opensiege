package lofimodding.opensiege.formats.skrit.tokens.expressions;

public class SkritFloatLiteral extends SkritExpression {
  public final float value;

  public SkritFloatLiteral(final float value) {
    this.value = value;
  }

  @Override
  public boolean isConstant() {
    return true;
  }
}
