package lofimodding.opensiege.formats.skrit.tokenstates;

import lofimodding.opensiege.formats.skrit.Compilation;
import lofimodding.opensiege.formats.skrit.exceptions.InvalidTransitionException;
import lofimodding.opensiege.formats.skrit.Node;
import lofimodding.opensiege.formats.skrit.exceptions.SkritCompilerException;

public abstract class TokenState {
  public void compile(final Compilation state) {
    throw new SkritCompilerException("Compilation not implemented for " + this);
  }

  public TokenState transitionTo(final Compilation state, final Node node) {
    throw new InvalidTransitionException(this, state.currentNode(), node);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }
}
