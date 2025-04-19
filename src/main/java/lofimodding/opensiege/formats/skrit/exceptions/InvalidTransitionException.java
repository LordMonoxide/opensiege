package lofimodding.opensiege.formats.skrit.exceptions;

import lofimodding.opensiege.formats.skrit.Node;
import lofimodding.opensiege.formats.skrit.tokenstates.TokenState;

public class InvalidTransitionException extends SkritCompilerException {
  public InvalidTransitionException(final TokenState tokenState, final Node from, final Node to) {
    super(tokenState + ": cannot transition from " + from + " to " +  to);
  }
}
