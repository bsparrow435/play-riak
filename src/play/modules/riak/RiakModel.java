package play.modules.riak;

import static play.modules.riak.RiakPlugin.riak;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import play.Logger;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.bucket.Bucket;

public class RiakModel {
    public static Bucket bucket;
    public static String keyField = "key";

    public boolean save() {
        throw new UnsupportedOperationException("Please annotate your model with @RiakEntity annotation.");
    }

    public static <T extends RiakModel> List<T> findAll(Class clazz){
        throw new UnsupportedOperationException("Please annotate your model with @RiakEntity annotation.");
    }

    public static <T extends RiakModel> List<T> findAll(String bucket){
        throw new UnsupportedOperationException("Please annotate your model with @RiakEntity annotation.");
    }

    //TODO: fix in enhancer
    public static <T extends RiakModel> List<T> fetch(Class clazz, Type returnType, int start, int end){
        throw new UnsupportedOperationException("Please annotate your model with @RiakEntity annotation.");
    }

    public static Iterable<String> findKeys(Class clazz){
        return findKeys(RiakPlugin.getBucketName(clazz)); 
    }

    public static Iterable<String> findKeys(String bucket) {
        try {
            return riak.fetchBucket(bucket)
                .execute()
                .keys();
        } catch (RiakException e) {
            Logger.error("Error during listKeys for bucket: %s", bucket);
            e.printStackTrace();
            return null;
        }
    }

    public static <T extends RiakModel> T find(Class clazz, String key){
        throw new UnsupportedOperationException("Please annotate your model with @RiakEntity annotation.");
    }
    public static <T extends RiakModel> T find(String bucket, String key){
        throw new UnsupportedOperationException("Please annotate your model with @RiakEntity annotation.");
    }

    public static void delete(Class clazz, String key){
        delete(RiakPlugin.getBucketName(clazz), key);
    }

    public static void delete(String bucket, String key){
        try {
            riak.fetchBucket(bucket)
                .execute()
                .delete(key)
                .execute();
        } catch (RiakException e) {
            Logger.error("Error during deletion of bucket: %s, key: %s", bucket, key);
            e.printStackTrace();
        }
    }

    public boolean delete(){
        throw new UnsupportedOperationException("Please annotate your model with @RiakEntity annotation.");
    }
}
