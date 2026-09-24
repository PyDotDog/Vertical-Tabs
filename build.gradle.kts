plugins {
    java
    id("org.jetbrains.intellij.platform")
}

group = "pl.solutiontabs"
version = "0.5.5"

dependencies {
    intellijPlatform {
        rider("2026.2.2") { useInstaller = false }
        jetbrainsRuntime()
    }
    testImplementation("junit:junit:4.13.2")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    jar {
        from(rootProject.file("LICENSE")) { into("META-INF") }
    }
    patchPluginXml {
        sinceBuild.set("261")
    }
    test {
        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}

intellijPlatform {
    // Rider 2026.2's standalone compiler package expects a JBR-specific
    // "Packages" directory that is absent from stock OpenJDK 25. This plugin
    // has no GUI Designer forms and does not require bytecode instrumentation.
    instrumentCode = false
    buildSearchableOptions = false
    pluginConfiguration {
        id = "pl.solutiontabs"
        name = "Vertical Tabs"
        version = project.version.toString()
        vendor {
            name = "Vertical Tabs"
        }
        description = """
            A configurable left-side open-files navigator for JetBrains IDEs with project/module grouping,
            group colors, ordering rules, and optional replacement of native editor tabs.
        """.trimIndent()
    }
}
