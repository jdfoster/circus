plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.50"
    application
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.typesafe.akka:akka-actor-typed_2.13:2.5.26")
    implementation("com.typesafe.akka:akka-slf4j_2.12:2.6.0-RC2")
    runtimeOnly("ch.qos.logback:logback-classic:1.2.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.3.0")
    testImplementation("com.typesafe.akka:akka-actor-testkit-typed_2.13:2.5.26")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

application {
    mainClassName = "circus.AppKt"
}
