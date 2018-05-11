package spiderproxy;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.HttpException;
import org.apache.http.Consts;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;


public class SpiderProxyTest {
    
    public SpiderProxyTest() {}
    
    /**
     * Run SpiderProxy before running tests
     */
    @BeforeClass
    public static void setUpClass() {
        new Thread() {
        @Override
            public void run() {
                SpiderProxy.main(new String[]{"8080"});
            }
        }.start();
    }
    
    @AfterClass
    public static void tearDownClass() {}
    
    @Before
    public void setUp() throws MalformedURLException, IOException {}
        
    /**
     * Tests:
     *    GET to 404 web page
     *    Response should return 404
     *    Access the internal sitemap for this page
     *    If the internal sitemap for this page exists,
     *      Make sure the 404 page wasn't added
     * @throws java.lang.InterruptedException
     */
    @org.junit.Test
    public void testHTTPGETinvalidPage() throws InterruptedException{
        HttpHost proxy = new HttpHost("127.0.0.1", 8080);
        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
        CloseableHttpClient httpclient = HttpClients.custom()
                .setRoutePlanner(routePlanner)
                .build();
      
        String url = "http://wiu.edu/cats";
        HttpGet httpget;
        CloseableHttpResponse response = null;
        try {
            int statuscode;
            // Execute the GET.
            httpget = new HttpGet(url);
            response = httpclient.execute(httpget); 
            statuscode = response.getStatusLine().getStatusCode();
            // Verify it's a 404 page
            assertEquals(statuscode,404);
            Thread.sleep(1000); // make sure server has time to add to sitemap
            response = null;
            // Check to make sure the 404 page wasn't added to sitemap
            httpget = new HttpGet("http://127.0.0.1:8080/map/wiu/edu"); // check if sitemap generated
            response = httpclient.execute(httpget); // invalid internal request
            String body = EntityUtils.toString(response.getEntity(), "UTF-8");
            if(!body.contains("404")){
                assertTrue(!body.contains(url));
            }
        } catch (HttpException e) {
            System.err.println("Fatal protocol violation: " + e.getMessage());
            e.printStackTrace();
            fail("HTTP Exception");
        } catch (IOException e) {
            fail("IO Exception");
            System.err.println("Fatal transport error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Release the connection.
            if(response != null) {
                try {
                    response.close();
                } catch (IOException ex) {
                    Logger.getLogger(SpiderProxyTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    /**
     * Tests:
     *    POST to 404 web page
     *    Response should return 404
     *    Access the internal sitemap for this page
     *    If the internal sitemap for this page exists,
     *      Make sure the 404 page wasn't added
     * @throws java.lang.InterruptedException
     */
    @org.junit.Test
    public void testHTTPOSTinvalidPage() throws InterruptedException{
        HttpHost proxy = new HttpHost("127.0.0.1", 8080);
        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
        CloseableHttpClient httpclient = HttpClients.custom()
                .setRoutePlanner(routePlanner)
                .build();
      
        String url = "http://wiu.edu/cats";
        HttpPost httppost;
        HttpGet httpget;
        CloseableHttpResponse response = null;
        try {
            int statuscode;
            // Execute the POST.
            httppost = new HttpPost(url);
            response = httpclient.execute(httppost); 
            statuscode = response.getStatusLine().getStatusCode();
            // Verify it's a 404 page
            assertEquals(statuscode,404);
            Thread.sleep(1000); // make sure server has time to add to sitemap
            response = null;
            // Check to make sure the 404 page wasn't added to sitemap
            httpget = new HttpGet("http://127.0.0.1:8080/map/wiu/edu"); // check if sitemap generated
            response = httpclient.execute(httpget); // invalid internal request
            String body = EntityUtils.toString(response.getEntity(), "UTF-8");
            if(!body.contains("404")){
                assertTrue(!body.contains(url));
            }
        } catch (HttpException e) {
            System.err.println("Fatal protocol violation: " + e.getMessage());
            e.printStackTrace();
            fail("HTTP Exception");
        } catch (IOException e) {
            fail("IO Exception");
            System.err.println("Fatal transport error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Release the connection.
            if(response != null) {
                try {
                    response.close();
                } catch (IOException ex) {
                    Logger.getLogger(SpiderProxyTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
     /**
     * Tests:
     *    POST to web forms with form data encoded in parameters
     *    Verify 200 is returned
     *    Verify web page is put into sitemap
     * @throws java.lang.InterruptedException
     */
    @org.junit.Test
    public void testHTTPOSTvalidPage() throws InterruptedException{
        HttpHost proxy = new HttpHost("127.0.0.1", 8080);
        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
        CloseableHttpClient httpclient = HttpClients.custom()
                .setRoutePlanner(routePlanner)
                .build();
      
        String url = "http://testing-ground.scraping.pro/login";
        HttpPost httppost;
        HttpGet httpget;
        CloseableHttpResponse response = null;
        try {
            int statuscode = 0;
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("usr", "admin"));
            formparams.add(new BasicNameValuePair("pwd", "12345"));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, Consts.UTF_8);
            // Execute the POST.
            httppost = new HttpPost(url);
            httppost.setEntity(entity);
            response = httpclient.execute(httppost); 
            statuscode = response.getStatusLine().getStatusCode();
            // Verify it's 200 OK
            assertEquals(statuscode,200);
            Thread.sleep(1000); // make sure server has time to add to sitemap
            statuscode = 0;
            response = null;
            // Check to make sure the 404 page wasn't added to sitemap
            httpget = new HttpGet("http://127.0.0.1:8080/map/testing-ground/scraping/pro"); // check if sitemap generated
            response = httpclient.execute(httpget); // invalid internal request
            String body = EntityUtils.toString(response.getEntity(), "UTF-8");
            assertTrue(body.contains("http://testing-ground.scraping.pro/login"));
        } catch (HttpException e) {
            System.err.println("Fatal protocol violation: " + e.getMessage());
            e.printStackTrace();
            fail("HTTP Exception");
        } catch (IOException e) {
            fail("IO Exception");
            System.err.println("Fatal transport error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Release the connection.
            if(response != null) {
                try {
                    response.close();
                } catch (IOException ex) {
                    Logger.getLogger(SpiderProxyTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    /**
     * Tests:
     *    GET to HTTPS 404 web page
     *    It will do the secure tunnel and CONNECT
     *    Response should return 404
     *    Access the internal sitemap for this page
     *    If the internal sitemap for this page exists,
     *      Make sure the 404 page wasn't added
     * @throws java.lang.InterruptedException
     */
    @org.junit.Test
    public void testHTTPCONNECTinvalidPage() throws InterruptedException{
        System.getProperties().put("https.proxySet" , "true");
        System.getProperties().put("https.proxyHost","127.0.0.1");
        System.getProperties().put("https.proxyPort", "8080");
        CloseableHttpClient httpclient = HttpClients
        .custom()
        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
        .build();
      
        String url = "https://www.google.com/404";
        HttpGet httpGet;
        CloseableHttpResponse response = null;
        try {
            httpGet = new HttpGet(url);
            response = httpclient.execute(httpGet); // valid external request
            int statuscode = response.getStatusLine().getStatusCode();
            assertEquals(statuscode, 404);
            String body = EntityUtils.toString(response.getEntity(), "UTF-8");
            if(!body.contains("404")){
                assertTrue(!body.contains(url));
            }
        } catch (HttpException e) {
            System.err.println("Fatal protocol violation: " + e.getMessage());
            e.printStackTrace();
            fail("HTTP Exception");
        } catch (IOException e) {
            System.err.println("Fatal transport error: " + e.getMessage());
            e.printStackTrace();
            fail("IO Exception");
            e.printStackTrace();
        } finally {
            // Release the connection.
            if(response != null) {
                try {
                    response.close();
                } catch (IOException ex) {
                    Logger.getLogger(SpiderProxyTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    /**
     * Tests:
     *    GET to HTTPS web page
     *    It will do the secure tunnel and CONNECT
     *    Response should return 404
     *    Access the internal sitemap for this page
     *    If the internal sitemap for this page exists,
     *      Make sure the 404 page wasn't added
     * @throws java.lang.InterruptedException
     */
    @org.junit.Test
    public void testHTTPCONNECTvalidPage() throws InterruptedException{
        System.getProperties().put("https.proxySet" , "true");
        System.getProperties().put("https.proxyHost","127.0.0.1");
        System.getProperties().put("https.proxyPort", "8080");
        CloseableHttpClient httpclient = HttpClients
        .custom()
        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
        .build();
      
        String url = "https://www.google.com/";
        HttpGet httpGet;
        CloseableHttpResponse response = null;
        try {
            httpGet = new HttpGet(url);
            response = httpclient.execute(httpGet); // valid external request
            int statuscode = response.getStatusLine().getStatusCode();
            assertEquals(statuscode, 200);
            String body = EntityUtils.toString(response.getEntity(), "UTF-8");
            assertTrue(body.contains(url));    
        } catch (HttpException e) {
            System.err.println("Fatal protocol violation: " + e.getMessage());
            e.printStackTrace();
            fail("HTTP Exception");
        } catch (IOException e) {
            System.err.println("Fatal transport error: " + e.getMessage());
            e.printStackTrace();
            fail("IO Exception");  
            e.printStackTrace();
        } finally {
            // Release the connection.
            if(response != null) {
                try {
                    response.close();
                } catch (IOException ex) {
                    Logger.getLogger(SpiderProxyTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    /**
     * Tests:
     *    GET to legitimate web page
     *    Connects to web page fine
     *    Responses to client without issues
     *    Creates and populates a sitemap file
     *    GET Request valid sitemap web page, of path /map/
     *    Working “homepage” link
     *    Show webpage with formatted sitemap
     * @throws java.lang.InterruptedException
     */
    @org.junit.Test
    public void testHTTPGETRequestAndDomainSitemap() throws InterruptedException{
        HttpHost proxy = new HttpHost("127.0.0.1", 8080);
        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
        CloseableHttpClient httpclient = HttpClients.custom()
                .setRoutePlanner(routePlanner)
                .build();
      
        String url = "http://www.wiu.edu";
        HttpGet httpget;
        CloseableHttpResponse response = null;
        try {
            // Execute the method.
            httpget = new HttpGet(url);
            response = httpclient.execute(httpget); // valid external request
            
            int statuscode = response.getStatusLine().getStatusCode();
            assertEquals(statuscode, 200);
            Thread.sleep(1000); // make sure server has time to add to sitemap
            httpget = new HttpGet("http://127.0.0.1:8080/map/wiu/edu"); // check if sitemap generated
            response = httpclient.execute(httpget); // valid internal request
            statuscode = response.getStatusLine().getStatusCode();
            assertEquals(statuscode, 200); // OK response
            
            String body = EntityUtils.toString(response.getEntity(), "UTF-8");
            assertTrue(body.contains("<a class=\"navbar-brand\" href=\"/index.html\">ProxySpider</a>")); // has homepage link
            assertTrue(body.contains("<table class=\"table\"><thead><tr><th scope=\"col\">#</th><th scope=\"col\">loc</th></tr></thead>")); // has sitemap table
        } catch (HttpException e) {
            System.err.println("Fatal protocol violation: " + e.getMessage());
            e.printStackTrace();
            fail("HTTP Exception");
        } catch (IOException e) {
            fail("IO Exception");
            System.err.println("Fatal transport error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Release the connection.
            if(response != null) {
                try {
                    response.close();
                } catch (IOException ex) {
                    Logger.getLogger(SpiderProxyTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    /**
     * Tests:
     *    GET request to /index.html or / on the java application defined host and port.
     *    Check for presence of all previously crawled domains (crawl http://www.stealmylogin.com/ first)
     *    Respond with a simple directory webpage with links to domain sitemaps
     *    Check that client receives webpage in full (end HTML tag)
     *    Check that no error happens (implicit upon failure of the 3 above)
     * @throws java.lang.InterruptedException
     */
    @org.junit.Test
    public void testIndexPage() throws InterruptedException{
        HttpHost proxy = new HttpHost("127.0.0.1", 8080);
        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
        CloseableHttpClient httpclient = HttpClients.custom()
                .setRoutePlanner(routePlanner)
                .build();
      
        String url = "http://www.stealmylogin.com/";
        HttpGet httpget;
        CloseableHttpResponse response = null;
        try {
            // Execute the method.
            httpget = new HttpGet(url);
            response = httpclient.execute(httpget); // valid external request
            
            int statuscode = response.getStatusLine().getStatusCode();
            assertEquals(statuscode, 200);
            Thread.sleep(1000); // make sure server has time to add to sitemap
            httpget = new HttpGet("http://127.0.0.1:8080/"); // check homepage has stealmylogin link
            response = httpclient.execute(httpget); // valid internal request
            statuscode = response.getStatusLine().getStatusCode();
            assertEquals(statuscode, 200); // OK response
            
            String body = EntityUtils.toString(response.getEntity(), "UTF-8");
            assertTrue(body.contains("stealmylogin")); // has crawled webpage link
            assertTrue(body.contains("<table class=\"table\">")); // has table of sitemaps
            assertTrue(body.contains("</html>")); // received sitemap list page in full
        } catch (HttpException e) {
            System.err.println("Fatal protocol violation: " + e.getMessage());
            e.printStackTrace();
            fail("HTTP Exception");
        } catch (IOException e) {
            fail("IO Exception");
            System.err.println("Fatal transport error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Release the connection.
            if(response != null) {
                try {
                    response.close();
                } catch (IOException ex) {
                    Logger.getLogger(SpiderProxyTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    /**
     * Tests:
     *    GET Request to invalid internal webpage
     *    Working “homepage” link
     *    Show 404 Not Found HTML page
     *    Respond with 404 Not Found Status Code
     */
    @org.junit.Test
    public void testInvalidInternalPage(){
        HttpHost proxy = new HttpHost("127.0.0.1", 8080);
        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
        CloseableHttpClient httpclient = HttpClients.custom()
                .setRoutePlanner(routePlanner)
                .build();
      
        String url = "http://127.0.0.1:8080/puppies";
        HttpGet httpget;
        CloseableHttpResponse response = null;
        try {
            // Execute the method.
            httpget = new HttpGet(url);
            response = httpclient.execute(httpget); // valid external request
            
            int statuscode = response.getStatusLine().getStatusCode();
            assertEquals(statuscode, 404);
            String body = EntityUtils.toString(response.getEntity(), "UTF-8");
            assertTrue(body.contains("<a class=\"navbar-brand\" href=\"/index.html\">ProxySpider</a>")); // has homepage link
            assertTrue(body.contains("404"));
        } catch (HttpException e) {
            System.err.println("Fatal protocol violation: " + e.getMessage());
            e.printStackTrace();
            fail("HTTP Exception");
        } catch (IOException e) {
            fail("IO Exception");
            System.err.println("Fatal transport error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Release the connection.
            if(response != null) {
                try {
                    response.close();
                } catch (IOException ex) {
                    Logger.getLogger(SpiderProxyTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    /**
     * Tests:
     *    Internal Request not of method type GET
     *    Working “homepage” link
     *    Show 400 Bad Request HTML page
     *    Respond with 400 Bad Request Status Code
     */
    @org.junit.Test
    public void testInvalidInternalRequest(){
        HttpHost proxy = new HttpHost("127.0.0.1", 8080);
        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
        CloseableHttpClient httpclient = HttpClients.custom()
                .setRoutePlanner(routePlanner)
                .build();
      
        String url = "http://127.0.0.1:8080/";
        HttpPost httpPost;
        CloseableHttpResponse response = null;
        try {
            // Execute the method.
            httpPost = new HttpPost(url);
            response = httpclient.execute(httpPost); // valid external request
            
            int statuscode = response.getStatusLine().getStatusCode();
            assertEquals(statuscode, 400);
            String body = EntityUtils.toString(response.getEntity(), "UTF-8");
            assertTrue(body.contains("<a class=\"navbar-brand\" href=\"/index.html\">ProxySpider</a>")); // has homepage link
            assertTrue(body.contains("400"));
        } catch (HttpException e) {
            System.err.println("Fatal protocol violation: " + e.getMessage());
            e.printStackTrace();
            fail("HTTP Exception");
        } catch (IOException e) {
            fail("IO Exception");
            System.err.println("Fatal transport error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Release the connection.
            if(response != null) {
                try {
                    response.close();
                } catch (IOException ex) {
                    Logger.getLogger(SpiderProxyTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    /**
     * Tests:
     *    Web-page requested resulted in 302 Redirect
     *    Ignore requested URL
     *    Include new redirected URL in Sitemap
     *    Process this new page normally
     */
    @org.junit.Test
    public void testRedirection(){
        HttpHost proxy = new HttpHost("127.0.0.1", 8080);
        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
        CloseableHttpClient httpclient = HttpClients.custom()
                .setRoutePlanner(routePlanner)
                .build();
      
        String url = "http://www.fox.com/";
        HttpGet httpGet;
        CloseableHttpResponse response = null;
        try {
            // Execute the method.
            HttpParams params = new BasicHttpParams();
            params.setParameter(ClientPNames.HANDLE_REDIRECTS, false);
            httpGet = new HttpGet(url);
            httpGet.setParams(params);
            response = httpclient.execute(httpGet); // valid external request
            int statuscode = response.getStatusLine().getStatusCode();
            assertEquals(statuscode, 301);
            
            httpGet = new HttpGet("http://127.0.0.1:8080/map/fox/com"); // check if sitemap wasnt generated
            response = httpclient.execute(httpGet); // valid internal request
            statuscode = response.getStatusLine().getStatusCode();
            
            if(statuscode != 404){
                String body = EntityUtils.toString(response.getEntity(), "UTF-8");
                assertTrue(!body.contains("https://www.fox.com/movies/"));
            }
        } catch (HttpException e) {
            System.err.println("Fatal protocol violation: " + e.getMessage());
            e.printStackTrace();
            fail("HTTP Exception");
        } catch (IOException e) {
            System.err.println("Fatal transport error: " + e.getMessage());
            fail("IO Exception");
            e.printStackTrace();
        } finally {
            // Release the connection.
            if(response != null) {
                try {
                    response.close();
                } catch (IOException ex) {
                    Logger.getLogger(SpiderProxyTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
}
