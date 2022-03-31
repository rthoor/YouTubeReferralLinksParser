package ru.itis.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.itis.models.Company;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;


public class URLService {
    private static final String WHOIS = "https://www.whois.com/whois/";
    private static final String PROTOCOL = "https://";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.119 YaBrowser/22.3.0.2430 Yowser/2.5 Safari/537.36";
    private static final String REFERRER = "https://www.google.com";
    private static List<String> exclusions =
            Arrays.asList("vk.com", "instagram.com", "youtube.com", "t.me", "twitter.com", "facebook.com",
                    "apple.com", "google.com", "tiktok.com", "youtu.be", "twitch.tv", "teleg.run", "bit.ly",
                    "taplink.cc", "beclick.cc", "clck.ru", "tinyurl.com", "is.gd", "cli.co", "onelink.me",
                    "goo.gl", "tglink.ru", "clc.to", "httpslink.com");

    public Company getCompanyByUrl(URL url){
        try {
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.addRequestProperty("user-agent", USER_AGENT);
            connection.connect();
            Thread.sleep(30000);
            String host = connection.getURL().getHost();
            if(!isExclusion(host)){
                return new Company(getTopDomain(host));
            }
            return null;
        } catch (IOException | InterruptedException e) {
            return null;
        }
    }

    private boolean isExclusion(String host){
        for(String site : exclusions){
            if(host.toLowerCase().contains(site.toLowerCase())){
                return true;
            };
        }
        return false;
    }

    private String getTopDomain(String urlString) {
        int lastDot = urlString.lastIndexOf('.');
        String zerolvl = urlString.substring(lastDot, urlString.length());
        urlString = urlString.substring(0, lastDot);
        String firstlvl = urlString.substring(urlString.lastIndexOf('.')+1, urlString.length());

        return firstlvl + zerolvl;
    }

    public void setSiteOwner(Company company){
        try {
            Document doc = Jsoup.connect(WHOIS + company.getSite())
                    .userAgent(USER_AGENT)
                    .referrer(REFERRER)
                    .timeout(30000)
                    .get();
            Elements rows = doc.getElementsByClass("df-row");
            HashMap<String, String> map = new HashMap<>();
            for(Element row : rows){
                String label = Objects.requireNonNull(row.getElementsByClass("df-label").first()).text();
                String value = Objects.requireNonNull(row.getElementsByClass("df-value").first()).text();
                map.put(label, value);
            }

            String org =  map.get("Organization:");
            if(org != null) {
                if (!org.toLowerCase().contains("whois") && !org.toLowerCase().contains("proxy")) {
                    company.setCompanyName(map.get("Organization:"));
                }
            }
        } catch (IOException e) {
        }
    }

    public void setSiteTitle(Company company){
        try {
            Document doc = Jsoup.connect(PROTOCOL + company.getSite())
                    .userAgent(USER_AGENT)
                    .referrer(REFERRER)
                    .timeout(60000)
                    .followRedirects(true)
                    .get();
            company.setTitle(doc.title());
        } catch (IOException e) {
        }
    }
}
