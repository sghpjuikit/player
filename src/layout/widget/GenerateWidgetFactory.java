/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package layout.widget;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.atteo.classindex.IndexAnnotated;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


/**
 * Denotes widget class which will have its widget factory auto-generated. This alleviates the
 * developer from having to create the widget factory (without factory, widget can not be created).
 * <p/>
 * Use: simply annotate the controller class of the widget. It is recommended to always use this
 * annotation.
 * <p/>
 * Note: external widgets (not packaged in the project jar) have widget factories generated
 * automatically. Using this annotation would either have no effect or potentially cause
 * performance degradation during app start.
 *
 * @author Martin Polakovic
 */
@Documented
@IndexAnnotated
@Retention(RUNTIME)
@Target(TYPE)
public @interface GenerateWidgetFactory {}
