package play.modules.riak;

import javassist.CtClass;
import javassist.CtMethod;
import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.Enhancer;

import com.basho.riak.client.bucket.DomainBucket;
import com.basho.riak.client.bucket.Bucket;

public class RiakEnhancer extends Enhancer {

    public static final String PACKAGE_NAME = "play.modules.riak";

    public static final String ENTITY_ANNOTATION_NAME = "play.modules.riak.RiakEntity";
    public static final String ENTITY_ANNOTATION_VALUE = "value";

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        final CtClass ctClass = makeClass(applicationClass);

        // Enhance RiakEntity annotated classes
        if (hasAnnotation(ctClass, ENTITY_ANNOTATION_NAME)) {
            enhanceRiakEntity(ctClass, applicationClass);
        }
        else {
            return;
        }
    }

    /**
     * Enhance classes marked with the RiakEntity annotation.
     *
     * @param ctClass
     * @throws Exception
     */
    private void enhanceRiakEntity(CtClass ctClass, ApplicationClass applicationClass) throws Exception {
        // Don't need to fully qualify types when compiling methods below
        classPool.importPackage(PACKAGE_NAME);
        Class clazz = ctClass.toClass();
        ctClass.defrost();

        RiakEntity annotation = (RiakEntity) clazz.getAnnotation(RiakEntity.class);

        if (annotation == null) {
            Logger.error("Annotation missing: %s", this.toString());
            return;
        }

        // - Implement methods
        Logger.debug(this.getClass().getName() + "-->enhancing RiakEntity-->" + ctClass.getName());

        /*String saveMethod = "public boolean save() {" +
            "try {" +
                "bucket.store(this.getKey(), this).execute();" +
                "return true;" +
            "} catch (Exception e) {" +
                "e.printStackTrace();" +
                "return false;" +
            "}" +
        "}";
        CtMethod save = CtMethod.make(saveMethod, ctClass);
        ctClass.addMethod(save);*/

        String findMethod = "public static RiakModel find(String key) {" +
            "try {" +
                "return (RiakModel)bucket.fetch(key, " + clazz.getName() + ".class).execute();" +
            "} catch(com.basho.riak.client.RiakException e1){" +
                "e1.printStackTrace();" +
                "return null;" +
            "}" +
        "}";
        CtMethod find = CtMethod.make(findMethod, ctClass);
        ctClass.addMethod(find);

        // Done.
        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.detach();
    }
}
