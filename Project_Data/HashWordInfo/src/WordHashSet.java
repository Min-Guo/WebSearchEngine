import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

/**
 * Created by Min on 4/21/16.
 */
public class WordHashSet {
  private static HashSet<PageInfo> hashSet = new HashSet<>();

  public static HashSet<PageInfo> getHashSet(String filePath) {
    try {
      File file = new File(filePath);
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line;
      while((line = br.readLine()) != null) {
        String pageID = line;
        double pageRank =Double.parseDouble(br.readLine());
        hashSet.add(new PageInfo(pageID, pageRank));
      }
      return hashSet;
    }catch (IOException e) {}
    return null;
  }

  public static void main(String[] args) {
    getHashSet(args[0]);
  }

}