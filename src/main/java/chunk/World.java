package chunk;

import java.util.ArrayList;
import java.util.Random;

public class World {
    private ArrayList<Chunk> chunks;
    private ArrayList<Chunk> chunksToRender;
    private ArrayList<Chunk> chunksToDestroy;

    public World(){
        chunks = new ArrayList<>();
        chunksToRender = new ArrayList<>();
        chunksToDestroy = new ArrayList<>();
    }

    public ArrayList<Chunk> getChunks(){
        return chunks;
    }

    private int seed = new Random().nextInt(Integer.MAX_VALUE);

    public Chunk getChunkAt(int x, int z){
        for(Chunk chunk:chunks){
            if(chunk.getX() == x && chunk.getZ() == z) return chunk;
        }
        return null;
    }

    public void updateChunks(){
        ArrayList<Chunk> chunkToRender = new ArrayList<>(chunksToRender);
        for(Chunk chunk : chunkToRender) {for(int i=0;i<16;i++){chunk.generateVbo(i);}}
        chunksToRender.removeIf(chunkToRender::contains);
        ArrayList<Chunk> chunkToDestroy = new ArrayList<>(chunksToDestroy);
        for(Chunk chunk : chunkToDestroy) {for(int i=0;i<16;i++){chunk.destroy();}}
        chunksToDestroy.removeIf(chunkToDestroy::contains);
    }

    public void updateChunks(int x, int z, int renderDistance) {
        // Remove all chunks outside the render distance
        chunks.forEach(chunk -> {if(chunk.getX() > x + renderDistance + 1 || chunk.getX() < x - renderDistance - 1 || chunk.getZ() > z + renderDistance + 1 || chunk.getZ() < z - renderDistance - 1) chunksToDestroy.add(chunk);});
        chunks.removeIf(chunk -> chunk.getX() > x + renderDistance + 1 || chunk.getX() < x - renderDistance - 1 || chunk.getZ() > z + renderDistance + 1 || chunk.getZ() < z - renderDistance - 1);

        // Adding chunks to the list that are within the render distance
        ArrayList<Chunk> newChunks = new ArrayList<>();
        for(int i = x - renderDistance; i <= x + renderDistance; i++) {
            for(int j = z - renderDistance; j <= z + renderDistance; j++) {
                int finalI = i;
                int finalJ = j;
                if(chunks.stream().noneMatch(chunk -> chunk.getX() == finalI && chunk.getZ() == finalJ)) {
                    newChunks.add(new Chunk(i, j));
                }
            }
        }
        chunks.addAll(newChunks);

        ArrayList<Chunk> chunksToGenerate = new ArrayList<>();

        for(Chunk chunk : chunks) {
            if(chunk.isMeshGenerated()) continue;

            int sides = 0;
            for(Chunk search : chunks) {
                if(search.getX()==chunk.getX()-1 && search.getZ()==chunk.getZ()-1){ sides++; }
                if(search.getX()==chunk.getX() && search.getZ()==chunk.getZ()-1){ sides++; }
                if(search.getX()==chunk.getX()+1 && search.getZ()==chunk.getZ()-1){ sides++; }

                if(search.getX()==chunk.getX()-1 && search.getZ()==chunk.getZ()){ sides++; }
                if(search.getX()==chunk.getX()+1 && search.getZ()==chunk.getZ()){ sides++; }

                if(search.getX()==chunk.getX()-1 && search.getZ()==chunk.getZ()+1){ sides++; }
                if(search.getX()==chunk.getX() && search.getZ()==chunk.getZ()+1){ sides++; }
                if(search.getX()==chunk.getX()+1 && search.getZ()==chunk.getZ()+1){ sides++; }
            }
            if(sides == 8) {
                chunksToGenerate.add(chunk);
            }
        }

        //Sort chunks by distance
        chunksToGenerate.sort((a, b) -> {
            double valueA = Math.sqrt(Math.pow(a.getX() - x, 2) + Math.pow(a.getZ() - z, 2));
            double valueB = Math.sqrt(Math.pow(b.getX() - x, 2) + Math.pow(b.getZ() - z, 2));
            int value = Double.compare(valueA, valueB);
            return Integer.compare(value, 0);
        });

        //Generate Chunks
        for(Chunk chunk : chunksToGenerate) {
            if(!chunk.isTerrainGenerated()) chunk.generateTerrain();
            if(!getChunkAt(chunk.getX() - 1, chunk.getZ() - 1).isTerrainGenerated()){ getChunkAt(chunk.getX() - 1, chunk.getZ() - 1).generateTerrain();}
            if(!getChunkAt(chunk.getX() + 0, chunk.getZ() - 1).isTerrainGenerated()){ getChunkAt(chunk.getX() + 0, chunk.getZ() - 1).generateTerrain();}
            if(!getChunkAt(chunk.getX() + 1, chunk.getZ() - 1).isTerrainGenerated()){ getChunkAt(chunk.getX() + 1, chunk.getZ() - 1).generateTerrain();}
            if(!getChunkAt(chunk.getX() - 1, chunk.getZ() + 0).isTerrainGenerated()){ getChunkAt(chunk.getX() - 1, chunk.getZ() + 0).generateTerrain();}
            if(!getChunkAt(chunk.getX() + 1, chunk.getZ() + 0).isTerrainGenerated()){ getChunkAt(chunk.getX() + 1, chunk.getZ() + 0).generateTerrain();}
            if(!getChunkAt(chunk.getX() - 1, chunk.getZ() + 1).isTerrainGenerated()){ getChunkAt(chunk.getX() - 1, chunk.getZ() + 1).generateTerrain();}
            if(!getChunkAt(chunk.getX() + 0, chunk.getZ() + 1).isTerrainGenerated()){ getChunkAt(chunk.getX() + 0, chunk.getZ() + 1).generateTerrain();}
            if(!getChunkAt(chunk.getX() + 1, chunk.getZ() + 1).isTerrainGenerated()){ getChunkAt(chunk.getX() + 1, chunk.getZ() + 1).generateTerrain();}

            for(int i=0;i<16;i++){
                chunk.generateMesh(i,
                        getChunkAt(chunk.getX() - 1, chunk.getZ() - 1),
                        getChunkAt(chunk.getX() + 0, chunk.getZ() - 1),
                        getChunkAt(chunk.getX() + 1, chunk.getZ() - 1),
                        getChunkAt(chunk.getX() - 1, chunk.getZ() + 0),
                        getChunkAt(chunk.getX() + 1, chunk.getZ() + 0),
                        getChunkAt(chunk.getX() - 1, chunk.getZ() + 1),
                        getChunkAt(chunk.getX() + 0, chunk.getZ() + 1),
                        getChunkAt(chunk.getX() + 1, chunk.getZ() + 1)
                );
            }
            chunksToRender.add(chunk);
        }
    }
}
