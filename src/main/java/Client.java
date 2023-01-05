import chunk.Chunk;
import chunk.World;
import engine.stable.Key;
import engine.stable.OpenGlRenderer;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class Client {
    private Vector3f location, prevLocation;
    private float yaw, pitch, prevMouseX, prevMouseY;

    private OpenGlRenderer engine;
    private World world;

    private int renderDistance;

    private Thread chunkThread;

    public Client() {
        this.location = new Vector3f(0, 70, 0);
        this.prevLocation = new Vector3f(0, 70, 0);

        prevMouseX = 0;
        prevMouseY = 0;

        this.renderDistance = 2;

        this.world = new World();

        this.engine = new OpenGlRenderer(
                "VoxelGame",
                840,
                680,
                this::onMouseMove,
                this::onKeyPress
        );

        // Load in the first chunks
        chunkThread = new Thread(() -> world.updateChunks(
                (int) Math.floor(location.x / Chunk.CHUNK_WIDTH),
                (int) Math.floor(location.z / Chunk.CHUNK_WIDTH),
                renderDistance
        ));
        chunkThread.start();

        boolean running = true;
        while(running) {
            world.updateChunks();

            Vector3f front = new Vector3f();
            front.x = (float) Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
            front.y = (float) Math.sin(Math.toRadians(pitch));
            front.z = (float) Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));

            running = engine.render(
                    location,
                    front.normalize(),
                    90,
                    new ArrayList<>(new ArrayList<>(world.getChunks()).stream().filter(Chunk::isMeshGenerated).collect(Collectors.toList()))
            );
        }
        chunkThread.interrupt();
    }

    public void onKeyPress(Key key, float deltaTime) {
        float velocity = 20f * deltaTime;

        Vector3f front = new Vector3f();
        front.x = (float) Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
        front.y = (float) Math.sin(Math.toRadians(pitch));
        front.z = (float) Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
        Vector3f Front = front.normalize();

        Vector3f WorldUp = new Vector3f(0, 1, 0);
        Vector3f Right = new Vector3f(Front).cross(WorldUp).normalize();
        Vector3f Up = new Vector3f(Right).cross(Front).normalize();

        if (key == Key.WALK_FORWARD) {
            location.add(new Vector3f(Front).set(Front.x,0.0f, Front.z).normalize().mul(velocity));
        }
        if (key == Key.WALK_BACKWARD) {
            location.sub(new Vector3f(Front).set(Front.x,0.0f, Front.z).normalize().mul(velocity));
        }
        if (key == Key.WALK_LEFT) {
            location.sub(new Vector3f(Right).mul(velocity));
        }
        if (key == Key.WALK_RIGHT) {
            location.add(new Vector3f(Right).mul(velocity));
        }
        if (key == Key.JUMP) {
            location.add(new Vector3f(WorldUp).mul(velocity));
        }
        if (key == Key.CROUCH) {
            location.sub(new Vector3f(WorldUp).mul(velocity));
        }

        if((int)Math.floor(location.x/Chunk.CHUNK_WIDTH) != (int)Math.floor(prevLocation.x/Chunk.CHUNK_WIDTH) || (int)Math.floor(location.z/Chunk.CHUNK_WIDTH) != (int)Math.floor(prevLocation.z/Chunk.CHUNK_WIDTH)) {
            if(chunkThread.isAlive()) chunkThread.interrupt();
            chunkThread = new Thread(() -> world.updateChunks(
                    (int) Math.floor(location.x / Chunk.CHUNK_WIDTH),
                    (int) Math.floor(location.z / Chunk.CHUNK_WIDTH),
                    renderDistance
            ));
            chunkThread.start();
        }

        prevLocation = new Vector3f(location.x, location.y, location.z);
    }

    public void onMouseMove(double xpos, double ypos) {
        float xoffset = (float) (xpos - prevMouseX);
        float yoffset = (float) (prevMouseY - ypos);
        prevMouseX = (float) xpos;
        prevMouseY = (float) ypos;

        xoffset *= 0.1f;
        yoffset *= 0.1f;

        yaw += xoffset;
        pitch += yoffset;

        if (pitch > 89.0f) {
            pitch = 89.0f;
        }
        if (yaw < -89.0f) {
            yaw = -89.0f;
        }
    }
    public static void main(String[] args) {
        new Client();
    }
}
