/** 
* BusinessLogic
* @author Andrew Kulpa & Darren Wolbers
* Handles almost all XML and HTML document parsing, generation, and retrieval.
*/
package spiderproxy;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.commons.compress.compressors.CompressorException;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class BusinessLogic {

    public BusinessLogic() {

    }

    /**
     * Conditionally returns an internal web-page based upon the url that is provided.
     * It will return either an index page, a site map page, or a 404 page.
     * 
     * @param url a URL for an internal path
     * @return the HTTP response for the internal request
     * @throws java.net.URISyntaxException from getPath() from getPath()
     * @throws org.xml.sax.SAXException from checkAndReturnSitemapHTML()
     * @throws java.io.IOException from checkAndReturnSitemapHTML()
     * @throws javax.xml.parsers.ParserConfigurationException from checkAndReturnSitemapHTML()
    */
    public static String returnHTTPResponse(String url) throws URISyntaxException, SAXException, IOException, ParserConfigurationException{
        String urlLoc = getPath(url);
        String response = "";
        if(urlLoc.trim().equals("/") || urlLoc.trim().equals("/index.html")){
            response += generateHTTPHeader(url, "200 OK") + Proxy.carriageReturn; // created header
            String sitemapsListTable = buildHomepageSitemapListTable();
            response += htmlDocumentWrapper(sitemapsListTable);
        } else if (urlLoc.trim().startsWith("/" + DataAccessor.path)){
            String hostname = urlLoc.replace("/" + DataAccessor.path,"").replace('/', '.'); // REMOVE FIRST '/'? or is it removed?
            File sitemap = DataAccessor.getXMLFile(hostname);
            if (!sitemap.exists()) {
                System.out.println("Did not find sitemap for: " + url);
                response = return404(url);
            } else {
                System.out.println("Found sitemap for: " + url);
                String responseBody = checkAndReturnSitemapHTML(hostname);
                response += generateHTTPHeader(url, "200 OK") + Proxy.carriageReturn;
                response += htmlDocumentWrapper(responseBody);
                
            }
        } else {
            System.out.println("URL requested not within whitelist: " + url);
            response = return404(url);
        }
        return response;
    }
    
    /**
     * Returns an HTTP header including the passed responseMessage
     * 
     * @param url the url for the requested resource
     * @param responseMessage the response code and status message
     * @return the HTTP header as a String
    */
    public static String generateHTTPHeader(String url, String responseMessage){
        String header = "HTTP/1.1 " + responseMessage + Proxy.carriageReturn
                      + "Content-type: text/html" + Proxy.carriageReturn;
        return header;
    }
    
    /**
     * Returns an 404 message, stating that the url requested could not be found.
     * 
     * @param url the url for the requested resource
     * @return the generic 404 message HTML String
    */
    public static String return404(String url){
        String response = "";
        String notFoundMessage = "404 Resource '" + url + "' Not Found.";
        response += generateHTTPHeader(url, "404 Not Found") + Proxy.carriageReturn;
        notFoundMessage = htmlHeader() + notFoundMessage + htmlFooter();
        return response += htmlDocumentWrapper(notFoundMessage);
    }
    
    /**
     * Returns an 404 message, stating that the url requested could not be found.
     * 
     * @param url the url for the requested resource
     * @return a generic 400 message HTML String
    */
    public static String return400(String url){
        String response = "";
        response += generateHTTPHeader(url, "400 Bad Request") + Proxy.carriageReturn;
        String notFoundMessage = "400 Bad Request."; 
        notFoundMessage = htmlHeader() + notFoundMessage + htmlFooter();
        return response += htmlDocumentWrapper(notFoundMessage);
    }
    
    /**
     * Encloses the currentHTML input in html and body tags, with a prepended head tag to boot!
     * 
     * @param currentHTML the HTML that is to be 'wrapped'
     * @return the currentHTML wrapped in proper and standard HTML tags
    */
    public static String htmlDocumentWrapper(String currentHTML){
        String prepended = "<!DOCTYPE html>"
                         + "<html>"
                            + "<head>"
                                + "<title>ProxySpider</title>\n"
                                + "<meta charset=\"utf-8\">"
                                + "<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\" integrity=\"sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u\" crossorigin=\"anonymous\">"
                                + "<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js\"></script>\n"
                                + "<script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js\" integrity=\"sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa\" crossorigin=\"anonymous\"></script>"
                            + "</head>"
                            + "<body>";
        
        String appended = "</body>"
                        + "</html>";
        return prepended + currentHTML + appended;
    }
    
    /**
     * Returns a string with a valid HTML header
     * 
     * @return the header for the HTML page as a String
    */
    public static String htmlHeader(){
        String header = "<div id=\"header\">"
                        + "<nav class=\"navbar navbar-inverse\">"
                            + "<div class=\"container-fluid\">"
                                + "<div class=\"navbar-header\">"
                                    + "<a class=\"navbar-brand\" href=\"/index.html\">ProxySpider</a>"
                                + "</div>"
                            + "</div>"
                        + "</nav>"
                      + "</div>";
        return header; 
    }
    
    /**
     * Returns a string with a valid HTML footer
     * 
     * @return the footer for the HTML page as a String
    */
    public static String htmlFooter(){
        String footer = "<footer class=\"page-footer font-small pt-4 mt-4 navbar-inverse\" style=\"color:#DDD\">\n" +
                            "<div class=\"container-fluid text-center text-md-left\">\n" +
                                "<div class=\"row\">\n" +
                                    "<div class=\"col-md-6\">\n" +
                                        "<h5 class=\"text-uppercase\">ProxySpider</h5>\n" +
                                        "<p>A Maven project meant to merge the concepts of a web spider and a forward proxy.</p>\n" +
                                    "</div>\n" +
                                    "<div class=\"col-md-6\">\n" +
                                        "<h5 class=\"text-uppercase\">Inspirations/Sources</h5>\n" +
                                        "<ul class=\"list-unstyled\">\n" +
                                            "<li>\n" +
                                                "<a href=\"http://mdsec.net/wahh/\">Web Application Hacker's Handbook</a>\n" +
                                            "</li>\n" +
                                            "<li>\n" +
                                                "<a href=\"https://nostarch.com/pentesting\">Penetration Testing</a>\n" +
                                            "</li>\n" +
                                            "<li>\n" +
                                                "<a href=\"https://www.ietf.org/rfc/rfc2068.txt\">RFC-2068</a>\n" +
                                            "</li>\n" +
                                            "<li>\n" +
                                                "<a href=\"https://www.ietf.org/rfc/rfc3143.txt\">RFC-3143</a>\n" +
                                            "</li>\n" +
                                        "</ul>\n" +
                                    "</div>\n" +
                                "</div>\n" +
                                "<div class=\"footer-copyright py-3 text-center row\">\n" +
                                    "© 2018 Copyright:\n" +
                                    "<a href=\"https://gitlab.wiu.edu/aj-kulpa/ProxySpider\"> ProxySpider</a>\n" +
                                "</div>\n" +
                            "</div>\n" +
                        "</footer>" +
                        "<style>" +
                            "html {\n" +
                                "position: relative;\n" +
                                "min-height: 100%;\n" +
                            "}\n" +
                            "body {\n" +
                                "height:100%;\n" + 
                                "margin-bottom: 150px;\n" +
                            "}\n" + 
                            "footer {\n" +
                                "position: absolute;\n" +
                                "bottom: 0;\n" +
                                "width: 100%;\n" +
                            "}" +
                        "</style>";
        return footer;
    }
    
    /**
     * Returns a string with a valid HTML table containing all domains crawled
     * 
     * @return a String of the HTML table from the sitemap listings
    */
    public static String buildHomepageSitemapListTable(){
        ArrayList<String> xmlFiles = DataAccessor.getXMLFileList();
        String htmlRows = "";
        int i = 0;
        for(String xmlFile : xmlFiles){
            String baseFilename = xmlFile;
            if (baseFilename.indexOf(".") > 0) {
                baseFilename = baseFilename.substring(0, baseFilename.lastIndexOf("."));
            }
            htmlRows += "<tr>"
                        + "<th scope=\"row\">" + i + "</th>"
                        + "<td>"
                            + "<a href=\"" + DataAccessor.path + baseFilename.replace('.', '/') + "\">" // x.y.z --> x\y\z
                                + baseFilename // x.y.z
                            + "</a>"
                        + "</td>"
                     + "</tr>";
            i += 1;
        }
        String table = "<table class=\"table\">"
                + "<thead>"
                    + "<tr>"
                        + "<th scope=\"col\">#</th>"
                        + "<th scope=\"col\">loc</th>"
                    + "</tr>"
                + "</thead>"
                    + "<tbody>"
                        + htmlRows
                    + "</tbody>"
                + "</table>";
        return htmlHeader() + table + htmlFooter();
    }
    
    /**
     * Returns a string with a valid HTML table resulting from a parsed XML document.
     * 
     * @param xmlDocument the XML document that is to be scraped to generate an HTML table
     * @return the HTML table related to the XML document
     * @throws javax.xml.parsers.ParserConfigurationException thrown by buildSiteMapHTMLTable
     * @throws org.xml.sax.SAXException thrown by buildSiteMapHTMLTable
     * @throws java.io.IOException thrown by buildSiteMapHTMLTable
    */
    public static String buildHTMLTableFromDoc(Document xmlDocument) throws ParserConfigurationException, SAXException, IOException {
        String treeString = buildSiteMapHTMLTable(xmlDocument);
        return treeString;
    }

    /**
     * Returns a string with a set of rows resulting from a parsed XML document.
     * Based upon schemas defined at https://www.xml-sitemaps.com/ and
     * https://www.sitemaps.org/protocol.html
     * 
     * @param xmlDocument the XML document that is to be parsed for its 'loc' tags
     * @return HTML rows from parse 'loc's in the xmlDocument
    */
    public static String xmlToHTMLRows(Document xmlDocument) {
        NodeList children = xmlDocument.getElementsByTagName("loc");
        String rows = "";
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            String row = "<tr>"
                    + "<th scope=\"row\">" + i + "</th>" //.get)ElementsByTagName("loc")
                    + "<td>" 
                        + "<a href=\"" + node.getTextContent() + "\">"
                            + node.getTextContent() 
                        + "</a>"
                    + "</td>"
                    + "</tr>";
            rows += row;
        }
        return rows;
    }

    /**
     * Constructs an HTML table from an XML document.
     * 
     * @param xmlDocument the XML doc that is being used to generate an HTML sitemap
     * @return an HTML table from a sitemap XML document
    */
    public static String buildSiteMapHTMLTable(Document xmlDocument) {
        String tableBegin = "<table class=\"table\">"
                            + "<thead>"
                                + "<tr>"
                                    + "<th scope=\"col\">#</th>"
                                    + "<th scope=\"col\">loc</th>"
                                + "</tr>"
                            + "</thead>"
                                + "<tbody>"
                                + xmlToHTMLRows(xmlDocument)
                                + "</tbody>"
                            + "</table>";
        return tableBegin;
    }

    /**
     * Resolves a hostname using a URI. 
     * 
     * 'www.' is implied for hosts of form x.y, and thus will remove any 'www.' 
     * to make sure missing prefixes will resolve to the same hostname.
     * 
     * 
     * @param urlLoc the url of a given resource
     * @return the hostname of the urlLoc 
     * @throws java.net.URISyntaxException caused by miscreation of URI object using the urlLoc
    */
    public static String getHostname(String urlLoc) throws URISyntaxException {
        URI uri = new URI(urlLoc);
        String host = uri.getHost();
        if(host == null)
            return "";
        if (host.startsWith("www.")) {
            return host.substring(4); // get everything after 'www.' since it's implied.
        } else {
            return host;
        }
    }
    
    /**
     * Returns the path of the passed URI string.
     * 
     * 
     * @param urlLoc the url of a given resource
     * @return the path of the urlLoc
     * @throws java.net.URISyntaxException caused by miscreation of URI object using the urlLoc
    */
    public static String getPath(String urlLoc) throws URISyntaxException{
        URI uri = new URI(urlLoc);
        String path = uri.getPath();
        return path;
    }

    /**
     * Returns whether the passed XML document already includes the passed URI.
     * 
     * 
     * @param uri the resource string that is checked for in the xml doc
     * @param xmlDocument the xml doc that may or may not contain the given node
     * @return whether the xmlDocument has a node resulting from uri
    */
    public static boolean hasUrlNode(String uri, Document xmlDocument) {
        NodeList children = xmlDocument.getElementsByTagName("loc");
        for (int i = 0; i < children.getLength(); i++) {
            String childVal = children.item(i).getTextContent();
            if (childVal != null && childVal.equals(uri)) {
                return true;
            }  else {
            }
        }
        return false;
    }

    /**
     * Generates an XML element using the passed doc for the passed uri.
     * 
     * 
     * @param uri the resource identifier used to generate an XML node
     * @param doc a Document object used to generate an XML node
     * @return the Element resulting from the uri
    */
    public static Element generateUrlNode(String uri, Document doc) {
        // "Include a <url> entry for each URL, as a parent XML tag."
        Element urlNode = doc.createElement("url");
        // "Include a <loc> child entry for each <url> parent tag."
        Element locNode = doc.createElement("loc");
        locNode.setTextContent(uri.split("r\\?")[0]);
        urlNode.appendChild(locNode);

        return urlNode;
    }

    /**
     * Generates a skeleton sitemap XML conforming to the schema listed 
     * on http://www.sitemaps.org/schemas/sitemap/0.9
     * 
     * 
     * @param uri the resource identifier to be inserted into the new sitemap
     * @return the XML Document of a generic sitemap including the passed uri
     * @throws javax.xml.parsers.ParserConfigurationException caused by docFactory.newDocumentBuilder()
    */
    public static Document generateBaseSitemap(String uri) throws ParserConfigurationException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        Document newDoc = docBuilder.newDocument();
        // "Begin with an opening <urlset> tag and end with a closing </urlset> tag."
        Element rootNode = newDoc.createElement("urlset");
        newDoc.appendChild(rootNode);
        // "Specify the namespace (protocol standard) within the <urlset> tag."
        rootNode.setAttribute("xmlns", "http://www.sitemaps.org/schemas/sitemap/0.9");
        // "Include a <url> entry for each URL, as a parent XML tag.""
        rootNode.appendChild(generateUrlNode(uri, newDoc));

        return newDoc;
    }
    
    /**
     * Checks whether the hostname has a sitemap already created for it. If it 
     * exists, then it will return an HTML sitemap for the given hostname.
     * 
     * 
     * @param hostname a domains hostname
     * @return an XML to HTML translated sitemap if one exists for the hostname
     * @throws org.xml.sax.SAXException caused by docBuilder.parse(sitemap)
     * @throws javax.xml.parsers.ParserConfigurationException caused by docFactory.newDocumentBuilder()
     * @throws java.io.IOException caused by docBuilder.parse(sitemap)
    */
    public static String checkAndReturnSitemapHTML(String hostname) throws SAXException, IOException, ParserConfigurationException{
        String html = "";
        File sitemap = DataAccessor.getXMLFile(hostname);
        if (!sitemap.exists()) {
            System.out.println("No sitemap exists yet for the requested hostname: " + hostname);
            // return null and resolve as 404?
            // return generated 404 page here?
        } else {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document xmlDocument = docBuilder.parse(sitemap);
            xmlDocument.getDocumentElement().normalize();
            String htmlTable = buildSiteMapHTMLTable(xmlDocument);
            html += htmlHeader() + htmlTable + htmlFooter();
        }
        return html;
    }

    /**
     * Decompresses the fullData using the specified encodingString into an HTML document. 
     * Then, the HTML document is parsed using the urlRequested and Jsoup. All URLs 
     * are retrieved from the file and inserted into a set. For each URL in the set, 
     * including the urlRequested, a sitemap 'loc' entry is checked for and created if
     * it has not been created yet.
     * 
     * 
     * @param fullData the data in byte[] format
     * @param encodingString the string defining the method of encoding
     * @param urlRequested the string representing the url that is requested
     * @throws java.util.zip.DataFormatException caused by Decompressor.decompress(fullData, encodingString)
     * @throws org.apache.commons.compress.compressors.CompressorException caused by Decompressor.decompress(fullData, encodingString)
     * @throws java.io.IOException caused by Decompressor.decompress(fullData, encodingString)
    */
    public static void checkAndInsertSet(byte[] fullData, String encodingString, String urlRequested) throws DataFormatException, CompressorException, IOException{
        String decodedDocument = Decompressor.decompress(fullData, encodingString);
        org.jsoup.nodes.Document doc = Jsoup.parse(decodedDocument, urlRequested);
        Elements links = doc.select("a[href]");
        Set<String> URLs = new HashSet<>();
        for(org.jsoup.nodes.Element link: links){
            String strLink = link.attr("abs:href");
            if(strLink != null && !strLink.isEmpty())
                URLs.add(strLink.split("\\?")[0].split("#")[0]);
        }
        URLs.add(urlRequested);
        for(String url : URLs){
            System.out.println("URL: " + url);
        }
        System.out.println(URLs);
        for(String url : URLs){
            try { // If the URL is not valid or somehow incorrect, log and move on.
                String hostname = getHostname(url);
                checkAndInsert(url, hostname);
            } catch (URISyntaxException | ParserConfigurationException | SAXException | TransformerException ex) {
                Logger.getLogger(BusinessLogic.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * Checks whether a sitemap exists for a given hostname. If it does not, then
     * create it. Once created or retrieved, it is parsed as an XML document and 
     * the urlLoc is checked for and inserted into it as a standard 'loc' entry.
     * 
     * 
     * @param urlLoc the String representing a web resource 
     * @param hostname a domain hostname
     * @throws java.net.URISyntaxException results from the URI urlLoc being malformed
     * @throws java.io.IOException caused by either sitemap.createNewFile() or docBuilder.parse(sitemap)
     * @throws javax.xml.parsers.ParserConfigurationException caused by either generateBaseSitemap(urlLoc) or docFactory.newDocumentBuilder()
     * @throws javax.xml.transform.TransformerException thrown only during writing to an XML file
     * @throws org.xml.sax.SAXException caused by docBuilder.parse(sitemap)
    */
    // if a given URI for a domain hasn't been tracked yet, write it to the sitemap.
    // Also, if a given URI does not have its corresponding sitemap made yet, call generateSitemapRootNode()
    public static void checkAndInsert(String urlLoc, String hostname) throws URISyntaxException, IOException, ParserConfigurationException, SAXException, TransformerException {
        // Retrieve the file using the hostname generated from urlLoc
        if(hostname.isEmpty()){
            return;
        }
        File sitemap = DataAccessor.getXMLFile(hostname);
        // if sitemap not made yet, create it
        if (!sitemap.exists()) {
            sitemap.createNewFile();
            DataAccessor.writeXMLToFile(generateBaseSitemap(urlLoc), sitemap);
        } else {
            // Now generate a mutable in-memory XML document for manipulation
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document xmlDocument = docBuilder.parse(sitemap);
            xmlDocument.getDocumentElement().normalize();
            // if the corresponding node hasnt been created yet, add it!
            if (!hasUrlNode(urlLoc, xmlDocument)) {
                xmlDocument.getDocumentElement().appendChild(generateUrlNode(urlLoc, xmlDocument));
                DataAccessor.writeXMLToFile(xmlDocument, sitemap);
            }
        }
    }
}
