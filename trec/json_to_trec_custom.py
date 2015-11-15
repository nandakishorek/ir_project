# -*- coding: utf-8 -*-
"""
Created on Tue Oct 27 17:47:48 2015

@author: ruhansa

1. added functionality to retrieve results for all queries in queries.txt
2. added query language detection
3. added basic auth
"""
import time
import json
# if you are using python 3, you should 
# import urllib.request 
import urllib
import urllib2
from langdetect import detect
import argparse
import base64

parser = argparse.ArgumentParser(description="Converts Solr search result from json to trec eval format.")
parser.add_argument("query_file", help="absolute path to the query file", type=str)
parser.add_argument("model_name", help="name of the model, which is also the name of the core", type=str)
#parser.add_argument("user_name", help="solr username", type=str)
#parser.add_argument("password", help="solr password", type=str)
args = parser.parse_args()

# change the url according to your own koding username and query
inUrlBeg = "http://localhost:8983/solr/"+ args.model_name + "/select?q="
inUrlEnd = "&fl=id%2Cscore%2Ctext_en%2Ctext_de%2Ctext_ru&wt=json&indent=true&defType=dismax&mm=1&rows=4000"

"""
commented out the auth part

# init the auth handler
auth_handler = urllib2.HTTPBasicAuthHandler()
auth_handler.add_password(realm="Auth",
                      uri="http://nandakishorek.koding.io:8983/",
                      user=args.user_name,
                      passwd=args.password)
opener = urllib2.build_opener(auth_handler)
urllib2.install_opener(opener)
"""

supported_langs = ["en", "de", "ru"]

queryFile = open(args.query_file, "r");
outfn = "trec_op_" + str(int(time.time()))
outf = open(outfn, "w")

for line in queryFile:
    
    indexOfSpace = line.index(" ")
    qid = line[0:indexOfSpace]
    lang = line.find(" ", line.find(" ") + 1)
    query = line[lang+1:]
    print query
    lang = line[indexOfSpace+1:lang]
    lang = lang[lang.find(":") + 1:]
    print lang

    # try to detect the query language
    #langs = detect(query.decode("UTF-8"))
    
    # if the lang is not in {en, de, ru}, then assume it is en
    # otherwise boost the field for the language
    #boost = [1] * len(supported_langs)
    #for i in range(0, len(boost)):
    #    if lang == supported_langs[i]:
    #        boost[i] = 2
    #print boost
    #qf="&qf=text_en^" + str(boost[0]) + "+text_de^" + str(boost[1]) + "+text_ru^" + str(boost[2]) + "&pf=text_en^" + str(boost[0]) + "+text_ru" + str(boost[1]) + "+text_de^" + str(boost[2]) + "&ps=3"
    qf="&qf=text_en+text_de+text_ru&pf=text_en+text_ru+text_de&ps=3"

    inUrl = inUrlBeg + urllib.quote(query) + inUrlEnd + qf
    print inUrl
  
    data = urllib2.urlopen(inUrl, timeout=30)

    print "ran query " + qid
    # if you are using python 3, you should use
    # data = urllib.request.urlopen(inurl)

    docs = json.load(data)["response"]["docs"]
    # the ranking should start from 1 and increase
    rank = 1
    for doc in docs:
        outf.write(qid + " " + "Q0" + " " + str(doc["id"]) + " " + str(rank) + " " + str(doc["score"]) + " " + args.model_name + "\n")
        rank += 1
outf.close()
