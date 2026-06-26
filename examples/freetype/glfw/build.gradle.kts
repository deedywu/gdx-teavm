dependencies {
    implementation("com.badlogicgames.gdx:gdx:${LibExt.gdxVersion}")
    implementation(project(":examples:freetype:core"))
    implementation(project(":backends:backend-glfw"))
    implementation(project(":extensions:glfw:gdx-freetype-glfw"))
}

val mainClassName = "BuildFreetypeGlfwDemo"

tasks.register<JavaExec>("freetype_generate_teavm_glfw") {
    group = "example-teavm"
    description = "Generate TeaVM C sources for the freetype GLFW example"
    mainClass.set(mainClassName)
    classpath = sourceSets["main"].runtimeClasspath
    args("Debug")
}

tasks.register<JavaExec>("freetype_build_teavm_glfw_debug") {
    group = "example-teavm"
    description = "Generate TeaVM C sources and build the Debug GLFW freetype executable"
    mainClass.set(mainClassName)
    classpath = sourceSets["main"].runtimeClasspath
    args("Debug", "build")
}

tasks.register<JavaExec>("freetype_run_teavm_glfw_debug") {
    group = "example-teavm"
    description = "Generate, build, and run the Debug GLFW freetype executable"
    mainClass.set(mainClassName)
    classpath = sourceSets["main"].runtimeClasspath
    args("Debug", "run")
}

tasks.register<JavaExec>("freetype_build_teavm_glfw_debug_console") {
    group = "example-teavm"
    description = "Generate TeaVM C sources and build the Debug GLFW freetype executable with full console output"
    mainClass.set(mainClassName)
    classpath = sourceSets["main"].runtimeClasspath
    args("Debug", "build")
}
