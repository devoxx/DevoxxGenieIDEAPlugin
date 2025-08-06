so # Changelog

All notable changes to this project will be documented in this file.

## [0.7.0] - Unreleased

### Added
- Added Claude Sonnet 3.5 v2 to Amazon Bedrock models (#761)
- Added configuration setting to enable/disable regional inference for Amazon Bedrock models (i.e. prefixing model names with 'us', 'eu', or 'apac') (#759)
- Configure Dependabot for Gradle dependencies (#749)

### Fixed
- Fixed issue where the entire file was added in the context when adding code snippet (#761)
- Fixed fallback for empty submit and newline shortcuts on macOS 15.5 with IntelliJ IDEA 2025.1.3 (#753)
- Fixed duplicated user message issue (#748)

### Dependencies
- Bump the gradle-dependencies group with 22 updates (#765)
  - software.amazon.awssdk:sts from 2.25.6 to 2.32.16
  - software.amazon.awssdk:sso from 2.31.64 to 2.32.16
  - software.amazon.awssdk:ssooidc from 2.31.64 to 2.32.16
  - com.squareup.retrofit2:converter-gson from 2.11.0 to 3.0.0
  - org.xerial:sqlite-jdbc from 3.48.0.0 to 3.50.3.0
  - com.github.docker-java:docker-java from 3.4.0 to 3.5.3
  - com.github.docker-java:docker-java-transport-httpclient5 from 3.4.0 to 3.5.3
  - com.knuddels:jtokkit from 1.0.0 to 1.1.0
  - org.commonmark:commonmark from 0.22.0 to 0.25.1
  - io.netty:netty-all from 4.1.100.Final to 4.2.3.Final
  - ch.qos.logback:logback-classic from 1.4.12 to 1.5.18
  - nl.basjes.gitignore:gitignore-reader from 1.6.0 to 1.12.0
  - org.junit.jupiter:junit-jupiter-api from 5.11.0-M2 to 6.0.0-M2
  - org.junit.jupiter:junit-jupiter-engine from 5.11.0-M2 to 6.0.0-M2
  - org.junit.platform:junit-platform-launcher from 1.11.3 to 1.13.4
  - org.projectlombok:lombok from 1.18.34 to 1.18.38
  - org.junit.jupiter:junit-jupiter-params from 5.10.3 to 5.13.4
  - org.mockito:mockito-core from 5.11.0 to 5.18.0
  - org.mockito:mockito-junit-jupiter from 5.15.2 to 5.18.0
  - org.assertj:assertj-core from 3.26.0 to 3.27.3
  - org.jetbrains.intellij from 1.17.2 to 1.17.4
  - org.gradle.toolchains.foojay-resolver-convention from 0.5.0 to 1.0.0
- Bump brace-expansion from 1.1.11 to 1.1.12 in /docusaurus (#764)
- Bump on-headers and compression in /docusaurus (#752)

### Contributors
- @mydeveloperplanet
- @jaginn
- @ffeifel
- @aivantsov
- @teramawi
- @fchill
- @dependabot[bot]