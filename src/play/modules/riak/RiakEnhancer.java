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
        String entityName = ctClass.getName();
        Class clazz = this.getClass();

        RiakEntity annotation = (RiakEntity) clazz.getAnnotation(RiakEntity.class);

        String key = annotation.key();
        if (key.equals("")) {
            Logger.error("Key for class %s is not defined", this.toString());
        } else {
            clazz.getDeclaredField("keyField").set(this, key);

            String bucketName = annotation.bucket();
            if(bucketName.equals("")){
                bucketName = entityName;
                bucketName = bucketName.substring(bucketName.lastIndexOf(".") + 1);
            }
            Bucket bucket = RiakPlugin.riak.createBucket(bucketName).execute();
            clazz.getDeclaredField("bucket").set(this, bucket);
        }

        // - Implement methods
        Logger.debug( clazz.getName() + "-->enhancing RiakEntity-->" + ctClass.getName());

        CtMethod save = CtMethod.make("public void save() {" +
            "try {" +
                "this.bucket.store(this." + key + ", this).execute();" +
            "} catch(com.basho.riak.client.RiakException e1){" +
                "e1.printStackTrace();" +
            "}", ctClass);
        ctClass.addMethod(save);

        CtMethod find = CtMethod.make("public static RiakModel find(String bucket, String key){" +
            "try {" +
                "com.basho.riak.client.IRiakObject ro = play.modules.riak.RiakPlugin.riak.fetchBucket(bucket).execute().fetch(key).execute();" +
                "if(ro != null){" +
                    entityName +" e = (" + entityName + ")new com.google.gson.Gson().fromJson(ro.getValueAsString(), " +
                    entityName +".class);" +
                    "e.setObj(ro);" +
                    "return e;" +
                "}" +
            "} catch(com.basho.riak.client.RiakException e1){" +
                "e1.printStackTrace();" +
            "}"+
            "return null;}", ctClass);
        ctClass.addMethod(find);


        CtMethod find2 = CtMethod.make("public static RiakModel find(Class clazz, String key){" +
                "return find(play.modules.riak.RiakPlugin.getBucketName(clazz), key); }",ctClass);
        ctClass.addMethod(find2);

        CtMethod findAll = CtMethod.make("public static java.util.List findAll(String bucket){" +
                "java.util.Collection keys = "+ entityName + ".findKeys(bucket);" +
                "java.util.List result = new java.util.ArrayList();"+
                "for (java.util.Iterator iterator = keys.iterator(); iterator.hasNext();) {"+
                    "String key = (String) iterator.next();"+
                    "result.add(find(bucket,key));"+
                "}"+
                "return result;}", ctClass);

        ctClass.addMethod(findAll);


        CtMethod findAll2 = CtMethod.make("public static java.util.List findAll(Class clazz){" +
                "return findAll(play.modules.riak.RiakPlugin.getBucketName(clazz));}",ctClass);
        ctClass.addMethod(findAll2);

        // Done.
        applicationClass.enhancedByteCode = ctClass.toBytecode(); 
        ctClass.detach();
    }
}
