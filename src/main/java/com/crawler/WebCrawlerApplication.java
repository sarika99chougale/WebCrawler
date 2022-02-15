package com.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

@SpringBootApplication
public class WebCrawlerApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebCrawlerApplication.class, args);
		String URL = "https://www.cochranelibrary.com/cdsr/reviews/topics";
		try {

			WebClient webClient = new WebClient();

			// Add other cookies/ Session ...

			webClient.getOptions().setJavaScriptEnabled(true);
			webClient.getOptions().setCssEnabled(false);
			webClient.getOptions().setUseInsecureSSL(true);
			webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
			webClient.getCookieManager().setCookiesEnabled(true);
			webClient.setAjaxController(new NicelyResynchronizingAjaxController());
			// Wait time
			webClient.waitForBackgroundJavaScript(15000);
			webClient.getOptions().setThrowExceptionOnScriptError(false);

			Document document = getDocument(URL, webClient);
			Map<String, String> topicList = new HashMap<String, String>();

			Elements categoryContents = document.getElementsByClass("browse-by-category-content");

			int i =0;
			for (Element categoryContent : categoryContents) {
				Elements termList = categoryContent.children();
				for (Element listItem : termList) {
					
					Element topic = listItem.child(0).child(0);
					String url = topic.attr("href");
					String title = listItem.text();
					System.out.println(title + " : " + url);
					topicList.put(title, url);
					/*if(i< 2)
					{
						topicList.put(title, url);
					}
					else
					{
						continue;
					}
					i++;*/
				}
			}
			
			CompletableFuture<?>[] completableFutures = new CompletableFuture<?>[topicList.size()];
			int index = 0;
			topicList.forEach((key, value) -> { 
				BasicWebCrawler basicWebCrawler = new BasicWebCrawler(value, key); 
				completableFutures[index] = CompletableFuture.runAsync(basicWebCrawler);
			});			
		   
		    CompletableFuture<Void> all = CompletableFuture.allOf(completableFutures);

		    try {
		        all.get(); // get the 'combined' result
		    } catch (ExecutionException e) {
		        // task failed with an exception : e.getCause() to see which
		    	System.out.println(e.getCause());
		    } catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		    System.exit(0);
		    
		    
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static Document getDocument(String URL, WebClient webClient) throws MalformedURLException, IOException {
		URL url = new URL(URL);
		WebRequest requestSettings = new WebRequest(url, HttpMethod.POST);
		HtmlPage page = null;
		try {
			page = webClient.getPage(requestSettings);
		} catch (Exception ex) {
			// do nothing
		}

		// Wait
		synchronized (page) {
			try {
				page.wait(15000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// convert page to generated HTML and convert to document
		Document document = Jsoup.parse(page.asXml());
		// System.out.println(document.html());
		return document;
	}

}
