import java.util.*
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.lombok") version "2.3.10"
    kotlin("plugin.compose") version "2.3.10"
    id("org.jetbrains.intellij.platform") version "2.12.0"
    jacoco
}

group = "com.devoxx.genie"
version = "1.3.2"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
    google()
}

jacoco {
    toolVersion = "0.8.12"
}

val binaryIncompatibleRuntimeJarPatterns = listOf(
    "kotlin-stdlib-*.jar",
    "kotlin-stdlib-jdk7-*.jar",
    "kotlin-stdlib-jdk8-*.jar",
    "kotlinx-coroutines-core-*.jar",
    "kotlinx-coroutines-core-jvm-*.jar"
)
val packagedPluginDirName = "DevoxxGenie"
val pluginVerifierCommunityIdeVersions = listOf(
    "2025.1.7",   // 251 line
    "2025.2.6.1"  // 252 line
)
val pluginVerifierUnifiedIdeVersions = listOf(
    "2025.3.3"    // 253 line
)

fun Project.stripBinaryIncompatibleRuntimeJars(sandboxPluginPath: String) {
    fileTree(layout.buildDirectory.dir("idea-sandbox")) {
        binaryIncompatibleRuntimeJarPatterns.forEach { pattern ->
            include("**/$sandboxPluginPath/lib/$pattern")
        }
    }.files.forEach { file ->
        file.delete()
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(
        files(
            fileTree("build/instrumented/instrumentCode")
        )
    )

    sourceDirectories.setFrom(
        files(
            "src/main/java",
            "src/main/kotlin"
        )
    )

    executionData.setFrom(
        files("build/jacoco/test.exec")
    )
}

// Configure test task for JaCoCo
tasks.test {
    finalizedBy(tasks.jacocoTestReport)
    forkEvery = 10
}

tasks.register("updateProperties") {
    group = "build"
    description = "Updates application.properties with the current project version"
    doLast {
        val projectVersion = version
        val propertiesFile = file("src/main/resources/application.properties")

        if (propertiesFile.exists()) {
            val properties = Properties()

            // Read existing properties while removing comments
            val filteredLines = propertiesFile.readLines()
                .filterNot { it.trim().startsWith("#") } // Remove comments

            properties.load(filteredLines.joinToString("\n").byteInputStream())

            // Keep only the version property
            properties.clear()
            properties.setProperty("version", projectVersion.toString())

            // Manually write the properties file without the timestamp
            propertiesFile.printWriter().use { writer ->
                properties.forEach { key, value ->
                    writer.println("$key=$value")
                }
            }
        } else {
            println("application.properties file not found!")
        }
    }
}

tasks.named("buildPlugin") {
    dependsOn("updateProperties")
}

dependencies {
    intellijPlatform {
        // Allow overriding IDE version via property: ./gradlew runIde -PideVersion=2025.1.7
        create("IC", providers.gradleProperty("ideVersion").orElse("2025.1.7")) {}
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.intellij.plugins.markdown")  // Required by markdown renderer
        composeUI()
        testFramework(TestFrameworkType.Platform)
    }
    
    val lg4j_version = "1.12.2"
    val lg4j_beta_version = "1.12.2-beta22"
    val awsSdkVersion = "2.42.10"
    val retrofitVersion = "3.0.0"
    val sqliteVersion = "3.51.2.0"
    val dockerJavaVersion = "3.7.0"
    val jtokkitVersion = "1.1.0"
    val commonmarkVersion = "0.27.1"
    val jsoupVersion = "1.22.1"
    val nettyVersion = "4.2.10.Final"
    val composeCompileVersion = "1.10.2"
    val markdownRendererVersion = "0.39.2"
    val skikoVersion = "0.9.47"
    val logbackVersion = "1.5.32"
    val gitignoreReaderVersion = "1.14.1"
    val junitJupiterVersion = "6.1.0-M1"
    val junitPlatformVersion = "6.0.3"
    val lombokVersion = "1.18.44"
    val mockitoVersion = "5.22.0"
    val mockitoInlineVersion = "5.2.0"
    val assertjVersion = "3.27.7"
    val mockwebserverVersion = "5.3.2"
    val dotenvVersion = "5.2.2"
    val opentest4jVersion = "1.3.0"

    // Langchain4J dependencies
    implementation("dev.langchain4j:langchain4j:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-core:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-http-client-jdk:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-ollama:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-open-ai:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-anthropic:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-bedrock:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-mistral-ai:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-google-ai-gemini:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-web-search-engine-google-custom:$lg4j_beta_version")
    implementation("dev.langchain4j:langchain4j-web-search-engine-tavily:$lg4j_beta_version")
    implementation("dev.langchain4j:langchain4j-azure-open-ai:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-chroma:$lg4j_beta_version")
    implementation("dev.langchain4j:langchain4j-mcp:$lg4j_beta_version")
    implementation("dev.langchain4j:langchain4j-reactor:$lg4j_beta_version")
    implementation(platform("software.amazon.awssdk:bom:$awsSdkVersion"))
    implementation("software.amazon.awssdk:bedrock")
    implementation("software.amazon.awssdk:sts")
    implementation("software.amazon.awssdk:sso")
    implementation("software.amazon.awssdk:ssooidc")
    // Retrofit
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")
    implementation("org.xerial:sqlite-jdbc:$sqliteVersion")
    // Docker
    implementation("com.github.docker-java:docker-java:$dockerJavaVersion")
    implementation("com.github.docker-java:docker-java-transport-httpclient5:$dockerJavaVersion")
    // Tokenizer, markdown, HTML, networking
    implementation("com.knuddels:jtokkit:$jtokkitVersion")
    implementation("org.commonmark:commonmark:$commonmarkVersion")
    implementation("org.jsoup:jsoup:$jsoupVersion")
    implementation("io.netty:netty-all:$nettyVersion")
    // Compose Markdown Renderer aligned with the 251 Compose/Kotlin toolchain
    implementation("com.mikepenz:multiplatform-markdown-renderer-jvm:$markdownRendererVersion") {
        exclude(group = "org.jetbrains", module = "markdown")
    }
    implementation("com.mikepenz:multiplatform-markdown-renderer-code-jvm:$markdownRendererVersion") {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
        exclude(group = "org.jetbrains", module = "markdown")
    }
    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    // GitIgnore Reader
    implementation("nl.basjes.gitignore:gitignore-reader:$gitignoreReaderVersion")
    // TDG
    implementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    implementation("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    implementation("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")

    // Compile against and package the 251-era Compose desktop API jars.
    compileOnly("org.jetbrains.compose.runtime:runtime-desktop:$composeCompileVersion")
    compileOnly("org.jetbrains.compose.foundation:foundation-desktop:$composeCompileVersion")
    compileOnly("org.jetbrains.compose.ui:ui-desktop:$composeCompileVersion")
    compileOnly("org.jetbrains.compose.components:components-animatedimage-desktop:$composeCompileVersion")
    runtimeOnly("org.jetbrains.compose.runtime:runtime-desktop:$composeCompileVersion") {
        exclude(group = "org.jetbrains.skiko")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
    }
    runtimeOnly("org.jetbrains.compose.foundation:foundation-desktop:$composeCompileVersion") {
        exclude(group = "org.jetbrains.skiko")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
    }
    runtimeOnly("org.jetbrains.compose.ui:ui-desktop:$composeCompileVersion") {
        exclude(group = "org.jetbrains.skiko")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
    }
    runtimeOnly("org.jetbrains.compose.components:components-animatedimage-desktop:$composeCompileVersion") {
        exclude(group = "org.jetbrains.skiko")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
    }
    runtimeOnly("org.jetbrains.skiko:skiko-awt:$skikoVersion") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
    }
    runtimeOnly("org.jetbrains.skiko:skiko-awt-runtime-linux-x64:$skikoVersion")
    runtimeOnly("org.jetbrains.skiko:skiko-awt-runtime-macos-arm64:$skikoVersion")
    runtimeOnly("org.jetbrains.skiko:skiko-awt-runtime-macos-x64:$skikoVersion")
    runtimeOnly("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:$skikoVersion")

    compileOnly("org.projectlombok:lombok:$lombokVersion")

    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitPlatformVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.mockito:mockito-inline:$mockitoInlineVersion")
    testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("com.squareup.okhttp3:mockwebserver:$mockwebserverVersion")
    testImplementation("io.github.cdimascio:java-dotenv:$dotenvVersion")
    testImplementation("org.opentest4j:opentest4j:$opentest4jVersion")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
            untilBuild = "261.*"
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    pluginVerification {
        ides {
            pluginVerifierCommunityIdeVersions.forEach { ideVersion ->
                create("IC", ideVersion) {}
            }
            pluginVerifierUnifiedIdeVersions.forEach { ideVersion ->
                create("IU", ideVersion) {}
            }
        }
    }
}

// Filter unsupported Compose compiler plugin option (Kotlin 2.1.10 / IntelliJ platform incompatibility)
afterEvaluate {
    tasks.withType<KotlinCompile>().configureEach {
        val currentArgs = compilerOptions.freeCompilerArgs.get().toList()
        val filtered = mutableListOf<String>()
        var i = 0
        while (i < currentArgs.size) {
            if (currentArgs[i] == "-P" && currentArgs.getOrNull(i + 1)?.contains("generateFunctionKeyMetaAnnotations") == true) {
                i += 2
            } else {
                filtered += currentArgs[i]
                i++
            }
        }
        compilerOptions.freeCompilerArgs.set(filtered)
    }
}

tasks {
    // Run plugin on different IntelliJ versions for testing
    // Usage: ./gradlew runIde -PideVersion=2024.3.5
    //        ./gradlew runIde -PideVersion=2025.1.1
    //        ./gradlew runIde -PideVersion=2025.2.2
    //        ./gradlew runIde -PideVersion=2025.3.3

    withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    test {
        doFirst {
            val tmpDir = File("/tmp/test")
            tmpDir.deleteRecursively()
            tmpDir.mkdirs()
        }
        systemProperty("java.io.tmpdir", "/tmp/test")
        // Add these to help with platform tests
        systemProperty("idea.home.path", file("build/idea-sandbox"))

        useJUnitPlatform()
        testLogging { events("passed", "skipped", "failed") }

        maxHeapSize = "1g"

        // Configure JaCoCo agent to include plugin classes loaded from sandbox JARs
        extensions.configure<JacocoTaskExtension> {
            includes = listOf("com.devoxx.genie.*")
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }

        reports {
            junitXml.required.set(true)
            html.required.set(true)
        }

        // IntelliJ 2024.3 ships an older coroutines runtime than Compose 1.10 pulls in.
        // Keeping the plugin's transitive coroutines jars on the test worker classpath
        // causes IDE startup to fail inside LightPlatformTestCase before tests execute.
        classpath = classpath.filter { file ->
            !file.name.startsWith("kotlinx-coroutines-core")
        }
    }

    named("prepareTestSandbox") {
        doLast {
            project.stripBinaryIncompatibleRuntimeJars("plugins-test/$packagedPluginDirName")
        }
    }

    named("prepareSandbox") {
        doLast {
            project.stripBinaryIncompatibleRuntimeJars("plugins/$packagedPluginDirName")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlinLombok {
    lombokConfigurationFile(file("lombok.config"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
