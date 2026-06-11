import com.linecorp.support.project.multi.recipe.configureByTypeHaving
import com.linecorp.support.project.multi.recipe.configureByTypePrefix
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jlleitschuh.gradle.ktlint.KtlintPlugin
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    java
    `java-library`
    `jvm-test-suite`
    alias(libs.plugins.build.recipe)
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false

    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
}

allprojects {
    findProperty("group")?.let { group = it }
    findProperty("version")?.let { version = it }
}

// =============================================================================
// type=kotlin* (prefix) — 모든 Kotlin 모듈 공통 설정
// =============================================================================
configureByTypePrefix("kotlin") {
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "kotlin")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply<KtlintPlugin>()
    apply(plugin = "io.gitlab.arturbosch.detekt")

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    configure<KotlinJvmProjectExtension> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            freeCompilerArgs =
                listOf(
                    "-Xjsr305=strict",
                    "-Xjvm-default=all",
                    "-opt-in=kotlin.RequiresOptIn",
                )
        }
    }

    configure<DetektExtension> {
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
    }

    testing {
        suites {
            val test by getting(JvmTestSuite::class)
            val integrationTest by registering(JvmTestSuite::class)

            withType<JvmTestSuite> {
                useJUnitJupiter()
                targets {
                    all {
                        dependencies {
                            implementation(project())
                        }
                        testTask.configure {
                            shouldRunAfter(test)
                            testLogging {
                                events = mutableSetOf(TestLogEvent.FAILED)
                                exceptionFormat = TestExceptionFormat.FULL
                            }
                        }
                    }
                }
            }
        }
    }

    val integrationTestImplementation by configurations.getting {
        extendsFrom(configurations.testImplementation.get())
    }

    tasks.named("check") {
        dependsOn("integrationTest")
    }

    dependencies {
        implementation(enforcedPlatform(rootProject.libs.kotlin.bom))
        implementation(enforcedPlatform(rootProject.libs.kotlinx.coroutine.bom))

        implementation(kotlin("reflect"))
        implementation(kotlin("stdlib"))

        testImplementation(enforcedPlatform(SpringBootPlugin.BOM_COORDINATES))
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
        testImplementation(rootProject.libs.kotest.runner.junit5)
        testImplementation(rootProject.libs.kotest.assertions.core)
        testImplementation(rootProject.libs.mockk)
    }
}

// =============================================================================
// type having "boot" — Spring Boot 컨텍스트가 필요한 모듈
// =============================================================================
configureByTypeHaving("boot") {
    apply(plugin = "kotlin-spring")

    dependencies {
        implementation(enforcedPlatform(SpringBootPlugin.BOM_COORDINATES))
        implementation("org.springframework.boot:spring-boot-starter")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    }
}

// =============================================================================
// type having "boot", "mvc" — REST API 컨트롤러 모듈
// =============================================================================
configureByTypeHaving("boot", "mvc") {
    dependencies {
        implementation("org.springframework.boot:spring-boot-starter-web")
        implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")
    }
}

// =============================================================================
// type having "boot", "exposed", "repository" — Exposed 기반 Out-Port 구현체
// =============================================================================
configureByTypeHaving("boot", "exposed", "repository") {
    dependencies {
        api(rootProject.libs.exposed.spring.boot.starter)
        api(rootProject.libs.exposed.core)
        api(rootProject.libs.exposed.dao)
        api(rootProject.libs.exposed.jdbc)
        api(rootProject.libs.exposed.java.time)
        api(rootProject.libs.exposed.json)

        implementation("org.springframework.boot:spring-boot-starter-jdbc")
        runtimeOnly(rootProject.libs.postgres)

        "integrationTestImplementation"("org.testcontainers:testcontainers")
        "integrationTestImplementation"("org.testcontainers:postgresql")
        "integrationTestImplementation"("org.testcontainers:junit-jupiter")
    }
}

// =============================================================================
// type having "boot", "application" — 조립 모듈 (bootJar 가능)
// =============================================================================
configureByTypeHaving("boot", "application") {
    apply(plugin = "org.springframework.boot")

    dependencies {
        implementation("org.springframework.boot:spring-boot-starter-actuator")
        implementation(rootProject.libs.liquibase.core)
        runtimeOnly(rootProject.libs.postgres)
    }
}

// =============================================================================
// type having "boot", "mvc", "application" — REST API 서버 진입점
// =============================================================================
configureByTypeHaving("boot", "mvc", "application") {
    dependencies {
        implementation("org.springframework.boot:spring-boot-starter-web")
    }
}
