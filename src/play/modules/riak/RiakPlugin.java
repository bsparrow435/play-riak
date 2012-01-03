package play.modules.riak;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;

import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.RiakFactory;
import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakException;

public class RiakPlugin extends PlayPlugin {

    /**
     * riak url like http://localhost:8091/riak
     */
    public static String RIAK_URL   = "";
    public static int    RIAK_PORT  = 0;
    /**
     * riak url without /riak
     */
    public static String RIAK_BASE  = "";
    /**
     * Hardcode URL in riak
     */
    public static String RIAK_URL_STATS = "stats";

    /**
     * main client
     */
    public static IRiakClient riak = null;

    public static String DEFAULT_MODEL_PREFIX = "models.riak.";
    public static String MODEL_PREFIX;

    private RiakEnhancer enhancer = new RiakEnhancer();

    public static String getBucketName(Class clazz) throws Exception {
        return ((Bucket)clazz.getField("bucket").get(null)).getName();
    }

    @Override
    public void enhance(ApplicationClass applicationClass) throws Exception {
        enhancer.enhanceThisClass(applicationClass);
    }

    public boolean init() {
        RIAK_URL = Play.configuration.getProperty("riak.protobuf.url");
        RIAK_PORT = Integer.valueOf(Play.configuration.getProperty("riak.protobuf.port"));
        if(RIAK_URL.isEmpty() || RIAK_PORT == 0){
            Logger.error("riak.protobuf.url or riak.protobuf.port is empty");
            return false;
        }

        try {
            Logger.info("Init riak client (url: %s port: %s )", RIAK_URL, RIAK_PORT);
            riak = RiakFactory.pbcClient(RIAK_URL, RIAK_PORT);
            riak.getClientId();
            Logger.info("Successfully connected to riak");
            return true;
        } catch (RiakException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onApplicationStart() {
        if(!init()) Logger.error("Riak %s cluster not responding !", RIAK_URL);

        List<ApplicationClass> classes = Play.classes.getAnnotatedClasses(RiakEntity.class);
        for (ApplicationClass aclazz : classes) {
            Class clazz = aclazz.javaClass;
            String entityName = clazz.getName();

            RiakEntity annotation = (RiakEntity) clazz.getAnnotation(RiakEntity.class);

            String key = annotation.key();
            if (key.equals("")) {
                Logger.error("Key for class %s is not defined.", this.toString());
                key = "key";
            }

            try {
                clazz.getField("keyField").set(null, key);
            } catch (Exception e) {
                Logger.error("Issue setting keyField of %s.", entityName);
            }

            String bucketName = annotation.bucket();
            if (bucketName.equals("")) {
                bucketName = entityName;
                bucketName = bucketName.substring(bucketName.lastIndexOf(".") + 1);
            }

            Bucket bucket;
            try {
                bucket = RiakPlugin.riak.createBucket(bucketName).execute();
            } catch (RiakException e) {
                Logger.error("Enable to create bucket for %s.", entityName);
                return;
            }

            try {
                clazz.getField("bucket").set(null, bucket);
            } catch (Exception e) {
                Logger.error("Issue setting bucket of %s.", entityName);
            }
        }
    }
}
