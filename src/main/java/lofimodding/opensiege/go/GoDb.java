package lofimodding.opensiege.go;

import lofimodding.opensiege.formats.gas.GasEntry;
import org.reflections8.Reflections;
import org.reflections8.util.ClasspathHelper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoDb {
  private static final Pattern OBJECT_KEY = Pattern.compile("t:(\\w+),n:([\\w*]*)");

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
        final String type = matcher.group(1);
        final String name = matcher.group(2);

        this.addObject(type, name, entry.getValue());
      }
    }

    for(final Map.Entry<String, List<GasEntry>> entry : root.arrayChildren()) {
      final Matcher matcher = OBJECT_KEY.matcher(entry.getKey());

      if(matcher.matches()) {
        final String type = matcher.group(1);
        final String name = matcher.group(2);

        for(final GasEntry go : entry.getValue()) {
          this.addObject(type, name, go);
        }
      }
    }
  }

  private void addObject(final String type, final String name, final GasEntry data) {
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

    final Map<String, GameObject> objMap = this.objects.computeIfAbsent(type, key -> new HashMap<>());
    final String nameOrHash = name.isEmpty() ? String.valueOf(objMap.size()) : name;

    if(objMap.containsKey(nameOrHash)) {
      throw new RuntimeException("Object map " + type + " already contains object " + nameOrHash);
    }

    this.addObject(data);
    this.fillValues(nameOrHash, data, cls, inst);

    objMap.put(nameOrHash, inst);
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

        // Array child objects
        final List<GasEntry> arrayChildren = data.getArrayChildren(fieldName);

        if(arrayChildren != null) {
          if(field.getType() != List.class) {
            throw new RuntimeException("Field " + field + " must be of type List");
          }

          final Class<?> genericType = (Class<?>)((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];

          try {
            if(field.get(inst) == null) {
              field.set(inst, new ArrayList<>());
            }

            final List<Object> children = (List<Object>)field.get(inst);

            for(final GasEntry child : arrayChildren) {
              final Object childInst;
              final String classId = this.classToId.get(field.getType());
              if(classId != null) {
                childInst = this.objects.get(classId).get(name);
              } else {
                childInst = genericType.getConstructor().newInstance();
              }

              assert childInst != null;

              children.add(childInst);
              this.fillValues(name, child, genericType, childInst);
            }
          } catch(final IllegalAccessException | NoSuchMethodException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
          }

          continue;
        }

        // Child objects
        final GasEntry child = data.getChild(fieldName);

        if(child != null) {
          try {
            final Object childInst;
            final String classId = this.classToId.get(field.getType());
            if(classId != null) {
              childInst = this.objects.get(classId).get(name);
            } else {
              childInst = field.getType().getConstructor().newInstance();
            }

            assert childInst != null;

            field.set(inst, childInst);
            this.fillValues(name, child, field.getType(), field.get(inst));
          } catch(final IllegalAccessException | NoSuchMethodException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
          }

          continue;
        }

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

  public <T extends GameObject> T get(final Class<T> type, final String name) {
    //noinspection unchecked
    return (T)this.objects.get(this.classToId.get(type)).get(name);
  }

  public <T extends GameObject> Map<String, T> get(final Class<T> type) {
    //noinspection unchecked
    return (Map<String, T>)this.objects.get(this.classToId.get(type));
  }
}
