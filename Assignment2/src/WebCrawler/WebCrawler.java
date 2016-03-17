package WebCrawler;
import sun.java2d.pipe.AAShapePipe;

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
    public static List<String> plainContent;

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


    static int calcScore(List<String> plainContent, String newUrlString, List<String> query, String mAnchor) {
        int score;
        int anchorNumber = 0;
        int urlNumber = 0;
        int uNumber = 0;
        int vNumber = 0;
        String[] anchorWords = mAnchor.replaceAll("[^a-zA-Z\\s]", "").toLowerCase().split("\\s+");
        int ibegin = plainContent.indexOf(anchorWords[0]);
        int iend = plainContent.indexOf(anchorWords[anchorWords.length - 1]);
        for (String word:query) {
            if (mAnchor.toLowerCase().contains(word.toLowerCase())) {
                anchorNumber ++;
            } else if (newUrlString.toLowerCase().contains(word.toLowerCase())) {
                urlNumber ++;
            } else {
                if(plainContent.subList(ibegin - 5, ibegin).contains(word) || plainContent.subList(iend + 1, iend + 6).contains(word)) {
                    uNumber ++;
                }
                if (plainContent.contains(word)) {
                    vNumber++;
                }
            }
        }

        if (anchorNumber != 0) {
            score  = 50 * anchorNumber;
        } else if (urlNumber != 0) {
            score = 40 * urlNumber;
        } else {
            score = Math.abs(4 * uNumber) + Math.abs(vNumber - uNumber);
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

    static void updateScore (int score, String url) {
        for (URLInfo urlInfo: newUrls) {
            if (urlInfo.sameUrl(url)) {
                urlInfo.setLinkScore(score);
            }
            break;
        }
    }


    static void addNewurl(URL oldURL, String newUrlString, List<String> query, List<String> plainContent, String mAnchor)

    { URL url;
        if (DEBUG) System.out.println("URL String " + newUrlString);
        try { url = new URL(oldURL,newUrlString);
            int linkScore = calcScore(plainContent, newUrlString, query, mAnchor);
            URLInfo urlInfo = new URLInfo(url, linkScore);
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
                    updateScore(linkScore, url.toString());
                    System.out.println("adding score " + linkScore + "to queue" + url.toString());
                }
            }
        } catch (MalformedURLException e) { return; }
    }

    static String getPage(URL url)

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
        plainContent = new ArrayList<>(Arrays.asList(content.toLowerCase().replaceAll("\\<.*?>","").replaceAll("[^a-zA-Z\\s]", "").split("\\s+")));
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

        // skip white space
        while (fCharacter == ' ') {
            frontIndexCopy --;
            fCharacter = lcpage.charAt(frontIndexCopy);
        }

        while (wordCount < 5) {
            if (Character.isLetter(fCharacter)) {
                wordEnd = frontIndexCopy;
                while (Character.isLetter(fCharacter)) {
                    frontIndexCopy --;
                    fCharacter = lcpage.charAt(frontIndexCopy);
                }
                if ((wordEnd - frontIndexCopy) == 1) {
                    frontFive = String.valueOf(lcpage.charAt(wordEnd)) + " " + frontFive;
                } else {
                    frontFive = lcpage.substring(frontIndexCopy + 1, wordEnd + 1) + " " + frontFive;
                }
                wordCount ++;
            } else {
                if (fCharacter != '>') {
                    frontIndexCopy --;
                    fCharacter = lcpage.charAt(frontIndexCopy);
                }
                if (fCharacter == '>') {
                    while (fCharacter != '<') {
                        frontIndexCopy--;
                        fCharacter = lcpage.charAt(frontIndexCopy);
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

        // skip white space
        while (bCharacter == ' ') {
            backIndexCopy ++;
            bCharacter = lcpage.charAt(backIndexCopy);
        }

        while (wordCount < 5) {
            if (Character.isLetter(bCharacter)) {
                wordBegin = backIndexCopy;
                while (Character.isLetter(bCharacter)) {
                    backIndexCopy ++;
                    bCharacter = lcpage.charAt(backIndexCopy);
                }
                if ((wordBegin - backIndexCopy) == 1) {
                    backFive = backFive + " " + String.valueOf(lcpage.charAt(wordBegin));
                } else {
                    backFive = backFive + " " + lcpage.substring(wordBegin, backIndexCopy);
                }
                wordCount ++;
            } else {
                if (bCharacter != '<') {
                    backIndexCopy ++;
                    bCharacter = lcpage.charAt(backIndexCopy);
                }
                if (bCharacter == '<') {
                    while (bCharacter != '>') {
                        backIndexCopy++;
                        bCharacter = lcpage.charAt(backIndexCopy);
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
                        String five = findFiveWords(index - 1, ilinkEnd + 1, lcPage);
                        String mAnchor = page.substring(iEnd + 2, ianchorEnd);
                        String newUrlString = page.substring(iURL,iEnd);
                        seenUrls.put(url ,new Integer(1));
                        addNewurl(url, newUrlString, query, plainContent, mAnchor);
                    } } }
            index = iEndAngle;
        }
    }

    static void run(String[] args){
        parsingArgs(args);
        initialize();
        for (int i = 0; i < fileLimit; i++) {
            URL url = newUrls.poll().getUrl();
            if (DEBUG) System.out.println("Searching " + url.toString());
            if (robotSafe(url)) {
                String page = getPage(url);
                if (DEBUG) System.out.println(page);
                if (page.length() != 0) processPage(url, page, argsMap.get("-q"));
                if (newUrls.isEmpty()) break;
            }
        }
        System.out.println("Search complete.");
    }
    public static void main(String[] args) {
//        WebCrawler webCrawler = new WebCrawler();
//        webCrawler.run(args);
        run(args);
    }
}
