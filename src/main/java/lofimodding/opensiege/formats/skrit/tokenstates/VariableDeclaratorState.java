package lofimodding.opensiege.formats.skrit.tokenstates;

import lofimodding.opensiege.formats.skrit.Compilation;
import lofimodding.opensiege.formats.skrit.Node;
import lofimodding.opensiege.formats.skrit.SkritParserTreeConstants;

import java.util.function.Consumer;

public class VariableDeclaratorState extends TokenState {
  private final Consumer<String> nameFeedback;

  public VariableDeclaratorState(final Consumer<String> nameFeedback) {
    this.nameFeedback = nameFeedback;
  }

  @Override
  public TokenState transitionTo(final Compilation state, final Node node) {
    if(node.getId() == SkritParserTreeConstants.JJTVARIABLEDECLARATORID) {
      return new VariableDeclaratorIdState(this.nameFeedback);
    }

    if(node.getId() == SkritParserTreeConstants.JJTVARIABLEINITIALIZER) {
    }

    return super.transitionTo(state, node);
  }
}
