import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
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

tasks.register<Jar>("freetype_nativesJar") {
    group = "extensions"
    description = "Packages one platform/architecture's FreeType native binaries into a classifier jar."

    archiveBaseName.set(moduleName)
    archiveClassifier.set(freetypeNativeClassifier)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    onlyIf { freetypeNativeSourceDirProperty.isPresent }

    from(providers.provider { resolveFreetypeNativeSourceDir() }) {
        include("**/*.dll")
        include("**/*.so")
        include("**/*.so.*")
        include("**/*.dylib")
        include("**/*.lib")
        include("**/*.a")
        includeEmptyDirs = false
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
            if(freetypeNativeSourceDirProperty.isPresent) {
                artifact(tasks.named("freetype_nativesJar"))
            }
        }
    }
}
