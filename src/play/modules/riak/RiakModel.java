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
    public static String keyField;

    public String key;

    public String getKey() {
        try {
            return (String)(this.getClass().getDeclaredField(keyField).get(this));
        } catch (Exception e) {
            return null;
        }
    }

    public boolean save() {
        try {
            bucket.store(this.getKey(), this).execute();
            return true;
        } catch (RiakException e) {
            Logger.error("Error during save for bucket: %s", this.getKey());
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete() {
        try {
            bucket.delete(this.getKey()).execute();
            return true;
        } catch (RiakException e) {
            return false;
        }
    }

    public static <T extends RiakModel> T find(String key) {
        throw new UnsupportedOperationException("Please annotate your model with @RiakEntity annotation.");
    }
}
