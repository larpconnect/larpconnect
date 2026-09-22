import java.security.SecureRandom
import java.util.HexFormat

/*
 * Root project build script providing Docker Compose orchestration tasks.
 */

abstract class GenerateComposeEnvTask : DefaultTask() {
    @get:OutputFile
    abstract val envFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val target = envFile.get().asFile
        if (!target.exists()) {
            val randomBytes = ByteArray(16)
            SecureRandom().nextBytes(randomBytes)
            val secret = HexFormat.of().formatHex(randomBytes)
            target.writeText("NJALL_DB_SECRET=$secret\n")
            logger.lifecycle("Generated local environment secret file: .env")
        }
    }
}

abstract class ComposeCleanTask : Exec() {
    @get:Internal
    abstract val envFile: RegularFileProperty

    init {
        commandLine("docker", "compose", "down", "-v")
    }

    @TaskAction
    override fun exec() {
        super.exec()
        val target = envFile.orNull?.asFile
        if (target != null && target.exists()) {
            target.delete()
            logger.lifecycle("Removed local environment file: .env")
        }
    }
}

val generateComposeEnv = tasks.register<GenerateComposeEnvTask>("generateComposeEnv") {
    group = "application"
    description = "Generates a random master secret seed into .env if the file is absent."
    envFile.convention(layout.projectDirectory.file(".env"))
    mustRunAfter("composeClean")
}

tasks.register<Exec>("composeUp") {
    group = "application"
    description = "Builds application distribution and launches Docker Compose stack (attached)."
    dependsOn(generateComposeEnv, ":server:installDist")
    commandLine("docker", "compose", "up", "--build")
}

tasks.register<Exec>("composeStart") {
    group = "application"
    description = "Builds application distribution and launches Docker Compose stack in background (-d)."
    dependsOn(generateComposeEnv, ":server:installDist")
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

tasks.register<ComposeCleanTask>("composeClean") {
    group = "application"
    description = "Tears down Docker Compose services, wipes persistent database volume, and removes .env."
    envFile.convention(layout.projectDirectory.file(".env"))
}


