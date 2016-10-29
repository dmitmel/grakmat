import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.wrapper.Wrapper
import org.jetbrains.dokka.gradle.DokkaTask

buildscript {
    repositories {
        gradleScriptKotlin()
        maven { setUrl("https://plugins.gradle.org/m2/") }
    }

    dependencies {
        classpath(kotlinModule("gradle-plugin"))
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:0.9.9")
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
    }
}

apply {
    plugin("kotlin")
    plugin("org.jetbrains.dokka")
    plugin("com.github.johnrengelman.shadow")
}

group = "org.drimachine.grakmat"
version = "1.9"

repositories {
    mavenLocal()
    mavenCentral()
    gradleScriptKotlin()
}

dependencies {
    compile(kotlinModule("stdlib"))
    testCompile("junit:junit:4.12")
}

var dokkaOutputDirectory: String = ""
tasks.withType<DokkaTask> {
    sourceDirs = files("src/main/kotlin")
    dokkaOutputDirectory = outputDirectory
}

tasks.withType<ShadowJar> {
    classifier = "fatjar"
}

tasks.withType<Zip> {     // Also will match tasks with type 'Jar'
    from(files("LICENSE", "README.adoc", "README.html"))
}


tasks.withType<Jar> {
    manifest.attributes.apply {
        put("Main-Class", "org.drimachine.grakmat.grammars.Runner")
    }
}

configure<JavaPluginConvention> {
    // Configuring each source set
    configure(sourceSets) {
        output.setResourcesDir(output.classesDir)
    }
}

val sourcesZip = task<Zip>("sourcesZip") {
    group = "build"
    description = "Assembles a zip archive containing all sources."

    classifier = "sources"
    from(the<JavaPluginConvention>().sourceSets.getAt("main").allSource)
}

val docsZip = task<Zip>("docsZip") {
    group = "build"
    description = "Assembles a zip archive containing documentation."
    dependsOn("dokka")

    classifier = "docs"
    from(dokkaOutputDirectory)
}

task<Wrapper>("wrapper") {
    distributionUrl = "https://repo.gradle.org/gradle/dist-snapshots/gradle-script-kotlin-3.3-20161022102727+0000-all.zip"
}

artifacts.add("archives", sourcesZip)
artifacts.add("archives", docsZip)
