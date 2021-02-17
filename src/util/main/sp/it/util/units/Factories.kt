package sp.it.util.units

import java.net.URI
import java.time.Year
import java.util.UUID

/** Equivalent to [KotlinVersion]. */
fun version(major: Int, minor: Int, patch: Int) = KotlinVersion(major, minor, patch)

/** Equivalent to [Year.of[]. */
fun year(year: Int): Year = Year.of(year)

/** Equivalent to [UUID.randomUUID]. */
fun uuid(): UUID = UUID.randomUUID()

/** Equivalent to [UUID.fromString]. */
fun uuid(text: String): UUID = UUID.fromString(text)

/** Equivalent to [URI.create]. */
fun uri(uri: String): URI = URI.create(uri)