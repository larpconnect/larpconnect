/*
 * bom platform module build script.
 * Publishes the Bill of Materials containing only the submodules of this system.
 */

plugins {
    id("njall.java-platform-conventions")
}

dependencies {
    constraints {
        // Declare all submodules of LarpConnect in the BOM
        api(project(":common"))
        api(project(":events"))
        api(project(":test"))
        api(project(":queue"))
        api(project(":data"))
        api(project(":base"))
        api(project(":api"))
        api(project(":server"))
        api(project(":integration"))
    }
}
