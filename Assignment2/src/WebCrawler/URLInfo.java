package WebCrawler;
import java.net.*;


public class URLInfo {
    URL url;
    int linkScore;

    public void setUrl(String urlString) {
        try {
            this.url = new URL(urlString);
        }
        catch (MalformedURLException e) {
            System.out.println("Invalid starting URL " + urlString);
            return;
        }
    }
}
