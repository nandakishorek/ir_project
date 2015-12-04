package com.ir_prj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import twitter4j.JSONArray;
import twitter4j.JSONException;
import twitter4j.JSONObject;

public class HashTagTrends {
    
    private static Map<String, int[]> counts = new HashMap<String, int[]>();
    private static Date corpusStartDate = parseDate("2015-11-24T00:00:00Z");
    private static Date corpusEndDate = parseDate("2015-12-01T00:00:00Z");
    
    private static final int K = 10; // top k trending hashtags
    
    public static void main(String[] args) {
        init();
        String sdate = "2015-11-24T00:00:00Z";
        String edate = "2015-11-30T00:00:00Z";

        Date start_date = parseDate(sdate);
        Date end_date = parseDate(edate);
        
        System.out.println(getTrendingHashTags(start_date, end_date));
        
    }
    
    private static void init(){
              
        int NUM_OF_DAYS = getDateDiffInDays(corpusStartDate, corpusEndDate);
        Date start_date = (Date) corpusStartDate.clone(); // dirty, but works
        Date end_date = (Date) corpusStartDate.clone();
        end_date.setDate(end_date.getDate() + 1); // set end date to next day
        
        String solrURLString = getSolrURLString();

        try {

            // get all the hashtags
            JSONObject resp = getResponse(solrURLString
                    + "solr/twitter/terms?wt=json&terms.fl=entities_tweet_hashtags&terms.sort=count&terms.limit=-1");
            JSONArray hashTagAndCounts = ((JSONObject) resp.get("terms")).getJSONArray("entities_tweet_hashtags");

            for (int i = 0; i < hashTagAndCounts.length(); i += 2) {
                counts.put(hashTagAndCounts.getString(i), new int[NUM_OF_DAYS]);
                
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        // get hashtag counts for individual day
        for (int day = 0; day < NUM_OF_DAYS; ++day) {
                       
            try {
                
                JSONObject resp = getResponse(solrURLString + "solr/twitter/select?q=*:*&fq=created_at:["
                        + URLEncoder.encode(formatDate(start_date) + " TO " + formatDate(end_date), "UTF-8")
                        + "]&wt=json&facet=true&facet.field=entities_tweet_hashtags&facet.sort=count");
                
                JSONArray hashTagAndCounts = ((JSONObject) resp.get("facet_counts")).getJSONObject("facet_fields")
                        .getJSONArray("entities_tweet_hashtags");

                for (int i = 0; i < hashTagAndCounts.length(); i += 2) {
                    int[] dayCounts = counts.get(hashTagAndCounts.getString(i));
                    dayCounts[day] = hashTagAndCounts.getInt(i + 1);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            
            start_date.setDate(start_date.getDate() + 1); // dirty, but works
            end_date.setDate(end_date.getDate() + 1);
        }

    }
    
    private static List<String> getTrendingHashTags(Date startDate, Date endDate){
        List<String> ret = new ArrayList<String>(counts.size());
        
        int numOfDays = getDateDiffInDays(startDate, endDate);
        int start = getDateDiffInDays(corpusStartDate, startDate); // diff b/w req start date and corpus data start date
        
        List<Score> scores = new ArrayList<Score>(counts.size());
        
        Set<String> keys = counts.keySet();
        for (String key : keys) {
            int daysAppeared = 0;
            double score = 0.0;
            double weight = 0.01;
            int[] dayCounts = counts.get(key);
            for (int i = start; i < start + numOfDays; ++i) {
                
                if (dayCounts[i] > 0) {
                    score += (double)dayCounts[i] * weight;
                    weight *= 3;
                    ++daysAppeared;
                }
            }
            
            // if numOfDays == daysAppeared, then idf does not make sense
            if (daysAppeared > 0 && numOfDays != daysAppeared) {
                score *= Math.log((double)numOfDays/daysAppeared);
            }
            
            if (key.equalsIgnoreCase("isis")) {
                System.out.println(score);
                System.out.println(Arrays.toString(dayCounts));
            }
            
            scores.add(new Score(key, score));
        }
        
        Collections.sort(scores);
        
        for (int i = 0; i < K; ++i) {
            ret.add(scores.get(i).getHashTag());
        }
        
        return ret;
    }

    /**
     * Method to read the search keywords from the file
     * 
     * @return
     */
    private static String getSolrURLString() {
        String solrURLString = null;

        // load the dates from the config file
        Properties props = new Properties();
        try {
            props.load(Application.class.getResourceAsStream("/urls.properties"));
            solrURLString = props.getProperty("twitter_core");
        } catch (IOException e) {
            System.err.println("Could not load the urls");
            e.printStackTrace();
        }

        return solrURLString;
    }

    /**
     * 
     * @param dateInString
     * @return
     */
    private static Date parseDate(String dateInString) {
        String pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        try {
            return formatter.parse(dateInString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JSONObject getResponse(String ipUrl) throws JSONException {
        JSONObject jsonResponse = null;

        StringBuilder response = new StringBuilder();
        URLConnection con;
        try {
            URL url = new URL(ipUrl);
            con = url.openConnection();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String line = "";
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                jsonResponse = new JSONObject(response.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e1) {
            e1.printStackTrace();
        }

        return jsonResponse;
    }
    
    public static String formatDate(Date date){
        String pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }
    
    public static int getDateDiffInDays(Date startDate, Date endDate) {
        // casting to int, since this diff will be a small value
        return (int)TimeUnit.DAYS.convert(endDate.getTime() - startDate.getTime(), TimeUnit.MILLISECONDS);
    }

}
