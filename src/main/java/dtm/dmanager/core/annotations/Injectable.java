package dtm.dmanager.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dtm.dmanager.enums.DependencyCreatorType;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Injectable {
    DependencyCreatorType createStrategy() default DependencyCreatorType.SINGLETON;
    String qualifier() default "default";
}