plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "dev.amazepr"
version = "0.1.2"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    val localIdePath = providers.gradleProperty("localIdePath")
        .orElse("C:/Program Files/JetBrains/PyCharm Community Edition 2024.3")
        .get()

    intellijPlatform {
        local(localIdePath)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val diffTest = sourceSets.create("diffTest") {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
}

val diffNavigationTest = tasks.register<JavaExec>("diffNavigationTest") {
    group = "verification"
    description = "Checks diff snapshots and line ranges against temporary Git repositories."
    dependsOn(tasks.named(diffTest.classesTaskName))
    classpath = diffTest.runtimeClasspath
    mainClass.set("dev.amazepr.plugin.DiffNavigationTest")
}

tasks.check { dependsOn(diffNavigationTest) }

intellijPlatform {
    pluginConfiguration {
        name = "aMazePR"
        version = project.version.toString()

        ideaVersion {
            sinceBuild = "243"
        }
    }
}
