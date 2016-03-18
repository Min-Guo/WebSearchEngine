package WebCrawler;
import java.util.*;
import java.net.*;
import java.io.*;



public class WebCrawler {
    public static String fileLocation;
    public static PriorityQueue<URLInfo> newUrls;
    public static Hashtable<URL, Integer> seenUrls;
    public static boolean DEBUG = false;
    public static final String DISALLOW = "Disallow:";
    public static int maxPages = 0;
    public static final int fileLimit = 20000;
    public static final Map<String, List<String>> argsMap = new HashMap<>();
    public static String plainContent;

    static void parsingArgs (String[] args) {
        int argsNumber = args.length;
        int count = 0;
        String flag = "";
        List<String> fArgs = new ArrayList<>();

        for (String arg:args) {
            if (arg.charAt(0) == '-') {
                if (!flag.equals("") && arg.charAt(1) != 't') {
                    argsMap.put(flag, new ArrayList<>(fArgs));
                    fArgs.clear();
                } else if (!flag.equals("") && arg.charAt(1) == 't'){
                    argsMap.put(flag, new ArrayList<>(fArgs));
                    fArgs.clear();
                    fArgs.add("true");
                    argsMap.put(arg, new ArrayList<>(fArgs));
                }
                flag = arg;
                count ++;
            } else {
                count ++;
                fArgs.add(arg);
                if (count == argsNumber) {
                    argsMap.put(flag, new ArrayList<>(fArgs));
                }
            }
        }
    }

    static void initialize() {
        URL url;
        if (argsMap.containsKey("-t")) {
            DEBUG = Boolean.valueOf(argsMap.get("-t").get(0));
        }
        fileLocation = argsMap.get("-docs").get(0);
        maxPages = Integer.parseInt(argsMap.get("-m").get(0));
        newUrls =
                new PriorityQueue<URLInfo>(fileLimit, new LinkScoreComparator());
        seenUrls = new Hashtable<>();
        try { url = new URL(argsMap.get("-u").get(0)); }
        catch (MalformedURLException e) {
            System.out.println("Invalid starting URL " + argsMap.get("-u").get(0));
            return;
        }
        seenUrls.put(url,new Integer(1));
        int linkScore = 0;
        int order = 0;
        String pageName = "source";
        URLInfo urlInfo = new URLInfo(url, linkScore, order, pageName);
        newUrls.add(urlInfo);
        System.out.println("Starting search: Initial URL " + url.toString());

/*Behind a firewall set your proxy and port here!
*/
        Properties props= new Properties(System.getProperties());
        props.put("http.proxySet", "true");
        props.put("http.proxyHost", "webcache-cup");
        props.put("http.proxyPort", "8080");
        Properties newprops = new Properties(props);
        System.setProperties(newprops);
    }

    static boolean robotSafe(URL url) {
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


    static int calcScore(String uvWords, String newUrlString, List<String> query, String mAnchor) {
        int score;
        int anchorNumber = 0;
        int tagCount = 0;
        int uCount = 0;
        int vCount = 0;
        for (String word:query) {
            if (mAnchor.toLowerCase().contains(word.toLowerCase())) {
                anchorNumber ++;
            } else if (newUrlString.toLowerCase().contains(word.toLowerCase())) {
                tagCount ++;
            } else {
                if(uvWords.contains(word.toLowerCase())) {
                    uCount ++;
                }
                if (plainContent.contains(word.toLowerCase())) {
                    vCount++;
                }
            }
        }

        if (anchorNumber != 0) {
            score  = 50 * anchorNumber;
        } else if (tagCount != 0) {
            score = 40 * tagCount;
        } else {
            score = Math.abs(4 * uCount) + Math.abs(vCount - uCount);
        }
        return score;
    }

    static boolean duplicateNewUrl (String newUrl) {
        for (URLInfo urlInfo : newUrls) {
            if (urlInfo.sameUrl(newUrl)) {
                return true;
            }
        }
        return false;
    }

    static URLInfo updateScore (int score, URLInfo info) {
        for (URLInfo urlInfo: newUrls) {
            if (urlInfo.sameUrl(info.getUrlString())) {
                urlInfo.updateScore(score);
                info.setLinkScore(urlInfo.getLinkScore());
                break;
            }
        }
        return info;
    }


    static void addNewurl(URL oldURL, String newUrlString, List<String> query, String uvWords, String mAnchor, int order)

    { URL url;
        if (DEBUG) System.out.println("URL String " + newUrlString);
        try { url = new URL(oldURL,newUrlString);
            int linkScore = calcScore(uvWords, newUrlString, query, mAnchor);
            URLInfo urlInfo = new URLInfo(url, linkScore, order, mAnchor);
            if (!seenUrls.containsKey(url)) {
                if (!duplicateNewUrl(url.toString())) {
                    String filename =  url.getFile();
                    int iSuffix = filename.lastIndexOf("htm");
                    if ((iSuffix == filename.length() - 3) ||
                        (iSuffix == filename.length() - 4)) {
                        seenUrls.put(oldURL,new Integer(1));
                        newUrls.add(urlInfo);
                        System.out.println("adding to queue: " + url.toString() + "score: " + linkScore);
//                      System.out.println("Found new URL " + url.toString());
                     }
                } else if (duplicateNewUrl(url.toString())){
                    urlInfo = updateScore(linkScore, urlInfo);
                    System.out.println("adding score " + linkScore + " to queue " + url.toString() + " " + urlInfo.getLinkScore());
                }
            }
        } catch (MalformedURLException e) { return; }
    }

    static void copyPage(InputStream from, OutputStream to) throws IOException {
        byte[] buffer = new byte[4096];
        while (true) {
            int numBytes = from.read(buffer);
            if (numBytes == -1) {
                break;
            }
            to.write(buffer, 0, numBytes);
        }
    }

    static void savePage( URL url, String pageName) throws Exception {
        OutputStream out = new FileOutputStream(pageName);
        URLConnection urlConnection = url.openConnection();
        urlConnection.connect();
        InputStream is = urlConnection.getInputStream();
        copyPage(is, out);
        is.close();
        out.close();
    }

    static String getPage(URL url) throws Exception

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
        plainContent = content.toLowerCase().replaceAll("\\<.*?>","").replaceAll("[^a-zA-Z\\s]", "");
        return content;

    } catch (IOException e) {
        System.out.println("ERROR: couldn't open URL ");
        return "";
    }  }

    static String findFrontFive (int frontIndex, String lcpage) {
        String frontFive = "";
        int wordCount = 0;
        int wordEnd;
        int frontIndexCopy = frontIndex;
        Character fCharacter = lcpage.charAt(frontIndexCopy);

        if (frontIndexCopy >= 0) {
            // skip white space
            while (fCharacter == ' ' && frontIndex >= 0) {
                frontIndexCopy--;
                fCharacter = lcpage.charAt(frontIndexCopy);
            }
            if (frontIndexCopy >= 0) {
                while (wordCount < 5 && frontIndexCopy >= 0) {
                    if (Character.isLetter(fCharacter)) {
                        wordEnd = frontIndexCopy;
                        while (Character.isLetter(fCharacter)) {
                            frontIndexCopy--;
                            if (frontIndexCopy < 0) {
                                break;
                            } else {
                                fCharacter = lcpage.charAt(frontIndexCopy);
                            }
                        }
                        if ((wordEnd - frontIndexCopy) == 1 && frontIndexCopy >= 0) {
                            frontFive = String.valueOf(lcpage.charAt(wordEnd)) + " " + frontFive;
                        } else if (frontIndexCopy > 0){
                            frontFive = lcpage.substring(frontIndexCopy + 1, wordEnd + 1) + " " + frontFive;
                        }
                        wordCount++;
                    } else {
                        if (fCharacter != '>') {
                            frontIndexCopy--;
                            if (frontIndexCopy >= 0) {
                                fCharacter = lcpage.charAt(frontIndexCopy);
                            }
                        }
                        if (fCharacter == '>') {
                            while (fCharacter != '<') {
                                frontIndexCopy--;
                                if (frontIndexCopy < 0) {
                                    break;
                                } else {
                                    fCharacter = lcpage.charAt(frontIndexCopy);
                                }
                            }
                        }
                    }
                }
            }

        }
        return frontFive;
    }

    static String findBackFive (int backIndex, String lcpage) {
        String backFive = "";
        int wordCount = 0;
        int wordBegin;
        int backIndexCopy = backIndex;
        Character bCharacter = lcpage.charAt(backIndexCopy);

        if (backIndex < lcpage.length()) {
            // skip white space
            while (bCharacter == ' ' && backIndexCopy < lcpage.length()) {
                backIndexCopy++;
                bCharacter = lcpage.charAt(backIndexCopy);
            }

            if (backIndexCopy < lcpage.length()) {
                while (wordCount < 5 && backIndexCopy < lcpage.length()) {
                    if (Character.isLetter(bCharacter)) {
                        wordBegin = backIndexCopy;
                        while (Character.isLetter(bCharacter)) {
                            backIndexCopy++;
                            if (backIndexCopy >= lcpage.length()) {
                                break;
                            } else {
                                bCharacter = lcpage.charAt(backIndexCopy);
                            }
                        }
                        if ((wordBegin - backIndexCopy) == 1 && backIndexCopy < lcpage.length()) {
                            backFive = backFive + " " + String.valueOf(lcpage.charAt(wordBegin));
                        } else if (backIndexCopy < lcpage.length()){
                            backFive = backFive + " " + lcpage.substring(wordBegin, backIndexCopy);
                        }
                        wordCount++;
                    } else {
                        if (bCharacter != '<') {
                            backIndexCopy++;
                            if (backIndexCopy < lcpage.length()) {
                                bCharacter = lcpage.charAt(backIndexCopy);
                            }
                        }
                        if (bCharacter == '<') {
                            while (bCharacter != '>') {
                                backIndexCopy++;
                                if (backIndexCopy >= lcpage.length()) {
                                    break;
                                } else {
                                    bCharacter = lcpage.charAt(backIndexCopy);
                                }
                            }
                        }
                    }
                }
            }
        }
        return backFive;
    }

    static String findFiveWords (int frontIndex, int backIndex, String lcpage) {
        return findFrontFive(frontIndex, lcpage) + findBackFive(backIndex, lcpage);
    }

    static void processPage(URL url, String page, List<String> query)

    { String lcPage = page.toLowerCase(); // Page in lower case
        int index = 0; // position in page
        int order = 1;
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
                        int ianchorEnd = lcPage.indexOf("<", iEnd);
                        int ilinkEnd = lcPage.indexOf(">", ianchorEnd);
                        String uvWords = findFiveWords(index - 1, ilinkEnd + 1, lcPage);
                        String mAnchor = page.substring(iEnd + 2, ianchorEnd).replaceAll("[^a-zA-Z ]", "");
                        String newUrlString = page.substring(iURL,iEnd);
                        seenUrls.put(url ,new Integer(1));
                            addNewurl(url, newUrlString, query, uvWords, mAnchor, order);
                            order ++;
                    }
                }
            }
            index = iEndAngle;
        }
    }

    static void run(String[] args) throws Exception{
        parsingArgs(args);
        initialize();
        for (int i = 0; i < fileLimit; i++) {
            if (seenUrls.size() < maxPages) {
                URLInfo urlInfo = newUrls.poll();
                URL url = urlInfo.getUrl();
                if (DEBUG) System.out.println("Searching " + url.toString());
                if (robotSafe(url)) {
                    String page = getPage(url);
                    if (DEBUG) System.out.println(page);
                    if (page.length() != 0) processPage(url, page, argsMap.get("-q"));
                    savePage(url, urlInfo.getPageName());
                    if (newUrls.isEmpty()) break;
                }
            } else {
                break;
            }
        }
        System.out.println("Search complete.");
    }
    public static void main(String[] args) throws  Exception{
//        WebCrawler webCrawler = new WebCrawler();
//        webCrawler.run(args);
        run(args);
    }
}
