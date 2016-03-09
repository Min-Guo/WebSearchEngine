package WebCrawler;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.PriorityQueue;


public class WebCrawler {
    public static String fileLocation;
    Comparator<URLInfo> comparator = new StringLengthComparator();
    PriorityQueue<URLInfo> queue =
            new PriorityQueue<String>(10, comparator);

    static void initialize(String[] args) {

    }
    public static void main(String[] args) {

    }
}
