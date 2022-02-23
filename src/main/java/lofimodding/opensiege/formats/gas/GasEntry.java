package lofimodding.opensiege.formats.gas;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class GasEntry {
  private final Map<String, GasEntry> children;
  private final Map<String, List<GasEntry>> arrayChildren;
  private final Map<String, Object> values;

  public GasEntry(final Map<String, GasEntry> children, final Map<String, List<GasEntry>> arrayChildren, final Map<String, Object> values) {
    this.children = children;
    this.arrayChildren = arrayChildren;
    this.values = values;
  }

  public Set<Map.Entry<String, GasEntry>> children() {
    return this.children.entrySet();
  }

  public Set<Map.Entry<String, List<GasEntry>>> arrayChildren() {
    return this.arrayChildren.entrySet();
  }

  public GasEntry getChild(final String key) {
    return this.children.get(key);
  }

  public List<GasEntry> getArrayChildren(final String key) {
    return this.arrayChildren.get(key);
  }

  public Object get(final String key) {
    return this.values.get(key);
  }

  public String getString(final String key) {
    return (String)this.values.get(key);
  }
}
