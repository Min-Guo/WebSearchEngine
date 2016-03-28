package WeightedPageRank;


public class PageInfo {
    int wordCount;
    String pageName;
    Long baseScore;
    String content;


    public PageInfo(String name, int wordCount, String content) {
        this.pageName = name;
        this.wordCount = wordCount;
        this.content = content;
    }

    public void setBaseScore(Long baseScore) {
        this.baseScore = baseScore;
    }


}
