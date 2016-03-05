package dominus.junit;


import dominus.junit.annotation.HdfsClient;
import dominus.junit.annotation.MySqlDataSource;
import junit.framework.TestCase;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;


/**
 * EE:
 * [Spring] ResourceLoader
 * [HDFS] hdfs client
 */
public class DominusBaseTestCase extends TestCase {


    protected static ResourceLoader resourceLoader = new DefaultResourceLoader();
    protected static PrintStream out = System.out;
    protected static Properties properties;
    protected static FileSystem hdfsClient;
    protected DataSource localMysqlDS;
//    private static Boolean isInitialized = false;

    //color stdout
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    protected static final int KB = 1024;
    protected static final int MB = 1048576;
    protected static final long GB = 1073741824L;

    public DominusBaseTestCase() {
        super();
    }

    public DominusBaseTestCase(String name) {
        super(name);
    }


    private boolean isHdfsClientEnabled() {
        return this.getClass().getAnnotation(HdfsClient.class) != null;
    }

    private boolean isMySqlDataSourceEnabled() {
        return this.getClass().getAnnotation(MySqlDataSource.class) != null;
    }

    @Override
    protected void setUp() throws Exception {

//        synchronized (isInitialized) {
//            if (!isInitialized) {

        properties = PropertiesLoaderUtils.loadProperties(resourceLoader.getResource("classpath:cdh.properties"));
        PropertiesLoaderUtils.fillProperties(properties, resourceLoader.getResource("classpath:jdbc.properties"));
        assertTrue(properties.size() > 0);
        out.println("[Global Properties]:" + properties.size());

        //EE: hdfs client
        if (isHdfsClientEnabled()) {
            Configuration conf = new Configuration();
            conf.addResource("hdfs-clientconfig-cdh/core-site.xml");
            conf.addResource("hdfs-clientconfig-cdh/hdfs-site.xml");
            try {
                hdfsClient = FileSystem.get(conf);
                out.printf("[Global] HDFS File System Capacity:%sG\n", hdfsClient.getStatus().getCapacity() / GB);
            } catch (IOException e) {
                e.printStackTrace();
            }
            assertNotNull(hdfsClient);
        }

        if (isMySqlDataSourceEnabled()) {
            ApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"jdbc_context.xml"});
            localMysqlDS = (DataSource) context.getBean("mysql_dataSource");
            out.println("[DataSource] mysql_dataSource is initialized..");
        }

        assertNotNull(properties);
//                isInitialized = true;
    }


    @Override
    protected void tearDown() throws Exception {

        //console color workaround
        Thread.sleep(500);
    }

    @Override
    protected void runTest() throws Throwable {
        out.printf("\t\t\t\t\t\t[%s] %s begin\n", this.getClass().getSimpleName(), super.getName());
        super.runTest();
        out.printf("\t\t\t\t\t\t[%s] %s end\n", this.getClass().getSimpleName(), super.getName());
    }

    @Override
    public void runBare() throws Throwable {
        out.printf(ANSI_CYAN + "*************************[%s] %s.setUp*************************\n", this.getClass().getSimpleName(), super.getName());
        setUp();
        out.printf("*************************[%s] %s.setUp*************************\n" + ANSI_RESET, this.getClass().getSimpleName(), super.getName());
        try {
            runTest();
        } finally {
            out.printf(ANSI_CYAN + "*************************[%s] %s.tearDown*************************\n", this.getClass().getSimpleName(), super.getName());
            tearDown();
            out.printf("*************************[%s] %s.tearDown*************************\n" + ANSI_RESET, this.getClass().getSimpleName(), super.getName());
        }
    }
}
