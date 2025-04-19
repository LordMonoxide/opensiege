package lofimodding.opensiege.formats.skrit;

import lofimodding.opensiege.formats.skrit.tokens.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Compilation {
  private final Node root;
  private final Deque<Node> nodeStack = new LinkedList<>();

  /** Variables which are in the current scope */
  private final Deque<Map<String, SkritVar>> scopes = new LinkedList<>();

  private final List<Token> tokens = new ArrayList<>();

  public Compilation(final Node root) {
    this.root = root;
    this.nodeStack.push(root);
    this.scopes.push(new HashMap<>());
  }

  public Node currentNode() {
    return this.nodeStack.peek();
  }

  public void pushNode(final Node node) {
    this.nodeStack.push(node);
  }

  public void popNode() {
    this.nodeStack.pop();
  }

  /** Add a token to the compilation output */
  public void addToken(final Token tokens) {
    this.tokens.add(tokens);
  }

  /** Add tokens to the compilation output */
  public void addToken(final Token... tokens) {
    Collections.addAll(this.tokens, tokens);
  }
}
