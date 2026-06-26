package com.github.xpenatan.gdx.teavm.backends.glfw.config.backend;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class TeaGLFWNativeProject {
    private static final String BUILD_SCRIPT_TEMPLATE_ROOT = "templates/glfw/";

    private final ClassLoader classLoader;
    private final File buildRoot;
    private final File generatedSources;
    private final File releasePath;

    public TeaGLFWNativeProject(ClassLoader classLoader, File buildRoot, File generatedSources, File releasePath) {
        this.classLoader = classLoader;
        this.buildRoot = buildRoot;
        this.generatedSources = generatedSources;
        this.releasePath = releasePath;
    }

    public void write(String projectName) throws IOException {
        ensureDirectory(buildRoot, "GLFW output root");
        ensureDirectory(generatedSources, "GLFW generated sources");
        ensureDirectory(releasePath, "GLFW release path");

        copyAppInclude();
        writeCMakeLists(projectName);
        writeBuildScripts(projectName);
    }

    public void executeBuildScript(TeaGLFWBackend.NativeBuildType buildType) {
        if(isWindows()) {
            executeWindowsBuild(buildType);
            return;
        }

        File scriptFile = new File(buildRoot, getBuildScriptName(buildType));
        if(!scriptFile.isFile()) {
            throw new RuntimeException("Expected GLFW build script was not generated: " + scriptFile.getAbsolutePath());
        }

        ProcessBuilder processBuilder = new ProcessBuilder("bash", scriptFile.getAbsolutePath());
        processBuilder.directory(buildRoot);
        configureWindowsToolPath(processBuilder);
        executeProcess(processBuilder, "GLFW native build failed", false);
    }

    public void runExecutable(String projectName, TeaGLFWBackend.NativeBuildType buildType, boolean consoleLog) {
        File executableFile = new File(releasePath, getExecutableName(projectName, buildType));
        if(!executableFile.isFile()) {
            throw new RuntimeException("Expected GLFW executable was not built: " + executableFile.getAbsolutePath());
        }

        if(consoleLog && isWindows()) {
            ProcessBuilder processBuilder = createWindowsConsoleProcess(executableFile);
            configureWindowsToolPath(processBuilder);
            executeProcess(processBuilder, "GLFW executable failed", false);
        }
        else {
            ProcessBuilder processBuilder = new ProcessBuilder(executableFile.getAbsolutePath());
            processBuilder.directory(releasePath);
            configureWindowsToolPath(processBuilder);
            executeProcess(processBuilder, "GLFW executable failed", consoleLog);
        }
    }

    private void ensureDirectory(File file, String name) {
        if(file == null) {
            throw new IllegalStateException(name + " was not configured");
        }
        if(!file.exists() && !file.mkdirs()) {
            throw new IllegalStateException("Unable to create " + name + ": " + file.getAbsolutePath());
        }
    }

    private void copyAppInclude() throws IOException {
        try(var input = classLoader.getResourceAsStream("app_include.c")) {
            if(input == null) {
                throw new IOException("app_include.c not found in resources");
            }
            Files.writeString(new File(generatedSources, "app_include.c").toPath(),
                    new String(input.readAllBytes(), StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        }
    }

    private void writeCMakeLists(String projectName) throws IOException {
        try(var input = classLoader.getResourceAsStream("CMakeLists.txt")) {
            if(input == null) {
                throw new IOException("CMakeLists.txt template not found in resources");
            }
            String template = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            template = template.replace("${PROJECT_NAME}", projectName);
            template = template.replace("${RELEASE_PATH}", releasePath.getAbsolutePath().replace("\\", "/"));
            Files.writeString(new File(buildRoot, "CMakeLists.txt").toPath(), template, StandardCharsets.UTF_8);
        }
    }

    private void writeBuildScripts(String projectName) throws IOException {
        writeBuildScript("app_release.bat", projectName, "Release", false);
        writeBuildScript("app_debug.bat", projectName, "Debug", false);
        writeBuildScript("app_release.sh", projectName, "Release", true);
        writeBuildScript("app_debug.sh", projectName, "Debug", true);
    }

    private void writeBuildScript(String scriptName, String projectName, String buildConfig, boolean executable)
            throws IOException {
        String templatePath = BUILD_SCRIPT_TEMPLATE_ROOT + scriptName;
        try(var input = classLoader.getResourceAsStream(templatePath)) {
            if(input == null) {
                throw new IOException(templatePath + " template not found in resources");
            }
            String content = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("${PROJECT_NAME}", scriptName.endsWith(".sh") ? escapeShellValue(projectName) : projectName)
                    .replace("${BUILD_CONFIG}", buildConfig);
            File script = new File(buildRoot, scriptName);
            Files.writeString(script.toPath(), content, StandardCharsets.UTF_8);
            if(executable) {
                script.setExecutable(true, false);
            }
        }
    }

    private ProcessBuilder createWindowsConsoleProcess(File executableFile) {
        File consoleScript = writeWindowsConsoleRunScript(executableFile);
        String title = "GLFW " + executableFile.getName();
        return new ProcessBuilder("cmd", "/c", "start", title, "/wait", consoleScript.getAbsolutePath());
    }

    private File writeWindowsConsoleRunScript(File executableFile) {
        File scriptFile = new File(buildRoot, removeExtension(executableFile.getName()) + "_console.bat");
        String templatePath = BUILD_SCRIPT_TEMPLATE_ROOT + "app_console.bat";
        try(var input = classLoader.getResourceAsStream(templatePath)) {
            if(input == null) {
                throw new RuntimeException(templatePath + " template not found in resources");
            }
            String content = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("${WINDOW_TITLE}", escapeBatchValue("GLFW " + executableFile.getName()))
                    .replace("${WORKING_DIRECTORY}", escapeBatchValue(releasePath.getAbsolutePath()))
                    .replace("${EXECUTABLE_PATH}", escapeBatchValue(executableFile.getAbsolutePath()));
            Files.writeString(scriptFile.toPath(), content, StandardCharsets.UTF_8);
            return scriptFile;
        } catch(IOException e) {
            throw new RuntimeException("Failed to setup " + scriptFile.getAbsolutePath(), e);
        }
    }

    private void executeProcess(ProcessBuilder processBuilder, String failureMessage, boolean inheritIO) {
        processBuilder.redirectErrorStream(true);
        try {
            if(inheritIO) {
                processBuilder.inheritIO();
            }
            Process process = processBuilder.start();
            if(!inheritIO) {
                try(BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                }
            }
            int exitCode = process.waitFor();
            if(exitCode != 0) {
                throw new RuntimeException(failureMessage + " with exit code " + exitCode);
            }
        } catch(Exception e) {
            throw new RuntimeException(failureMessage, e);
        }
    }

    private String getBuildScriptName(TeaGLFWBackend.NativeBuildType buildType) {
        return "app_" + buildType.getOutputSuffix() + (isWindows() ? ".bat" : ".sh");
    }

    private String getExecutableName(String projectName, TeaGLFWBackend.NativeBuildType buildType) {
        return projectName + "_" + buildType.getOutputSuffix() + (isWindows() ? ".exe" : "");
    }

    private String removeExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if(dotIndex == -1) {
            return fileName;
        }
        return fileName.substring(0, dotIndex);
    }

    private String escapeShellValue(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("$", "\\$")
                .replace("`", "\\`");
    }

    private String escapeBatchValue(String value) {
        return value.replace("%", "%%");
    }

    private void configureWindowsToolPath(ProcessBuilder processBuilder) {
        if(!isWindows()) {
            return;
        }

        StringBuilder extraPath = new StringBuilder();
        ArrayList<File> roots = new ArrayList<>();
        addProgramFilesRoot(roots, System.getenv("ProgramFiles"));
        addProgramFilesRoot(roots, System.getenv("ProgramFiles(x86)"));
        addProgramFilesRoot(roots, "C:\\Program Files");
        addProgramFilesRoot(roots, "C:\\Program Files (x86)");
        addProgramFilesRoot(roots, "D:\\Program Files");
        addProgramFilesRoot(roots, "D:\\Program Files (x86)");

        for(File root : roots) {
            File vsRoot = new File(root, "Microsoft Visual Studio");
            File[] yearDirs = vsRoot.listFiles(File::isDirectory);
            if(yearDirs == null) {
                continue;
            }
            for(File yearDir : yearDirs) {
                File[] editionDirs = yearDir.listFiles(File::isDirectory);
                if(editionDirs == null) {
                    continue;
                }
                for(File editionDir : editionDirs) {
                    appendIfDirectory(extraPath, new File(editionDir,
                            "Common7\\IDE\\CommonExtensions\\Microsoft\\CMake\\CMake\\bin"));
                    appendIfDirectory(extraPath, new File(editionDir, "MSBuild\\Current\\Bin"));
                    appendIfDirectory(extraPath, new File(editionDir, "MSBuild\\Current\\Bin\\amd64"));
                    appendIfDirectory(extraPath, new File(editionDir, "MSBuild\\Current\\Bin\\x86"));
                }
            }
        }

        if(extraPath.length() == 0) {
            return;
        }

        Map<String, String> environment = processBuilder.environment();
        String existingPath = getEnvironmentValue(environment, "Path");
        if(existingPath == null || existingPath.isEmpty()) {
            existingPath = System.getenv("Path");
        }
        if(existingPath == null || existingPath.isEmpty()) {
            existingPath = System.getenv("PATH");
        }
        String combinedPath = existingPath == null || existingPath.isEmpty()
                ? extraPath.toString()
                : extraPath + ";" + existingPath;
        environment.put("Path", combinedPath);
        removeEnvironmentKeys(environment, "PATH");
    }

    private void addProgramFilesRoot(ArrayList<File> roots, String path) {
        if(path == null || path.isBlank()) {
            return;
        }
        File root = new File(path);
        if(root.isDirectory() && !roots.contains(root)) {
            roots.add(root);
        }
    }

    private void appendIfDirectory(StringBuilder builder, File directory) {
        if(!directory.isDirectory()) {
            return;
        }
        if(builder.length() > 0) {
            builder.append(';');
        }
        builder.append(directory.getAbsolutePath());
    }

    private void executeWindowsBuild(TeaGLFWBackend.NativeBuildType buildType) {
        File cmakeDir = new File(buildRoot, "build/cmake");
        if(!cmakeDir.exists() && !cmakeDir.mkdirs()) {
            throw new RuntimeException("Unable to create GLFW CMake build directory: " + cmakeDir.getAbsolutePath());
        }

        File cmakeExecutable = findWindowsTool(
                "Common7\\IDE\\CommonExtensions\\Microsoft\\CMake\\CMake\\bin\\cmake.exe");
        if(cmakeExecutable == null) {
            throw new RuntimeException("Unable to locate cmake.exe in PATH or Visual Studio installation");
        }

        File vcvarsBatch = findWindowsTool("Common7\\Tools\\VsDevCmd.bat");
        String vcvarsArgs = "-arch=x64";
        if(vcvarsBatch == null) {
            vcvarsBatch = findWindowsTool("VC\\Auxiliary\\Build\\vcvars64.bat");
            vcvarsArgs = "";
        }
        Map<String, String> buildEnvironment = createWindowsBuildEnvironment(vcvarsBatch, vcvarsArgs);
        String visualStudioGenerator = findVisualStudioGenerator(vcvarsBatch);
        if(visualStudioGenerator == null) {
            visualStudioGenerator = findVisualStudioGenerator(cmakeExecutable);
        }
        if(visualStudioGenerator != null) {
            resetCmakeCache(cmakeDir);
        }

        ArrayList<String> cmakeCommand = new ArrayList<>();
        cmakeCommand.add(cmakeExecutable.getAbsolutePath());
        cmakeCommand.add("-S");
        cmakeCommand.add(buildRoot.getAbsolutePath());
        cmakeCommand.add("-B");
        cmakeCommand.add(cmakeDir.getAbsolutePath());
        if(visualStudioGenerator != null) {
            cmakeCommand.add("-G");
            cmakeCommand.add(visualStudioGenerator);
            cmakeCommand.add("-A");
            cmakeCommand.add("x64");
        }
        else {
            cmakeCommand.add("-DCMAKE_BUILD_TYPE=" + buildType.getCmakeConfig());
        }
        ProcessBuilder cmakeBuilder = new ProcessBuilder(cmakeCommand);
        cmakeBuilder.directory(buildRoot);
        applyEnvironment(cmakeBuilder, buildEnvironment);
        executeProcess(cmakeBuilder, "GLFW native CMake generation failed", false);

        ArrayList<String> buildCommand = new ArrayList<>();
        buildCommand.add(cmakeExecutable.getAbsolutePath());
        buildCommand.add("--build");
        buildCommand.add(cmakeDir.getAbsolutePath());
        if(visualStudioGenerator != null) {
            buildCommand.add("--config");
            buildCommand.add(buildType.getCmakeConfig());
        }
        ProcessBuilder buildBuilder = new ProcessBuilder(buildCommand);
        buildBuilder.directory(cmakeDir);
        applyEnvironment(buildBuilder, buildEnvironment);
        executeProcess(buildBuilder, "GLFW native build failed", false);
    }

    private File findWindowsTool(String relativePath) {
        String pathValue = System.getenv("PATH");
        if(pathValue != null && !pathValue.isBlank()) {
            String[] pathEntries = pathValue.split(";");
            for(String pathEntry : pathEntries) {
                if(pathEntry == null || pathEntry.isBlank()) {
                    continue;
                }
                pathEntry = pathEntry.trim().replace("\"", "");
                File candidate = new File(pathEntry, new File(relativePath).getName());
                if(candidate.isFile()) {
                    return candidate;
                }
            }
        }

        ArrayList<File> roots = new ArrayList<>();
        addProgramFilesRoot(roots, System.getenv("ProgramFiles"));
        addProgramFilesRoot(roots, System.getenv("ProgramFiles(x86)"));
        addProgramFilesRoot(roots, "C:\\Program Files");
        addProgramFilesRoot(roots, "C:\\Program Files (x86)");
        addProgramFilesRoot(roots, "D:\\Program Files");
        addProgramFilesRoot(roots, "D:\\Program Files (x86)");

        for(File root : roots) {
            File vsRoot = new File(root, "Microsoft Visual Studio");
            File[] yearDirs = vsRoot.listFiles(File::isDirectory);
            if(yearDirs == null) {
                continue;
            }
            for(File yearDir : yearDirs) {
                File[] editionDirs = yearDir.listFiles(File::isDirectory);
                if(editionDirs == null) {
                    continue;
                }
                for(File editionDir : editionDirs) {
                    File candidate = new File(editionDir, relativePath);
                    if(candidate.isFile()) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private Map<String, String> createWindowsBuildEnvironment(File vcvarsBatch, String vcvarsArgs) {
        if(vcvarsBatch == null || !vcvarsBatch.isFile()) {
            ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", "set");
            processBuilder.directory(buildRoot);
            configureWindowsToolPath(processBuilder);
            return captureEnvironment(processBuilder, "Unable to initialize Windows build environment");
        }

        StringBuilder commandLine = new StringBuilder();
        commandLine.append("call ").append(quoteForCmd(vcvarsBatch.getAbsolutePath()));
        if(vcvarsArgs != null && !vcvarsArgs.isBlank()) {
            commandLine.append(' ').append(vcvarsArgs);
        }
        commandLine.append(" >nul && set");

        ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", commandLine.toString());
        processBuilder.directory(buildRoot);
        configureWindowsToolPath(processBuilder);
        return captureEnvironment(processBuilder, "Unable to initialize Visual Studio build environment");
    }

    private Map<String, String> captureEnvironment(ProcessBuilder processBuilder, String failureMessage) {
        processBuilder.redirectErrorStream(true);
        try {
            Map<String, String> environment = new LinkedHashMap<>();
            mergeEnvironment(environment, processBuilder.environment());
            Process process = processBuilder.start();
            try(BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while((line = reader.readLine()) != null) {
                    int equalsIndex = line.indexOf('=');
                    if(equalsIndex <= 0) {
                        continue;
                    }
                    String key = line.substring(0, equalsIndex);
                    String value = line.substring(equalsIndex + 1);
                    putEnvironmentValue(environment, key, value);
                }
            }
            int exitCode = process.waitFor();
            if(exitCode != 0) {
                throw new RuntimeException(failureMessage + " with exit code " + exitCode);
            }
            return environment;
        } catch(Exception e) {
            throw new RuntimeException(failureMessage, e);
        }
    }

    private void applyEnvironment(ProcessBuilder processBuilder, Map<String, String> environment) {
        Map<String, String> processEnvironment = processBuilder.environment();
        processEnvironment.clear();
        mergeEnvironment(processEnvironment, environment);
    }

    private String findVisualStudioGenerator(File toolFile) {
        if(toolFile == null) {
            return null;
        }
        File current = toolFile;
        while(current != null) {
            File parent = current.getParentFile();
            if(parent != null && "Microsoft Visual Studio".equals(parent.getName())) {
                return mapVisualStudioYearToGenerator(current.getName());
            }
            current = parent;
        }
        return null;
    }

    private String mapVisualStudioYearToGenerator(String year) {
        return switch(year) {
            case "2022" -> "Visual Studio 17 2022";
            case "2019" -> "Visual Studio 16 2019";
            case "2017" -> "Visual Studio 15 2017";
            case "18" -> "Visual Studio 18 2026";
            default -> null;
        };
    }

    private void resetCmakeCache(File cmakeDir) {
        if(!cmakeDir.exists()) {
            return;
        }
        try(var stream = Files.walk(cmakeDir.toPath())) {
            stream.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .map(Path::toFile)
                    .forEach(file -> {
                        if(!file.delete() && file.exists()) {
                            throw new RuntimeException("Unable to delete stale CMake output: " + file.getAbsolutePath());
                        }
                    });
        } catch(IOException e) {
            throw new RuntimeException("Unable to reset stale CMake output: " + cmakeDir.getAbsolutePath(), e);
        }
        if(!cmakeDir.exists() && !cmakeDir.mkdirs()) {
            throw new RuntimeException("Unable to recreate GLFW CMake build directory: " + cmakeDir.getAbsolutePath());
        }
    }

    private String getEnvironmentValue(Map<String, String> environment, String key) {
        for(Map.Entry<String, String> entry : environment.entrySet()) {
            if(entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void putEnvironmentValue(Map<String, String> environment, String key, String value) {
        removeEnvironmentKeys(environment, key);
        environment.put(key, value);
    }

    private void mergeEnvironment(Map<String, String> target, Map<String, String> source) {
        for(Map.Entry<String, String> entry : source.entrySet()) {
            putEnvironmentValue(target, entry.getKey(), entry.getValue());
        }
    }

    private void removeEnvironmentKeys(Map<String, String> environment, String key) {
        Set<String> keysToRemove = new LinkedHashSet<>();
        for(String existingKey : environment.keySet()) {
            if(existingKey.equalsIgnoreCase(key)) {
                keysToRemove.add(existingKey);
            }
        }
        for(String existingKey : keysToRemove) {
            environment.remove(existingKey);
        }
    }

    private String quoteForCmd(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows");
    }
}
