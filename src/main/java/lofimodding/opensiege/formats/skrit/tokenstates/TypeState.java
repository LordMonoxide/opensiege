package lofimodding.opensiege.formats.skrit.tokenstates;

import lofimodding.opensiege.formats.skrit.Compilation;
import lofimodding.opensiege.formats.skrit.Node;
import lofimodding.opensiege.formats.skrit.SkritParserTreeConstants;
import lofimodding.opensiege.formats.skrit.types.SkritType;

import java.util.function.Consumer;

public class TypeState extends TokenState {
  private final Consumer<SkritType> feedback;

  public TypeState(final Consumer<SkritType> feedback) {
    this.feedback = feedback;
  }

  @Override
  public void compile(final Compilation state) {

  }

  @Override
  public TokenState transitionTo(final Compilation state, final Node node) {
    if(node.getId() == SkritParserTreeConstants.JJTPRIMITIVETYPE) {
      return new PrimitiveTypeState(this.feedback);
    }

    return super.transitionTo(state, node);
  }
}
