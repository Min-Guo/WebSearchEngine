package IndexSearch;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.io.ByteArrayInputStream;


/**
 * IndexSearch.IndexFiles from https://lucene.apache.org/core/5_4_1/demo/src-html/org/apache/lucene/demo/IndexFiles.html
 */

public class IndexFiles {
    private IndexFiles() {}
    public static void main(String[] args) {
        String usage = "java org.apache.lucene.demo.IndexSearch.IndexFiles" +
                " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n" +
                "This indexes the documents in DOCS_PATH, creating a Lucene index" +
                "in INDEX_PATH that can be searched with SearchFiles";
        String indexPath = "index";
        String docsPath = null;
        boolean create = true;
        for(int i=0;i<args.length;i++) {
            if ("-index".equals(args[i])) {
                indexPath = args[i+1];
                i++;
            } else if ("-docs".equals(args[i])) {
                docsPath = args[i+1];
                i++;
            } else if ("-update".equals(args[i])) {
                create = false;
            }
        }

        if (docsPath == null) {
            System.err.print("Usage: " + usage);
            System.exit(1);
        }

        final Path docDir = Paths.get(docsPath);
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
                iwc.setOpenMode(OpenMode.CREATE);
            } else {
                iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
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

    static void indexDocs(final IndexWriter writer, Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
                    } catch (IOException ignore) {
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
        }
    }


    static String getExtension (Path file) {
        String extension = "";
        String fileName = file.toString();
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1);
        }
        return extension;
    }

    static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
        try  {
            String fileExtension = getExtension(file);
            if (fileExtension.equals("html") || fileExtension.equals("htm")) {
                JTidyParser jtidyParser = new JTidyParser();
                String[] parsedInfo = jtidyParser.parser(file.toString());
                InputStream stream = new ByteArrayInputStream(parsedInfo[1].getBytes(StandardCharsets.UTF_8));

                Document doc = new Document();
                Field pathField = new StringField("path", file.toString(), Field.Store.YES);
                doc.add(pathField);
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
                doc.add(new LongField("modified", lastModified, Field.Store.NO));
                doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));
                if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
                    System.out.println("adding " + file);
                    writer.addDocument(doc);
                } else {
                    System.out.println("updating " + file);
                    writer.updateDocument(new Term("path", file.toString()), doc);
                }
            }
        } catch (IOException ex){
            System.out.println (ex.toString());
            System.out.println("Could not find file " + file);

        }
    }

}


