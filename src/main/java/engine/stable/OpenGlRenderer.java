package engine.stable;

import chunk.Chunk;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.function.BiConsumer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;


public class OpenGlRenderer {
    private final long window;
    private int windowWidth, windowHeight;

    private final int CHUNK_VAO;
    private final int SKYBOX_VAO;

    private final Shader SHADER_DEFAULT_BLOCK;
    private final Shader SHADER_SKYBOX;

    private final Skybox SKYBOX;

    private float deltaTime;
    private float lastFrame;

    private static final Vector3f worldUp = new Vector3f(0, 1, 0);

    private final BiConsumer<Key, Float> onKeyPress;

    private Texture TEXTURE_ATLAS;

    public OpenGlRenderer(
            String windowTitle,
            int windowWidth,
            int windowHeight,
            BiConsumer<Double, Double> onMouseMove,
            BiConsumer<Key, Float> onKeyPress
    ) {
        //////////////////////////////////////////////////////////////////////////////////////
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        //////////////////////////////////////////////////////////////////////////////////////
        Log.init();
        Log.info("Initializing Logging...");
        System.out.println("LWJGL Version: " + Version.getVersion());
        System.out.println("GLFW Version: " + org.lwjgl.glfw.GLFW.glfwGetVersionString());
        //////////////////////////////////////////////////////////////////////////////////////
        Log.info("Initializing GLFW...");
        GLFWErrorCallback.createPrint(System.err).set();
        if(!glfwInit()) throw new IllegalStateException("GLFW initialization failed!");
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        window = glfwCreateWindow(windowWidth, windowHeight, windowTitle, NULL, NULL);
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        //////////////////////////////////////////////////////////////////////////////////////
        Log.info("Setting GLFW window callbacks...");
        glfwSetWindowSizeCallback(window, (window, width, height) -> {
            this.windowWidth = width;
            this.windowHeight = height;
        });
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if(key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) glfwSetWindowShouldClose(window, true);
        });
        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            onMouseMove.accept(xpos, ypos);
        });
        this.onKeyPress = onKeyPress;
        //////////////////////////////////////////////////////////////////////////////////////
        Log.info("Initialize GLFW framebuffer...");
        try ( MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            assert vidmode != null;
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        }
        //////////////////////////////////////////////////////////////////////////////////////
        Log.info("Initializing GLFW OpenGL Context...");
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        GL.createCapabilities();
        glClearColor(0.0f,0.0f,0.0f, 0.0f);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CW);

        CHUNK_VAO = glGenVertexArrays();
        SKYBOX_VAO = glGenVertexArrays();
        //////////////////////////////////////////////////////////////////////////////////////
        Log.info("Initializing Shaders...");
        SHADER_DEFAULT_BLOCK = new Shader(
                AssetLoader.getCoreShaderPath("block.vert"),
                AssetLoader.getCoreShaderPath("block.frag")
        );
        SHADER_SKYBOX = new Shader(
                AssetLoader.getCoreShaderPath("skybox.vert"),
                AssetLoader.getCoreShaderPath("skybox.frag")
        );
        //////////////////////////////////////////////////////////////////////////////////////
        Log.info("Initializing skybox...");
        SKYBOX = new Skybox();

        //////////////////////////////////////////////////////////////////////////////////////
        Log.info("Initializing Textures...");
        TEXTURE_ATLAS = new Texture("atlas.png", GL_TEXTURE1);

        Log.info("Finished initializing");
    }

    private void processInput() {
        float cameraSpeed = 2.5f * deltaTime;
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) onKeyPress.accept(Key.WALK_FORWARD, deltaTime);
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) onKeyPress.accept(Key.WALK_BACKWARD, deltaTime);
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) onKeyPress.accept(Key.WALK_LEFT, deltaTime);
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) onKeyPress.accept(Key.WALK_RIGHT, deltaTime);
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) onKeyPress.accept(Key.JUMP, deltaTime);
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) onKeyPress.accept(Key.CROUCH, deltaTime);
    }

    public boolean render(Vector3f position, Vector3f direction, int fov, ArrayList<Chunk> chunks) {
        if(glfwWindowShouldClose(window)) return false;

        Matrix4f modelMatrix = new Matrix4f();
        Matrix4f viewMatrix = new Matrix4f().lookAt(position, new Vector3f(position).add(direction), new Vector3f(new Vector3f(direction).cross(worldUp).normalize()).cross(direction).normalize());
        Matrix4f projectionMatrix = new Matrix4f().perspective((float) Math.toRadians(fov), ((float) windowWidth) / ((float) windowHeight), 0.1f, Chunk.CHUNK_WIDTH * 48);

        float currentFrame = (float) glfwGetTime();
        deltaTime = currentFrame - lastFrame;
        lastFrame = currentFrame;

        processInput();
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Draw the Chunks
        SHADER_DEFAULT_BLOCK.use();
        SHADER_DEFAULT_BLOCK.setInt("TEXTURE_ATLAS", 1);
        SHADER_DEFAULT_BLOCK.setMatrix4f("model", modelMatrix);
        SHADER_DEFAULT_BLOCK.setMatrix4f("view", viewMatrix);
        SHADER_DEFAULT_BLOCK.setMatrix4f("projection", projectionMatrix);
        glBindVertexArray(CHUNK_VAO);
        for(Chunk chunk : chunks) {
            for(int v:chunk.getVBOs()) {
                glBindBuffer(GL_ARRAY_BUFFER, v);
                glVertexAttribPointer(0, 3, GL_FLOAT, false, 20, 0);
                glEnableVertexAttribArray(0); // Position
                glVertexAttribPointer(1, 2, GL_FLOAT, false, 20, 12);
                glEnableVertexAttribArray(1); // TexCoords
                glDrawArrays(GL_TRIANGLES, 0, 9999999);
            }
        }

        // Draw the Skybox
        SHADER_SKYBOX.use();
        SHADER_SKYBOX.setMatrix4f("view", new Matrix4f(new Matrix3f(viewMatrix)));
        SHADER_SKYBOX.setMatrix4f("projection", projectionMatrix);
        glBindVertexArray(SKYBOX_VAO);

        glCullFace(GL_FRONT);
        glBindBuffer(GL_ARRAY_BUFFER, SKYBOX.getVBO());
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 24, 0);
        glEnableVertexAttribArray(0); // Position
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 24, 12);
        glEnableVertexAttribArray(1); // Color
        glDrawArrays(GL_TRIANGLES, 0, 36);
        glCullFace(GL_BACK);

        glfwSwapBuffers(window);
        glfwPollEvents();
        return true;
    }
}
