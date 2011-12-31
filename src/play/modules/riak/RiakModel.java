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
import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.builders.RiakObjectBuilder;
import com.basho.riak.client.RiakLink;

import com.basho.riak.client.query.MapReduce;
import com.basho.riak.client.query.MapReduceResult;
import com.basho.riak.client.query.functions.NamedJSFunction;
import com.basho.riak.client.query.functions.JSSourceFunction;

import com.google.gson.Gson;
import com.google.protobuf.ByteString;

public class RiakModel {
    // Raw object
    private IRiakObject obj;

    public String getBucket() {
        return obj.getBucket();
    }

    public String getKey() {
        return obj.getKey();
    }

    public IRiakObject getObj(){
        return obj;
    }
    public void setObj(IRiakObject obj){
        this.obj = obj;
    }

    public Iterable<Map.Entry<String,String>> getUserMeta(){
        return obj.userMetaEntries();
    }

    public void setUserMeta(Map<String,String> usermeta){
        for(Map.Entry<String, String> entry : usermeta.entrySet()) {
            obj.addUsermeta(entry.getKey(), entry.getValue());
        }
    }

    public static String generateUID(){
        return String.valueOf(UUID.randomUUID());
    }

    public boolean save() {
        Logger.debug("RiakModel save %s", this.toString());

        IRiakObject o = null;
        // Hack to prevent serialisation of obj
        if(this.obj != null){
            o = this.obj;

        }
        this.obj = null;

        String jsonValue = new Gson().toJson(this);
        RiakPath path = this.getPath();

        if(path != null){
            Logger.debug("Create new object %s, %s", path.getBucket(), path.getValue());

            RiakObjectBuilder builder = this.obj != null
                ? RiakObjectBuilder.from(this.obj)
                : RiakObjectBuilder.newBuilder(path.getBucket(), path.getKey());

            builder = builder.withValue(jsonValue);
                //.withContentType("application/json");

            if(o != null && o.getLinks() != null)
                builder.withLinks(o.getLinks());

            this.obj = builder.build();

            try {
                this.obj = riak.fetchBucket(path.getBucket())
                    .execute()
                    .store(this.obj)
                    .execute();
                ;
                return true;
            } catch (RiakException e) {
                Logger.error("Error during save of %s: %s", path.getKey(), jsonValue);
                e.printStackTrace();
                return false;
            }
        }else{
            return false;
        }
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

    public static Iterable<String> findKeys(String bucket){
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
        RiakPath path = this.getPath();
        if(path != null){
            Logger.debug("Delete bucket: %s , keyValue %s", path.getBucket(), path.getValue());
            delete(path.getBucket(), path.getValue());
        }
        return false;
    }

    public void addLink(Class clazz, String key, String tag){
        this.addLink(RiakPlugin.getBucketName(clazz), key, tag);
    }
    public void addLink(String bucket, String key, String tag){
        obj.addLink(new RiakLink(tag, bucket, key));
    }
}
