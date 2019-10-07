package com.DeepIR.utils.jsoup;

import com.DeepIR.utils.Lucene.CoreNLPAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.concurrent.*;

public class DocumentCrawler {

    private String url;
    private String content;
    private String title;
    private String website;
    private String time;

    private static String dirPath;
    private static String indexPath;
    BufferedReader br = null;
    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void getfile() throws IOException {

        File   fileList = new File(dirPath);
        File[] files    = fileList.listFiles();

        for (File file : files) {
            String filepath = file.getPath();
            website = file.getName().split("\\.")[0];
            indexPath = "docs/res_index/" + website + "_index";
            processFile(file);
        }

    }

    public void processFile(File file) throws IOException {

        String filepath = file.getPath();
        website = file.getName().split("\\.")[0];

        BufferedReader br      = new BufferedReader(new FileReader(filepath));
        String         strLine = br.readLine();

        while (strLine != null) {

            try {

                if (strLine.split(" ")[0].matches("^[0-9]*$")) {
                    continue;
                }else{
                    br.skip(strLine.length());
                }


                url = strLine.split(" ")[1];
                time = strLine.split(" ")[2];
                //author = strLine.split("\t")[3];
                title = strLine.split(" ")[4];
                content = strLine.split(" ")[5];
                strLine = br.readLine();
                //if (!url.endsWith(" \"^\\\\d.*?$\"") && !url.endsWith("1") && !url.endsWith("2"))
                if (!url.endsWith("0") && !url.endsWith("1") && !url.endsWith("2")&& !url.endsWith("3")&& !url.endsWith("4")&& !url.endsWith("5")&& !url.endsWith("6")&& !url.endsWith("7")&& !url.endsWith("8")&& !url.endsWith("9")){
                    strLine = br.readLine();
                    continue;
                }


                if (content.equals(" ")) {
                    strLine = br.readLine();
                    continue;
                }

                ExecutorService executor = Executors.newSingleThreadExecutor();
                FutureTask<String> futureTask = new FutureTask<>(new Callable<String>() {

                    @Override
                    public String call() throws Exception {
                        return writeIndex();
                    }
                });

                System.out.println("开始写入索引：" + title);
                long startTime = System.currentTimeMillis();
                executor.execute(futureTask);
                try {
                    futureTask.get(30000, TimeUnit.MILLISECONDS);
                    long endTime = System.currentTimeMillis();
                    System.out.println("耗时：" + (endTime - startTime) * 1.0 / 1000 + "s" + "\n");
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    //e.printStackTrace();
                    System.err.println("超时错误\n");

//                    BufferedWriter bw = new BufferedWriter(new FileWriter("docs\\log" + File.separator + website + ".log", true));
//                    bw.write("URL:\t" + url + "\nTITLE:\t" + title + "\n" + "ERROR:\t" + e.getMessage() + "\n");
//                    bw.flush();
//                    bw.close();
                    futureTask.cancel(true);
                }
            } catch (Exception e) {
                strLine = br.readLine();
                BufferedWriter bw = new BufferedWriter(new FileWriter("docs\\log" + File.separator + website + ".log", true));
                bw.write("URL:\t" + url + "\nTITLE:\t" + title + "\n" + e.getMessage() + "\n");
                bw.flush();
                bw.close();
                continue;
            }

            strLine = br.readLine();
        }
    }
//获取content
    public String law_spider(String url) throws IOException {

        Connection con      = Jsoup.connect(url);

        Document   document = null;
        try {
            document = con.userAgent("Mozilla/4.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)").timeout(20000).get();
        } catch (SocketTimeoutException e) {
            System.err.println("访问超时");
            BufferedWriter bw = new BufferedWriter(new FileWriter("docs\\log" + File.separator + website + ".log", true));
            bw.write("URL:\t" + url + "\nTITLE:\t" + title + "\n" + e.getMessage() + "\n");
            bw.flush();
            bw.close();
            return "-1";
        }

        Elements selects = document.getElementsByClass("wp_articlecontent");

        String content = "";
        for (Element p : selects) {
            content += p.text();
        }

        if (content.replaceAll("\\u00A0+", "").trim().equals("")) {
            return "";
        }

        return content;

    }

    public String writeIndex() throws IOException {

        File indexdir = new File(indexPath);

        if (!indexdir.exists()) {
            indexdir.mkdir();
        }
        //创建索引
        //1.创建directory，指定索引库的存放位置Directory对象
        Directory directory = FSDirectory.open(indexdir);
        //索引库还可以存放到内存中
        //Directory directory = new RAMDirectory();

        //指定一个标准分析器，对文档内容进行分析
        Analyzer analyzer = new CoreNLPAnalyzer();

        //创建indexwriterCofig对象
        //第一个参数： Lucene的版本信息，可以选择对应的lucene版本也可以使用LATEST
        //第二根参数：分析器对象
        IndexWriterConfig config = new IndexWriterConfig(Version.LATEST, analyzer);

        //2. 创建IndexWriter，创建一个indexwriter对象
        IndexWriter indexWriter = new IndexWriter(directory, config);


        //3.创建document对象
        org.apache.lucene.document.Document document = new org.apache.lucene.document.Document();
        String line = "";
        String lineElement [];

            br = new BufferedReader(new FileReader("dirPath/1-4000data.txt"));

            /*while ((line = br.readLine()) != null) {
                lineElement = line.split(" ");
            if (lineElement[0].matches("^[0-9]*$")) {
                website = lineElement[1];
                time = lineElement[2];
                // author   = lineElement[3];
                title = lineElement[4];
                content = lineElement[5];
            }
            else {

            }
            }*/
        //4.为document 添加filed

        //url域
        Field urlField = new StringField("url", url, Field.Store.YES);

        //文件内容域
        Field contentField = null;
        //避免content为空的情况
        if (!content.equals("")) {
            contentField = new TextField("content", content, Field.Store.YES);
        }

        //日期域
        StringField dateField = new StringField("time", time, Field.Store.YES);

        //标题域
        TextField titleField = new TextField("title", title, Field.Store.YES);

        //来源
        Field websiteField = new StringField("website", this.website, Field.Store.YES);

        document.add(urlField);
        if (contentField != null) {
            document.add(contentField);
        }
        document.add(dateField);
        document.add(titleField);
        document.add(websiteField);

        //5.通过IndexWriter 添加到Filed
        //使用indexwriter对象将document对象写入索引库，此过程进行索引创建。并将索引和document对象写入索引库。
        indexWriter.addDocument(document);
        //关闭IndexWriter对象。
        indexWriter.close();
        System.out.println("成功写入索引：" + title);
        return "a";
    }

    public static void main(String[] args) throws IOException {
        DocumentCrawler documentCrawler = new DocumentCrawler();
        dirPath = "docs/input";
        documentCrawler.getfile();
//        documentCrawler.processFile("C:\\tmp\\input\\law.nankai.edu.cn.txt");
        System.exit(0);
    }

}

