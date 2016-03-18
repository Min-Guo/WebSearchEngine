package WebCrawler;
import java.net.*;


public class URLInfo {
    URL url;
    int order;
    int linkScore;
    String pageName;


    public String getPageName () {
        return this.pageName;
    }

    public URL getUrl() {
        return this.url;
    }

    public String getUrlString () {
        return this.url.toString();
    }

    public int getLinkScore() {
        return this.linkScore;
    }

    public URLInfo(URL url, int linkScore, int order, String pageName) {
        this.url = url;
        this.linkScore = linkScore;
        this.order = order;
        if (!pageName.contains(".html")) {
            this.pageName = pageName + ".html";
        } else {
            this.pageName = pageName;
        }
    }

    public int getOrder() {
        return order;
    }

    public boolean sameUrl (String newUrl) {
        return this.url.toString().equals(newUrl);
    }
}
