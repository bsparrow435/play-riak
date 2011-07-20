package play.modules.riak;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.query.MapReduce;
import com.basho.riak.client.query.MapReduceResult;
import com.basho.riak.client.query.functions.NamedJSFunction;
import com.basho.riak.client.query.functions.JSSourceFunction;

import com.google.protobuf.ByteString;

import play.Logger;
import play.Play;

public class RiakMapReduce {
	
	public static Map<String, String> function = new HashMap<String, String>();
	
	
	public static void loadQuery(){
		
		String rootPath = Play.modules.get("riak").getRealFile().getAbsolutePath() + "/src/play/modules/riak/mapreduce";
		Logger.debug("Load script in %s", rootPath);
		//load core directory (play.modules.riak.mapreduce)
		//TODO: and custom define in riak.mapreduce.input
		File dir = new File(rootPath);
		
		if(!dir.exists()){
			Logger.info("Dir play.modules.riak.mapreduce not exist" );
			return;
		}
		
		String[] scriptList = dir.list();
		
		for (String file : scriptList) {
			Logger.debug("Load script: %s", file);
			String content = "";
			String cleanFileName = "";
			
			if(file.endsWith(".js")){
				content = getJavascriptfile(rootPath + "/" + file);
				cleanFileName = file.substring(0, file.length() - ".js".length());
			}else if(file.endsWith(".coffee")){
				content = getCoffeeFile(rootPath + "/" + file);
				cleanFileName = file.substring(0, file.length() - ".coffee".length());
			}
			
			if(!content.isEmpty()){
				function.put(cleanFileName, content);
			}
		}
	}
	
	
	private static String getJavascriptfile(String filename){
		File file = new File(filename);
        String content;
		try {
			content = FileUtils.readFileToString(file);
			return content;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}		
	}
	
    private static String getCoffeeFile(String filename) {
        try {
            //File file = new File(filename + ".coffee");
        	File file = new File(filename);
            String content = FileUtils.readFileToString(file);
            return new org.jcoffeescript.JCoffeeScriptCompiler().compile(content);
        }
        catch (Exception e) {
        	e.printStackTrace();
            return "";
        }
    }

	public static long count(Class clazz){
		String bucket = RiakPlugin.getBucketName(clazz);
        IRiakClient client = RiakPlugin.riak;

		MapReduce mr = client.mapReduce(bucket)
            .addMapPhase(new JSSourceFunction(RiakMapReduce.function.get("count")), false)
		    .addReducePhase(new NamedJSFunction("Riak.reduceSum"), true);
		
		try {
			MapReduceResult mrs = mr.execute();

            for (Long l : mrs.getResult(Long.class)) {
                if(l != null) return l;
            }
		
		}catch (RiakException e) {
			e.printStackTrace();
		}		
		return -1;
	}
}
