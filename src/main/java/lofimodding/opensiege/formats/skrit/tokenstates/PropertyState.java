package lofimodding.opensiege.formats.skrit.tokenstates;

import lofimodding.opensiege.formats.skrit.Compilation;
import lofimodding.opensiege.formats.skrit.Node;
import lofimodding.opensiege.formats.skrit.SkritParserTreeConstants;
import lofimodding.opensiege.formats.skrit.types.SkritType;

public class PropertyState extends TokenState {
  private SkritType type;
  private String name;

  @Override
  public TokenState transitionTo(final Compilation state, final Node node) {
    if(node.getId() == SkritParserTreeConstants.JJTTYPE) {
      return new TypeState(type -> this.type = type);
    }

    if(node.getId() == SkritParserTreeConstants.JJTVARIABLEDECLARATOR) {
      return new VariableDeclaratorState(name -> this.name = name);
    }

    return super.transitionTo(state, node);
  }
}
