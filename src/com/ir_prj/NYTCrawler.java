package com.ir_prj;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import twitter4j.JSONException;
import twitter4j.JSONObject;

public class NYTCrawler {

    public static void main(String[] args) {

        // load the credentials from the config file
        Properties creds = new Properties();
        try {
            creds.load(Application.class.getResourceAsStream("/credentials.properties"));
        } catch (IOException e) {
            System.err.println("Could not load the credentials");
            e.printStackTrace();
        }

        List<String> dates = getDates();
        
        // using a simple url connection
        // no need for any rest client
        String apiKey = creds.getProperty("nytArtSearchKey");
        String artSearchURL = "http://api.nytimes.com/svc/search/v2/articlesearch.json?api-key=" + apiKey;
        
        if (!dates.get(0).equals("0")){
            artSearchURL += "&start_date=" + dates.get(0);
        }
        artSearchURL += "&end_date=" + dates.get(1);
        artSearchURL += "&q=";

        List<String> keyWords = getKeyWords();
        for (String str : keyWords) {
            String[] vals = str.split(":");
            int j = Integer.parseInt(vals[0]);
            String words = vals[1];
            
            int page = 1; // nyt result pagination
            while (page > 0) {
                
                if (page % 10 == 0) {
                    // api req limit is 10 calls per second
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                
                try {
                    
                    String newsOpDir = "nyt_news";
                    String fileName = "news_" + j + "_page_"+ page +"_" + dates.get(1);
                    File outFile = new File(newsOpDir + File.separator + fileName);
                    outFile.getParentFile().mkdirs();
                    
                    
                    StringBuilder inputJson = new StringBuilder();

                    URL url = new URL(artSearchURL + URLEncoder.encode(words, "UTF-8") + "&page=" + page);
                    URLConnection con = url.openConnection();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                            BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))) {
                        String line = "";
                        while ((line = br.readLine()) != null) {
                            System.out.println(line);
                            inputJson.append(line);
                            bw.write(line);
                        }
                        
                        JSONObject jObj = new JSONObject(inputJson.toString());
                        JSONObject response = jObj.getJSONObject("response");
                        JSONObject meta = response.getJSONObject("meta");
                        int hits = (int) meta.get("hits");
                        System.out.println("hits " + hits);
                        
                        if (page * 10 < hits && page < 100) {
                            ++page;
                        } else {
                            page = -1;
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }

    /**
     * Method to read the search keywords from the file
     *  
     * @return
     */
    private static List<String> getKeyWords() {
        List<String> keyWordsList = new LinkedList<String>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(NYTCrawler.class.getResourceAsStream("/news_keys.txt")))) {
            String keywordPhrase;
            while ((keywordPhrase = br.readLine()) != null) {
                keyWordsList.add(keywordPhrase);
            }
        } catch (IOException ioe) {
            System.err.println("Error while reading the keyword file");
            ioe.printStackTrace();
        }
        return keyWordsList;
    }
    
    /**
     * Method to read the search keywords from the file
     *  
     * @return
     */
    private static List<String> getDates() {
        List<String> dates = new ArrayList<String>(2);

        // load the dates from the config file
        Properties props = new Properties();
        try {
            props.load(Application.class.getResourceAsStream("/dates.properties"));
            dates.add(props.getProperty("start_date"));
            dates.add(props.getProperty("end_date"));
        } catch (IOException e) {
            System.err.println("Could not load the dates");
            e.printStackTrace();
        }
        return dates;
    }
}
