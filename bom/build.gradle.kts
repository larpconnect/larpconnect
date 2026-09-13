/*
 * bom platform module build script.
 * Publishes the Bill of Materials containing only the submodules of this system.
 */

plugins {
    id("njall.java-platform-conventions")
}

dependencies {
    constraints {
        // Declare active submodules of LarpConnect in the BOM
        api(project(":test"))
        api(project(":common"))
        api(project(":api"))
        api(project(":server"))
        api(project(":integration"))
    }
}
