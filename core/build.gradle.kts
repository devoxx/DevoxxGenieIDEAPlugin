plugins {
    java
}

group = "com.devoxx.genie"
version = "0.1.16"

repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.langchain4j:langchain4j:0.31.0")
    implementation("dev.langchain4j:langchain4j-ollama:0.31.0")
    implementation("dev.langchain4j:langchain4j-local-ai:0.31.0")
    implementation("dev.langchain4j:langchain4j-open-ai:0.31.0")
    implementation("dev.langchain4j:langchain4j-anthropic:0.31.0")
    implementation("dev.langchain4j:langchain4j-mistral-ai:0.31.0")
    implementation("dev.langchain4j:langchain4j-web-search-engine-google-custom:0.31.0")
    implementation("dev.langchain4j:langchain4j-web-search-engine-tavily:0.31.0")

    implementation("org.commonmark:commonmark:0.22.0")

    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0-M1")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.assertj:assertj-core:3.25.3")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
