# -*- coding: utf-8 -*-
"""
Created on Tue Oct 27 17:47:48 2015

@author: ruhansa

1. added functionality to retrieve results for all queries in queries.txt
2. added query language detection
3. added basic auth
"""
import json
# if you are using python 3, you should 
# import urllib.request 
import urllib
import urllib2
from langdetect import detect_langs
import argparse
import base64

parser = argparse.ArgumentParser(description="Converts Solr search result from json to trec eval format.")
parser.add_argument("query_file", help="absolute path to the query file", type=str)
parser.add_argument("model_name", help="name of the model", type=str)
parser.add_argument("user_name", help="solr username", type=str)
parser.add_argument("password", help="solr password", type=str)
args = parser.parse_args()

# change the url according to your own koding username and query
inUrlBeg = "http://nandakishorek.koding.io:8983/solr/vsm_core/select?q="
inUrlEnd = "&fl=id%2Cscore%2Ctext_en%2Ctext_de%2Ctext_ru&wt=json&indent=true&defType=dismax&qf=text_en+text_de+text_ru&mm=1&rows=4000"


# init the auth handler
auth_handler = urllib2.HTTPBasicAuthHandler()
auth_handler.add_password(realm='Auth',
                      uri='http://nandakishorek.koding.io:8983/',
                      user=args.user_name,
                      passwd=args.password)
opener = urllib2.build_opener(auth_handler)
urllib2.install_opener(opener)

queryFile = open(args.query_file, "r");
for line in queryFile:
    
    indexOfSpace = line.index(" ")
    qid = line[0:indexOfSpace]
    query = line[indexOfSpace+1:-1]

    langs = detect_langs(query.decode("UTF-8"))

    print langs
 
    outfn = "trec_op_" + str(qid)
    outf = open(outfn, "w")

    inUrl = inUrlBeg + urllib.quote(query) + inUrlEnd
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
