package com.ir_prj;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import twitter4j.Query;
import twitter4j.Query.ResultType;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterObjectFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 * 
 * @author kishore
 *
 */
public class TwitterCrawler {

    public static final int RET_COUNT = 100; // number of tweets per request

    public static Properties paginationProperties;

    private enum Language {
        /*EN("en"),*/ RU("ru")/*, DE("de")*/;

        private String lang;

        private Language(String lang) {
            this.lang = lang;
        }

        @Override
        public String toString() {
            return lang;
        }

    }

    public static void main(String[] args) {

        // load the credentials from the config file
        Properties creds = new Properties();
        try {
            creds.load(Application.class.getResourceAsStream("/credentials.properties"));
        } catch (IOException e) {
            System.err.println("Could not load the credentials");
            e.printStackTrace();
        }

        // set oAuth credentials
        ConfigurationBuilder confBuilder = new ConfigurationBuilder();
        confBuilder.setOAuthConsumerKey(creds.getProperty("consumerKey"))
                .setOAuthConsumerSecret(creds.getProperty("consumerSecret"))
                .setOAuthAccessToken(creds.getProperty("accessToken"))
                .setOAuthAccessTokenSecret(creds.getProperty("accessTokenSecret")).setDebugEnabled(true)
                .setJSONStoreEnabled(true); // enable JSON storage while parsing

        // get twitter instance based on the configuration
        TwitterFactory twitterFactory = new TwitterFactory(confBuilder.build());
        Twitter twitter = twitterFactory.getInstance();

        for (Language lang : Language.values()) {
            // read the max_id's and since_id's from the previous search
            Properties paginationProperties = readPaginationProperties(lang);

            // get the keywords from the file
            List<String> keyWords = getKeyWords(lang);

            for (String keyWord : keyWords) {

                // read the max_id and since_id from the previous
                // search(persisted for each language separately)
                long[] maxIdAndSinceId = new long[2];

                String value = paginationProperties.getProperty(keyWord);
                if (value != null) {
                    String[] vals = value.split(",");
                    for (int i = 0; i < vals.length; ++i) {
                        maxIdAndSinceId[i] = Long.parseLong(vals[i]);
                    }
                }

                // instantiate the query object and configure it
                Query query = new Query(keyWord);
                query.setCount(TwitterCrawler.RET_COUNT);
                query.setResultType(ResultType.recent);
                query.setLang(lang.toString());
                if (maxIdAndSinceId[1] > 0L ) {
                    //query.setMaxId(maxIdAndSinceId[0] - 1);
                    query.setSinceId(maxIdAndSinceId[1]);
                }

                // As of now we will retrieve only RET_COUNT, if necessary we
                // can use pagination to get multiple pages of tweets
                /*
                 * try { do { QueryResult result = twitter.search(query);
                 * System.out.println(result.getCount()); List<Status> tweets =
                 * result.getTweets(); for (Status tweet : tweets) {
                 * //System.out.println(tweet.getId() + " >> " +
                 * tweet.getText()); String tweetJson =
                 * TwitterObjectFactory.getRawJSON(tweet);
                 * System.out.println(tweetJson); } for (int i = 0; i < 80; ++i)
                 * { System.out.print("-"); } if (result.getCount() <
                 * TwitterCrawler.RET_COUNT) { break; } else { // load next page
                 * long max_id = tweets.get(tweets.size() - 1).getId() - 1;
                 * query.setMaxId(max_id); } } while (true); } catch
                 * (TwitterException e) { e.printStackTrace(); }
                 */

                String fileName = "tweets_" + lang + "_" + System.currentTimeMillis();
                File outFile = new File("tweets" + File.separator + fileName);
                outFile.getParentFile().mkdirs();
                try {
                    outFile.createNewFile();
                } catch (IOException e1) {
                    System.err.println("Error creating output file for storing the tweets");
                    e1.printStackTrace();
                }

                try (BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))) {
                    // search tweets
                    QueryResult result = twitter.search(query);
                    List<Status> tweets = result.getTweets();
                    long sinceId = -1;
                    for (Status tweet : tweets) {
                        // System.out.println(tweet.getId() + " >> " +
                        // tweet.getText());
                        String tweetJson = TwitterObjectFactory.getRawJSON(tweet);
                        System.out.println(tweetJson);
                        bw.write(tweetJson + System.lineSeparator());
                        
                        if (tweet.getId() > sinceId) {
                            sinceId = tweet.getId();
                        }
                    }

                    // update the total number of tweets for this language
                    String temp = paginationProperties.getProperty("total");
                    int count = result.getCount();
                    if (temp != null) {
                        count += Integer.parseInt(temp);
                    }
                    paginationProperties.setProperty("total", Integer.toString(count));
                    
                    // store the max_id and since_id of the last search
                    long maxId = sinceId;
                    paginationProperties.setProperty(keyWord,
                            Long.toString(maxId) + "," + Long.toString(sinceId));

                } catch (TwitterException te) {
                    te.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            // persist the pagination properties for this language
            storePaginationProperties(lang, paginationProperties);
        }

    }

    /**
     * Method to read the keywords from keyword files in the resources directory
     * 
     * @param lang
     * @return
     */
    private static List<String> getKeyWords(Language lang) {
        List<String> keyWordsList = new LinkedList<String>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(Application.class.getResourceAsStream("/keywords_" + lang + ".txt")))) {
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

    private static Properties readPaginationProperties(Language lang) {
        Properties paginationProperties = new Properties();
        File inputFile = new File("output" + File.separator + lang + "_maxid_sinceid.properties");
        if (inputFile.exists()) {
            try {
                paginationProperties.load(new FileInputStream(inputFile));
            } catch (IOException ioe) {
                System.err.println("Error while reading pagination properties file");
                ioe.printStackTrace();
            }
        }
        return paginationProperties;
    }

    private static void storePaginationProperties(Language lang, Properties pageProps) {

        File inputFile = new File("output" + File.separator + lang + "_maxid_sinceid.properties");
        inputFile.getParentFile().mkdirs();
        try {
            inputFile.createNewFile();
        } catch (IOException e) {
            System.err.println("Error creating output file to store pagination properties");
            e.printStackTrace();
        }

        try (OutputStream os = new FileOutputStream(inputFile)) {
            pageProps.store(os, "");
        } catch (IOException ioe) {
            System.err.println("Error writing pagingation properties to file");
            ioe.printStackTrace();
        }
    }

}
