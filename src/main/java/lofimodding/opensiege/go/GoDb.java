package lofimodding.opensiege.go;

import lofimodding.opensiege.formats.gas.GasEntry;
import org.reflections8.Reflections;
import org.reflections8.util.ClasspathHelper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoDb {
  private static final Pattern OBJECT_KEY = Pattern.compile("t:(\\w+),n:(\\w+)");

  private final Map<String, Class<? extends GameObject>> idToClass = new HashMap<>();
  private final Map<Class<?>, String> classToId = new HashMap<>();
  private final Map<String, Map<String, GameObject>> objects = new HashMap<>();

  public GoDb() {
    System.out.println("Searching for GameObject loaders...");

    final Reflections reflections = new Reflections(ClasspathHelper.forClassLoader());
    final Set<Class<?>> types = reflections.getTypesAnnotatedWith(GoType.class);

    for(final Class<?> cls : types) {
      final GoType type = cls.getAnnotation(GoType.class);

      this.idToClass.put(type.type(), (Class<? extends GameObject>)cls);
      this.classToId.put(cls, type.type());

      System.out.println(" - " + type.type() + " -> " + cls.getSimpleName());
    }
  }

  public void addObject(final GasEntry root) {
    for(final Map.Entry<String, GasEntry> entry : root.children()) {
      final Matcher matcher = OBJECT_KEY.matcher(entry.getKey());

      if(matcher.matches()) {
        final GasEntry data = entry.getValue();
        final String type = matcher.group(1);
        final String name = matcher.group(2);

        final Class<? extends GameObject> cls = this.idToClass.get(type);

        if(cls == null) {
          throw new RuntimeException("No class defined for type " + type);
        }

        final GameObject inst;
        try {
          inst = cls.getConstructor().newInstance();
        } catch(final InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
          throw new RuntimeException(e);
        }

        this.addObject(data);

        this.fillValues(name, data, cls, inst);

        this.objects.computeIfAbsent(type, key -> new HashMap<>()).put(name, inst);
      }
    }
  }

  private void fillValues(final String name, final GasEntry data, final Class<?> cls, final Object inst) {
    for(final Field field : cls.getDeclaredFields()) {
      final GoName goName = field.getAnnotation(GoName.class);

      if(goName != null) {
        field.setAccessible(true);

        try {
          field.set(inst, name);
        } catch(final IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }

      final GoValue goValue = field.getAnnotation(GoValue.class);

      if(goValue != null) {
        field.setAccessible(true);

        final String fieldName = goValue.name().isEmpty() ? field.getName() : goValue.name();
        final GasEntry child = data.getChild(fieldName);

        if(child != null) {
          // Child objects

          try {
            final Object childInst;
            final String classId = this.classToId.get(field.getType());
            if(classId != null) {
              childInst = this.objects.get(classId).get(name);
            } else {
              childInst = field.getType().getConstructor().newInstance();
            }

            field.set(inst, childInst);
            this.fillValues(name, child, field.getType(), field.get(inst));
          } catch(final IllegalAccessException | NoSuchMethodException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
          }
        } else {
          // Regular properties

          try {
            final Object val = data.get(fieldName);

            if(val != null) {
              field.set(inst, val);
            }
          } catch(final IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
  }

  public <T extends GameObject> T get(final Class<T> type, final String name) {
    //noinspection unchecked
    return (T)this.objects.get(this.classToId.get(type)).get(name);
  }
}
