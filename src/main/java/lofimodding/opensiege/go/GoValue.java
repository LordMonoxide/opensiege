package lofimodding.opensiege.go;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface GoValue {
  String name() default "";
}
