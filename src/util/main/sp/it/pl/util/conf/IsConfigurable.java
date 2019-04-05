package sp.it.pl.util.conf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks any class that can be configured. This is different from {@link Configurable}
 * interface and is part of a framework to create centralized globally accessible
 * application configuration. It is used in conjunction with {@link IsConfig}
 * and {@link Config}.
 * <p/>
 * Any class annotated with this annotation will be auto-discovered and added
 * into configuration class pool. If class is not annotated by this annotation,
 * it will not be discovered and its configurable fields will not be detected.
 * <p/>
 * Also allows to specify default category name for all configuration fields
 * within the annotated class.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface IsConfigurable {
	/*
	 * Specifies default configuration category name for configuration fields of
	 * given class. If none is specified, name of the class is used to specify default
	 * configuration field's category
	 * and this annotation makes it possible to customized the name. The name can
	 * also be specified individually for each configuration field. See {@link IsConfig}.
	 */
	String value() default "";
}