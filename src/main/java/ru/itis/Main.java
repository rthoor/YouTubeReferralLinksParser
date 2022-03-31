package ru.itis;

import ru.itis.models.Company;
import ru.itis.service.URLService;
import ru.itis.service.YoutubeConnectService;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args){
        YoutubeConnectService ycs = new YoutubeConnectService();
        ArrayList<String> videoIds = new ArrayList<>();

        //здесь вставить айди канала (именно тот, что ютуб присваивает, а не сам пользователь)
        String channelId = "UCpTuLPXfIOcsPbvb2SIwD8w";
        try {
            videoIds = ycs.getVideosIds(channelId, "null");
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayList<URL> urls = new ArrayList<>();
        videoIds.forEach(videoId -> urls.addAll(ycs.getUrls(videoId)));

        HashMap<Company, Integer> sites = Main.getCompanies(urls);
        getCompaniesInfo(sites);

        System.out.println("Всего видео - " + videoIds.size() + "\n");
        sites.entrySet().stream()
                .sorted((k1, k2) -> -k1.getValue().compareTo(k2.getValue()))
                .forEach(k -> {
            System.out.println(k.getKey().toString());
            System.out.println("Количество: " + sites.get(k.getKey()) + "\n");
        });
    }

    static void getCompaniesInfo(HashMap<Company, Integer> companies){
        SiteThread siteThread = new SiteThread(companies);
        Thread thread = new Thread(siteThread);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static HashMap<Company, Integer> getCompanies(ArrayList<URL> urls){
        HostsCallable hostsCallable = new HostsCallable(urls);
        HashMap<Company, Integer> sites = new HashMap<>();
        try {
            Future<HashMap<Company, Integer>> result = Executors.newCachedThreadPool().submit(hostsCallable);
            sites = result.get();
        } catch (InterruptedException | ExecutionException e) {}
        return sites;
    }

    private static class SiteThread implements Runnable {
        private HashMap<Company, Integer> companies;

        public SiteThread(HashMap<Company, Integer> companies) {
            this.companies = companies;
        }

        @Override
        public void run() {
            List<Thread> threads = new ArrayList<>();
            companies.forEach(((company, integer) -> {
                SiteRunnable siteRunnable = new SiteRunnable(company);
                Thread thread = new Thread(siteRunnable);
                threads.add(thread);
                thread.start();
            }));
            for(Thread thread : threads){
                try {
                    thread.join();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private static class SiteRunnable implements Runnable {
        private Company company;

        public SiteRunnable(Company company) {
            this.company = company;
        }

        @Override
        public void run() {
            URLService.setSiteTitle(company);
            URLService.setSiteOwner(company);
        }
    }

    private static class HostsCallable implements Callable<HashMap<Company, Integer>> {
        private ArrayList<URL> urls;
        ConcurrentHashMap<Company, Integer> sites = new ConcurrentHashMap<>();

        public HostsCallable(ArrayList<URL> urls) {
            this.urls = urls;
        }

        @Override
        public HashMap<Company, Integer> call() {
            List<Thread> threads = new ArrayList<>();
            for(URL url : urls){
                HostRunnable hostRunnable = new HostRunnable(url);
                Thread thread = new Thread(hostRunnable);
                threads.add(thread);
                thread.start();
            }for(Thread thread : threads){
                try {
                    thread.join();
                } catch (InterruptedException e) {
                }
            }
            return new HashMap<>(sites);
        }

        private  class HostRunnable implements Runnable {
            private URL url;

            public HostRunnable(URL url) {
                this.url = url;
            }

            @Override
            public void run() {
                Company company = URLService.getCompanyByUrl(url);
                if(company != null) {
                    if (sites.containsKey(company)) {
                        sites.put(company, sites.get(company) + 1);
                    } else {
                        sites.put(company, 1);
                    }
                }
            }
        }
    }
}


