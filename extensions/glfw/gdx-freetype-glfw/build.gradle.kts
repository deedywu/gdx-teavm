import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Locale
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.Sync

val moduleName = "gdx-freetype-glfw"

val freetypeRepositoryUrl = "https://github.com/freetype/freetype.git"
val freetypeTrackingBranch = "gdx-teavm-freetype-latest"
val freetypeCacheRoot = rootProject.file(".gradle/gdx-freetype")
val freetypeSourceCacheDir = File(freetypeCacheRoot, "source")
val freetypeGeneratedResourcesDir = layout.buildDirectory.dir("generated/gdx-freetype/resources").get().asFile
val freetypeNativeSourceDirProperty = providers.gradleProperty("gdxFreetypeNativeSourceDir")
val freetypeNativePlatformProperty = providers.gradleProperty("gdxFreetypeNativePlatform")
val freetypeNativeArchProperty = providers.gradleProperty("gdxFreetypeNativeArch")
val freetypeNativeClassifierProperty = providers.gradleProperty("gdxFreetypeNativeClassifier")
val freetypeNativeBridgeSourceDirProperty = providers.gradleProperty("gdxFreetypeNativeBridgeSourceDir")
val freetypeLinuxArm64NativeSourceDirProperty = providers.gradleProperty("gdxFreetypeLinuxArm64NativeSourceDir")
val freetypeLinuxArm64NativeBridgeSourceDirProperty = providers.gradleProperty("gdxFreetypeLinuxArm64NativeBridgeSourceDir")
val freetypeMacosX64NativeSourceDirProperty = providers.gradleProperty("gdxFreetypeMacosX64NativeSourceDir")
val freetypeMacosX64NativeBridgeSourceDirProperty = providers.gradleProperty("gdxFreetypeMacosX64NativeBridgeSourceDir")
val freetypeMacosArm64NativeSourceDirProperty = providers.gradleProperty("gdxFreetypeMacosArm64NativeSourceDir")
val freetypeMacosArm64NativeBridgeSourceDirProperty = providers.gradleProperty("gdxFreetypeMacosArm64NativeBridgeSourceDir")
val freetypeWindowsX64NativeSourceDirProperty = providers.gradleProperty("gdxFreetypeWindowsX64NativeSourceDir")
val freetypeWindowsX64NativeBridgeSourceDirProperty = providers.gradleProperty("gdxFreetypeWindowsX64NativeBridgeSourceDir")
val freetypeWindowsArm64NativeSourceDirProperty = providers.gradleProperty("gdxFreetypeWindowsArm64NativeSourceDir")
val freetypeWindowsArm64NativeBridgeSourceDirProperty = providers.gradleProperty("gdxFreetypeWindowsArm64NativeBridgeSourceDir")
val linuxX64NativeBuildRoot = layout.buildDirectory.dir("generated/gdx-freetype/native/linux-x64").get().asFile
val linuxX64NativeSourceDir = File(linuxX64NativeBuildRoot, "src")
val linuxX64NativeBuildDir = File(linuxX64NativeBuildRoot, "build")
val linuxArm64NativeBuildRoot = layout.buildDirectory.dir("generated/gdx-freetype/native/linux-arm64").get().asFile
val linuxArm64NativeSourceDir = File(linuxArm64NativeBuildRoot, "src")
val linuxArm64NativeBuildDir = File(linuxArm64NativeBuildRoot, "build")
val macosX64NativeBuildRoot = layout.buildDirectory.dir("generated/gdx-freetype/native/macos-x64").get().asFile
val macosX64NativeSourceDir = File(macosX64NativeBuildRoot, "src")
val macosX64NativeBuildDir = File(macosX64NativeBuildRoot, "build")
val macosArm64NativeBuildRoot = layout.buildDirectory.dir("generated/gdx-freetype/native/macos-arm64").get().asFile
val macosArm64NativeSourceDir = File(macosArm64NativeBuildRoot, "src")
val macosArm64NativeBuildDir = File(macosArm64NativeBuildRoot, "build")
val windowsX64NativeBuildRoot = layout.buildDirectory.dir("generated/gdx-freetype/native/windows-x64").get().asFile
val windowsX64NativeSourceDir = File(windowsX64NativeBuildRoot, "src")
val windowsX64NativeBuildDir = File(windowsX64NativeBuildRoot, "build")
val windowsArm64NativeBuildRoot = layout.buildDirectory.dir("generated/gdx-freetype/native/windows-arm64").get().asFile
val windowsArm64NativeSourceDir = File(windowsArm64NativeBuildRoot, "src")
val windowsArm64NativeBuildDir = File(windowsArm64NativeBuildRoot, "build")
val hostOsName = System.getProperty("os.name").lowercase(Locale.ROOT)
val hostArchName = System.getProperty("os.arch").lowercase(Locale.ROOT)
val isLinuxHost = hostOsName.contains("linux")
val isMacHost = hostOsName.contains("mac") || hostOsName.contains("darwin")
val isWindowsHost = hostOsName.contains("windows")
val isLinuxX64Host = isLinuxHost && setOf("x86_64", "amd64").contains(hostArchName)
val isLinuxArm64Host = isLinuxHost && setOf("aarch64", "arm64").contains(hostArchName)
val isMacX64Host = isMacHost && setOf("x86_64", "amd64").contains(hostArchName)
val isMacArm64Host = isMacHost && setOf("aarch64", "arm64").contains(hostArchName)
val isWindowsX64Host = isWindowsHost && setOf("x86_64", "amd64").contains(hostArchName)
val isWindowsArm64Host = isWindowsHost && setOf("aarch64", "arm64").contains(hostArchName)

fun resolveFreetypeNativeClassifier(): String {
    val explicitClassifier = freetypeNativeClassifierProperty.orNull?.trim()
    if(!explicitClassifier.isNullOrEmpty()) {
        return explicitClassifier
    }

    val platform = freetypeNativePlatformProperty.orNull?.trim().orEmpty()
    val arch = freetypeNativeArchProperty.orNull?.trim().orEmpty()
    return when {
        platform.isNotEmpty() && arch.isNotEmpty() -> "natives-$platform-$arch"
        platform.isNotEmpty() -> "natives-$platform"
        arch.isNotEmpty() -> "natives-$arch"
        else -> "natives"
    }
}

val freetypeNativeClassifier = providers.provider { resolveFreetypeNativeClassifier() }

fun isPreparedFreetypeSource(dir: File): Boolean {
    return dir.isDirectory &&
        File(dir, "CMakeLists.txt").isFile &&
        File(dir, "include/ft2build.h").isFile
}

fun deleteDirectoryWithRetries(dir: File, description: String) {
    if(!dir.exists()) {
        return
    }

    val attempts = 5
    for(attempt in 1..attempts) {
        if(dir.deleteRecursively() || !dir.exists()) {
            return
        }

        if(attempt < attempts) {
            logger.lifecycle("Retrying delete of $description: attempt ${attempt + 1}/$attempts")
            Thread.sleep(500L * attempt)
        }
    }

    throw GradleException(
        "Unable to delete $description at ${dir.absolutePath}. Close any process that still holds files there and rerun the task."
    )
}

fun runGitCommand(gitWorkingDir: File, args: List<String>): String {
    val stdout = ByteArrayOutputStream()
    val process = ProcessBuilder(listOf("git") + args)
        .directory(gitWorkingDir)
        .redirectErrorStream(true)
        .start()

    process.inputStream.use { input ->
        input.copyTo(stdout)
    }

    val exitCode = process.waitFor()
    val outputText = stdout.toString(StandardCharsets.UTF_8)
    if(exitCode != 0) {
        throw GradleException(
            "Git command failed in ${gitWorkingDir.absolutePath}: git ${args.joinToString(" ")}" +
                if(outputText.isBlank()) "" else System.lineSeparator() + outputText.trim()
        )
    }

    return outputText
}

fun runCommand(workingDir: File, args: List<String>) {
    val stdout = ByteArrayOutputStream()
    val process = ProcessBuilder(args)
        .directory(workingDir)
        .redirectErrorStream(true)
        .start()

    process.inputStream.use { input ->
        input.copyTo(stdout)
    }

    val exitCode = process.waitFor()
    val outputText = stdout.toString(StandardCharsets.UTF_8)
    if(exitCode != 0) {
        throw GradleException(
            "Command failed in ${workingDir.absolutePath}: ${args.joinToString(" ")}" +
                if(outputText.isBlank()) "" else System.lineSeparator() + outputText.trim()
        )
    }
}

fun refreshCachedFreetypeSource() {
    freetypeCacheRoot.mkdirs()

    val hasGitCheckout = File(freetypeSourceCacheDir, ".git/config").isFile
    if(!hasGitCheckout) {
        if(freetypeSourceCacheDir.exists()) {
            logger.lifecycle("Initializing existing FreeType cache directory at ${freetypeSourceCacheDir.absolutePath}")
            runGitCommand(freetypeSourceCacheDir, listOf("init"))
            runGitCommand(freetypeSourceCacheDir, listOf("remote", "add", "origin", freetypeRepositoryUrl))
        }
        else {
            logger.lifecycle("Cloning FreeType from $freetypeRepositoryUrl")
            runGitCommand(freetypeCacheRoot, listOf("clone", "--depth", "1", freetypeRepositoryUrl, "source"))
        }
    }
    else {
        logger.lifecycle("Fetching latest FreeType updates in ${freetypeSourceCacheDir.absolutePath}")
        runGitCommand(freetypeSourceCacheDir, listOf("fetch", "--depth", "1", "origin"))
    }

    runGitCommand(freetypeSourceCacheDir, listOf("remote", "set-url", "origin", freetypeRepositoryUrl))
    runGitCommand(freetypeSourceCacheDir, listOf("remote", "set-head", "origin", "-a"))
    runGitCommand(freetypeSourceCacheDir, listOf("checkout", "--force", "-B", freetypeTrackingBranch, "origin/HEAD"))
    runGitCommand(freetypeSourceCacheDir, listOf("clean", "-fd"))
}

fun shouldRefreshFreetypeSource(): Boolean {
    val refresh = providers.gradleProperty("gdxFreetypeRefresh").orNull
    return refresh != null && refresh.toBoolean()
}

fun forceDownloadFreetypeSource(): Boolean {
    val forceDownload = providers.gradleProperty("gdxFreetypeForceDownload").orNull
    return forceDownload != null && forceDownload.toBoolean()
}

fun resolveFreetypeNativeSourceDir(): File {
    val sourceDir = freetypeNativeSourceDirProperty.orNull
        ?: throw GradleException(
            "Set -PgdxFreetypeNativeSourceDir=<path> to package a FreeType natives jar."
        )
    val dir = File(sourceDir)
    if(!dir.isDirectory) {
        throw GradleException("FreeType native source directory does not exist: ${dir.absolutePath}")
    }
    return dir
}

fun resolveFreetypeNativeBridgeSourceDir(): File {
    val sourceDir = freetypeNativeBridgeSourceDirProperty.orNull
        ?: throw GradleException(
            "Set -PgdxFreetypeNativeBridgeSourceDir=<path> to package a FreeType natives jar."
        )
    val dir = File(sourceDir)
    if(!dir.isDirectory) {
        throw GradleException("FreeType native bridge directory does not exist: ${dir.absolutePath}")
    }
    return dir
}

fun resolveDirectoryFromProperty(propertyValue: String?, description: String): File {
    val sourceDir = propertyValue?.trim()
        ?: throw GradleException(description)
    val dir = File(sourceDir)
    if(!dir.isDirectory) {
        throw GradleException("$description Directory does not exist: ${dir.absolutePath}")
    }
    return dir
}

dependencies {
    implementation(project(":backends:backend-glfw"))
    implementation("com.badlogicgames.gdx:gdx:${LibExt.gdxVersion}")
    implementation("com.badlogicgames.gdx-controllers:gdx-controllers-core:${LibExt.gdxControllerVersion}")
}

sourceSets["main"].resources.srcDir(freetypeGeneratedResourcesDir)

tasks.register("freetype_prepare_source") {
    group = "extensions"
    description = "Clones or refreshes the FreeType source cache used by the GLFW freetype extension."

    outputs.dir(freetypeSourceCacheDir)
    outputs.upToDateWhen {
        isPreparedFreetypeSource(freetypeSourceCacheDir) && !forceDownloadFreetypeSource() && !shouldRefreshFreetypeSource()
    }

    doLast {
        if(forceDownloadFreetypeSource()) {
            deleteDirectoryWithRetries(freetypeSourceCacheDir, "cached FreeType source directory")
        }

        val cacheReady = isPreparedFreetypeSource(freetypeSourceCacheDir)
        val needsRefresh = forceDownloadFreetypeSource() || !cacheReady || shouldRefreshFreetypeSource()

        if(!needsRefresh) {
            logger.lifecycle("Using cached FreeType source at ${freetypeSourceCacheDir.absolutePath}")
            return@doLast
        }

        try {
            refreshCachedFreetypeSource()
        }
        catch(ex: Exception) {
            if(cacheReady && isPreparedFreetypeSource(freetypeSourceCacheDir) && !forceDownloadFreetypeSource()) {
                logger.warn("Unable to refresh FreeType from remote, continuing with cached source: ${ex.message}")
            }
            else {
                throw ex
            }
        }

        if(!isPreparedFreetypeSource(freetypeSourceCacheDir)) {
            throw GradleException("Downloaded FreeType source is incomplete at ${freetypeSourceCacheDir.absolutePath}")
        }
    }
}

tasks.register<Sync>("freetype_sync_source") {
    group = "extensions"
    description = "Copies the cached FreeType source tree into generated resources for the GLFW freetype extension."

    dependsOn("freetype_prepare_source")
    from(freetypeSourceCacheDir)
    into(File(freetypeGeneratedResourcesDir, "external_cpp/thirdparty/freetype"))
    exclude(".git/**")
}

tasks.named("processResources").configure {
    dependsOn("freetype_sync_source")
}

tasks.named<Jar>("sourcesJar").configure {
    dependsOn("freetype_sync_source")
}

tasks.named<Jar>("jar").configure {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("external_cpp/thirdparty/**")
    exclude("external_cpp/src/freetype/src/**")
    exclude("external_cpp/src/freetype/CMakeLists.txt")
}

tasks.register<Jar>("freetype_nativesJar") {
    group = "extensions"
    description = "Packages one platform/architecture's FreeType native binaries into a classifier jar."

    archiveBaseName.set(moduleName)
    archiveClassifier.set(freetypeNativeClassifier)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    onlyIf { freetypeNativeSourceDirProperty.isPresent && freetypeNativeBridgeSourceDirProperty.isPresent }

    from(providers.provider { resolveFreetypeNativeSourceDir() }) {
        include("**/*.dll")
        include("**/*.so")
        include("**/*.so.*")
        include("**/*.dylib")
        includeEmptyDirs = false
        eachFile {
            path = name
        }
    }

    from(providers.provider { resolveFreetypeNativeBridgeSourceDir() }) {
        include("**/*.lib")
        include("**/*.a")
        includeEmptyDirs = false
        eachFile {
            path = name
        }
    }
}

tasks.register<Jar>("freetype_withCSourcesJar") {
    group = "extensions"
    description = "Packages the GLFW freetype runtime jar including bundled C source trees."

    archiveBaseName.set(moduleName)
    archiveVersion.set(LibExt.libVersion)
    archiveClassifier.set("with-c-sources")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    dependsOn("processResources")
    from(sourceSets["main"].output)
}

tasks.register<Sync>("prepareLinuxX64NativeBridgeSource") {
    group = "extensions"
    description = "Copies the GLFW FreeType bridge sources into a Linux x64 native build workspace."

    from("src/main/resources/external_cpp/src/freetype")
    into(linuxX64NativeSourceDir)
}

tasks.register("buildLinuxX64NativeBridge") {
    group = "extensions"
    description = "Builds the Linux x64 GLFW FreeType native bridge and bundled FreeType static library."

    dependsOn("freetype_prepare_source")
    dependsOn("prepareLinuxX64NativeBridgeSource")
    onlyIf { isLinuxX64Host }

    outputs.file(File(linuxX64NativeBuildDir, "libgdx2d_freetype_bridge.so"))
    outputs.file(File(linuxX64NativeBuildDir, "freetype-build/libfreetype.a"))

    doLast {
        linuxX64NativeBuildDir.mkdirs()
        runCommand(
            linuxX64NativeSourceDir,
            listOf(
                "cmake",
                "-S", linuxX64NativeSourceDir.absolutePath,
                "-B", linuxX64NativeBuildDir.absolutePath,
                "-DGDX_FREETYPE_SOURCE_DIR=${freetypeSourceCacheDir.absolutePath}",
                "-DGDX_FREETYPE_BUILD_SHARED=ON",
                "-DCMAKE_BUILD_TYPE=Release"
            )
        )
        runCommand(
            linuxX64NativeSourceDir,
            listOf("cmake", "--build", linuxX64NativeBuildDir.absolutePath)
        )
    }
}

tasks.register<Jar>("freetype_natives_linux_x64Jar") {
    group = "extensions"
    description = "Packages the Linux x64 GLFW FreeType native bridge and native FreeType library."

    archiveBaseName.set(moduleName)
    archiveVersion.set(LibExt.libVersion)
    archiveClassifier.set("natives-linux-x64")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn("buildLinuxX64NativeBridge")
    onlyIf { isLinuxX64Host }

    from(providers.provider { linuxX64NativeBuildDir }) {
        include("**/libgdx2d_freetype_bridge.so")
        include("**/libfreetype.a")
        includeEmptyDirs = false
        eachFile {
            path = name
        }
    }
}

tasks.register<Jar>("freetype_natives_linux_arm64Jar") {
    group = "extensions"
    description = "Packages the Linux arm64 GLFW FreeType native bridge and native FreeType library."

    archiveBaseName.set(moduleName)
    archiveVersion.set(LibExt.libVersion)
    archiveClassifier.set("natives-linux-arm64")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    onlyIf { isLinuxArm64Host || (freetypeLinuxArm64NativeSourceDirProperty.isPresent && freetypeLinuxArm64NativeBridgeSourceDirProperty.isPresent) }

    if(isLinuxArm64Host) {
        dependsOn("freetype_prepare_source")
        doFirst {
            linuxArm64NativeSourceDir.mkdirs()
        }
        from(providers.provider {
            deleteDirectoryWithRetries(linuxArm64NativeSourceDir, "Linux arm64 native source directory")
            project.copy {
                from("src/main/resources/external_cpp/src/freetype")
                into(linuxArm64NativeSourceDir)
            }
            linuxArm64NativeSourceDir
        })
        doFirst {
            runCommand(
                linuxArm64NativeSourceDir,
                listOf(
                    "cmake",
                    "-S", linuxArm64NativeSourceDir.absolutePath,
                    "-B", linuxArm64NativeBuildDir.absolutePath,
                    "-DGDX_FREETYPE_SOURCE_DIR=${freetypeSourceCacheDir.absolutePath}",
                    "-DGDX_FREETYPE_BUILD_SHARED=ON",
                    "-DCMAKE_BUILD_TYPE=Release"
                )
            )
            runCommand(
                linuxArm64NativeSourceDir,
                listOf("cmake", "--build", linuxArm64NativeBuildDir.absolutePath)
            )
        }
        from(providers.provider { linuxArm64NativeBuildDir }) {
            include("**/libgdx2d_freetype_bridge.so")
            include("**/libfreetype.a")
            includeEmptyDirs = false
            eachFile {
                path = name
            }
        }
    }
    else {
        from(providers.provider {
            resolveDirectoryFromProperty(
                freetypeLinuxArm64NativeSourceDirProperty.orNull,
                "Set -PgdxFreetypeLinuxArm64NativeSourceDir=<path> to package Linux arm64 FreeType natives."
            )
        }) {
            include("**/*.so")
            include("**/*.so.*")
            includeEmptyDirs = false
            eachFile {
                path = name
            }
        }
        from(providers.provider {
            resolveDirectoryFromProperty(
                freetypeLinuxArm64NativeBridgeSourceDirProperty.orNull,
                "Set -PgdxFreetypeLinuxArm64NativeBridgeSourceDir=<path> to package Linux arm64 FreeType natives."
            )
        }) {
            include("**/*.a")
            includeEmptyDirs = false
            eachFile {
                path = name
            }
        }
    }
}

tasks.register<Sync>("prepareMacosX64NativeBridgeSource") {
    group = "extensions"
    description = "Copies the GLFW FreeType bridge sources into a macOS x64 native build workspace."

    from("src/main/resources/external_cpp/src/freetype")
    into(macosX64NativeSourceDir)
}

tasks.register("buildMacosX64NativeBridge") {
    group = "extensions"
    description = "Builds the macOS x64 GLFW FreeType native bridge and bundled FreeType static library."

    dependsOn("freetype_prepare_source")
    dependsOn("prepareMacosX64NativeBridgeSource")
    onlyIf { isMacHost }

    outputs.file(File(macosX64NativeBuildDir, "libgdx2d_freetype_bridge.dylib"))
    outputs.file(File(macosX64NativeBuildDir, "freetype-build/libfreetype.a"))

    doLast {
        macosX64NativeBuildDir.mkdirs()
        runCommand(
            macosX64NativeSourceDir,
            listOf(
                "cmake",
                "-S", macosX64NativeSourceDir.absolutePath,
                "-B", macosX64NativeBuildDir.absolutePath,
                "-DGDX_FREETYPE_SOURCE_DIR=${freetypeSourceCacheDir.absolutePath}",
                "-DGDX_FREETYPE_BUILD_SHARED=ON",
                "-DCMAKE_BUILD_TYPE=Release",
                "-DCMAKE_OSX_ARCHITECTURES=x86_64"
            )
        )
        runCommand(
            macosX64NativeSourceDir,
            listOf("cmake", "--build", macosX64NativeBuildDir.absolutePath)
        )
    }
}

tasks.register<Jar>("freetype_natives_macos_x64Jar") {
    group = "extensions"
    description = "Packages the macOS x64 GLFW FreeType native bridge and static FreeType library."

    archiveBaseName.set(moduleName)
    archiveVersion.set(LibExt.libVersion)
    archiveClassifier.set("natives-macos-x64")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    onlyIf { isMacHost || (freetypeMacosX64NativeSourceDirProperty.isPresent && freetypeMacosX64NativeBridgeSourceDirProperty.isPresent) }

    if(isMacHost) {
        dependsOn("buildMacosX64NativeBridge")
        from(providers.provider { macosX64NativeBuildDir }) {
            include("**/libgdx2d_freetype_bridge.dylib")
            include("**/libfreetype.a")
            includeEmptyDirs = false
            eachFile {
                path = name
            }
        }
    }
    else {
        from(providers.provider {
            resolveDirectoryFromProperty(
                freetypeMacosX64NativeSourceDirProperty.orNull,
                "Set -PgdxFreetypeMacosX64NativeSourceDir=<path> to package macOS x64 FreeType natives."
            )
        }) {
            include("**/*.dylib")
            includeEmptyDirs = false
            eachFile {
                path = name
            }
        }
        from(providers.provider {
            resolveDirectoryFromProperty(
                freetypeMacosX64NativeBridgeSourceDirProperty.orNull,
                "Set -PgdxFreetypeMacosX64NativeBridgeSourceDir=<path> to package macOS x64 FreeType natives."
            )
        }) {
            include("**/*.a")
            includeEmptyDirs = false
            eachFile {
                path = name
            }
        }
    }
}

tasks.register<Sync>("prepareMacosArm64NativeBridgeSource") {
    group = "extensions"
    description = "Copies the GLFW FreeType bridge sources into a macOS arm64 native build workspace."

    from("src/main/resources/external_cpp/src/freetype")
    into(macosArm64NativeSourceDir)
}

tasks.register("buildMacosArm64NativeBridge") {
    group = "extensions"
    description = "Builds the macOS arm64 GLFW FreeType native bridge and bundled FreeType static library."

    dependsOn("freetype_prepare_source")
    dependsOn("prepareMacosArm64NativeBridgeSource")
    onlyIf { isMacHost }

    outputs.file(File(macosArm64NativeBuildDir, "libgdx2d_freetype_bridge.dylib"))
    outputs.file(File(macosArm64NativeBuildDir, "freetype-build/libfreetype.a"))

    doLast {
        macosArm64NativeBuildDir.mkdirs()
        runCommand(
            macosArm64NativeSourceDir,
            listOf(
                "cmake",
                "-S", macosArm64NativeSourceDir.absolutePath,
                "-B", macosArm64NativeBuildDir.absolutePath,
                "-DGDX_FREETYPE_SOURCE_DIR=${freetypeSourceCacheDir.absolutePath}",
                "-DGDX_FREETYPE_BUILD_SHARED=ON",
                "-DCMAKE_BUILD_TYPE=Release",
                "-DCMAKE_OSX_ARCHITECTURES=arm64"
            )
        )
        runCommand(
            macosArm64NativeSourceDir,
            listOf("cmake", "--build", macosArm64NativeBuildDir.absolutePath)
        )
    }
}

tasks.register<Jar>("freetype_natives_macos_arm64Jar") {
    group = "extensions"
    description = "Packages the macOS arm64 GLFW FreeType native bridge and static FreeType library."

    archiveBaseName.set(moduleName)
    archiveVersion.set(LibExt.libVersion)
    archiveClassifier.set("natives-macos-arm64")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    onlyIf { isMacHost || (freetypeMacosArm64NativeSourceDirProperty.isPresent && freetypeMacosArm64NativeBridgeSourceDirProperty.isPresent) }

    if(isMacHost) {
        dependsOn("buildMacosArm64NativeBridge")
        from(providers.provider { macosArm64NativeBuildDir }) {
            include("**/libgdx2d_freetype_bridge.dylib")
            include("**/libfreetype.a")
            includeEmptyDirs = false
            eachFile {
                path = name
            }
        }
    }
    else {
        from(providers.provider {
            resolveDirectoryFromProperty(
                freetypeMacosArm64NativeSourceDirProperty.orNull,
                "Set -PgdxFreetypeMacosArm64NativeSourceDir=<path> to package macOS arm64 FreeType natives."
            )
        }) {
            include("**/*.dylib")
            includeEmptyDirs = false
            eachFile {
                path = name
            }
        }
        from(providers.provider {
            resolveDirectoryFromProperty(
                freetypeMacosArm64NativeBridgeSourceDirProperty.orNull,
                "Set -PgdxFreetypeMacosArm64NativeBridgeSourceDir=<path> to package macOS arm64 FreeType natives."
            )
        }) {
            include("**/*.a")
            includeEmptyDirs = false
            eachFile {
                path = name
            }
        }
    }
}

tasks.register<Jar>("freetype_natives_windows_x64Jar") {
    group = "extensions"
    description = "Packages the Windows x64 GLFW FreeType native bridge and import library."

    archiveBaseName.set(moduleName)
    archiveVersion.set(LibExt.libVersion)
    archiveClassifier.set("natives-windows-x64")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    onlyIf { freetypeWindowsX64NativeSourceDirProperty.isPresent && freetypeWindowsX64NativeBridgeSourceDirProperty.isPresent }

    from(providers.provider {
        resolveDirectoryFromProperty(
            freetypeWindowsX64NativeSourceDirProperty.orNull,
            "Set -PgdxFreetypeWindowsX64NativeSourceDir=<path> to package Windows x64 FreeType natives."
        )
    }) {
        include("**/*.dll")
        includeEmptyDirs = false
        eachFile {
            path = name
        }
    }
    from(providers.provider {
        resolveDirectoryFromProperty(
            freetypeWindowsX64NativeBridgeSourceDirProperty.orNull,
            "Set -PgdxFreetypeWindowsX64NativeBridgeSourceDir=<path> to package Windows x64 FreeType natives."
        )
    }) {
        include("**/*.lib")
        includeEmptyDirs = false
        eachFile {
            path = name
        }
    }
}

tasks.register<Jar>("freetype_natives_windows_arm64Jar") {
    group = "extensions"
    description = "Packages the Windows arm64 GLFW FreeType native bridge and import library."

    archiveBaseName.set(moduleName)
    archiveVersion.set(LibExt.libVersion)
    archiveClassifier.set("natives-windows-arm64")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    onlyIf { freetypeWindowsArm64NativeSourceDirProperty.isPresent && freetypeWindowsArm64NativeBridgeSourceDirProperty.isPresent }

    from(providers.provider {
        resolveDirectoryFromProperty(
            freetypeWindowsArm64NativeSourceDirProperty.orNull,
            "Set -PgdxFreetypeWindowsArm64NativeSourceDir=<path> to package Windows arm64 FreeType natives."
        )
    }) {
        include("**/*.dll")
        includeEmptyDirs = false
        eachFile {
            path = name
        }
    }
    from(providers.provider {
        resolveDirectoryFromProperty(
            freetypeWindowsArm64NativeBridgeSourceDirProperty.orNull,
            "Set -PgdxFreetypeWindowsArm64NativeBridgeSourceDir=<path> to package Windows arm64 FreeType natives."
        )
    }) {
        include("**/*.lib")
        includeEmptyDirs = false
        eachFile {
            path = name
        }
    }
}

tasks.register("freetype_clean_cache") {
    group = "extensions"
    description = "Deletes the cached FreeType source downloaded for the GLFW freetype extension."

    doLast {
        deleteDirectoryWithRetries(freetypeCacheRoot, "cached FreeType root directory")
    }
}

tasks.register("freetype_clean_source") {
    group = "extensions"
    description = "Deletes the generated FreeType resources used by the GLFW freetype extension."

    doLast {
        deleteDirectoryWithRetries(
            freetypeGeneratedResourcesDir,
            "generated FreeType resources directory"
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = moduleName
            group = LibExt.groupId
            version = LibExt.libVersion
            from(components["java"])
            artifact(tasks.named("freetype_withCSourcesJar"))
            if(isLinuxX64Host) {
                artifact(tasks.named("freetype_natives_linux_x64Jar"))
            }
            if(isLinuxArm64Host || (freetypeLinuxArm64NativeSourceDirProperty.isPresent && freetypeLinuxArm64NativeBridgeSourceDirProperty.isPresent)) {
                artifact(tasks.named("freetype_natives_linux_arm64Jar"))
            }
            if(isMacHost || (freetypeMacosX64NativeSourceDirProperty.isPresent && freetypeMacosX64NativeBridgeSourceDirProperty.isPresent)) {
                artifact(tasks.named("freetype_natives_macos_x64Jar"))
            }
            if(isMacHost || (freetypeMacosArm64NativeSourceDirProperty.isPresent && freetypeMacosArm64NativeBridgeSourceDirProperty.isPresent)) {
                artifact(tasks.named("freetype_natives_macos_arm64Jar"))
            }
            if(freetypeWindowsX64NativeSourceDirProperty.isPresent && freetypeWindowsX64NativeBridgeSourceDirProperty.isPresent) {
                artifact(tasks.named("freetype_natives_windows_x64Jar"))
            }
            if(freetypeWindowsArm64NativeSourceDirProperty.isPresent && freetypeWindowsArm64NativeBridgeSourceDirProperty.isPresent) {
                artifact(tasks.named("freetype_natives_windows_arm64Jar"))
            }
            if(freetypeNativeSourceDirProperty.isPresent) {
                artifact(tasks.named("freetype_nativesJar"))
            }
        }
    }
}
