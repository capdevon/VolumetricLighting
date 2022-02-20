package org.volumelighting.vl;

import java.io.IOException;
import java.util.ArrayList;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.post.Filter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;

public class VolumeLightFilter extends Filter {

    private Filter.Pass lightVolumePass;
    private RenderManager renderManager;
    private ViewPort viewPort;
    private VolumeLightRenderer shadowVolumeRenderer;

    private Material lightVolumeMat;
    private Geometry lightVolume;
    private int resolution;
    private float intensity = 10f;

    private Camera lightCam;
    private SpotLight light;
    private FrustumVolumeMesh fvm;

    /**
     * Constructor.
     * 
     * @param spot
     * @param resolution
     * @param startFrom
     * @param rootNode
     */
    public VolumeLightFilter(SpotLight spot, int resolution, float startFrom, Node rootNode) {
        super("Volumetric Light Filter");

        this.light = spot;
        this.resolution = resolution;

        lightCam = new Camera(resolution, resolution);
        lightCam.setFrustumPerspective(light.getSpotOuterAngle() * FastMath.RAD_TO_DEG * 2.0f, 1, startFrom, light.getSpotRange());
        lightCam.update();

        // !! NEED TO SET METHOD OF LIGHT FRONT PLANE
        fvm = new FrustumVolumeMesh(resolution, lightCam);
        lightVolume = new Geometry("fvm", fvm);
        lightVolume.setIgnoreTransform(false);
        lightVolume.setCullHint(Spatial.CullHint.Always);
        rootNode.attachChild(lightVolume); // pretty dirty

        syncLightCam();
    }

    private void syncLightCam() {
        lightCam.getRotation().lookAt(light.getDirection(), lightCam.getUp());
        lightCam.setLocation(light.getPosition());
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {

        // System.out.println("Init Filter");
        shadowVolumeRenderer = new VolumeLightRenderer(manager, resolution);
        shadowVolumeRenderer.setShadowCam(lightCam);
        shadowVolumeRenderer.initialize(renderManager, vp);

        this.renderManager = renderManager;
        this.viewPort = vp;

        postRenderPasses = new ArrayList<Filter.Pass>();

        lightVolumeMat = new Material(manager, "MatDefs/VolumetricLighting/VolumetricLight.j3md");
        lightVolumeMat.getAdditionalRenderState().setWireframe(false); // good for debugging
        // volumeShadow_mat.setTexture("CookieMap", assetManager.loadTexture("Textures/Cookie2.png")); // Cookie coming soon
        lightVolumeMat.setMatrix4("LightViewProjectionMatrix", lightCam.getViewProjectionMatrix());
        lightVolumeMat.setMatrix4("LightViewProjectionInverseMatrix", lightCam.getViewProjectionMatrix().invert());
        lightVolumeMat.setVector3("CameraPos", vp.getCamera().getLocation());
        lightVolumeMat.setColor("LightColor", light.getColor());
        lightVolumeMat.setFloat("LightIntensity", intensity);
        lightVolumeMat.setVector2("LinearDepthFactorsLight", getLinearDepthFactors(lightCam));
        lightVolumeMat.setVector2("LinearDepthFactorsCam", getLinearDepthFactors(vp.getCamera()));
        // this is nasty, but is silly to calculate every frame
        lightVolumeMat.setVector2("LightNearFar", new Vector2f(lightCam.getFrustumNear(), fvm.farPlaneGridDistance()));

        lightVolume.setMaterial(lightVolumeMat);

        lightVolumePass = new Filter.Pass() {
//            @Override
//            public boolean requiresDepthAsTexture() {
//                return true;
//            }

//            @Override
//            public boolean requiresSceneAsTexture() {
//                return true;
//            }
        };

        lightVolumePass.init(renderManager.getRenderer(), w, h, Format.RGBA32F, Format.Depth, 1, true);
        // could perhaps change to a more supported format, and render front faces then back

        material = new Material(manager, "MatDefs/VolumetricLighting/VolumetricLightFilter.j3md");
        material.setTexture("LightingVolumeTex", lightVolumePass.getRenderedTexture());
        shadowVolumeRenderer.setPostShadowMaterial2(lightVolumeMat);
    }

    /**
     * Pre calculate the depth linearization factors so they can be passed to
     * the shader since they rarely change
     *
     * @param cam
     * @return
     */
    private Vector2f getLinearDepthFactors(Camera cam) {
        float near = cam.getFrustumNear();
        float far = cam.getFrustumFar();
        float a = far / (far - near);
        float b = far * near / (near - far);

        return new Vector2f(a, b);
    }

    @Override
    protected Material getMaterial() {
        return material;
    }

    @Override
    protected boolean isRequiresDepthTexture() {
        return true;
    }

    @Override
    protected void postQueue(RenderQueue queue) {
        shadowVolumeRenderer.postQueue(queue);
    }

    @Override
    protected void postFrame(RenderManager renderManager, ViewPort viewPort, FrameBuffer prevFilterBuffer, FrameBuffer sceneBuffer) {

        shadowVolumeRenderer.postFrame(sceneBuffer);

        renderManager.setCamera(viewPort.getCamera(), false);

        lightVolumeMat.setTexture("SceneDepthTexture", sceneBuffer.getDepthBuffer().getTexture());

        // sync volume
        lightVolume.setLocalTranslation(lightCam.getLocation());
        lightVolume.setLocalRotation(lightCam.getRotation());

        renderManager.getRenderer().setFrameBuffer(lightVolumePass.getRenderFrameBuffer());
        renderManager.getRenderer().clearBuffers(true, true, true);
        // renderManager.setForcedTechnique("ShadowVolume");
        renderManager.renderGeometry(lightVolume);
        // renderManager.setForcedTechnique(null);
    }

    @Override
    protected void preFrame(float tpf) {
        syncLightCam();

        lightVolumeMat.setVector3("CameraPos", viewPort.getCamera().getLocation());
        lightVolumeMat.setVector3("LightPos", lightCam.getLocation());
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        // oc.write(intensity, "Intensity", 1.0f);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        // intensity = ic.readFloat("Intensity", 1.0f);
    }

    /**
     * @return the intensity
     */
    public float getIntensity() {
        return intensity;
    }

    /**
     * @param intensity the intensity to set
     */
    public void setInensity(float intensity) {
        this.intensity = intensity;
        if (lightVolumeMat != null) {
            lightVolumeMat.setFloat("LightIntensity", intensity);
        }
    }
}
