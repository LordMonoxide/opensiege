package lofimodding.opensiege.formats.skrit;

import lofimodding.opensiege.formats.skrit.exceptions.SkritCompilerException;
import lofimodding.opensiege.formats.skrit.tokenstates.StartState;
import lofimodding.opensiege.formats.skrit.tokenstates.TokenState;

import java.io.InputStream;

public class SkritCompiler {
  public void compile(final InputStream input) {
    final SkritParser parser = new SkritParser(input);

    final Node node;
    try {
      node = parser.CompilationUnit();
    } catch(final ParseException e) {
      throw new SkritCompilerException("Failed to parse skrit", e);
    }

    node.dump("");

    final Compilation compilation = new Compilation(node);
    this.processNode(compilation, new StartState());
  }

  private void processNode(final Compilation compilation, final TokenState tokenState) {
    final Node node = compilation.currentNode();

    if(node.children != null) {
      for(int i = 0; i < node.children.length; i++) {
        final Node child = node.children[i];
        final TokenState newState = tokenState.transitionTo(compilation, child);
        compilation.pushNode(child);
        this.processNode(compilation, newState);
      }
    }

    tokenState.compile(compilation);
    compilation.popNode();
  }
}
