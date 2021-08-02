package edu.northwestern.ssa;

import org.archive.io.ArchiveReader;

import org.archive.io.warc.WARCReaderFactory;
import org.json.JSONObject;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.sync.ResponseTransformer;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

public class App {
    public static String getObject(String bucketName, S3Client s3){

        if(System.getenv("COMMON_CRAWL_FILENAME") == null || System.getenv("COMMON_CRAWL_FILENAME").equals("")) //if no filename is given then just give the last one
        {

            // source for iterating through buckets: https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3-objects.html?fbclid=IwAR0unILy1J8y3ZOv0Xzx-SLtEvpIygUtv7v1nltQLXHqEc08QvKM8ED6trw#list-object
            ListObjectsRequest listObjects = ListObjectsRequest
                    .builder()
                    .bucket(bucketName)
                    .prefix("crawl-data/CC-NEWS/2021/01")
                    .build();

            ListObjectsResponse res = s3.listObjects(listObjects);
            List<S3Object> objects = res.contents();

            //get last object of bucket:
            //System.out.println(objects.get(objects.size() - 1).key());
            return objects.get(objects.size() - 1).key();
        }
        else{
            return System.getenv("COMMON_CRAWL_FILENAME"); //else if filename is given, then use that
        }

    }

    public static void main(String[] args) throws IOException {

        S3Client s3 = S3Client.builder() //create s3 object
                .region(Region.US_EAST_1)
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofMinutes(30)).build())
                .build();

        String key1 = getObject("commoncrawl", s3); //get filename if its given, else get the latest warc file

        //source for creating getObjectRequest object: https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3-objects.html
        GetObjectRequest getObjectRequest = GetObjectRequest.builder() //create getObjectRequest object
                .bucket("commoncrawl")
                .key(key1)
                .build();


        File f = new File("commoncrawl2.warc"); //create commoncrawl.warc
        //System.out.println(f);
        s3.getObject(getObjectRequest, ResponseTransformer.toFile(f));

        s3.close();
        //System.out.println("done");

        //STEP 2:
        //create archiveReader object: 
        ArchiveReader reader;

        //source for creating reader: //source: https://github.com/Smerity/cc-warc-examples/blob/master/src/org/commoncrawl/examples/WARCReaderTest.java
        FileInputStream is = new FileInputStream(f);
        reader = WARCReaderFactory.get(key1, is, true);

        ElasticSearch es = new ElasticSearch("es");
        es.NewElasticSearchIndex(); //??


        //source for iterating through each record r in reader: https://github.com/Smerity/cc-warc-examples/blob/master/src/org/commoncrawl/examples/WARCReaderTest.java
        for (org.archive.io.ArchiveRecord r : reader) {
            //if(r.getHeader().getHeaderValue("WARC-Type").equals("response"))
            if(r.getHeader().getHeaderValue("Content-Type").equals("application/http; msgtype=response")) //from PM office hours
            {

                //source for reading bytes: https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayInputStream.html and PM office hours
                byte[] rawData = new byte[r.available()];
                int offset = 0;
                while(offset != -1){
                    offset += r.read(rawData, offset, r.available());
                } //will give byte array

                    String content = new String(rawData, StandardCharsets.UTF_8.name()); //from campuswire
                    String page_url = r.getHeader().getUrl();

                    //source for splitting body and head: https://github.com/Smerity/cc-warc-examples/blob/master/src/org/commoncrawl/examples/mapreduce/TagCounterMap.java
                    String body = content.substring(content.indexOf("\r\n\r\n") + 4);
                    body = body.replace("\0", ""); //replace null characters with nothing, from PM office hrs

                    //STEP 3:
                    try {
                        //source for parsing with jsoup: https://jsoup.org/cookbook/input/parse-document-from-string
                        Document doc = Jsoup.parse(body); //body is the html body we care about
                        String body_text = doc.body().text(); //extract plain text
                        //System.out.println(body_text + "\n"); //print out for test

                        String title = doc.title(); //extract title source: https://jsoup.org
                        //System.out.println(title);

                        //source for JSON objects: https://processing.github.io/processing-javadocs/core/processing/data/JSONObject.html
                        JSONObject object1 = new JSONObject();
                        object1.put("title", title);
                        object1.put("txt", body_text);
                        object1.put("url", page_url);

                        while (true) { //source: from campuswire-- keep posting document until it's successful
                            try {
                                es.PostNewDocument(object1);
                                break;
                            }
                            catch (Exception ignored) {}
                        }
                    }
                    catch (Exception e){ //post empty documents for PDFs (from campuswire)
                        JSONObject object2 = new JSONObject();
                        object2.put("url", page_url);
                        object2.put("title", "");
                        object2.put("txt", "");

                        while (true) {
                            try {
                                es.PostNewDocument(object2);
                                break;
                            }
                            catch (Exception ignored) {}
                        }
                    }
            }
        }
        f.delete();
        es.close();

    }

}