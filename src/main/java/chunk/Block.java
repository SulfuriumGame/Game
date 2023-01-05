package chunk;

public enum Block {
    AIR, GRASS, COBBLESTONE;

    public boolean isTransparent(){
        return this == AIR;
    }
}
