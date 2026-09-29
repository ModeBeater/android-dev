plugins {
    kotlin("jvm") version "2.2.0"
    application
}

group = "furnistore"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    // Coroutines are not part of the standard library — this is the only external dependency.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

application {
    mainClass.set("furnistore.MainKt")
}
