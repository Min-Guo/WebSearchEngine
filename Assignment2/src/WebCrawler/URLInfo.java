package WebCrawler;
import java.net.*;


public class URLInfo {
    URL url;
    int order;
    int linkScore;

//    public void setUrl(String urlString) {
//        try {
//            this.url = new URL(urlString);
//        }
//        catch (MalformedURLException e) {
//            System.out.println("Invalid starting URL " + urlString);
//            return;
//        }
//    }
    public void setLinkScore(int score) {
        this.linkScore = score;
    }

    public URL getUrl() {
        return this.url;
    }

    public String getUrlString () {
        return this.url.toString();
    }


    public void updateScore (int score) {
        this.linkScore = linkScore + score;
    }

    public int getLinkScore() {
        return this.linkScore;
    }

    public URLInfo(URL url, int linkScore, int order) {
        this.url = url;
        this.linkScore = linkScore;
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    public boolean sameUrl (String newUrl) {
        return this.url.toString().equals(newUrl);
    }
}
