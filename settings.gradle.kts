import java.util.Properties
import java.io.File
import java.io.FileInputStream

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Look in local cache first for intermediate -SNAPSHOT testing!
        mavenLocal()
        google()
        mavenCentral()
        
        // Veg Sahayyak B2B GitHub Packages Registry
        maven {
            name = "VegSahayyakSDK"
            url = uri("https://maven.pkg.github.com/VegSahayyak/veg-sahayyak-sdk")
            credentials {
                val localProps = Properties()
                val localPropsFile = File(rootDir, "local.properties")
                if (localPropsFile.exists()) {
                    localProps.load(FileInputStream(localPropsFile))
                }
                username = System.getenv("GITHUB_ACTOR") ?: localProps.getProperty("gpr.user") ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: localProps.getProperty("gpr.key") ?: ""
            }
        }
    }
}

rootProject.name = "VegSahayyakSamples"
include(":basic-integration")
include(":fleet-tracking")
include(":ubi-insurance")
include(":background-service")
include(":custom-ui")
include(":oem-automotive")
