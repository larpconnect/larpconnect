import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

fun Project.getLibrary(name: String): Provider<MinimalExternalModuleDependency> {
    val libs = this.extensions.getByType<VersionCatalogsExtension>().named("libs")
    val lib = libs.findLibrary(name)
    if (!lib.isPresent) {
        throw RuntimeException("Library '$name' not found in version catalog")
    }
    return lib.get()
}

fun Project.getVersion(name: String): String {
    val libs = this.extensions.getByType<VersionCatalogsExtension>().named("libs")
    val ver = libs.findVersion(name)
    if (!ver.isPresent) {
        throw RuntimeException("Version '$name' not found in version catalog")
    }
    return ver.get().requiredVersion
}
