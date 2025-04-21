package lofimodding.opensiege.formats.skrit;

public class NodeContext {
  public final Node node;
  public int childIndex;

  public NodeContext(final Node node) {
    this.node = node;
  }

  @Override
  public String toString() {
    return this.node + "[" + this.node.value + ']';
  }
}
