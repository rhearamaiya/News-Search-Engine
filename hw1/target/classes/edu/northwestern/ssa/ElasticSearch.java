package edu.northwestern.ssa;
import edu.northwestern.ssa.AwsSignedRestRequest;
import org.json.JSONObject;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpMethod;

import java.io.IOException;
import java.util.Optional;

public class ElasticSearch extends AwsSignedRestRequest {

    String serviceName;

    ElasticSearch(String serviceName){ //constructor
        super(serviceName);
    }

        public HttpExecuteResponse NewElasticSearchIndex() throws IOException {
            restRequest(SdkHttpMethod.DELETE, System.getenv("ELASTIC_SEARCH_HOST"), System.getenv("ELASTIC_SEARCH_INDEX"), java.util.Optional.empty()); //from campuswire
            HttpExecuteResponse resp= restRequest(SdkHttpMethod.PUT, System.getenv("ELASTIC_SEARCH_HOST"), System.getenv("ELASTIC_SEARCH_INDEX"), java.util.Optional.empty());
            resp.responseBody().get().close(); //campuswire
            return resp;
        }

        public void PostNewDocument(JSONObject obj) throws IOException{
            HttpExecuteResponse resp = restRequest(SdkHttpMethod.POST, System.getenv("ELASTIC_SEARCH_HOST"), System.getenv("ELASTIC_SEARCH_INDEX")+ "/_doc/", java.util.Optional.empty(), java.util.Optional.of(obj)); //from campuswire and PM office hrs
            resp.responseBody().get().close(); //campuswire
            return;
        }

}