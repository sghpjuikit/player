package util.plugin;

import java.lang.annotation.*;

import org.atteo.classindex.IndexAnnotated;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Martin Polakovic
 */
@Documented
@IndexAnnotated
@Retention(RUNTIME)
@Target(TYPE)
public @interface IsPlugin {}