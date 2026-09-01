plugins {
    id("java")
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.lombok") version "2.3.20"
    id("io.freefair.lombok") version "9.2.0"
}

group = "org.saintqd"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven(url = "https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven(url = "https://jitpack.io")
    maven(url = "https://nexus.scarsz.me/content/groups/public/")
    maven(url = "https://maven.enginehub.org/repo/")
    maven(url = "https://repo.nexomc.com/releases")
    maven(url = "https://mvn.lumine.io/repository/maven-public/")
    maven(url = "https://repo.hibiscusmc.com/releases")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6") // repo.extendedclip.com
    compileOnly(files("../VineriumLib/build/libs/VineriumLib-1.0-SNAPSHOT.jar"))
    compileOnly("com.github.Zrips:CMI-API:9.7.14.3")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.14-SNAPSHOT")
    compileOnly("com.gitlab.ruany:LiteBansAPI:0.6.1")
    compileOnly("net.luckperms:api:5.5")
    compileOnly("com.nexomc:nexo:1.17.0")
    compileOnly("io.lumine:Mythic-Dist:5.12.2-SNAPSHOT")
    compileOnly("com.hibiscusmc:HMCCosmetics:2.8.3")
    compileOnly("me.lojosho:HibiscusCommons:0.8.3-a89bcec3")

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    implementation(kotlin("stdlib-jdk8"))
}

tasks.test {
    useJUnitPlatform()
}
tasks.withType<Jar> {

    // To avoid the duplicate handling strategy error
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // To add all the dependencies otherwise a "NoClassDefFoundError" error
    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })

}
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
kotlin {
    jvmToolchain(21)
}