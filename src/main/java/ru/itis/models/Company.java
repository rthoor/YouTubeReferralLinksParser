package ru.itis.models;


import java.util.Objects;

public class Company {
    private String site;
    private String companyName;
    private String title;

    public Company(String site) {
        this.site = site;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    @Override
    public String toString() {
        String text = "Компания:";
        if(site!=null){
            text += "\n" +  "Сайт: " + site;
        }
        if(title!=null){
            text += "\n" +  "Заголовок сайта: " + title;
        }
        if(companyName!=null){
            text += "\n" +  "Название организации по Whois: " + companyName;
        }
        return  text;
    }

    @Override
    public boolean equals(Object o) {
        Company c = (Company) o;
        return this.getSite().equals(c.getSite());
    }

    @Override
    public int hashCode() {
        return Objects.hash(site);
    }
}
