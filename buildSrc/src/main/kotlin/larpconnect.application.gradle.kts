plugins {
    `application`
    id("larpconnect.java-common")
    id("larpconnect.testing")
}

dependencies {
    implementation(getLibrary("logback-classic"))
}
