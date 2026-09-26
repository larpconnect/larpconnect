import java.security.SecureRandom
import java.util.HexFormat

import javax.inject.Inject
import org.gradle.process.ExecOperations

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

abstract class GenerateComposeTlsTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {
    @get:OutputFile
    abstract val certFile: RegularFileProperty

    @get:InputFile
    abstract val scriptFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val target = certFile.get().asFile
        if (!target.exists()) {
            val script = scriptFile.get().asFile
            execOperations.exec {
                commandLine("bash", script.absolutePath)
            }
            logger.lifecycle("Generated local HAProxy TLS certificate: ${target.path}")
        }
    }
}

abstract class ComposeStopCleanTask : Exec() {
    @get:Internal
    abstract val envFile: RegularFileProperty

    @get:Internal
    abstract val certFile: RegularFileProperty

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
        val targetCert = certFile.orNull?.asFile
        if (targetCert != null && targetCert.exists()) {
            targetCert.delete()
            logger.lifecycle("Removed local certificate file: ${targetCert.path}")
        }
    }
}

val generateComposeEnv = tasks.register<GenerateComposeEnvTask>("generateComposeEnv") {
    group = "application"
    description = "Generates a random master secret seed into .env if the file is absent."
    envFile.convention(layout.projectDirectory.file(".env"))
    mustRunAfter("composeStopClean")
}

val generateComposeTls = tasks.register<GenerateComposeTlsTask>("generateComposeTls") {
    group = "application"
    description = "Generates a self-signed TLS certificate bundle into docker/haproxy/certs/haproxy.pem if absent."
    certFile.convention(layout.projectDirectory.file("docker/haproxy/certs/haproxy.pem"))
    scriptFile.convention(layout.projectDirectory.file("docker/haproxy/generate-certs.sh"))
    mustRunAfter("composeStopClean")
}

tasks.register<Exec>("composeStart") {
    group = "application"
    description = "Builds application distribution and launches Docker Compose stack in background (-d)."
    dependsOn(generateComposeEnv, generateComposeTls, ":server:installDist")
    commandLine("docker", "compose", "up", "--build", "-d")
}

tasks.register<Exec>("composeStop") {
    group = "application"
    description = "Stops and tears down Docker Compose containers and networks."
    commandLine("docker", "compose", "down")
}

tasks.register<Exec>("composeLogs") {
    group = "application"
    description = "Streams logs from all Docker Compose services."
    commandLine("docker", "compose", "logs", "-f")
}

tasks.register<ComposeStopCleanTask>("composeStopClean") {
    group = "application"
    description = "Tears down Docker Compose services, wipes persistent database volume, and removes .env and TLS certificates."
    envFile.convention(layout.projectDirectory.file(".env"))
    certFile.convention(layout.projectDirectory.file("docker/haproxy/certs/haproxy.pem"))
}


