package sp.it.util.async.future

import sp.it.util.functional.TryList

/** [List] of [Fut]. It is its own type, to allow disambiguating coincidental list of futures from intentional one */
class FutList<T>(list: List<Fut<T>>): List<Fut<T>> by list

/** @return [FutList] from this list of [Fut] */
fun <T> List<Fut<T>>.asFutList(): FutList<T> = FutList(this)

/** Future of list of [TryList] */
typealias Futs<R, E> = Fut<TryList<R, E>>