plugins {
    java
    application
}

group = "tv.banko"
version = "1.2.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.discord4j", "discord4j-core", "3.2.1")
    implementation("org.mongodb", "mongodb-driver-sync", "4.4.1")
    implementation("com.google.code.gson", "gson", "2.8.9")
    implementation("com.squareup.okhttp3", "okhttp", "4.9.3")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

application {
    mainClass.set("tv.banko.antiscam.Main")
}
