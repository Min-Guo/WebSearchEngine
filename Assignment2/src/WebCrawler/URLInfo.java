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

    public URL getUrl() {
        return this.url;
    }

    public void setLinkScore (int score) {
        this.linkScore = score;
    }

    public int getLinkScore() {
        return linkScore;
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
