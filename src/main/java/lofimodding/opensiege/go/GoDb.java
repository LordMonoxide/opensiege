package lofimodding.opensiege.go;

import lofimodding.opensiege.formats.gas.GasEntry;
import org.reflections8.Reflections;
import org.reflections8.util.ClasspathHelper;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoDb {
  private static final Pattern OBJECT_KEY = Pattern.compile("t:(\\w+),n:(\\w+)");

  private final Map<String, GoLoader<?>> loaders = new HashMap<>();
  private final Map<Class<?>, String> classToId = new HashMap<>();
  private final Map<String, Map<String, GameObject>> objects = new HashMap<>();

  public GoDb() {
    System.out.println("Searching for GameObject loaders...");

    final Reflections reflections = new Reflections(ClasspathHelper.forClassLoader());
    final Set<Class<?>> types = reflections.getTypesAnnotatedWith(GoType.class);

    for(final Class<?> cls : types) {
      final GoType type = cls.getAnnotation(GoType.class);

      try {
        this.loaders.put(type.type(), type.loader().getConstructor().newInstance());
      } catch(final InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }

      this.classToId.put(cls, type.type());

      System.out.println(" - " + type.type() + " -> " + type.loader().getSimpleName());
    }
  }

  public void addObject(final GasEntry root) {
    for(final Map.Entry<String, GasEntry> entry : root.children()) {
      final Matcher matcher = OBJECT_KEY.matcher(entry.getKey());

      if(matcher.matches()) {
        final String type = matcher.group(1);
        final String name = matcher.group(2);

        final GoLoader<?> loader = this.loaders.get(type);

        if(loader == null) {
          throw new RuntimeException("No loader defined for type " + type);
        }

        this.objects.computeIfAbsent(type, key -> new HashMap<>()).put(name, loader.load(name, entry.getValue()));
      }
    }
  }

  public <T extends GameObject> T get(final Class<T> type, final String name) {
    //noinspection unchecked
    return (T)this.objects.get(this.classToId.get(type)).get(name);
  }
}
