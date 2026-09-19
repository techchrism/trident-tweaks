plugins {
    kotlin("jvm") version "2.4.0"
}

group = "me.techchrism"
version = "2.0.0"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(project.properties)
    }
}

tasks.jar {
    exclude("META-INF/**", "META-INF")
    archiveFileName.set("${project.name}.jar")
}
