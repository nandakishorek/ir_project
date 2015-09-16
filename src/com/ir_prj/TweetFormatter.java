package com.ir_prj;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import twitter4j.HashtagEntity;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;
import twitter4j.URLEntity;

/**
 * Creates JSON array of tweets from tweets separated by line breaks
 * 
 * 
 * TODO: Not using String buffer. If this turns out to be slow, then use it.
 * 
 * @author kishore
 *
 */
public class TweetFormatter {

    public static void main(String[] args) {

        File tweetsDir = new File("tweets");
        for (File tweetFile : tweetsDir.listFiles()) {
            String fileName = "formatted" + File.separator + "formatted_" + tweetFile.getName();
            File formattedTweetFile = new File(fileName);
            formattedTweetFile.getParentFile().mkdirs();

            // create JSON array of the tweets in this file
            try (BufferedReader br = new BufferedReader(new FileReader(tweetFile));
                    BufferedWriter bw = new BufferedWriter(new FileWriter(formattedTweetFile))) {
                
                bw.write("[");
                String tweet = null;
                boolean isStart = true;
                while ((tweet = br.readLine()) != null) {
                    if (!isStart) {
                        bw.write(",");
                    }
                    try {
                        // tweet start
                        bw.write("{");
                        Status status = TwitterObjectFactory.createStatus(tweet);
                        
                        // id
                        bw.write("\"id\":" + Long.toString(status.getId()));
                        
                        // lang
                        bw.write(",\"lang\":\"" + status.getLang() + "\"");
                        
                        // text
                        // sanitize the text
                        String text = status.getText();
                        text = text.replace('"', '\''); // replace double quotes by single quotes
                        text = text.replace('\n', ' '); // replace '\n' by space
                        if (fileName.contains("en")) {
                            bw.write(",\"tw_text_en\":\"");
                        } else if (fileName.contains("de")) {
                            bw.write(",\"tw_text_de\":\"");
                        } else {
                            bw.write(",\"tw_text_ru\":\"");
                        }
                        
                        bw.write(text + "\"");
                        
                        // timestamp
                        bw.write(",\"created_at\":\"" + formatDate(status.getCreatedAt()) + "\"");
                        
                        // urls
                        bw.write(",\"urls\":[");
                        int i = 0;
                        for (URLEntity ue : status.getURLEntities()) {
                            if (i != 0) {
                                bw.write(",");
                            }
                            bw.write("\"" + ue.getURL() + "\"");
                            ++i;
                        }
                        bw.write("]");
                        
                        // hashtags
                        bw.write(",\"hashtags\":[");
                        i = 0;
                        for (HashtagEntity he : status.getHashtagEntities()) {
                            if (i != 0) {
                                bw.write(",");
                            }
                            bw.write("\"" + he.getText() + "\"");
                            ++i;
                        }
                        bw.write("]");
                        
                        // user name
                        bw.write(",\"user_name\":\"" + status.getUser().getName() + "\"");
                        
                        // user screen_name - name of the handle
                        bw.write(",\"user_screen_name\":\"" + status.getUser().getScreenName() + "\"");
                        
                        // tweet end
                        bw.write("}");
                    } catch (TwitterException e) {
                        e.printStackTrace();
                    }                    
                    /*bw.write(tweet);*/
                    isStart = false;
                }
                
                bw.write("]");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    
    
    public static String formatDate(Date date){
        String pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }

}
