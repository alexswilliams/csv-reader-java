@file:Suppress("UnstableApiUsage")

import de.fayard.refreshVersions.core.FeatureFlag.*
import de.fayard.refreshVersions.core.StabilityLevel

plugins {
    id("de.fayard.refreshVersions") version "0.60.5"
}

rootProject.name = "csv-reader-java"

include("lib")

refreshVersions {
    rejectVersionIf {
        this.
        candidate.stabilityLevel != StabilityLevel.Stable
    }
    featureFlags {
        enable(VERSIONS_CATALOG)
        enable(GRADLE_UPDATES)
    }
}
