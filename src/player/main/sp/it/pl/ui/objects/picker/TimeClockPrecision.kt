package sp.it.pl.ui.objects.picker

enum class TimeClockPrecision(val size: Int) {
   YEAR(10), MONTH(9), WEEK(8), DAY(7), HOUR(6), MINUTE(5), SECOND(4), MILLI(3), MICRO(2), NANO(1)
}