plugins {
    id("java")
}

group = "fr.enimaloc"
version = "1.0-SNAPSHOT"

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

repositories {
    mavenCentral().content {
        excludeModule("javax.media", "jai_core")
    }
    maven(url = "https://m2.enimaloc.fr/snapshots")
    maven(url = "https://m2.enimaloc.fr/releases")
    maven(url = "https://jitpack.io")
    mavenLocal()
}

dependencies {
    // Base dependencies
    implementation("net.dv8tion:JDA:5.0.0-beta.15")
    implementation("fr.enimaloc:jda-enutils:0.4.1")
    implementation("org.slf4j:slf4j-simple:2.0.5")
    implementation("commons-io:commons-io:2.11.0")
    implementation("fr.enimaloc.night-config:toml:3.7.0")
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
    implementation("fr.enimaloc:matcher:0.1.5")
    implementation("com.fasterxml.jackson.core:jackson-core:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.16.1")

    implementation("club.minnced:discord-webhooks:0.8.4")

    implementation("com.github.imcdonagh:image4j:0.7.2")

    implementation("com.theokanning.openai-gpt3-java:service:0.18.2")
    implementation("org.apache.commons:commons-text:1.10.0")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}