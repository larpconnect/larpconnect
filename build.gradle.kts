/*
 * Root project build script providing Docker Compose orchestration tasks.
 */

tasks.register<Exec>("composeUp") {
    group = "application"
    description = "Builds application distribution and launches Docker Compose stack (attached)."
    dependsOn(":server:installDist")
    commandLine("docker", "compose", "up", "--build")
}

tasks.register<Exec>("composeStart") {
    group = "application"
    description = "Builds application distribution and launches Docker Compose stack in background (-d)."
    dependsOn(":server:installDist")
    commandLine("docker", "compose", "up", "--build", "-d")
}

tasks.register<Exec>("composeDown") {
    group = "application"
    description = "Stops and tears down Docker Compose containers and networks."
    commandLine("docker", "compose", "down")
}

tasks.register<Exec>("composeLogs") {
    group = "application"
    description = "Streams logs from all Docker Compose services."
    commandLine("docker", "compose", "logs", "-f")
}

tasks.register<Exec>("composeClean") {
    group = "application"
    description = "Tears down Docker Compose services and wipes persistent database volume."
    commandLine("docker", "compose", "down", "-v")
}
