package lofimodding.opensiege.go;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface GoType {
  String type();
  Class<? extends GoLoader<?>> loader();
}
