package com.crawler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class BasicWebCrawler implements Runnable{

    String topicUrl;
    String topic;
    public BasicWebCrawler(String url, String topic) {
        this.topicUrl = url;
        this.topic = topic;
    }
    
    public void run() {
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
		
		
		try {
			extractedPaginatedDoc(this.topicUrl, webClient);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    public void crawlPage(Document document) {
    	

        try {
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            
            Elements  divSelectionBody = document.getElementsByClass("search-results-item");
            FileWriter fileWriter = null;
            BufferedWriter bufferedWriter = null;
            try
            {
            
            File tmpDir = new File("C:/data/cochrane_reviews.txt");
            if(!tmpDir.exists()){
                System.out.println("We had to make a new file.");
                tmpDir.createNewFile();
            }

            fileWriter = new FileWriter(tmpDir, true);

            bufferedWriter = new BufferedWriter(fileWriter);
            
            for (Element page : divSelectionBody) {
                Elements divSelectionItemChildren = page.children();
                for(Element e1:divSelectionItemChildren)
                {
                	if("search-results-item-body".equals(e1.attr("class")))
	            	{
	                	Elements divSelectionItemBodyChildren= e1.children();
	                	String url = null;
	                	String title = null;
	                	String authorName = null;
	                	String date = null;
	                	StringBuilder strBuilder = new StringBuilder();
	                	for(Element child:divSelectionItemBodyChildren)
	                	{
	                		if("result-title".equals(child.attr("class")))
	                		{
	                			Element searchTitleChild = child.child(0);
	                			url = "https://www.cochranelibrary.com/"+searchTitleChild.attr("href");
	                			title = searchTitleChild.text();
	                			//System.out.println(i+"   "+searchTitleChild.absUrl("href"));
	                		}
	                		else if("search-result-authors".equals(child.attr("class")))
	                		{
	                			Element searchTitleChild = child.child(0);
	                			authorName = searchTitleChild.text();
	                			//System.out.println(i+"   "+authorName);
	                		}
	                		else if("search-result-metadata-block".equals(child.attr("class")))
	                		{
	                			Element searchTitleChild = child.child(2).child(0).child(0);
	                			date = searchTitleChild.text();
	                			SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd MMMM yyyy");
	                			Date newDate = dateFormat1.parse(date);
	                			date = dateFormat.format(newDate).toString();
	                			//System.out.println(i+"   "+searchTitleChild.text());
	                		}
	                		
	                	}
	                	strBuilder.append(url).append("|")
	                	.append(topic).append("|")
                		.append(title).append("|")
                		.append(authorName).append("|")
                		.append(date).append("\n");
                		
                		
	                	bufferedWriter.write(strBuilder.toString());
	            	}
                }
                
                
            	
            }
            
            
            }
            catch(IOException e)
            {
            	e.printStackTrace();
            }
            catch(Exception e)
            {
            	e.printStackTrace();
            }
            finally {
            	bufferedWriter.close();
            	fileWriter.close();
			}
            
        } catch (Exception e) {
            System.err.println("For '" + "': " + e.getMessage());
        }
    
    }

    private void extractedPaginatedDoc(String URL, WebClient webClient)
			throws MalformedURLException, IOException {
		Document document = getDocument(URL, webClient);
		
		
		
		Elements  allPages = document.getElementsByClass("pagination-page-list");
		
		for(Element page1: allPages)
		{
			Elements allListElement = page1.children();
			int i = 0;
			for(Element list: allListElement)
			{
				if(i == 0)
				{
					this.crawlPage(document);
				}
				else
				{
					String urlForPage = list.child(0).attr("href");
					Document paginatedDocument = getDocument(urlForPage, webClient);
					this.crawlPage(paginatedDocument);
				}
				i++;
			}
			
		}
		
	}
    private static Document getDocument(String URL, WebClient webClient) throws MalformedURLException, IOException {
		URL url = new URL(URL);
		WebRequest requestSettings = new WebRequest(url, HttpMethod.POST);
		HtmlPage page = null;
		try
		{
			page = webClient.getPage(requestSettings);
		}
		catch(Exception ex)
		{
			//
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
		//System.out.println(document.html());
		return document;
	}
}