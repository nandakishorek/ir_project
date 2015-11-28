package com.ir_prj;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import twitter4j.JSONArray;
import twitter4j.JSONException;
import twitter4j.JSONObject;


public class NYTFormatter {

    public static void main(String[] args) {

        File newsDir = new File("nyt_news");
        for (File newsFile : newsDir.listFiles()) {
            String fileName = "formatted_nyt" + File.separator + "formatted_" + newsFile.getName() + ".json";
            File formattedNewsFile = new File(fileName);
            formattedNewsFile.getParentFile().mkdirs();

            try (BufferedReader br = new BufferedReader(new FileReader(newsFile));
                    BufferedWriter bw = new BufferedWriter(new FileWriter(formattedNewsFile))) {
                
                String line = null;
                while ((line = br.readLine()) != null) {
                    JSONObject jObj = new JSONObject(line);
                    JSONObject response = jObj.getJSONObject("response");
                    JSONArray docs = response.getJSONArray("docs");
                    
                    JSONArray formattedDocs = new JSONArray();
                    // only have fields that are required
                    for (int i = 0; i < docs.length(); ++i) {
                        JSONObject doc = docs.getJSONObject(i);
                        JSONObject formattedDoc = new JSONObject();
                        
                        
                        formattedDoc.put("_id", doc.get("_id"));
                        formattedDoc.put("lead_paragraph", doc.get("lead_paragraph"));
                        formattedDoc.put("headline", ((JSONObject)doc.get("headline")).get("main"));
                        formattedDoc.put("pub_date", doc.get("pub_date"));
                        formattedDoc.put("web_url", doc.get("web_url"));
                        formattedDoc.put("web_url", doc.get("web_url"));
                        formattedDoc.put("web_url", doc.get("web_url"));
                        
                        JSONArray keywords = ((JSONArray)doc.get("keywords"));
                        JSONArray names = new JSONArray();
                        JSONArray orgs = new JSONArray();
                        JSONArray glocs = new JSONArray();
                        for (int j = 0; j < keywords.length(); ++j) {
                            String name = keywords.getJSONObject(j).getString("name");
                            String value = keywords.getJSONObject(j).getString("value");
                            switch(name) {
                            case "persons":
                                names.put(value);
                                break;
                            case "organizations":
                                orgs.put(value);
                                break;
                            case "glocations":
                                glocs.put(value);
                                break;
                            default:
                                // ignore others
                                ;
                            }
                        }
                        formattedDoc.put("names", names);
                        formattedDoc.put("orgs", orgs);
                        formattedDoc.put("glocations", glocs);
                        
                        formattedDocs.put(formattedDoc);
                    }
                    
                    bw.write(formattedDocs.toString());
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

}
