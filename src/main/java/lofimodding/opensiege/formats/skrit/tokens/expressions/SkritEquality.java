package lofimodding.opensiege.formats.skrit.tokens.expressions;

public class SkritEquality extends SkritExpression {
  public final String operator;
  public final SkritExpression left;
  public final SkritExpression right;

  public SkritEquality(final String operator, final SkritExpression left, final SkritExpression right) {
    this.operator = operator;
    this.left = left;
    this.right = right;
  }

  @Override
  public boolean isConstant() {
    return false;
  }

  @Override
  public String toString() {
    return "(" + this.left + ' ' + this.operator + ' ' + this.right + ')';
  }
}
