package chunk;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;

public class Chunk {
    public static final int CHUNK_WIDTH = 16;
    public static final int CHUNK_HEIGHT = 256;
    public static final int SUBCHUNK_HEIGHT = 16;

    private final int chunkX, chunkZ;
    private final Block[][] voxels;
    private final Subchunk[] subchunks;
    private boolean isTerrainGenerated, isMeshGenerated;

    public Chunk(int x, int z){
        this.chunkX = x;
        this.chunkZ = z;
        this.voxels = new Block[CHUNK_HEIGHT][CHUNK_WIDTH * CHUNK_WIDTH];
        this.subchunks = new Subchunk[CHUNK_HEIGHT / SUBCHUNK_HEIGHT];
        for(int i=0;i<16;i++){
            subchunks[i] = new Subchunk();
        }
        this.isTerrainGenerated = false;
        this.isMeshGenerated = false;
    }

    public int getX() {
        return chunkX;
    }

    public int getZ() {
        return chunkZ;
    }

    public void setBlockAt(int x, int y, int z, Block block){
        this.voxels[y][x * CHUNK_WIDTH + z] = block;
    }

    public Block getBlockAt(int x, int y, int z){
        return this.voxels[y][x * CHUNK_WIDTH + z];
    }

    public static Block getBlockAt(int x, int y, int z, Chunk chunk, Chunk c0, Chunk c1, Chunk c2, Chunk c3, Chunk c4, Chunk c5, Chunk c6, Chunk c7){
        if(y<=0) return Block.COBBLESTONE;
        if(y>=Chunk.CHUNK_HEIGHT) return Block.AIR;
        if(x >= 0 && x < Chunk.CHUNK_WIDTH && z >= 0 && z < Chunk.CHUNK_WIDTH) return chunk.getBlockAt(x, y, z);                                        // If in Chunk "chunk": 0>=x<Chunk.CHUNK_SIZE && 0>=z<Chunk.CHUNK_SIZE
        if(x < 0 && z < 0)                                                     return c0.getBlockAt(x+Chunk.CHUNK_WIDTH, y, z+Chunk.CHUNK_WIDTH); // If in CHunk "c0": x<0 && z<0
        if(x >= 0 && x < Chunk.CHUNK_WIDTH && z < 0)                           return c1.getBlockAt(x, y, z+Chunk.CHUNK_WIDTH);                      // If in CHunk "c1": 0>=x<Chunk.CHUNK_SIZE && z<0
        if(x >= Chunk.CHUNK_WIDTH && z < 0)                                    return c2.getBlockAt(x-Chunk.CHUNK_WIDTH, y, z+Chunk.CHUNK_WIDTH); // If in CHunk "c2": x>=Chunk.CHUNK_SIZE && z<0
        if(x < 0 && z >= 0 && z < Chunk.CHUNK_WIDTH)                           return c3.getBlockAt(x+Chunk.CHUNK_WIDTH, y, z);                      // If in CHunk "c3": x<0 && 0>=z<Chunk.CHUNK_SIZE
        if(x >= Chunk.CHUNK_WIDTH && z >= 0 && z < Chunk.CHUNK_WIDTH)          return c4.getBlockAt(x-Chunk.CHUNK_WIDTH, y, z);                      // If in CHunk "c4": x>=Chunk.CHUNK_SIZE && 0>=z<Chunk.CHUNK_SIZE
        if(x < 0 && z >= Chunk.CHUNK_WIDTH)                                    return c5.getBlockAt(x+Chunk.CHUNK_WIDTH, y, z-Chunk.CHUNK_WIDTH); // If in CHunk "c5"
        if(x >= 0 && x < Chunk.CHUNK_WIDTH && z >= Chunk.CHUNK_WIDTH)          return c6.getBlockAt(x, y, z-Chunk.CHUNK_WIDTH);                      // If in CHunk "c6"
        if(x >= Chunk.CHUNK_WIDTH && z >= Chunk.CHUNK_WIDTH)                   return c7.getBlockAt(x-Chunk.CHUNK_WIDTH, y, z-Chunk.CHUNK_WIDTH); // If in CHunk "c7"
        throw new IllegalStateException("Should not be here");
    }

    public void generateTerrain(){
        for(int x=0;x<CHUNK_WIDTH;x++){
            for(int z=0;z<CHUNK_WIDTH;z++){
                setBlockAt(x, 60, z, Block.GRASS);
            }
        }
        isTerrainGenerated = true;
    }

    public int[] getVBOs(){
        int amount = 0;
        for(Subchunk subchunk:subchunks){ amount += subchunk.getVbo() == -1 ? 0 : 1; }
        int[] toReturn = new int[amount];
        int i = 0;
        for(Subchunk subchunk:subchunks){
            if(subchunk.getVbo() != -1) toReturn[i] = subchunk.getVbo();
            i++;
        }
        return toReturn;
    }

    public void generateMesh(int subchunk, Chunk c0, Chunk c1, Chunk c2, Chunk c3, Chunk c4, Chunk c5, Chunk c6, Chunk c7){
        isMeshGenerated = true;

        ArrayList<Float> data = new ArrayList<>();
        for (int x = 0; x < Chunk.CHUNK_WIDTH; x++) {
            for (int y = subchunk * Chunk.SUBCHUNK_HEIGHT; y < (subchunk + 1) * Chunk.SUBCHUNK_HEIGHT; y++) {
                for (int z = 0; z < Chunk.CHUNK_WIDTH; z++) {
                    Block block = getBlockAt(x, y, z);

                    int xp = x + this.getX() * Chunk.CHUNK_WIDTH;
                    int zp = z + this.getZ() * Chunk.CHUNK_WIDTH;

                    Block up = getBlockAt(x, y + 1, z, this,c0,c1,c2,c3,c4,c5,c6,c7);
                    if (up.isTransparent() && up != block) {
                        data.addAll(List.of(
                                -0.5f+xp, -0.5f+y, -0.5f+zp,  0.0f, 0.0f,
                                 0.5f+xp, -0.5f+y, -0.5f+zp,  1.0f, 0.0f,
                                 0.5f+xp,  0.5f+y, -0.5f+zp,  1.0f, 1.0f,
                                 0.5f+xp,  0.5f+y, -0.5f+zp,  1.0f, 1.0f,
                                -0.5f+xp,  0.5f+y, -0.5f+zp,  0.0f, 1.0f,
                                -0.5f+xp, -0.5f+y, -0.5f+zp,  0.0f, 0.0f));

                    }

                    Block down = getBlockAt(x, y - 1, z, this,c0,c1,c2,c3,c4,c5,c6,c7);
                    if (down.isTransparent() && down != block) {
                        data.addAll(List.of(
                                -0.5f+xp, -0.5f+y,  0.5f+zp,  0.0f, 0.0f,
                                 0.5f+xp, -0.5f+y,  0.5f+zp,  1.0f, 0.0f,
                                 0.5f+xp,  0.5f+y,  0.5f+zp,  1.0f, 1.0f,
                                 0.5f+xp,  0.5f+y,  0.5f+zp,  1.0f, 1.0f,
                                -0.5f+xp,  0.5f+y,  0.5f+zp,  0.0f, 1.0f,
                                -0.5f+xp, -0.5f+y,  0.5f+zp,  0.0f, 0.0f));
                    }

                    Block left = getBlockAt(x - 1, y, z, this,c0,c1,c2,c3,c4,c5,c6,c7);
                    if (left.isTransparent() && left != block) {
                        data.addAll(List.of(
                                -0.5f+xp,  0.5f+y,  0.5f+zp,  1.0f, 0.0f,
                                -0.5f+xp,  0.5f+y, -0.5f+zp,  1.0f, 1.0f,
                                -0.5f+xp, -0.5f+y, -0.5f+zp,  0.0f, 1.0f,
                                -0.5f+xp, -0.5f+y, -0.5f+zp,  0.0f, 1.0f,
                                -0.5f+xp, -0.5f+y,  0.5f+zp,  0.0f, 0.0f,
                                -0.5f+xp,  0.5f+y,  0.5f+zp,  1.0f, 0.0f));

                    }

                    Block right = getBlockAt(x + 1, y, z, this,c0,c1,c2,c3,c4,c5,c6,c7);
                    if (right.isTransparent() && right != block) {
                        data.addAll(List.of(
                                0.5f+xp,  0.5f+y,  0.5f+zp,  1.0f, 0.0f,
                                0.5f+xp,  0.5f+y, -0.5f+zp,  1.0f, 1.0f,
                                0.5f+xp, -0.5f+y, -0.5f+zp,  0.0f, 1.0f,
                                0.5f+xp, -0.5f+y, -0.5f+zp,  0.0f, 1.0f,
                                0.5f+xp, -0.5f+y,  0.5f+zp,  0.0f, 0.0f,
                                0.5f+xp,  0.5f+y,  0.5f+zp,  1.0f, 0.0f));

                    }

                    Block front = getBlockAt(x, y, z + 1, this,c0,c1,c2,c3,c4,c5,c6,c7);
                    if (front.isTransparent() && front != block) {
                        data.addAll(List.of(
                                -0.5f+xp, -0.5f+y, -0.5f+zp,  0.0f, 1.0f,
                                 0.5f+xp, -0.5f+y, -0.5f+zp,  1.0f, 1.0f,
                                 0.5f+xp, -0.5f+y,  0.5f+zp,  1.0f, 0.0f,
                                 0.5f+xp, -0.5f+y,  0.5f+zp,  1.0f, 0.0f,
                                -0.5f+xp, -0.5f+y,  0.5f+zp,  0.0f, 0.0f,
                                -0.5f+xp, -0.5f+y, -0.5f+zp,  0.0f, 1.0f));
                    }

                    Block back = getBlockAt(x, y, z - 1, this,c0,c1,c2,c3,c4,c5,c6,c7);
                    if (back.isTransparent() && back != block) {
                        data.addAll(List.of(
                                -0.5f+xp,  0.5f+y, -0.5f+zp,  0.0f, 1.0f,
                                 0.5f+xp,  0.5f+y, -0.5f+zp,  1.0f, 1.0f,
                                 0.5f+xp,  0.5f+y,  0.5f+zp,  1.0f, 0.0f,
                                 0.5f+xp,  0.5f+y,  0.5f+zp,  1.0f, 0.0f,
                                -0.5f+xp,  0.5f+y,  0.5f+zp,  0.0f, 0.0f,
                                -0.5f+xp,  0.5f+y, -0.5f+zp,  0.0f, 1.0f));
                    }
                }
            }
        }
        float[] dataArray = new float[data.size()];
        for (int i = 0; i < data.size(); i++) dataArray[i] = data.get(i);
        this.subchunks[subchunk].setMesh(dataArray);
    }

    public void generateVbo(int subchunk){
        int VBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBufferData(GL_ARRAY_BUFFER, this.subchunks[subchunk].getMesh(), GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        this.subchunks[subchunk].setVbo(VBO);
    }

    public void destroy(){
        for(int v:getVBOs()){
            glDeleteBuffers(v);
        }
    }

    public float[] getMesh(int subchunk){
        return this.subchunks[subchunk].getMesh();
    }

    public boolean isMeshGenerated() {
        return isMeshGenerated;
    }

    public boolean isTerrainGenerated() {
        return isTerrainGenerated;
    }
}
