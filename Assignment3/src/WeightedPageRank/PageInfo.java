package WeightedPageRank;


public class PageInfo {
    String pageName;
    double baseScore;


    public PageInfo(String name, double baseScore) {
        this.pageName = name;
        this.baseScore = baseScore;
    }

    public double getBaseScore() {
        return baseScore;
    }

    public void setNomalizedBaseScore(double baseScore) {
        this.baseScore = baseScore;
    }


}
