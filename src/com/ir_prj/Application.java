package com.ir_prj;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

public class Application {

	public static void main(String[] args) {
		
		// load the credentials from the config file
		Properties creds = new Properties();
		try {
			// creds.load(Application.class.getClassLoader().getResourceAsStream("res/credentials.properties"));
			creds.load(Application.class.getResourceAsStream("/credentials.properties"));
		} catch (IOException e) {
			System.out.println("Could not load the credentials");
			e.printStackTrace();
		}

		final BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);
		BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<Event>(1000);

		Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
		StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();

		List<String> terms = Lists.newArrayList("obama");
		hosebirdEndpoint.trackTerms(terms);

		Authentication hosebirdAuth = new OAuth1(creds.getProperty("consumerKey"), creds.getProperty("consumerSecret"),
				creds.getProperty("accessToken"), creds.getProperty("accessTokenSecret"));

		ClientBuilder clientBuilder = new ClientBuilder().name("Hosebird-Client-01").hosts(hosebirdHosts)
				.authentication(hosebirdAuth).endpoint(hosebirdEndpoint)
				.processor(new StringDelimitedProcessor(msgQueue)).eventMessageQueue(eventQueue);
		final Client hosebirdClient = clientBuilder.build();

		hosebirdClient.connect();

		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				while (!hosebirdClient.isDone()) {
					String msg;
					try {
						msg = msgQueue.take();
						System.out.println(msg);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}

			}

		});
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
