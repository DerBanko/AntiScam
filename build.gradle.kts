plugins {
    java
    application
}

group = "tv.banko"
version = "1.3"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.discord4j:discord4j-core:3.2.1")
    implementation("org.mongodb:mongodb-driver-sync:4.4.1")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
tasks.withType<Jar> {
    val classpath = configurations.runtimeClasspath

    inputs.files(classpath).withNormalizer(ClasspathNormalizer::class.java)

    manifest {
        attributes["Main-Class"] = "tv.banko.antiscam.Main"

        attributes(
            "Class-Path" to classpath.map { cp -> cp.joinToString(" ") { "./lib/" + it.name } }
        )
    }
    archiveBaseName.set("AntiScam")
    archiveVersion.set("")
}

application {
    mainClass.set("tv.banko.antiscam.Main")
}
