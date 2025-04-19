package lofimodding.opensiege.formats.skrit.tokenstates;

import lofimodding.opensiege.formats.skrit.Compilation;
import lofimodding.opensiege.formats.skrit.types.SkritFloatType;
import lofimodding.opensiege.formats.skrit.types.SkritType;

import java.util.function.Consumer;

public class PrimitiveTypeState extends TokenState {
  private final Consumer<SkritType> feedback;

  public PrimitiveTypeState(final Consumer<SkritType> feedback) {
    this.feedback = feedback;
  }

  @Override
  public void compile(final Compilation state) {
    final String type = (String)state.currentNode().jjtGetValue();

    this.feedback.accept(switch(type) {
      case "float" -> new SkritFloatType();
      default -> throw new IllegalStateException("Invalid primitive type: " + type);
    });
  }
}
