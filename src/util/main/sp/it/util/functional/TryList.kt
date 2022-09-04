package sp.it.util.functional

/** [List] of [Try]. It is its own type, to allow disambiguating coincidental list of tries from intentional one */
class TryList<R, E>(list: List<Try<R, E>>): List<Try<R, E>> by list

/** @return [TryList] from this list of [Try] */
fun <R, E> List<Try<R, E>>.asTryList(): TryList<R, E> = TryList(this)