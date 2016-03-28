package WeightedPageRank;


import java.io.BufferedReader;
import java.io.FileReader;
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

    static void readPages(String folderPath) throws IOException {
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();

        for (File file: listOfFiles) {
            if (file.isFile() && (getExtension(file.getName()).equals("html"))) {
                BufferedReader bufferedReader = null;
                try {
                    bufferedReader = new BufferedReader(new FileReader(file));
                    int count = 0;
                    String content = new String();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        String copyLine;
                        copyLine = line.replaceAll("\\<.*?>", "").replaceAll("\\[.*?\\]", "").replaceAll("\\p{Punct}+", "");
                        if (!copyLine.isEmpty()) {
                            content = content + " " + line;
                            String[] lineSpilt = line.replaceAll("\\<.*?>", "").replaceAll("\\[.*?\\]", "").replaceAll("\\p{Punct}+", "").split("\\s+");
                            count += lineSpilt.length;
                        }
                    }
                    System.out.println(content);
                    PageInfo pageInfo = new PageInfo(file.getName(), count, content);
                    pageInfos.add(pageInfo);
                }
                finally {
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws IOException{
        readPages(args[0]);
    }
}
