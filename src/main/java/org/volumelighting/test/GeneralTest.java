package org.volumelighting.test;

import java.util.Random;

import org.volumelighting.vl.VolumeLightFilter;

import com.jme3.animation.LoopMode;
import com.jme3.app.SimpleApplication;
import com.jme3.cinematic.MotionPath;
import com.jme3.cinematic.events.MotionEvent;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;

public class GeneralTest extends SimpleApplication {

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        GeneralTest app = new GeneralTest();
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1280, 720);
        settings.setSamples(4);

        app.setSettings(settings);
        app.setShowSettings(false);
        app.setPauseOnLostFocus(false);
        app.start();
    }

    private Random random = new Random(4l);
    private FilterPostProcessor fpp;

    private final float droneRange = 80f;
    private final float numOfObjects = 4;
    private final float randomObjectMaxRange = 4;
    private Node randObjects = new Node("randObjects");

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(20f);
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        for (int i = 0; i < 100; i++) {
            addRandomObject();
        }

        rootNode.attachChild(randObjects);

        fpp = new FilterPostProcessor(assetManager);
        addSpotLightDrone(new ColorRGBA(1.0f, 0.96f, 0.7f, 1.0f).mult(.8f), 20f * FastMath.DEG_TO_RAD);
        addSpotLightDrone(ColorRGBA.Cyan, 8f * FastMath.DEG_TO_RAD);
        addSpotLightDrone(new ColorRGBA(1.0f, 0.96f, 0.7f, 1.0f).mult(6f), 4f * FastMath.DEG_TO_RAD);
        viewPort.addProcessor(fpp);
    }

    /**
     *
     * @param droneColor
     * @param coneSize
     */
    private void addSpotLightDrone(ColorRGBA droneColor, float coneSize) {

        // drone box mesh
        Geometry drone = new Geometry("drone", new Box(.5f, .5f, 1));
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", droneColor);
        drone.setMaterial(mat);
        rootNode.attachChild(drone);

        // target Node
        Node target = new Node("droneTarget");
        rootNode.attachChild(target);

        // drone spotLight
        SpotLight spot = new SpotLight();
        spot.setSpotRange(200);
        spot.setSpotInnerAngle(coneSize / 10);
        spot.setSpotOuterAngle(coneSize * 1.4f);
        spot.setColor(droneColor);
        rootNode.addLight(spot);

        float duration = 10f + random.nextFloat() * 80f;
        createMotionPath(drone, duration, ColorRGBA.Orange, false);
        createMotionPath(target, duration, ColorRGBA.Red, false);

        drone.addControl(new DroneControl(target, spot));

//        SpotLightShadowFilter slsf = new SpotLightShadowFilter(assetManager, 128);
//        slsf.setLight(spot);    
//        slsf.setShadowIntensity(.8f);
//        slsf.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);  
//        filterPostProcessor.addFilter(slsf);

        VolumeLightFilter vsf = new VolumeLightFilter(spot, 128, coneSize * 5, rootNode);
        fpp.addFilter(vsf);
    }

    /**
     *
     * @param target
     * @param duration
     * @param debugColor
     * @param debug
     */
    private void createMotionPath(Spatial target, float duration, ColorRGBA debugColor, boolean debug) {
        // motion path for drone to target
        MotionPath path = new MotionPath();
        path.setCycle(true);
        for (int i = 0; i < 10; i++) {
            float x = random.nextFloat() * droneRange - droneRange / 2f;
            float y = random.nextFloat() * droneRange - droneRange / 2f;
            float z = random.nextFloat() * droneRange - droneRange / 2f;
            path.addWayPoint(new Vector3f(x, y, z));
        }

        if (debug) {
            Node debugNode = new Node("MotionPath");
            path.enableDebugShape(assetManager, debugNode);
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", debugColor);
            debugNode.setMaterial(mat);
            rootNode.attachChild(debugNode);
        }

        MotionEvent motionControl = new MotionEvent(target, path);
        motionControl.setInitialDuration(duration);
        motionControl.setLoopMode(LoopMode.Loop);
        motionControl.play();
    }

    private void addRandomObject() {

        float r = random.nextFloat();

        Node node = new Node("Random Object");
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.White.mult(.05f));

        Vector3f position = getRandomPosition(randomObjectMaxRange);

        if (r < 1f / numOfObjects) {
            Box b = new Box(1, 1, 1);
            Geometry g = new Geometry("randBox", b);
            g.scale(.2f);
            g.setLocalTranslation(position);
            node.attachChild(g);

        } else if (r < 2f / numOfObjects) {
            Sphere s = new Sphere(20, 20, .5f);
            Geometry g = new Geometry("randSphere", s);
            g.setLocalTranslation(position);
            node.attachChild(g);

        } else if (r < 3f / numOfObjects) {
            // Node jaime = (Node)assetManager.loadModel("Models/Jaime/Jaime.j3o");
            // jaime.setLocalTranslation(position);
            // node.attachChild(jaime);

        } else if (r < 4f / numOfObjects) {
            Spatial teapot = assetManager.loadModel("Models/Teapot/Teapot.obj");
            teapot.setLocalTranslation(position);
            node.attachChild(teapot);
        }

        node.scale(5f);
        node.rotate(random.nextFloat() * 10, random.nextFloat() * 10, random.nextFloat() * 10);
        node.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        node.setMaterial(mat);

        randObjects.attachChild(node);
    }

    private Vector3f getRandomPosition(float range) {
        float x = random.nextFloat() * range;
        float y = random.nextFloat() * range;
        float z = random.nextFloat() * range;
        return new Vector3f(x, y, z);
    }

    float time = -5;

    @Override
    public void simpleUpdate(float tpf) {
//        time += tpf;
//        if (time > 0) {
//            time = -5;
//            System.out.println("eh");
//            for (Filter f : fpp.getFilterList()) {
//                f.setEnabled(!f.isEnabled());
//            }
//        }
    }

    @Override
    public void simpleRender(RenderManager rm) {
        // TODO: add render code
    }

    class DroneControl extends AbstractControl {

        private Spatial target;
        private SpotLight light;

        public DroneControl(Spatial target, SpotLight light) {
            this.target = target;
            this.light = light;
        }

        @Override
        protected void controlUpdate(float tpf) {
            spatial.lookAt(target.getLocalTranslation(), Vector3f.UNIT_Y);

            // Synchronize the spot light with its parent drone
            light.setPosition(spatial.getLocalTranslation());
            light.setDirection(spatial.getLocalRotation().mult(Vector3f.UNIT_Z));
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) {
            // TODO Auto-generated method stub
        }

    }
}
