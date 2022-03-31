package ru.itis.service;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeConnectService {
    private YouTube youtube;
    private static final Pattern urlPattern = Pattern.compile(
            "(http|ftp|https):\\/\\/([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:\\/~+#-]*[\\w@?^=%&\\/~+#-])",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    private static List<String> apis = new ArrayList<>();
    private static List<String> exclusions = new ArrayList<>();

    public YoutubeConnectService() {
        this.youtube = new YouTube.Builder(YoutubeAuthService.HTTP_TRANSPORT, YoutubeAuthService.JSON_FACTORY, new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {
            }
        }).setApplicationName("APP_ID").build();
        loadAPIs();
        loadExclusions();
    }

    private void loadAPIs(){
        try {
            BufferedReader br = new BufferedReader(new FileReader("src/main/resources/googleApis.txt"));
            String s;
            while((s = br.readLine())!=null){
                apis.add(s);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadExclusions(){
        try {
            BufferedReader br = new BufferedReader(new FileReader("src/main/resources/exclusions.txt"));
            String s;
            while((s = br.readLine())!=null){
                exclusions.add(s);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> getVideosIds(String channelId, String pageToken) throws IOException {
        ArrayList<String> videoIds = new ArrayList<>();
        String apiKey = apis.get(0);


        YouTube.Search.List request = youtube.search()
                .list("id,snippet")
                .setChannelId(channelId)
                .setKey(apiKey)
                .setMaxResults(50L)
                .setOrder("date")
                .setType("video");

        if(pageToken != null && !pageToken.equals("null")){
            request.setPageToken(pageToken);
        }
        SearchListResponse searchListResponse = request.execute();
        searchListResponse.getItems().forEach(item -> videoIds.add(item.getId().getVideoId()));

        String nextPageToken = searchListResponse.getNextPageToken();
        if(nextPageToken != null){
            videoIds.addAll(getVideosIds(channelId, nextPageToken));
        }
        return videoIds;
    }

    public ArrayList<URL> getUrls(String id){
        String apiKey = apis.get(0);

        YouTube.Videos.List listVideosRequest = null;
        VideoListResponse listResponse = null;
        try {
            listVideosRequest = youtube.videos().list("snippet");
            listVideosRequest.setId(id); // add list of video IDs here
            listVideosRequest.setKey(apiKey);
            listResponse = listVideosRequest.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return extractUrls(listResponse.getItems().get(0).getSnippet().getDescription());
    }

    ArrayList<URL> extractUrls(String description){
        ArrayList<URL> urls = new ArrayList<>();
        Matcher urlMatcher = urlPattern.matcher(description);
        while (urlMatcher.find()){
            try {
                String link = description.substring(urlMatcher.start(0), urlMatcher.end(0));
                URL url = new URL(link);
                if(!isExclusion(url.getHost())) {
                    urls.add(url);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return urls;
    }

    private boolean isExclusion(String host){
        for(String site : exclusions){
            if(host.toLowerCase().contains(site.toLowerCase())){
                return true;
            }
        }
        return false;
    }

}