package Indexing;

public class IndexFiles {

    public  static void main(String[] args) {
        Thread thread1 = new Thread(new PageIndexer(args[0], args[2], 1));
        Thread thread2 = new Thread(new PageIndexer(args[1], args[3], 2));
        thread1.start();
        thread2.start();
    }
}