package WeightedPageRank;

import java.util.Map;
import java.util.HashMap;

public class PageInfo {
    String pageName;
    double baseScore;
    Map<String, Double> outLinkScore = new HashMap<>();


    public PageInfo(String name, double baseScore, Map<String, Double> outLinkScore) {
        this.pageName = name;
        this.baseScore = baseScore;
        this.outLinkScore = outLinkScore;
    }

    public double getBaseScore() {
        return baseScore;
    }

    public void setNomalizedBaseScore(double baseScore) {
        this.baseScore = baseScore;
    }

    public void setOutLinkScore(String outLink, double score) {
        outLinkScore.put(outLink, score);
    }


}
