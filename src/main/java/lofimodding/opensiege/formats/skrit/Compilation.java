package lofimodding.opensiege.formats.skrit;

import lofimodding.opensiege.formats.skrit.exceptions.UnexpectedTokenException;

import java.util.Deque;
import java.util.LinkedList;

public class Compilation {
  private final Node root;
  private final Deque<NodeContext> nodeStack = new LinkedList<>();

  public Compilation(final Node root) {
    this.root = root;
    this.nodeStack.push(new NodeContext(root));
  }

  public boolean hasNextChild() {
    final NodeContext current = this.nodeStack.peek();
    return current.node.children != null && current.childIndex < current.node.children.length;
  }

  public void pushChild() {
    final NodeContext current = this.nodeStack.peek();
    this.pushNode(current.node.children[current.childIndex]);
    current.childIndex++;
  }

  public void pop() {
    // There are more nodes in this branch
    if(this.hasNextChild()) {
      this.unexpectedToken();
    }

    this.nodeStack.pop();
  }

  public void backtrack() {
    this.nodeStack.pop();
    this.nodeStack.peek().childIndex--;
  }

  public int getTokenId() {
    return this.currentNode().getId();
  }

  public String getTokenValue() {
    return (String)this.currentNode().jjtGetValue();
  }

  public <T> T unexpectedToken() {
    final NodeContext current = this.nodeStack.peek();
    throw new UnexpectedTokenException("Unexpected token " + current + " in " + this.nodeStack.stream().skip(1).findFirst().orElse(null));
  }

  public void expectToken(final int tokenId) {
    if(this.getTokenId() != tokenId) {
      this.unexpectedToken();
    }
  }

  private Node currentNode() {
    return this.nodeStack.peek().node;
  }

  private void pushNode(final Node node) {
    this.nodeStack.push(new NodeContext(node));
  }
}
