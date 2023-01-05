package chunk;

public class Subchunk {
    private int vbo;
    private float[] mesh;

    public Subchunk(){
        this.vbo = -1;
    }

    public int getVbo() {
        return vbo;
    }

    public float[] getMesh() {
        return mesh;
    }

    public void setMesh(float[] mesh) {
        this.mesh = mesh;
    }

    public void setVbo(int vbo) {
        this.vbo = vbo;
    }
}
