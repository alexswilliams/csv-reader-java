import de.fayard.refreshVersions.core.FeatureFlag.*
import de.fayard.refreshVersions.core.StabilityLevel

plugins {
    id("de.fayard.refreshVersions") version "0.60.5"
}

rootProject.name = "csv-reader-java"

include("lib")

refreshVersions {
    rejectVersionIf {
        candidate.stabilityLevel != StabilityLevel.Stable ||
                // Logback classic only support java 8 up to 1.3.x
                this.moduleId.name == "logback-classic" && this.candidate.value.split('.')[1].toInt() > 3
    }
    featureFlags {
        enable(VERSIONS_CATALOG)
        enable(GRADLE_UPDATES)
    }
}
