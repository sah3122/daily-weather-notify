package com.api.main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class WeatherMain {
	private static final String OPEN_API_URL = "http://www.weather.go.kr/wid/queryDFSRSS.jsp?zone=4113562000";
	private static final String SEND_MESSAGE_URL = "https://notify-api.line.me/api/notify";
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		List<String> dailyWeather  = new ArrayList<>();
		BufferedReader br = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder;
		Document doc = null;
		
		try {
			//날씨 정보 파싱
			URL url = new URL(OPEN_API_URL);
			HttpURLConnection httpUrlConnection = (HttpURLConnection) url.openConnection();
			
			br = new BufferedReader(new InputStreamReader(httpUrlConnection.getInputStream(), "UTF-8"));
			StringBuilder xmlBuilder = new StringBuilder();
			StringBuilder resultBuilder = new StringBuilder();
			String line = "";
			
			while((line = br.readLine()) != null) {
				xmlBuilder.append(line.trim());
			}
			
			InputSource input = new InputSource(new StringReader(xmlBuilder.toString()));
			builder = factory.newDocumentBuilder();
			doc = builder.parse(input);
			XPathFactory xPathFactory = XPathFactory.newInstance();
			XPath xPath = xPathFactory.newXPath();
			
			XPathExpression expr = null;
			NodeList nodeList = null;
			
			expr = xPath.compile("//category");
			nodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
			dailyWeather.add(nodeList.item(0).getTextContent());
			
			expr = xPath.compile("//pubDate");
			nodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
			dailyWeather.add(nodeList.item(0).getTextContent());
			
			expr = xPath.compile("//data/tmx");
			nodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
			if(!nodeList.item(0).getTextContent().equals("-999.0")) {
				dailyWeather.add("최고 " + nodeList.item(0).getTextContent() + "도");
			}
			
			expr = xPath.compile("//data/tmn");
			nodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
			if(!nodeList.item(0).getTextContent().equals("-999.0")) {
				dailyWeather.add("최저" + nodeList.item(0).getTextContent() + "도");
			}
			expr = xPath.compile("//data");
			nodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
			
			dailyWeather.addAll(getWeather(nodeList));
			for(String text : dailyWeather) {
				resultBuilder.append(text+"\n");
			}
			resultBuilder.deleteCharAt(resultBuilder.length()-1);
			
			CloseableHttpClient httpClient = HttpClients.createDefault();
			
			HttpPost httpPost = new HttpPost(SEND_MESSAGE_URL);
			//Line API KEY 설정
			httpPost.addHeader("authorization", "Bearer LINE API KEY");
			httpPost.addHeader("Content-Type","application/x-www-form-urlencoded");
			httpPost.addHeader("charset","UTF-8");
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("message", resultBuilder.toString()));
			//한글 인코딩
			httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
			CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
	}
	
	
	public static List<String> getWeather(NodeList nodeList) {
		List<String> result = new ArrayList<>();
		StringBuilder resultBuilder = null;
		
		for(int i = 0; i < nodeList.getLength(); i++) {
			NodeList child = nodeList.item(i).getChildNodes();
			resultBuilder = new StringBuilder();
			for(int j = 0; j < child.getLength(); j++) {
				Node node = child.item(j);
				for(Type type : Type.values()) {
					if(node.getNodeName().equals(type.getName())) {
						if(node.getNextSibling().getNodeName().equals("day") && node.getNextSibling().getTextContent().equals("1")) {
							return result;
						}
						resultBuilder.append(node.getTextContent());
						
						switch(type.getName()) {
							case "hour" :
								resultBuilder.append("시 ");
								break; 
							case "temp" :
								resultBuilder.append("도 ");
								break;
							case "wfKor" :
								break;
							default :
								break;
						}
					}
				}
			}
			result.add(resultBuilder.toString());
		}
		return null;
	}
}
