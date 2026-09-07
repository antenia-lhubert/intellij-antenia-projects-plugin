package fr.antenia.project

import org.semver4j.Semver

// Remove this fallback after all Neo root POMs declare their project version.
internal object LegacyProjectVersionInference {
    fun infer(javaVersion: Int): Semver = when {
        javaVersion >= 25 -> Semver.create(1, 6, 0)
        javaVersion >= 17 -> Semver.create(1, 5, 0)
        else -> Semver.create(1, 4, 0)
    }
}
