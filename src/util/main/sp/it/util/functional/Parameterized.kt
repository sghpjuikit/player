package sp.it.util.functional

import sp.it.util.functional.Functors.Parameter

interface Parameterized<TYPE, PARAMETER> {
   val parameters: List<Parameter<out PARAMETER>>
   fun realize(parameters: List<PARAMETER>): TYPE
}