package edu.northwestern.ssa.api;

import edu.northwestern.ssa.AwsSignedRestRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;


import javax.swing.text.html.Option;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Path("/search")
public class Search {

    /**
     * when testing, this is reachable at http://localhost:8080/api/search?query=hello
     */
    @GET
    public Response getMsg(@QueryParam("query") String q, @QueryParam("language") String language, @QueryParam("date") String date, @QueryParam("offset") String offset, @QueryParam("count") String count) throws IOException {
        JSONArray results = new JSONArray();
        //results.put("hello world!");
        //results.put(q);
        //400 if a required query parameter is missing - only required query parameter is q
        if (q == null) { //is this right?
            return Response.status(400).type("text/plain").entity("query is missing from url.")
                    .header("Access-Control-Allow-Origin", "*").build();
        }

        Map<String, String> myMap = new HashMap<>();

        String[] query_params = q.split(" ");//https://www.baeldung.com/string/split
        String total_string = ""; //will be adding to the query string depending on what params are given

        for (String s : query_params) //iterating through all given parameters
        {
            if (total_string.equals("")) //empty to begin with
            {
                //total_string += "txt:(" + s + ")"; //if no parameters yet, must start with query parameters
                total_string += "txt:(" + s; //if no parameters yet, must start with query parameters
            } else
            {
                //total_string += " AND (" + s + ")";
                total_string += " AND " + s; //if there's already stuff in the string, then that means query parameters are there already. add to the string.
            }
        }
        total_string += ")"; //param string ends with ) (from campuswire)

        //myMap.put("q", "txt:" + q + " AND lang:" + language + " AND date:" + date + "AND offset:" + offset + "AND count:" + count);
        //myMap.put("q", "txt:(" + q + ") + "); //gotta do this every time because q is a required parameter

        if (language != null) {
            //myMap.put("language:", language);
            total_string += " AND lang:" + language;
        }

        if (date != null) {
            total_string += " AND date:" + date;
        }
        System.out.println(total_string);
        myMap.put("q", total_string);

        if (offset != null) {
            myMap.put("from", offset);
        }

        if (count == null) {
            count = "10";
            myMap.put("size", count);
        } else {
            myMap.put("size", count);
        }
        /*
        if(count != "10"){
            myMap.put("size", count);
        }
        */
        myMap.put("track_total_hits", "true"); //source: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-search.html
        //System.out.println(myMap);
        AwsSignedRestRequest es = new AwsSignedRestRequest("es");
        String path = System.getenv("ELASTIC_SEARCH_INDEX") + "/_search"; //is this right? Do I need the env. variable?
        HttpExecuteResponse response1 = es.restRequest(SdkHttpMethod.GET, System.getenv("ELASTIC_SEARCH_HOST"), path, Optional.of(myMap), Optional.empty());

        //es.close();//closing elastic search
        //from response I need to convert it to a string for use with JSON: response1.responseBody().get().read
        InputStream r = response1.responseBody().get(); //from campuswire/TA office hrs

        //source: https://stackoverflow.com/questions/309424/how-do-i-read-convert-an-inputstream-into-a-string-in-java
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = r.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        String content = result.toString(StandardCharsets.UTF_8.name()); //JSON String

        //String content = new String(rawData, StandardCharsets.UTF_8.name()); //responseBody to String
        //System.out.println(content);
        JSONObject object1 = new JSONObject(content); //make string into JSON object source: https://stackoverflow.com/questions/36912389/converting-byte-to-jsonobject
        JSONArray articles = new JSONArray(); //array of article JSON objects

        //int total_results = object1.getJSONObject("hits").getJSONObject("total").getInt("value");
        //https://www.elastic.co/guide/en/elasticsearch/reference/current/search-your-data.html

        //source: https://stackoverflow.com/questions/31681705/get-value-from-a-jsonobject
        //source: https://stackoverflow.com/questions/15699953/how-do-i-parse-json-into-an-int
        Integer total_results = object1.getJSONObject("hits").getJSONObject("total").getInt("value");
        JSONArray returned_results = object1.getJSONObject("hits").getJSONArray("hits");

        //for loop to put article_details into articles
        //https://www.programmersought.com/article/66414893733/
        for (int i = 0; i < returned_results.length(); i++) {
            JSONObject object2 = returned_results.getJSONObject(i);
            JSONObject source = object2.getJSONObject("_source");
            JSONObject article_details = new JSONObject();

            article_details.put("title", source.get("title"));
            article_details.put("url", source.get("url"));
            article_details.put("txt", source.get("txt"));

            if (source.has("date")) {
                article_details.put("date", source.get("date"));
            }

            else {
                //String x = null;
                article_details.put("date", JSONObject.NULL); //https://stackoverflow.com/questions/44631604/unable-to-put-null-values-in-json-object
            }
            if (source.has("lang")) {
                article_details.put("lang", source.get("lang"));
            }
            else {
                //String y = null;
                article_details.put("lang", JSONObject.NULL);
            }

            articles.put(article_details);//each article has article details
        }

        //System.out.println(articles);
        JSONObject object3 = new JSONObject();
        //object3.put("returned_results", Integer.valueOf(count));
        //idk about below
        if(returned_results.length()<Integer.valueOf(count)) {
            object3.put("returned_results", Integer.valueOf(returned_results.length())); //this right?
        }
        else{
            object3.put("returned_results", Integer.valueOf(count));
        }
        //object3.put("returned_results", Integer.valueOf(count));
        object3.put("total_results", Integer.valueOf(total_results));
        object3.put("articles", articles);
        response1.responseBody().get().close();
        es.close();
        return Response.status(200).type("application/json").entity(object3.toString(4))
                // below header is for CORS
                .header("Access-Control-Allow-Origin", "*").build();


    }
}