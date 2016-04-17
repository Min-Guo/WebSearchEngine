package Indexing;

import java.io.File;
import java.io.FilenameFilter;

public class IndexFiles {

    static String[] getDirectories(File file) {
        String[] directories = file.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });
        return directories;
    }

    public  static void main(String[] args) {
        File file = new File(args[0]);
        String indexDirPath = args[1];
        String[] directoryList = getDirectories(file);
        for (int threadNumber = 1; threadNumber <= directoryList.length; threadNumber++) {
            String subIndexPath = indexDirPath + threadNumber;
            String subPageDir = args[0] + directoryList[threadNumber - 1];
            File subIndexDir = new File(subIndexPath);
            if(!subIndexDir.exists()) {
                subIndexDir.mkdir();
            }
            Thread thread = new Thread(new PageIndexer(subPageDir, subIndexPath, threadNumber));
            thread.start();
        }
    }
}