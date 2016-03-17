package WebCrawler;


import java.util.Comparator;

public class LinkScoreComparator implements Comparator<URLInfo>{
    @Override
    public int compare(URLInfo url1, URLInfo url2) {
        int score1 = url1.getLinkScore();
        int score2 = url2.getLinkScore();
        int order1 = url1.getOrder();
        int order2 = url2.getOrder();
        if (score2 != score1) {
            return score2 - score1;
        } else {
            return order1 - order2;
        }
    }
}
