package com.ir_prj;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Creates JSON array of tweets from tweets separated by line breaks
 * 
 * @author kishore
 *
 */
public class TweetFormatter {

    public static void main(String[] args) {

        File tweetsDir = new File("tweets");
        for (File tweetFile : tweetsDir.listFiles()) {
            File formattedTweetFile = new File("formatted" + File.separator + "formatted_" + tweetFile.getName());
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
                    bw.write(tweet);
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

}
