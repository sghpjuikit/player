package sp.it.pl.core

/**
 * Component providing a set of related functionalities, intended to be implicitly available to the rest of the
 * application, i.e., can be designed as a singleton. It aims to prevent the use of static methods using
 * object oriented approach.
 *
 * * must be thread-safe
 * * should be as stateless as possible
 * * should
 */
interface Core {

   /**
    *  Initializes this core.
    *
    *  Implementation must not assume this method is called once.
    */
   fun init() = Unit

   /**
    *  Disposes this core.
    *
    *  Implementation must not assume this method is called once or that [init] was called prior to this.
    */
   fun dispose() = Unit

}