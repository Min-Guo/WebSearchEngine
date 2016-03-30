package WeightedPageRank;

import java.util.Map;
import java.util.HashMap;

public class PageInfo {
    int pageNumber;
    String pageName;
    double baseScore;
    Map<String, Double> outLinkScore = new HashMap<>();


    public PageInfo(int pageNumber, String name, double baseScore, Map<String, Double> outLinkScore) {
        this.pageNumber = pageNumber;
        this.pageName = name;
        this.baseScore = baseScore;
        this.outLinkScore = outLinkScore;
    }

    public int getPageNumber() {return pageNumber;}

    public double getBaseScore() {
        return baseScore;
    }

    public void setNomalizedBaseScore(double baseScore) {
        this.baseScore = baseScore;
    }

    public Map<String, Double> getOutLinkScore() {
        return outLinkScore;
    }


}
