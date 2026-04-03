package lvl0fixpipeline.global;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import lwjglutils.OGLBuffers;
import lwjglutils.ToFloatArray;
import transforms.*;

import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;

public class Teapot {
    private final List<Bicubic> teapot;

    public Teapot() {
        teapot = new ArrayList<>();
        List<Point3D> ctrlPoints = new ArrayList<>();
        
        //1
        ctrlPoints.add( new Point3D(1.4, 0, 2.4));
        ctrlPoints.add( new Point3D(1.4, -0.784, 2.4));
        ctrlPoints.add( new Point3D(0.784, -1.4, 2.4));
        ctrlPoints.add( new Point3D(0, -1.4, 2.4));
        ctrlPoints.add( new Point3D(1.3375, 0, 2.53125));
        ctrlPoints.add( new Point3D(1.3375, -0.805, 2.53125));
        ctrlPoints.add( new Point3D(0.749, -1.3375, 2.53125));
        ctrlPoints.add( new Point3D(0, -1.3375, 2.53125));
        ctrlPoints.add( new Point3D(1.4375, 0, 2.53125));
        ctrlPoints.add( new Point3D(1.4375, -0.805, 2.53125));
        ctrlPoints.add( new Point3D(0.805, -1.4375, 2.53125));
        ctrlPoints.add( new Point3D(0, -1.4375, 2.53125));
        ctrlPoints.add( new Point3D(1.5, 0, 2.4));
        ctrlPoints.add( new Point3D(1.5, -0.84, 2.4));
        ctrlPoints.add( new Point3D(0.84, -1.5, 2.4));
        ctrlPoints.add( new Point3D(0, -1.5, 2.4));
        Point3D[] array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));
        
        //2
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(0, -1.4, 2.4));
        ctrlPoints.add( new Point3D(-0.784, -1.4, 2.4));
        ctrlPoints.add( new Point3D(-1.4, -0.784, 2.4));
        ctrlPoints.add( new Point3D(-1.4, 0, 2.4));
        ctrlPoints.add( new Point3D(0, -1.3375, 2.53125));
        ctrlPoints.add( new Point3D(-0.749, -1.3375, 2.53125));
        ctrlPoints.add( new Point3D(-1.3375, -0.749, 2.53125));
        ctrlPoints.add( new Point3D(-1.3375, 0, 2.53125));
        ctrlPoints.add( new Point3D(0, -1.4375, 2.53125));
        ctrlPoints.add( new Point3D(-0.805, -1.4375, 2.53125));
        ctrlPoints.add( new Point3D(-1.4375, -0.805, 2.53125));
        ctrlPoints.add( new Point3D(-1.4375, 0, 2.53125));
        ctrlPoints.add( new Point3D(0, -1.5, 2.4));
        ctrlPoints.add( new Point3D(-0.84, -1.5, 2.4));
        ctrlPoints.add( new Point3D(-1.5, -0.84, 2.4));
        ctrlPoints.add( new Point3D(-1.5, 0, 2.4));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //3
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(-1.4, 0, 2.4));
        ctrlPoints.add( new Point3D(-1.4, 0.784, 2.4));
        ctrlPoints.add( new Point3D(-0.784, 1.4, 2.4));
        ctrlPoints.add( new Point3D(0, 1.4, 2.4));
        ctrlPoints.add( new Point3D(-1.3375, 0, 2.53125));
        ctrlPoints.add( new Point3D(-1.3375, 0.805, 2.53125));
        ctrlPoints.add( new Point3D(-0.749, 1.3375, 2.53125));
        ctrlPoints.add( new Point3D(0, 1.3375, 2.53125));
        ctrlPoints.add( new Point3D(-1.4375, 0, 2.53125));
        ctrlPoints.add( new Point3D(-1.4375, 0.805, 2.53125));
        ctrlPoints.add( new Point3D(-0.805, 1.4375, 2.53125));
        ctrlPoints.add( new Point3D(0, 1.4375, 2.53125));
        ctrlPoints.add( new Point3D(-1.5, 0, 2.4));
        ctrlPoints.add( new Point3D(-1.5, 0.84, 2.4));
        ctrlPoints.add( new Point3D(-0.84, 1.5, 2.4));
        ctrlPoints.add( new Point3D(0, 1.5, 2.4));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //4
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(0, 1.4, 2.4));
        ctrlPoints.add( new Point3D(0.784, 1.4, 2.4));
        ctrlPoints.add( new Point3D(1.4, 0.784, 2.4));
        ctrlPoints.add( new Point3D(1.4, 0, 2.4));
        ctrlPoints.add( new Point3D(0, 1.3375, 2.53125));
        ctrlPoints.add( new Point3D(0.749, 1.3375, 2.53125));
        ctrlPoints.add( new Point3D(1.3375, 0.749, 2.53125));
        ctrlPoints.add( new Point3D(1.3375, 0, 2.53125));
        ctrlPoints.add( new Point3D(0, 1.4375, 2.53125));
        ctrlPoints.add( new Point3D(0.805, 1.4375, 2.53125));
        ctrlPoints.add( new Point3D(1.4375, 0.805, 2.53125));
        ctrlPoints.add( new Point3D(1.4375, 0, 2.53125));
        ctrlPoints.add( new Point3D(0, 1.5, 2.4));
        ctrlPoints.add( new Point3D(0.84, 1.5, 2.4));
        ctrlPoints.add( new Point3D(1.5, 0.84, 2.4));
        ctrlPoints.add( new Point3D(1.5, 0, 2.4));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //5
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(1.5, 0, 2.4));
        ctrlPoints.add( new Point3D(1.5, -0.84, 2.4));
        ctrlPoints.add( new Point3D(0.84, -1.5, 2.4));
        ctrlPoints.add( new Point3D(0, -1.5, 2.4));
        ctrlPoints.add( new Point3D(1.75, 0, 1.875));
        ctrlPoints.add( new Point3D(1.75, -0.98, 1.875));
        ctrlPoints.add( new Point3D(0.98, -1.75, 1.875));
        ctrlPoints.add( new Point3D(0, -1.75, 1.875));
        ctrlPoints.add( new Point3D(2, 0, 1.35));
        ctrlPoints.add( new Point3D(2, -1.12, 1.35));
        ctrlPoints.add( new Point3D(1.12, -2, 1.35));
        ctrlPoints.add( new Point3D(0, -2, 1.35));
        ctrlPoints.add( new Point3D(2, 0, 0.9));
        ctrlPoints.add( new Point3D(2, -1.12, 0.9));
        ctrlPoints.add( new Point3D(1.12, -2, 0.9));
        ctrlPoints.add( new Point3D(0, -2, 0.9));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //6
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(0, -1.5, 2.4));
        ctrlPoints.add( new Point3D(-0.84, -1.5, 2.4));
        ctrlPoints.add( new Point3D(-1.5, -0.84, 2.4));
        ctrlPoints.add( new Point3D(-1.5, 0, 2.4));
        ctrlPoints.add( new Point3D(0, -1.75, 1.875));
        ctrlPoints.add( new Point3D(-0.98, -1.75, 1.875));
        ctrlPoints.add( new Point3D(-1.75, -0.98, 1.875));
        ctrlPoints.add( new Point3D(-1.75, 0, 1.875));
        ctrlPoints.add( new Point3D(0, -2, 1.35));
        ctrlPoints.add( new Point3D(-1.12, -2, 1.35));
        ctrlPoints.add( new Point3D(-2, -1.12, 1.35));
        ctrlPoints.add( new Point3D(-2, 0, 1.35));
        ctrlPoints.add( new Point3D(0, -2, 0.9));
        ctrlPoints.add( new Point3D(-1.12, -2, 0.9));
        ctrlPoints.add( new Point3D(-2, -1.12, 0.9));
        ctrlPoints.add( new Point3D(-2, 0, 0.9));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //7
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(-1.5, 0, 2.4));
        ctrlPoints.add( new Point3D(-1.5, 0.84, 2.4));
        ctrlPoints.add( new Point3D(-0.84, 1.5, 2.4));
        ctrlPoints.add( new Point3D(0, 1.5, 2.4));
        ctrlPoints.add( new Point3D(-1.75, 0, 1.875));
        ctrlPoints.add( new Point3D(-1.75, 0.98, 1.875));
        ctrlPoints.add( new Point3D(-0.98, 1.75, 1.875));
        ctrlPoints.add( new Point3D(0, 1.75, 1.875));
        ctrlPoints.add( new Point3D(-2, 0, 1.35));
        ctrlPoints.add( new Point3D(-2, 1.12, 1.35));
        ctrlPoints.add( new Point3D(-1.12, 2, 1.35));
        ctrlPoints.add( new Point3D(0, 2, 1.35));
        ctrlPoints.add( new Point3D(-2, 0, 0.9));
        ctrlPoints.add( new Point3D(-2, 1.12, 0.9));
        ctrlPoints.add( new Point3D(-1.12, 2, 0.9));
        ctrlPoints.add( new Point3D(0, 2, 0.9));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //8
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(0, 1.5, 2.4));
        ctrlPoints.add( new Point3D(0.84, 1.5, 2.4));
        ctrlPoints.add( new Point3D(1.5, 0.84, 2.4));
        ctrlPoints.add( new Point3D(1.5, 0, 2.4));
        ctrlPoints.add( new Point3D(0, 1.75, 1.875));
        ctrlPoints.add( new Point3D(0.98, 1.75, 1.875));
        ctrlPoints.add( new Point3D(1.75, 0.98, 1.875));
        ctrlPoints.add( new Point3D(1.75, 0, 1.875));
        ctrlPoints.add( new Point3D(0, 2, 1.35));
        ctrlPoints.add( new Point3D(1.12, 2, 1.35));
        ctrlPoints.add( new Point3D(2, 1.12, 1.35));
        ctrlPoints.add( new Point3D(2, 0, 1.35));
        ctrlPoints.add( new Point3D(0, 2, 0.9));
        ctrlPoints.add( new Point3D(1.12, 2, 0.9));
        ctrlPoints.add( new Point3D(2, 1.12, 0.9));
        ctrlPoints.add( new Point3D(2, 0, 0.9));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //9
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(2, 0, 0.9));
        ctrlPoints.add( new Point3D(2, -1.12, 0.9));
        ctrlPoints.add( new Point3D(1.12, -2, 0.9));
        ctrlPoints.add( new Point3D(0, -2, 0.9));
        ctrlPoints.add( new Point3D(2, 0, 0.45));
        ctrlPoints.add( new Point3D(2, -1.12, 0.45));
        ctrlPoints.add( new Point3D(1.12, -2, 0.45));
        ctrlPoints.add( new Point3D(0, -2, 0.45));
        ctrlPoints.add( new Point3D(1.5, 0, 0.225));
        ctrlPoints.add( new Point3D(1.5, -0.84, 0.225));
        ctrlPoints.add( new Point3D(0.84, -1.5 , 0.225));
        ctrlPoints.add( new Point3D(0, -1.5, 0.225));
        ctrlPoints.add( new Point3D(1.5, 0, 0.15));
        ctrlPoints.add( new Point3D(1.5, -0.84, 0.15));
        ctrlPoints.add( new Point3D(0.84, -1.5, 0.15));
        ctrlPoints.add( new Point3D(0, -1.5, 0.15));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //10
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(0, -2, 0.9));
        ctrlPoints.add( new Point3D(-1.12, -2, 0.9));
        ctrlPoints.add( new Point3D(-2, -1.12, 0.9));
        ctrlPoints.add( new Point3D(-2, 0, 0.9));
        ctrlPoints.add( new Point3D(0, -2, 0.45));
        ctrlPoints.add( new Point3D(-1.12, -2, 0.45));
        ctrlPoints.add( new Point3D(-2, -1.12, 0.45));
        ctrlPoints.add( new Point3D(-2, 0, 0.45));
        ctrlPoints.add( new Point3D(0, -1.5, 0.225));
        ctrlPoints.add( new Point3D(-0.84, -1.5, 0.225));
        ctrlPoints.add( new Point3D(-1.5 , -0.84, 0.225));
        ctrlPoints.add( new Point3D(-1.5, 0, 0.225));
        ctrlPoints.add( new Point3D(0, -1.5, 0.15));
        ctrlPoints.add( new Point3D(-0.84, -1.5, 0.15));
        ctrlPoints.add( new Point3D(-1.5, -0.84, 0.15));
        ctrlPoints.add( new Point3D(-1.5, 0, 0.15));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //11
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(-2, 0, 0.9));
        ctrlPoints.add( new Point3D(-2, 1.12, 0.9));
        ctrlPoints.add( new Point3D(-1.12, 2, 0.9));
        ctrlPoints.add( new Point3D(0, 2, 0.9));
        ctrlPoints.add( new Point3D(-2, 0, 0.45));
        ctrlPoints.add( new Point3D(-2, 1.12, 0.45));
        ctrlPoints.add( new Point3D(-1.12, 2, 0.45));
        ctrlPoints.add( new Point3D(0, 2, 0.45));
        ctrlPoints.add( new Point3D(-1.5, 0, 0.225));
        ctrlPoints.add( new Point3D(-1.5, 0.84, 0.225));
        ctrlPoints.add( new Point3D(-0.84, 1.5 , 0.225));
        ctrlPoints.add( new Point3D(0, 1.5, 0.225));
        ctrlPoints.add( new Point3D(-1.5, 0, 0.15));
        ctrlPoints.add( new Point3D(-1.5, 0.84, 0.15));
        ctrlPoints.add( new Point3D(-0.84, 1.5, 0.15));
        ctrlPoints.add( new Point3D(0, 1.5, 0.15));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //12
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(0, 2, 0.9));
        ctrlPoints.add( new Point3D(1.12, 2, 0.9));
        ctrlPoints.add( new Point3D(2, 1.12, 0.9));
        ctrlPoints.add( new Point3D(2, 0, 0.9));
        ctrlPoints.add( new Point3D(0, 2, 0.45));
        ctrlPoints.add( new Point3D(1.12, 2, 0.45));
        ctrlPoints.add( new Point3D(2, 1.12, 0.45));
        ctrlPoints.add( new Point3D(2, 0, 0.45));
        ctrlPoints.add( new Point3D(0, 1.5, 0.225));
        ctrlPoints.add( new Point3D(0.84, 1.5, 0.225));
        ctrlPoints.add( new Point3D(1.5 , 0.84, 0.225));
        ctrlPoints.add( new Point3D(1.5, 0, 0.225));
        ctrlPoints.add( new Point3D(0, 1.5, 0.15));
        ctrlPoints.add( new Point3D(0.84, 1.5, 0.15));
        ctrlPoints.add( new Point3D(1.5, 0.84, 0.15));
        ctrlPoints.add( new Point3D(1.5, 0, 0.15));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));



        /*
         * POKLICE
         */

        //21
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(0, 0, 3.15));
        ctrlPoints.add( new Point3D(0, 0, 3.15));
        ctrlPoints.add( new Point3D(0, 0, 3.15));
        ctrlPoints.add( new Point3D(0, 0, 3.15));
        ctrlPoints.add( new Point3D(0.8, 0, 3.15));
        ctrlPoints.add( new Point3D(0.8, -0.45, 3.15));
        ctrlPoints.add( new Point3D(0.45, -0.8, 3.15));
        ctrlPoints.add( new Point3D(0, -0.8, 3.15));
        ctrlPoints.add( new Point3D(0, 0, 2.85));
        ctrlPoints.add( new Point3D(0, 0, 2.85));
        ctrlPoints.add( new Point3D(0, 0, 2.85));
        ctrlPoints.add( new Point3D(0, 0, 2.85));
        ctrlPoints.add( new Point3D(0.2, 0, 2.7));
        ctrlPoints.add( new Point3D(0.2, -0.112, 2.7));
        ctrlPoints.add( new Point3D(0.112, -0.2, 2.7));
        ctrlPoints.add( new Point3D(0, -0.2, 2.7));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //22
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(0, 0, 3.15));
        ctrlPoints.add( new Point3D(0, 0, 3.15));
        ctrlPoints.add( new Point3D(0, 0, 3.15));
        ctrlPoints.add( new Point3D(0, 0, 3.15));
        ctrlPoints.add( new Point3D(0, -0.8, 3.15));
        ctrlPoints.add( new Point3D(-0.45, -0.8, 3.15));
        ctrlPoints.add( new Point3D(-0.8, -0.45, 3.15));
        ctrlPoints.add( new Point3D(-0.8, 0, 3.15));
        ctrlPoints.add( new Point3D(0, 0, 2.85));
        ctrlPoints.add( new Point3D(0, 0, 2.85));
        ctrlPoints.add( new Point3D(0, 0, 2.85));
        ctrlPoints.add( new Point3D(0, 0, 2.85));
        ctrlPoints.add( new Point3D(0, -0.2, 2.7));
        ctrlPoints.add( new Point3D(-0.112, -0.2, 2.7));
        ctrlPoints.add( new Point3D(-0.2, -0.112, 2.7));
        ctrlPoints.add( new Point3D(-0.2, 0, 2.7));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //23
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(0, 0, 3.15));
        ctrlPoints.add( new Point3D(0, 0, 3.15));
        ctrlPoints.add( new Point3D(0, 0, 3.15));
        ctrlPoints.add( new Point3D(0, 0, 3.15));
        ctrlPoints.add( new Point3D(-0.8, 0, 3.15));
        ctrlPoints.add( new Point3D(-0.8, 0.45, 3.15));
        ctrlPoints.add( new Point3D(-0.45, 0.8, 3.15));
        ctrlPoints.add( new Point3D(0, 0.8, 3.15));
        ctrlPoints.add( new Point3D(0, 0, 2.85));
        ctrlPoints.add( new Point3D(0, 0, 2.85));
        ctrlPoints.add( new Point3D(0, 0, 2.85));
        ctrlPoints.add( new Point3D(0, 0, 2.85));
        ctrlPoints.add( new Point3D(-0.2, 0, 2.7));
        ctrlPoints.add( new Point3D(-0.2, 0.112, 2.7));
        ctrlPoints.add( new Point3D(-0.112, 0.2, 2.7));
        ctrlPoints.add( new Point3D(0, 0.2, 2.7));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //24
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(0, 0, 3.15));
        ctrlPoints.add( new Point3D(0, 0, 3.15));
        ctrlPoints.add( new Point3D(0, 0, 3.15));
        ctrlPoints.add( new Point3D(0, 0, 3.15));
        ctrlPoints.add( new Point3D(0, 0.8, 3.15));
        ctrlPoints.add( new Point3D(0.45, 0.8, 3.15));
        ctrlPoints.add( new Point3D(0.8, 0.45, 3.15));
        ctrlPoints.add( new Point3D(0.8, 0, 3.15));
        ctrlPoints.add( new Point3D(0, 0, 2.85));
        ctrlPoints.add( new Point3D(0, 0, 2.85));
        ctrlPoints.add( new Point3D(0, 0, 2.85));
        ctrlPoints.add( new Point3D(0, 0, 2.85));
        ctrlPoints.add( new Point3D(0, 0.2, 2.7));
        ctrlPoints.add( new Point3D(0.112, 0.2, 2.7));
        ctrlPoints.add( new Point3D(0.2, 0.112, 2.7));
        ctrlPoints.add( new Point3D(0.2, 0, 2.7));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //25
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(0.2, 0, 2.7));
        ctrlPoints.add( new Point3D(0.2, -0.112, 2.7));
        ctrlPoints.add( new Point3D(0.112, -0.2, 2.7));
        ctrlPoints.add( new Point3D(0, -0.2, 2.7));
        ctrlPoints.add( new Point3D(0.4, -0.2, 2.55));
        ctrlPoints.add( new Point3D(0.4, 0, 2.55));
        ctrlPoints.add( new Point3D(0.224, -0.4, 2.55));
        ctrlPoints.add( new Point3D(0, -0.4, 2.55));
        ctrlPoints.add( new Point3D(1.3, 0, 2.55));
        ctrlPoints.add( new Point3D(1.3, -0.728, 2.55));
        ctrlPoints.add( new Point3D(0.728, -1.3, 2.55));
        ctrlPoints.add( new Point3D(0, -1.3, 2.55));
        ctrlPoints.add( new Point3D(1.3, 0, 2.4));
        ctrlPoints.add( new Point3D(1.3, -0.728, 2.4));
        ctrlPoints.add( new Point3D(0.728, -1.3, 2.4));
        ctrlPoints.add( new Point3D(0, -1.3, 2.4));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //26
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(0, -0.2, 2.7));
        ctrlPoints.add( new Point3D(-0.112, -0.2, 2.7));
        ctrlPoints.add( new Point3D(-0.2, -0.112, 2.7));
        ctrlPoints.add( new Point3D(-0.2, 0, 2.7));
        ctrlPoints.add( new Point3D(-0.2, -0.4, 2.55));
        ctrlPoints.add( new Point3D(0, -0.4, 2.55));
        ctrlPoints.add( new Point3D(-0.4, -0.224, 2.55));
        ctrlPoints.add( new Point3D(-0.4, 0, 2.55));
        ctrlPoints.add( new Point3D(0, -1.3, 2.55));
        ctrlPoints.add( new Point3D(-0.728, -1.3, 2.55));
        ctrlPoints.add( new Point3D(-1.3, -0.728, 2.55));
        ctrlPoints.add( new Point3D(-1.3, 0, 2.55));
        ctrlPoints.add( new Point3D(0, -1.3, 2.4));
        ctrlPoints.add( new Point3D(-0.728, -1.3, 2.4));
        ctrlPoints.add( new Point3D(-1.3, -0.728, 2.4));
        ctrlPoints.add( new Point3D(-1.3, 0, 2.4));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //27
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(-0.2, 0, 2.7));
        ctrlPoints.add( new Point3D(-0.2, 0.112, 2.7));
        ctrlPoints.add( new Point3D(-0.112, 0.2, 2.7));
        ctrlPoints.add( new Point3D(0, 0.2, 2.7));
        ctrlPoints.add( new Point3D(-0.4, 0.2, 2.55));
        ctrlPoints.add( new Point3D(-0.4, 0, 2.55));
        ctrlPoints.add( new Point3D(-0.224, 0.4, 2.55));
        ctrlPoints.add( new Point3D(0, 0.4, 2.55));
        ctrlPoints.add( new Point3D(-1.3, 0, 2.55));
        ctrlPoints.add( new Point3D(-1.3, 0.728, 2.55));
        ctrlPoints.add( new Point3D(-0.728, 1.3, 2.55));
        ctrlPoints.add( new Point3D(0, 1.3, 2.55));
        ctrlPoints.add( new Point3D(-1.3, 0, 2.4));
        ctrlPoints.add( new Point3D(-1.3, 0.728, 2.4));
        ctrlPoints.add( new Point3D(-0.728, 1.3, 2.4));
        ctrlPoints.add( new Point3D(0, 1.3, 2.4));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //28
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(0, 0.2, 2.7));
        ctrlPoints.add( new Point3D(0.112, 0.2, 2.7));
        ctrlPoints.add( new Point3D(0.2, 0.112, 2.7));
        ctrlPoints.add( new Point3D(0.2, 0, 2.7));
        ctrlPoints.add( new Point3D(0.2, 0.4, 2.55));
        ctrlPoints.add( new Point3D(0, 0.4, 2.55));
        ctrlPoints.add( new Point3D(0.4, 0.224, 2.55));
        ctrlPoints.add( new Point3D(0.4, 0, 2.55));
        ctrlPoints.add( new Point3D(0, 1.3, 2.55));
        ctrlPoints.add( new Point3D(0.728, 1.3, 2.55));
        ctrlPoints.add( new Point3D(1.3, 0.728, 2.55));
        ctrlPoints.add( new Point3D(1.3, 0, 2.55));
        ctrlPoints.add( new Point3D(0, 1.3, 2.4));
        ctrlPoints.add( new Point3D(0.728, 1.3, 2.4));
        ctrlPoints.add( new Point3D(1.3, 0.728, 2.4));
        ctrlPoints.add( new Point3D(1.3, 0, 2.4));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        /*
         * DNO
         */

        //29
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(0, 0, 0));
        ctrlPoints.add( new Point3D(0, 0, 0));
        ctrlPoints.add( new Point3D(0, 0, 0));
        ctrlPoints.add( new Point3D(0, 0, 0));
        ctrlPoints.add( new Point3D(1.425, 0, 0));
        ctrlPoints.add( new Point3D(1.425, 0.798, 0));
        ctrlPoints.add( new Point3D(0.798, 1.425, 0));
        ctrlPoints.add( new Point3D(0, 1.425, 0));
        ctrlPoints.add( new Point3D(1.5, 0, 0.075));
        ctrlPoints.add( new Point3D(1.5, 0.84, 0.075));
        ctrlPoints.add( new Point3D(0.84, 1.5, 0.075));
        ctrlPoints.add( new Point3D(0., 1.5, 0.075));
        ctrlPoints.add( new Point3D(1.5, 0, 0.15));
        ctrlPoints.add( new Point3D(1.5, 0.84, 0.15));
        ctrlPoints.add( new Point3D(0.84, 1.5, 0.15));
        ctrlPoints.add( new Point3D(0, 1.5, 0.15));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //30
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(0, 0, 0));
        ctrlPoints.add( new Point3D(0, 0, 0));
        ctrlPoints.add( new Point3D(0, 0, 0));
        ctrlPoints.add( new Point3D(0, 0, 0));
        ctrlPoints.add( new Point3D(0, 1.425, 0));
        ctrlPoints.add( new Point3D(-0.798, 1.425, 0));
        ctrlPoints.add( new Point3D(-1.425, 0.798, 0));
        ctrlPoints.add( new Point3D(-1.425, 0, 0));
        ctrlPoints.add( new Point3D(0, 1.5, 0.075));
        ctrlPoints.add( new Point3D(-0.84, 1.5, 0.075));
        ctrlPoints.add( new Point3D(-1.5, 0.84, 0.075));
        ctrlPoints.add( new Point3D(-1.5, 0., 0.075));
        ctrlPoints.add( new Point3D(0, 1.5, 0.15));
        ctrlPoints.add( new Point3D(-0.84, 1.5, 0.15));
        ctrlPoints.add( new Point3D(-1.5, 0.84, 0.15));
        ctrlPoints.add( new Point3D(-1.5, 0, 0.15));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //31
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(0, 0, 0));
        ctrlPoints.add( new Point3D(0, 0, 0));
        ctrlPoints.add( new Point3D(0, 0, 0));
        ctrlPoints.add( new Point3D(0, 0, 0));
        ctrlPoints.add( new Point3D(-1.425, 0, 0));
        ctrlPoints.add( new Point3D(-1.425, -0.798, 0));
        ctrlPoints.add( new Point3D(-0.798, -1.425, 0));
        ctrlPoints.add( new Point3D(0, -1.425, 0));
        ctrlPoints.add( new Point3D(-1.5, 0, 0.075));
        ctrlPoints.add( new Point3D(-1.5, -0.84, 0.075));
        ctrlPoints.add( new Point3D(-0.84, -1.5, 0.075));
        ctrlPoints.add( new Point3D(-0., -1.5, 0.075));
        ctrlPoints.add( new Point3D(-1.5, 0, 0.15));
        ctrlPoints.add( new Point3D(-1.5, -0.84, 0.15));
        ctrlPoints.add( new Point3D(-0.84, -1.5, 0.15));
        ctrlPoints.add( new Point3D(0, -1.5, 0.15));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //32
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(0, 0, 0));
        ctrlPoints.add( new Point3D(0, 0, 0));
        ctrlPoints.add( new Point3D(0, 0, 0));
        ctrlPoints.add( new Point3D(0, 0, 0));
        ctrlPoints.add( new Point3D(0, -1.425, 0));
        ctrlPoints.add( new Point3D(0.798, -1.425, 0));
        ctrlPoints.add( new Point3D(1.425, -0.798, 0));
        ctrlPoints.add( new Point3D(1.425, 0, 0));
        ctrlPoints.add( new Point3D(0, -1.5, 0.075));
        ctrlPoints.add( new Point3D(0.84, -1.5, 0.075));
        ctrlPoints.add( new Point3D(1.5, -0.84, 0.075));
        ctrlPoints.add( new Point3D(1.5, -0., 0.075));
        ctrlPoints.add( new Point3D(0, -1.5, 0.15));
        ctrlPoints.add( new Point3D(0.84, -1.5, 0.15));
        ctrlPoints.add( new Point3D(1.5, -0.84, 0.15));
        ctrlPoints.add( new Point3D(1.5, 0, 0.15));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        /*
         * OUSKO
         */

        //13
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(-1.6, 0, 2.025));
        ctrlPoints.add( new Point3D(-1.6, -0.3, 2.025));
        ctrlPoints.add( new Point3D(-1.5, -0.3, 2.25));
        ctrlPoints.add( new Point3D(-1.5, 0, 2.25));
        ctrlPoints.add( new Point3D(-2.3, 0, 2.025));
        ctrlPoints.add( new Point3D(-2.3, -0.3, 2.025));
        ctrlPoints.add( new Point3D(-2.5, -0.3, 2.25));
        ctrlPoints.add( new Point3D(-2.5, 0, 2.25));
        ctrlPoints.add( new Point3D(-2.7, 0, 2.025));
        ctrlPoints.add( new Point3D(-2.7, -0.3, 2.025));
        ctrlPoints.add( new Point3D(-3, -0.3, 2.25));
        ctrlPoints.add( new Point3D(-3, 0, 2.25));
        ctrlPoints.add( new Point3D(-2.7, 0, 1.8));
        ctrlPoints.add( new Point3D(-2.7, -0.3, 1.8));
        ctrlPoints.add( new Point3D(-3, -0.3, 1.8));
        ctrlPoints.add( new Point3D(-3, 0, 1.8));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //14
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(-1.6, 0, 2.025));
        ctrlPoints.add( new Point3D(-1.6, 0.3, 2.025));
        ctrlPoints.add( new Point3D(-1.5, 0.3, 2.25));
        ctrlPoints.add( new Point3D(-1.5, 0, 2.25));
        ctrlPoints.add( new Point3D(-2.3, 0, 2.025));
        ctrlPoints.add( new Point3D(-2.3, 0.3, 2.025));
        ctrlPoints.add( new Point3D(-2.5, 0.3, 2.25));
        ctrlPoints.add( new Point3D(-2.5, 0, 2.25));
        ctrlPoints.add( new Point3D(-2.7, 0, 2.025));
        ctrlPoints.add( new Point3D(-2.7, 0.3, 2.025));
        ctrlPoints.add( new Point3D(-3, 0.3, 2.25));
        ctrlPoints.add( new Point3D(-3, 0, 2.25));
        ctrlPoints.add( new Point3D(-2.7, 0, 1.8));
        ctrlPoints.add( new Point3D(-2.7, 0.3, 1.8));
        ctrlPoints.add( new Point3D(-3, 0.3, 1.8));
        ctrlPoints.add( new Point3D(-3, 0, 1.8));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //15
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(-2.7, 0, 1.8));
        ctrlPoints.add( new Point3D(-2.7, -0.3, 1.8));
        ctrlPoints.add( new Point3D(-3, -0.3, 1.8));
        ctrlPoints.add( new Point3D(-3, 0, 1.8));
        ctrlPoints.add( new Point3D(-2.7, 0, 1.575));
        ctrlPoints.add( new Point3D(-2.7, -0.3, 1.575));
        ctrlPoints.add( new Point3D(-3, -0.3, 1.35));
        ctrlPoints.add( new Point3D(-3, 0, 1.35));
        ctrlPoints.add( new Point3D(-2.5, 0, 1.25));
        ctrlPoints.add( new Point3D(-2.5, -0.3, 1.25));
        ctrlPoints.add( new Point3D(-2.65, -0.3, 0.9375));
        ctrlPoints.add( new Point3D(-2.65, 0, 0.9375));
        ctrlPoints.add( new Point3D(-2, 0, 0.9));
        ctrlPoints.add( new Point3D(-2, -0.3, 0.9));
        ctrlPoints.add( new Point3D(-1.9, -0.3, 0.6));
        ctrlPoints.add( new Point3D(-1.9, 0, 0.6));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //16
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(-2.7, 0, 1.8));
        ctrlPoints.add( new Point3D(-2.7, 0.3, 1.8));
        ctrlPoints.add( new Point3D(-3, 0.3, 1.8));
        ctrlPoints.add( new Point3D(-3, 0, 1.8));
        ctrlPoints.add( new Point3D(-2.7, 0, 1.575));
        ctrlPoints.add( new Point3D(-2.7, 0.3, 1.575));
        ctrlPoints.add( new Point3D(-3, 0.3, 1.35));
        ctrlPoints.add( new Point3D(-3, 0, 1.35));
        ctrlPoints.add( new Point3D(-2.5, 0, 1.25));
        ctrlPoints.add( new Point3D(-2.5, 0.3, 1.25));
        ctrlPoints.add( new Point3D(-2.65, 0.3, 0.9375));
        ctrlPoints.add( new Point3D(-2.65, 0, 0.9375));
        ctrlPoints.add( new Point3D(-2, 0, 0.9));
        ctrlPoints.add( new Point3D(-2, 0.3, 0.9));
        ctrlPoints.add( new Point3D(-1.9, 0.3, 0.6));
        ctrlPoints.add( new Point3D(-1.9, 0, 0.6));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        /*
         * PIPA
         */

        //17
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(1.7, 0, 1.425));
        ctrlPoints.add( new Point3D(1.7, -0.66, 1.425));
        ctrlPoints.add( new Point3D(1.7, -0.66, 0.6));
        ctrlPoints.add( new Point3D(1.7, 0, 0.6));
        ctrlPoints.add( new Point3D(2.6, 0, 1.425));
        ctrlPoints.add( new Point3D(2.6, -0.66, 1.425));
        ctrlPoints.add( new Point3D(3.1, -0.66, 0.825));
        ctrlPoints.add( new Point3D(3.1, 0, 0.825));
        ctrlPoints.add( new Point3D(2.3, 0, 2.1));
        ctrlPoints.add( new Point3D(2.3, -0.25, 2.1));
        ctrlPoints.add( new Point3D(2.4, -0.25, 2.025));
        ctrlPoints.add( new Point3D(2.4, 0, 2.025));
        ctrlPoints.add( new Point3D(2.7, 0, 2.4));
        ctrlPoints.add( new Point3D(2.7, -0.25, 2.4));
        ctrlPoints.add( new Point3D(3.3, -0.25, 2.4));
        ctrlPoints.add( new Point3D(3.3, 0, 2.4));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //18
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(1.7, 0, 1.425));
        ctrlPoints.add( new Point3D(1.7, 0.66, 1.425));
        ctrlPoints.add( new Point3D(1.7, 0.66, 0.6));
        ctrlPoints.add( new Point3D(1.7, 0, 0.6));
        ctrlPoints.add( new Point3D(2.6, 0, 1.425));
        ctrlPoints.add( new Point3D(2.6, 0.66, 1.425));
        ctrlPoints.add( new Point3D(3.1, 0.66, 0.825));
        ctrlPoints.add( new Point3D(3.1, 0, 0.825));
        ctrlPoints.add( new Point3D(2.3, 0, 2.1));
        ctrlPoints.add( new Point3D(2.3, 0.25, 2.1));
        ctrlPoints.add( new Point3D(2.4, 0.25, 2.025));
        ctrlPoints.add( new Point3D(2.4, 0, 2.025));
        ctrlPoints.add( new Point3D(2.7, 0, 2.4));
        ctrlPoints.add( new Point3D(2.7, 0.25, 2.4));
        ctrlPoints.add( new Point3D(3.3, 0.25, 2.4));
        ctrlPoints.add( new Point3D(3.3, 0, 2.4));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //19
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(2.7, 0, 2.4));
        ctrlPoints.add( new Point3D(2.7, -0.25, 2.4));
        ctrlPoints.add( new Point3D(3.3, -0.25, 2.4));
        ctrlPoints.add( new Point3D(3.3, 0, 2.4));
        ctrlPoints.add( new Point3D(2.8, 0, 2.475));
        ctrlPoints.add( new Point3D(2.8, -0.25, 2.475));
        ctrlPoints.add( new Point3D(3.525, -0.25, 2.49375));
        ctrlPoints.add( new Point3D(3.525, 0, 2.49375));
        ctrlPoints.add( new Point3D(2.9, 0, 2.475));
        ctrlPoints.add( new Point3D(2.9, -0.15, 2.475));
        ctrlPoints.add( new Point3D(3.45, -0.15, 2.5125));
        ctrlPoints.add( new Point3D(3.45, 0, 2.5125));
        ctrlPoints.add( new Point3D(2.8, 0, 2.4));
        ctrlPoints.add( new Point3D(2.8, -0.15, 2.4));
        ctrlPoints.add( new Point3D(3.2, -0.15, 2.4));
        ctrlPoints.add( new Point3D(3.2, 0, 2.4));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        //20
        ctrlPoints.clear();
        ctrlPoints.add( new Point3D(2.7, 0, 2.4));
        ctrlPoints.add( new Point3D(2.7, 0.25, 2.4));
        ctrlPoints.add( new Point3D(3.3, 0.25, 2.4));
        ctrlPoints.add( new Point3D(3.3, 0, 2.4));
        ctrlPoints.add( new Point3D(2.8, 0, 2.475));
        ctrlPoints.add( new Point3D(2.8, 0.25, 2.475));
        ctrlPoints.add( new Point3D(3.525, 0.25, 2.49375));
        ctrlPoints.add( new Point3D(3.525, 0, 2.49375));
        ctrlPoints.add( new Point3D(2.9, 0, 2.475));
        ctrlPoints.add( new Point3D(2.9, 0.15, 2.475));
        ctrlPoints.add( new Point3D(3.45, 0.15, 2.5125));
        ctrlPoints.add( new Point3D(3.45, 0, 2.5125));
        ctrlPoints.add( new Point3D(2.8, 0, 2.4));
        ctrlPoints.add( new Point3D(2.8, 0.15, 2.4));
        ctrlPoints.add( new Point3D(3.2, 0.15, 2.4));
        ctrlPoints.add( new Point3D(3.2, 0, 2.4));
        array = new Point3D[ctrlPoints.size()];
        ctrlPoints.toArray(array);
        teapot.add(new Bicubic(Cubic.BEZIER, array));

        int n = 10;
        verticesBuffer = ByteBuffer.allocateDirect(teapot.size() * (n+1)*(n+1) * 6 * 4 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        normalsBuffer = ByteBuffer.allocateDirect(teapot.size() * (n+1)*(n+1) * 6 * 3 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        texCoordsBuffer = ByteBuffer.allocateDirect(teapot.size() * (n+1)*(n+1) * 6 * 2 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        for (Bicubic bicubic : teapot) {
            verticesBuffer.put(ToFloatArray.convert(toVertexMesh(bicubic,n)));
            normalsBuffer.put(ToFloatArray.convert(toNormalMesh(bicubic,n)));
            texCoordsBuffer.put(ToFloatArray.convert(toTexCoordMesh(n)));
        }


    }

    private List<Point3D> toVertexMesh(Bicubic bicubic, int n){
        List<Point3D> mesh = new ArrayList<>();
        for(int x=0; x<n; x++)
            for(int y=0; y<n; y++){
                mesh.add(bicubic.compute((double)x/n,(double)y/n));
                mesh.add(bicubic.compute((double)(x+1)/n,(double)y/n));
                mesh.add(bicubic.compute((double)x/n,(double)(y+1)/n));
                mesh.add(bicubic.compute((double)(x+1)/n,(double)y/n));
                mesh.add(bicubic.compute((double)x/n,(double)(y+1)/n));
                mesh.add(bicubic.compute((double)(x+1)/n,(double)(y+1)/n));
            }
        return mesh;
    }

    private List<Vec3D> toNormalMesh(Bicubic bicubic, int n){
        List<Vec3D> mesh = new ArrayList<>();
        for(int x=0; x<n; x++)
            for(int y=0; y<n; y++){
                //TODO
                /*mesh.add(bicubic.normal((double)x/n,(double)y/n));
                mesh.add(bicubic.normal((double)(x+1)/n,(double)y/n));
                mesh.add(bicubic.normal((double)x/n,(double)(y+1)/n));
                mesh.add(bicubic.normal((double)(x+1)/n,(double)y/n));
                mesh.add(bicubic.normal((double)x/n,(double)(y+1)/n));
                mesh.add(bicubic.normal((double)(x+1)/n,(double)(y+1)/n));*/
                mesh.add(bicubic.compute((double)x/n,(double)y/n).ignoreW());
                mesh.add(bicubic.compute((double)(x+1)/n,(double)y/n).ignoreW());
                mesh.add(bicubic.compute((double)x/n,(double)(y+1)/n).ignoreW());
                mesh.add(bicubic.compute((double)(x+1)/n,(double)y/n).ignoreW());
                mesh.add(bicubic.compute((double)x/n,(double)(y+1)/n).ignoreW());
                mesh.add(bicubic.compute((double)(x+1)/n,(double)(y+1)/n).ignoreW());
            }
        return mesh;
    }

    private List<Vec2D> toTexCoordMesh(int n){
        List<Vec2D> mesh = new ArrayList<>();
        for(int x=0; x<n; x++)
            for(int y=0; y<n; y++){
                mesh.add(new Vec2D((double)x/n,(double)y/n));
                mesh.add(new Vec2D((double)(x+1)/n,(double)y/n));
                mesh.add(new Vec2D((double)x/n,(double)(y+1)/n));
                mesh.add(new Vec2D((double)(x+1)/n,(double)y/n));
                mesh.add(new Vec2D((double)x/n,(double)(y+1)/n));
                mesh.add(new Vec2D((double)(x+1)/n,(double)(y+1)/n));
            }
        return mesh;
    }

    public List<Bicubic> getTeapot() {
        return teapot;
    }

    public OGLBuffers toOGLBuffers() {
        return null;
    }

    private OGLBuffers buffer;
    private FloatBuffer verticesBuffer, normalsBuffer, texCoordsBuffer;


    public FloatBuffer getVerticesBuffer() {
        return verticesBuffer;
    }

    public FloatBuffer getNormalsBuffer() {
        return normalsBuffer;
    }

    public FloatBuffer getTexCoordsBuffer() {
        return texCoordsBuffer;
    }

    public OGLBuffers getBuffers() {
        return buffer;
    }

    public int getTopology() {
        return GL_TRIANGLES;
    }
}
