package WebCrawler;
import java.util.Hashtable;
import java.util.PriorityQueue;
import java.net.*;
import java.util.Properties;
import java.util.StringTokenizer;
import java.io.*;



public class WebCrawler {
    public static String fileLocation;
    public static PriorityQueue<URLInfo> newUrls;
    public static Hashtable<URL, Integer> seenUrls;
    public static final boolean DEBUG = false;
    public static final String DISALLOW = "Disallow:";
    public static int maxPages;


    static void initialize(String[] args) {
        URL url;
        fileLocation = args[2];
        maxPages = Integer.parseInt(args[3]);
        newUrls =
                new PriorityQueue<URLInfo>(maxPages, new LinkScoreComparator());
        seenUrls = new Hashtable<>();
        try { url = new URL(args[0]); }
        catch (MalformedURLException e) {
            System.out.println("Invalid starting URL " + args[0]);
            return;
        }
        seenUrls.put(url,new Integer(1));
        int linkScore = 0;
        URLInfo urlInfo = new URLInfo(url, linkScore);
        newUrls.add(urlInfo);
        System.out.println("Starting search: Initial URL " + url.toString());
//        if (args.length > 4) {
//            int iPages = Integer.parseInt(args[3]);
//            if (iPages < maxPages) maxPages = iPages; }
//        System.out.println("Maximum number of pages:" + maxPages);

/*Behind a firewall set your proxy and port here!
*/
        Properties props= new Properties(System.getProperties());
        props.put("http.proxySet", "true");
        props.put("http.proxyHost", "webcache-cup");
        props.put("http.proxyPort", "8080");
        Properties newprops = new Properties(props);
        System.setProperties(newprops);
    }

    public boolean robotSafe(URL url) {
        String strHost = url.getHost();
        if (strHost.length() == 0) {
            return false;
        }
        // form URL of the robots.txt file
        String strRobot = "http://" + strHost + "/robots.txt";
        URL urlRobot;
        try { urlRobot = new URL(strRobot);
        } catch (MalformedURLException e) {
            // something weird is happening, so don't trust it
            return false;
        }

        if (DEBUG) System.out.println("Checking robot protocol " +
                urlRobot.toString());
        String strCommands;
        try {
            InputStream urlRobotStream = urlRobot.openStream();

            // read in entire file
            byte b[] = new byte[1000];
            int numRead = urlRobotStream.read(b);
            strCommands = new String(b, 0, numRead);
            while (numRead != -1) {
                numRead = urlRobotStream.read(b);
                if (numRead != -1) {
                    String newCommands = new String(b, 0, numRead);
                    strCommands += newCommands;
                }
            }
            urlRobotStream.close();
        } catch (IOException e) {
            // if there is no robots.txt file, it is OK to search
            return true;
        }
        if (DEBUG) System.out.println(strCommands);

        // assume that this robots.txt refers to us and
        // search for "Disallow:" commands.
        String strURL = url.getFile();
        int index = 0;
        while ((index = strCommands.indexOf(DISALLOW, index)) != -1) {
            index += DISALLOW.length();
            String strPath = strCommands.substring(index);
            StringTokenizer st = new StringTokenizer(strPath);

            if (!st.hasMoreTokens())
                break;

            String strBadPath = st.nextToken();

            // if the URL starts with a disallowed path, it is not safe
            if (strURL.indexOf(strBadPath) == 0)
                return false;
        }

        return true;
    }

    public String[] queryWords(String query) {
        String[] words = query.split("\\s+");
        return words;
    }

    public int calcScore(String oldPage, URL url, String query) {
        int score = 0;
        String body = "";
        String linkString = "";
        String[] querywords = queryWords(query);
        for(String word: querywords) {
            if (body.contains(word)) {
               score += 50;
            }
            if (linkString.contains(word)) {
                score += 40;
            }

        }
        return score;
    }

    public void addNewurl(URL oldURL, String newUrlString, String query, String oldPage)

    { URL url;
        if (DEBUG) System.out.println("URL String " + newUrlString);
        try { url = new URL(oldURL,newUrlString);
            if (!seenUrls.containsKey(url)) {
                String filename =  url.getFile();
                int iSuffix = filename.lastIndexOf("htm");
                if ((iSuffix == filename.length() - 3) ||
                        (iSuffix == filename.length() - 4)) {
                    seenUrls.put(url,new Integer(1));
                    int linkScore = calcScore(oldPage, url, query);
                    URLInfo urlInfo = new URLInfo(url, linkScore);
                    newUrls.add(urlInfo);
                    System.out.println("Found new URL " + url.toString());
                } } }
        catch (MalformedURLException e) { return; }
    }

    public String getPage(URL url)

    { try {
        // try opening the URL
        URLConnection urlConnection = url.openConnection();
        System.out.println("Downloading " + url.toString());

        urlConnection.setAllowUserInteraction(false);

        InputStream urlStream = url.openStream();
        // search the input stream for links
        // first, read in the entire URL
        byte b[] = new byte[1000];
        int numRead = urlStream.read(b);
        String content = new String(b, 0, numRead);
        while ((numRead != -1)) {
            numRead = urlStream.read(b);
            if (numRead != -1) {
                String newContent = new String(b, 0, numRead);
                content += newContent;
            }
        }
        return content;

    } catch (IOException e) {
        System.out.println("ERROR: couldn't open URL ");
        return "";
    }  }

    public void processPage(URL url, String page, String query)

    { String lcPage = page.toLowerCase(); // Page in lower case
        int index = 0; // position in page
        int iEndAngle, ihref, iURL, iCloseQuote, iHatchMark, iEnd;
        while ((index = lcPage.indexOf("<a",index)) != -1) {
            iEndAngle = lcPage.indexOf(">",index);
            ihref = lcPage.indexOf("href",index);
            if (ihref != -1) {
                iURL = lcPage.indexOf("\"", ihref) + 1;
                if ((iURL != -1) && (iEndAngle != -1) && (iURL < iEndAngle))
                { iCloseQuote = lcPage.indexOf("\"",iURL);
                    iHatchMark = lcPage.indexOf("#", iURL);
                    if ((iCloseQuote != -1) && (iCloseQuote < iEndAngle)) {
                        iEnd = iCloseQuote;
                        if ((iHatchMark != -1) && (iHatchMark < iCloseQuote))
                            iEnd = iHatchMark;
                        String newUrlString = page.substring(iURL,iEnd);
                        addNewurl(url, newUrlString, query, page);
                    } } }
            index = iEndAngle;
        }
    }

    public void run(String[] args)

    { initialize(args);
        for (int i = 0; i < maxPages; i++) {
            URL url = newUrls.poll().getUrl();
            if (DEBUG) System.out.println("Searching " + url.toString());
            if (robotSafe(url)) {
                String page = getPage(url);
                if (DEBUG) System.out.println(page);
                if (page.length() != 0) processPage(url, page, args[1]);
                if (newUrls.isEmpty()) break;
            }
        }
        System.out.println("Search complete.");
    }
    public static void main(String[] args) {
        WebCrawler webCrawler = new WebCrawler();
        webCrawler.run(args);
    }
}
