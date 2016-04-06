package WeightedPageRank;

import java.io.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.util.*;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.Collections;
import java.text.DecimalFormat;

public class PageRank {
    static List<PageInfo> pageInfos = new ArrayList<>();
    static int pageCount;
    static double[][] scoreMatrix;
    static Map<String, Integer> pageNumberNameMap = new HashMap<>();
    static Map<String, Double> updatePageScore = new HashMap<>();

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
        int number = 0;

        for (File file: listOfFiles) {
            double unNomalizedbaseScore;
            Map<String, Double> outLinksScore;
            if (file.isFile() && (getExtension(file.getName()).equals("html"))) {
                pageCount++;
                Document doc = Jsoup.parse(file, null);
                outLinksScore = calcOutLinkScore(doc);
                String text = doc.body().text();
                String[] textSplit = text.split("\\s+");
                unNomalizedbaseScore = Math.log(textSplit.length) / Math.log(2);
                totalBaseScore += unNomalizedbaseScore;
                PageInfo pageInfo = new PageInfo(number, file.getName(), unNomalizedbaseScore, outLinksScore);
                pageNumberNameMap.put(file.getName(), number);
                pageInfos.add(pageInfo);
                number++;
            }
        }

        for (PageInfo pageInfo:pageInfos) {
            double nomalizedScore = calcBaseScore(pageInfo.getBaseScore(), totalBaseScore);
            pageInfo.setNomalizedBaseScore(nomalizedScore);
            pageInfo.setScore(nomalizedScore);
        }
    }

    static void setUpScoreMatrix() {
        scoreMatrix = new double[pageCount][pageCount];

        for (PageInfo pageInfo:pageInfos) {
            int fromPage = pageInfo.getPageNumber();
            if (pageInfo.getOutLinkScore().isEmpty()) {
                for (int i = 0; i < pageCount; i++) {
                    scoreMatrix[i][pageInfo.getPageNumber()] = 1.0/pageCount;
                }
            } else {
                Map<String, Double> outLinks = pageInfo.getOutLinkScore();
                for (Map.Entry<String, Double> entry : outLinks.entrySet()) {
                    String name = entry.getKey();
                    int toPage = pageNumberNameMap.get(name);
                    scoreMatrix[toPage][fromPage] = entry.getValue();
                }
            }
        }
    }

    static double calcPageNewScore(PageInfo pageInfo, double fParameter) {
        double qScore = 0.0;
        double pBase = pageInfo.getBaseScore();
            for (PageInfo page: pageInfos) {
                qScore += scoreMatrix[pageInfo.getPageNumber()][pageNumberNameMap.get(page.getPageName())] * page.getScore();
            }
        return (1 - fParameter)*(pBase) + fParameter*qScore;
    }

    static void calcPageRank(String fParameter) {
        boolean changed;
        double epsilon = 0.01/pageCount;
        do {
            changed = false;
            for(PageInfo pageInfo: pageInfos) {
                double newScore;
                double oldScore = pageInfo.score;
                newScore = calcPageNewScore(pageInfo, Double.valueOf(fParameter));
                if (Math.abs(newScore - oldScore) > epsilon) {
                    changed = true;
                    updatePageScore.put(pageInfo.getPageName(), newScore);
                }
            }

            for (PageInfo pageInfo: pageInfos) {
                pageInfo.setScore(updatePageScore.get(pageInfo.getPageName()));
            }

        } while (changed == true);
    }

    static void printResult() {
        Collections.sort(pageInfos, new Comparator<PageInfo>() {
        @Override
            public int compare(PageInfo p1, PageInfo p2) {
                return Double.compare(p2.getScore(), p1.getScore());
            }
        });

        DecimalFormat numberFormat = new DecimalFormat("#.####");
        for (PageInfo pageInfo: pageInfos) {
            System.out.println(pageInfo.getPageName().replace(".html", "") + " " + numberFormat.format(pageInfo.getScore()));
        }
    }

    public static void main(String[] args) throws IOException{
        readPages(args[0]);
        setUpScoreMatrix();
        calcPageRank(args[1]);
        printResult();
    }
}
