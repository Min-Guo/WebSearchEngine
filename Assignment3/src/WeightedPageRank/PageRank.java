package WeightedPageRank;


import java.io.BufferedReader;
import java.io.FileReader;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.File;
import java.util.*;
import java.io.IOException;

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

    static void readPages(String folderPath) throws IOException{
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();
        double totalScore = 0;

        for (File file: listOfFiles) {
            double unNomalizedbaseScore;
            if (file.isFile() && (getExtension(file.getName()).equals("html"))) {
                Document doc = Jsoup.parse(file, null);
                String text = doc.body().text();
                String[] textSplit = text.split("\\s+");
                unNomalizedbaseScore = Math.log(textSplit.length) / Math.log(2);
                System.out.println(textSplit.length);
                totalScore += unNomalizedbaseScore;
                PageInfo pageInfo = new PageInfo(file.getName(), unNomalizedbaseScore);
                pageInfos.add(pageInfo);
            }
        }

        for (PageInfo pageInfo:pageInfos) {
            pageInfo.setNomalizedBaseScore(calcBaseScore(pageInfo.getBaseScore(), totalScore));
        }

    }

    public static void main(String[] args) throws IOException{
        readPages(args[0]);
    }
}
