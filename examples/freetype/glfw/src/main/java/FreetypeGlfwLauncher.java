import com.github.xpenatan.gdx.teavm.backends.glfw.GLFWApplication;
import com.github.xpenatan.gdx.teavm.backends.glfw.GLFWApplicationConfiguration;
import com.github.xpenatan.gdx.teavm.examples.freetype.FreetypeDemo;

public class FreetypeGlfwLauncher {

    public static void main(String[] args) {
        GLFWApplicationConfiguration config = new GLFWApplicationConfiguration();
        config.useVsync(false);
        config.setForegroundFPS(60);

        System.setProperty("os.name", "Windows");

        new GLFWApplication(new FreetypeDemo(), config);
    }
}
