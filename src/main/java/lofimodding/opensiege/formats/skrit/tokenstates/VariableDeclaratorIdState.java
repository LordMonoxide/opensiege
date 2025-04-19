package lofimodding.opensiege.formats.skrit.tokenstates;

import lofimodding.opensiege.formats.skrit.Compilation;

import java.util.function.Consumer;

public class VariableDeclaratorIdState extends TokenState {
  private final Consumer<String> feedback;

  public VariableDeclaratorIdState(final Consumer<String> feedback) {
    this.feedback = feedback;
  }

  @Override
  public void compile(final Compilation state) {
    this.feedback.accept((String)state.currentNode().jjtGetValue());
  }
}
