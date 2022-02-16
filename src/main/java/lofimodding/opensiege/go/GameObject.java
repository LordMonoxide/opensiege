package lofimodding.opensiege.go;

import java.util.HashMap;
import java.util.Map;

public class GameObject {
  private final Map<String, GameObject> children = new HashMap<>();
  private final Map<String, Object> properties = new HashMap<>();
}
