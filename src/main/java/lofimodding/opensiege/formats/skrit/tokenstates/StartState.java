package lofimodding.opensiege.formats.skrit.tokenstates;

import lofimodding.opensiege.formats.skrit.Compilation;
import lofimodding.opensiege.formats.skrit.Node;
import lofimodding.opensiege.formats.skrit.SkritParserTreeConstants;

public class StartState extends TokenState {
  @Override
  public TokenState transitionTo(final Compilation state, final Node node) {
    if(node.getId() == SkritParserTreeConstants.JJTPROPERTYDELCARATION) {
      return new PropertyState();
    }

    return super.transitionTo(state, node);
  }
}
