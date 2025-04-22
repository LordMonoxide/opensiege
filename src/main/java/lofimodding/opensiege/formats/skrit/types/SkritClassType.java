package lofimodding.opensiege.formats.skrit.types;

public class SkritClassType extends SkritType {
  public final String type;

  public SkritClassType(final String type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return "Class[" + this.type + ']';
  }
}
