package lofimodding.opensiege.formats.skrit.tokens.expressions;

public class SkritEquality extends SkritExpression {
  public final SkritExpression left;
  public final SkritExpression right;

  public SkritEquality(final SkritExpression left, final SkritExpression right) {
    this.left = left;
    this.right = right;
  }

  @Override
  public boolean isConstant() {
    return false;
  }

  @Override
  public String toString() {
    return "(" + this.left + " == " + this.right + ')';
  }
}
