package club.readme.android

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionsTest {

    @Test fun equal() = assertEquals(0, Versions.compare("0.1.0", "0.1.0"))
    @Test fun numericNotLexical() = assertEquals(1, Versions.compare("0.10.0", "0.9.1"))
    @Test fun older() = assertEquals(-1, Versions.compare("0.1.0", "0.2.0"))
    @Test fun missingPartsAreZero() = assertEquals(0, Versions.compare("1.0", "1.0.0"))
}
