package Indexing;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

public class PageIndexer implements Runnable {
    private String dirPath;
    private String indexPath;
    private int threadNumber;
//    private IndexWriter writer;
//    private IndexWriterConfig iwc;

    public PageIndexer(String dirPath, String indexPath, int threadNumber){
        this.dirPath = dirPath;
//        this.writer = writer;
        this.indexPath = indexPath;
//        this.iwc = iwc;
        this.threadNumber = threadNumber;
    }

    private void indexDocs(final IndexWriter writer, Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
                    } catch (IOException ignore) {
                        // don't index files that can't be read.
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
        }
    }

    private static String getExtension (Path file) {
        String extension = "";
        String fileName = file.toString();
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1);
        }
        return extension;
    }

    /** Indexes a single document */
    private void indexDoc(final IndexWriter writer, Path file, long lastModified) throws IOException {
        String fileExtension = getExtension(file);
        if (fileExtension.equals("html") || fileExtension.equals("htm")) {
            try (InputStream fileStream = Files.newInputStream(file)) {
                JTidyParser jtidyParser = new JTidyParser();
                String[] parsedInfo = jtidyParser.parser(file.toString());
                Document doc = new Document();
                if (parsedInfo[0] == "") {
                    if (parsedInfo[2] != "") {
                        parsedInfo[0] = parsedInfo[2];
                    } else if (parsedInfo[3] != "") {
                        parsedInfo[0] = parsedInfo[3];
                    } else if (parsedInfo[4] != "") {
                        parsedInfo[0] = parsedInfo[4];
                    }
                }
                doc.add(new StringField("title", parsedInfo[0], Field.Store.YES));
                Field pathField = new StringField("path", file.toString(), Field.Store.YES);
                doc.add(pathField);
                doc.add(new LongField("modified", lastModified, Field.Store.NO));
                InputStream stream = new ByteArrayInputStream(parsedInfo[1].getBytes(StandardCharsets.UTF_8));
                doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));

                if (writer.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
                    System.out.println("adding " + file);
                    writer.addDocument(doc);
                } else {
                    System.out.println("updating " + file);
                    writer.updateDocument(new Term("path", file.toString()), doc);
                }
            }
        }
    }


    /** Index all text files under a directory. */
    private void indexStart(String indexPath, String dirPath) {
        String usage = "java org.apache.lucene.demo.IndexFiles"
                + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
                + "This indexes the documents in DOCS_PATH, creating a Lucene index"
                + "in INDEX_PATH that can be searched with SearchFiles";
        boolean create = true;

        if (dirPath == null) {
            System.err.println("Usage: " + usage);
            System.exit(1);
        }

        final Path docDir = Paths.get(dirPath);
        if (!Files.isReadable(docDir)) {
            System.out.println("Document directory '" +docDir.toAbsolutePath()+ "' does not exist or is not readable, please check the path");
            System.exit(1);
        }
        Date start = new Date();
        try {
            System.out.println("Indexing to directory '" + indexPath + "'...");

            Directory dir = FSDirectory.open(Paths.get(indexPath));
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

            if (create) {
                iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            } else {
                iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            }
            IndexWriter writer = new IndexWriter(dir, iwc);
            indexDocs(writer, docDir);
            writer.close();

            Date end = new Date();
            System.out.println(end.getTime() - start.getTime() + " total milliseconds");

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }

    @Override
    public void run(){
        indexStart(indexPath, dirPath);
    }
}
