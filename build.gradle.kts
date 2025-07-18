plugins {
    id("java")
    id("application")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("--enable-preview"))
}

tasks.withType<Test> {
    jvmArgs("--enable-preview")
    useJUnitPlatform()
}

tasks.withType<JavaExec> {
    jvmArgs("--enable-preview")
}

// Heroku-specific JAR configuration
tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.eventsourcing.api.RPGServerLauncher"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveFileName.set("ai-rpg-events.jar")
}

// Ensure build task creates the main JAR
tasks.build {
    dependsOn(tasks.jar)
}

tasks.register("stage") {
    dependsOn("installDist")
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("ch.qos.logback:logback-core:1.4.14")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

application {
    mainClass.set("com.eventsourcing.Main")
    applicationDefaultJvmArgs = listOf("-DPORT=${'$'}PORT")
}

repositories {
    mavenCentral()
}
