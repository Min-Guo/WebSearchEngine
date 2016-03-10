package WebCrawler;


import java.util.Comparator;

public class LinkScoreComparator implements Comparator<URLInfo>{
    @Override
    public int compare(URLInfo url1, URLInfo url2) {
        int score1 = url1.getLinkScore();
        int score2 = url2.getLinkScore();
        return score2 - score1;
    }
}
