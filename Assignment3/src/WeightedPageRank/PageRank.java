package WeightedPageRank;


import java.io.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.util.*;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PageRank {
    static List<PageInfo> pageInfos = new ArrayList<>();

    static String getExtension(String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1);
        }
        return extension;
    }

    static double calcBaseScore(double unNomalizedbaseScore, double totalScore) {
        return  (unNomalizedbaseScore/totalScore);
    }

    static Map<String, Double> calcOutLinkScore(Document doc) {
        int totalScore = 0;
        Map<String, Double> totalLinks = new HashMap<>();
        Map<String, Integer> tagScopeLinks = new HashMap<>();
        String[] tagList = {"H1", "H2", "H3", "H4", "em", "b"};
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String linkString = link.attr("href");
            if (totalLinks.containsKey(linkString)) {
                totalLinks.put(linkString, totalLinks.get(linkString) + 1);
            } else {
                totalLinks.put(linkString, 1.0);
            }
        }

        for (String tag:tagList) {
                Elements section = doc.select(tag);
                section = section.select("a").unwrap();
                String sectionHtml = section.attr("href");
                if (!sectionHtml.equals("")) {
                    if (tagScopeLinks.containsKey(sectionHtml)) {
                        tagScopeLinks.put(sectionHtml, tagScopeLinks.get(sectionHtml) + 1);
                    } else {
                        tagScopeLinks.put(sectionHtml, 1);
                    }
                }
        }

        for (Map.Entry<String, Double> entry : totalLinks.entrySet()) {
            double score;
            if (tagScopeLinks.containsKey(entry.getKey())) {
                score = tagScopeLinks.get(entry.getKey()) * 2 + (entry.getValue() - tagScopeLinks.get(entry.getKey()));
                totalLinks.put(entry.getKey(), score);
            }
        }

        for (Map.Entry<String, Double> entry : totalLinks.entrySet()) {
            totalScore += entry.getValue();
        }

        for (Map.Entry<String, Double> entry : totalLinks.entrySet()) {
            double nomalizedScore;
            nomalizedScore = entry.getValue()/totalScore;
            totalLinks.put(entry.getKey(), nomalizedScore);
        }
        return totalLinks;
    }

    static void readPages(String folderPath) throws IOException{
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();
        double totalBaseScore = 0;

        for (File file: listOfFiles) {
            double unNomalizedbaseScore;
            Map<String, Double> outLinksScore;
            if (file.isFile() && (getExtension(file.getName()).equals("html"))) {
                Document doc = Jsoup.parse(file, null);
                outLinksScore = calcOutLinkScore(doc);
                String text = doc.body().text();
                String[] textSplit = text.split("\\s+");
                unNomalizedbaseScore = Math.log(textSplit.length) / Math.log(2);
                totalBaseScore += unNomalizedbaseScore;
                PageInfo pageInfo = new PageInfo(file.getName(), unNomalizedbaseScore, outLinksScore);
                pageInfos.add(pageInfo);
            }
        }

        for (PageInfo pageInfo:pageInfos) {
            pageInfo.setNomalizedBaseScore(calcBaseScore(pageInfo.getBaseScore(), totalBaseScore));
        }
    }

    public static void main(String[] args) throws IOException{
        readPages(args[0]);
    }
}
