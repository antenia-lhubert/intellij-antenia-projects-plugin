package fr.antenia.project

import org.junit.Assert.assertEquals
import org.junit.Test
import org.semver4j.Semver

class LegacyProjectVersionInferenceTest {
    @Test
    fun `infers temporary semantic versions from java`() {
        assertEquals(Semver.create(1, 4, 0), LegacyProjectVersionInference.infer(8))
        assertEquals(Semver.create(1, 5, 0), LegacyProjectVersionInference.infer(17))
        assertEquals(Semver.create(1, 6, 0), LegacyProjectVersionInference.infer(25))
    }
}
