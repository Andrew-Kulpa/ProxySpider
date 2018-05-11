/**
 * @author Andrew Kulpa & Darren Wolbers
 * @since May. 2, 2018
 * @version 1.0
 *
 * A simple HTTP proxy interface for the ProxySpider program
 */
package spiderproxy;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.BindException;
import java.net.Socket;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.httpclient.ChunkedInputStream;
import org.xml.sax.SAXException;

public class Proxy {
    
    // CRLF characters.
    public static String carriageReturn = "\r\n";
    public static boolean debug = true;
    // TO/FROM CLIENT BROWSER //
    private Socket clientSocket; // Client Socket
    private DataOutputStream clientOut; // Data sent back to client
    private DataInputStream clientIn; // Request from client browser

    // TO/FROM EXTERNAL SERVER //
    private Socket externalSocket; // External Server Socket
    private DataOutputStream externalOut; // Outgoing from proxy
    private DataInputStream externalIn; // Incoming to proxy

    /**
    * Constructs a Proxy object when used with 'new'; runs server
    *
    * @param clientSocket Socket created from client request
    * @throws java.io.IOException Thrown by clientSocket
    */
    public Proxy(Socket clientSocket) throws IOException {
        run(clientSocket);
    }

    /**
    * Read in entire client request up to a carriage return, null, or empty
    * string
    *
    * @param input DataInputStream - The stream reader from clientIn/externalIn.
    * @return String - Request from DataInputStream (clientIn)
    * @throws java.io.IOException thrown by .readLine() on string reading.
    */
    public String slurpInput(DataInputStream input) throws IOException {
        String request = "";
        while (true) {
            String str = input.readLine();
            if ("".equals(str) || str == null || str.equals(carriageReturn)) {
                break;
            }
            request += (str + carriageReturn);
        }
        return request;
    }
    
    /**
     * Handles chunked response input. Reads chunked data into a byte array.
     *
     * @param header Header String - will have chunkedEncoding removed, then written to DataOutputStream.
     * @param is InputStream - used in creation of ChunkedInputStream. Must be InputStream of a socket.
     * @param dos DataOutputStream - used to write header out. Should be clientOut/externalOut.
     * @return byte[] Data that has been unchunked.
     * @throws java.io.IOException Thrown by writeBytes(), ChunkedInputStream(), ByteOutputStream(), and read().
    */
    public byte[] handleChunkedInput(String header, InputStream is, DataOutputStream dos) throws IOException{
        // Remove 'transfer-encoding: chunked' line from header, send to client.
        header = removeChunkedEncoding(header) + carriageReturn;
        dos.writeBytes(header);

        ChunkedInputStream cis = new ChunkedInputStream(is);
        byte[] unchunkedData;
        int offset;
        // Maybe change this later? It'll block if its too much, 
        // and reading 1 byte at a time is slow, but it works.
        int bufferSize = 1;
        byte buffer[] = new byte[bufferSize];

        // Dynamically growing byte array object
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) { 
            while((offset=cis.read(buffer, 0, bufferSize))!= -1)
                bos.write(buffer, 0, offset);
            unchunkedData = bos.toByteArray();
        }
        return unchunkedData;
    }
    
    /**
    * If it's a 200 OK and an HTML file, then the page is passed down to the business layer.
    *
    * @param data byte[] - contains response data from external host to be passed down to business layer.
    * @param header String - response header from external host.
    * @param urlRequested String - urlRequested by client, passed to business layer.
    * @throws java.io.IOException - thrown by checkAndInsertSet()
    * @throws java.util.zip.DataFormatException - thrown by checkAndInsertSet() related to bad data format
    * @throws org.apache.commons.compress.compressors.CompressorException - thrown by checkAndInsertSet() related to Compressor
    */
    public void sendToBusinessLayer(byte[] data, String header, String urlRequested) throws IOException , DataFormatException, CompressorException{
        Boolean isHTML = isHTMLFile(header);
        Boolean isOKResponse = isOKResponse(header);
        if(isHTML && isOKResponse){
            String encodingString = getContentEncoding(header);
            BusinessLogic.checkAndInsertSet(data, encodingString, urlRequested);
        }
    }
    
    /**
    * Slurps up all ServerSocket response input. Get data coming from external
    * host using externalIn, send it to client with clientOut.
    *
    * @param urlRequested String - urlRequested by client, passed to business layer.
    * @return String - Response read from external host. 
    * @throws java.io.IOException thrown by handleChunkedInput(), write(), writeBytes(), readFully(), sendToBusinessLayer(), readLine()
    * @throws java.util.zip.DataFormatException thrown by sendToBusinessLayer() related to bad data format.
    * @throws org.apache.commons.compress.compressors.CompressorException thrown by sendToBusinessLayer() related to Compressor
    */
    public String slurpResponse(String urlRequested) throws IOException, DataFormatException, CompressorException {
        StringBuilder strBuffer = new StringBuilder();
        String line;

        // Get response header from external host. 
        // readLine() will read in the CRLF only line,
        //   but the .length() method will interpret that line as length 0
        while ((line = externalIn.readLine()) != null && line.length() > 0) {
            strBuffer.append(line).append(carriageReturn);
        }
        String header = strBuffer.toString();
        // Fix header if https and http in line
        header = cleanLocationValue(header);
        String response = "";
        
        if(getTransferEncoding(header).contains("chunked")){
            // Read chunked response and send it to client
            byte[] unchunkedData = handleChunkedInput(header, externalSocket.getInputStream(), clientOut);
            sendToBusinessLayer(unchunkedData, header, urlRequested);
            clientOut.write(unchunkedData);
            clientOut.write(carriageReturn.getBytes());
        }
        else{ 
            response = header + carriageReturn;
            clientOut.writeBytes(response);
            int contentLength = getContentLength(header);
            if (contentLength > 0) { 
                // If there's data in the response, read and send to client
                byte[] data = new byte[contentLength];
                externalIn.readFully(data);
                clientOut.write(data);
                sendToBusinessLayer(data, header, urlRequested);
            }
        }
        return response;
    }
    
    /**
    * Removes the line containing 'transfer-encoding: chunked' from the header.
    * @param header String - HTTP header with chunked encoding.
    * @return String - HTTP header without chunked encoding.
    */
    public String removeChunkedEncoding(String header){
        String newHeader = "";
        String transferEncodingChunked = "transfer-encoding: chunked";
        String[] headerLines = header.split(carriageReturn);
        for(String headerLine : headerLines){
            boolean containsTransferEncodingChunked =  headerLine.toUpperCase().contains(transferEncodingChunked.toUpperCase());
            if (!containsTransferEncodingChunked)
                newHeader += headerLine + carriageReturn;
        }
        return newHeader;
    }
    
    /**
    * Replaces Proxy-Connection: Keep-Alive with Connection: close
    * @param header String - HTTP header.
    * @return String - header with Connection: close instead of keep-alive.
    */
    public String removeKeepAlive(String header){
        String newHeader = "";
        String proxyConnection = "Proxy-Connection";
        String[] headerLines = header.split(carriageReturn);
        for(String headerLine : headerLines){
            boolean containsProxyConnection =  headerLine.toUpperCase().contains(proxyConnection.toUpperCase());
            if (containsProxyConnection) {
                newHeader += "Connection: close" + carriageReturn;
            } else {
                newHeader += headerLine + carriageReturn;
            }
        }
        return newHeader;
    }
    
    /**
    * Read the client request and grab the absolute URI thats always passed to the proxy. 
    * Then read what the protocol and the host were.
    * Use that as a new URI passed to toURI().relativize() which will return 
    *   the relative path the client is actually requesting.
    * This is then used to create a new first line in the header. Then the header is recreated.
    * 
    * @param request String - HTTP request meant for external host
    * @return String - Updated HTTP request.
    * @throws java.net.MalformedURLException thrown by URL() when a malformed URL is found.
    * @throws java.net.URISyntaxException thrown by toURI() when a String cant be parsed as a URI reference.
    */
    public static String fixAbsoluteURI(String request) throws MalformedURLException, URISyntaxException{
        // Parse updated clientRequest
        String[] clientReqArr = request.split(carriageReturn)[0].split(" "); // Split first line of client request
        String[] requestLines= request.split(carriageReturn);
        String urlRequested = clientReqArr[1];
        URL u = new URL(urlRequested);
        URL hostURL = new URL(u.getProtocol() + "://" + u.getHost());
        String relativePath = "/" + hostURL.toURI().relativize(u.toURI());
        clientReqArr[1] = relativePath;
        String newFirstLine = String.join(" ", clientReqArr) + carriageReturn;
        request = newFirstLine + String.join(carriageReturn, Arrays.copyOfRange(requestLines, 1, requestLines.length));
        return request;
    }
    
    /**
    * Runs the HTTP proxy server
    *
    * @param clientSocket Socket - Socket for connection to/from client.
    * @throws java.io.IOException thrown by closeSocketStuff()
    */
    private void run(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        try {
            clientOut = new DataOutputStream(this.clientSocket.getOutputStream());
            clientIn = new DataInputStream(this.clientSocket.getInputStream());

            String clientReqLine = clientIn.readLine(); // Read request from client
            if (clientReqLine == null) {
                clientIn.close();
                clientOut.close();
                return;
            }
            
            // Parse request to determine if internal or external
            String[] clientReqArr = clientReqLine.split(" "); // Split first line of client request
            String urlRequested = clientReqArr[1].trim(); 
            String httpMethod = clientReqArr[0].trim();
            String request = slurpInput(clientIn);
            String[] reqSplit = request.split(carriageReturn);
            String hostLine = getHostLine(reqSplit);
            String[] hostLineArr = splitHostLine(hostLine); // Split host line

            if (isInternalRequest(hostLineArr)) {
                handleInternalRequest(httpMethod, urlRequested);
            }
            else { // External: form request, send it out
                handleExternalRequest(request,urlRequested,hostLineArr,clientReqLine,httpMethod);
            }
            closeSocketStuff();
        } 
        catch (IOException | URISyntaxException | DataFormatException | ParserConfigurationException | CompressorException | SAXException e) {
            if (debug)
                e.printStackTrace();
        }
        finally {
            closeSocketStuff();
        }
    }
    
    /**
    * Closes clientIn, Out, ExternalIn, Out.
    * @throws java.io.IOException thrown by close()
    */
    public void closeSocketStuff() throws IOException{
        clientIn.close();
        clientOut.close();
        if (externalIn != null){
            externalIn.close();
        }
        if (externalOut != null){
            externalOut.close();
        }
    }
    
    /**
    * Gets the hostLine from the request, split by the CRLF.
    *
    * @param reqSplit String[] - HTTP request split by the CRLF.
    * @return hostLine String - Host line from request.
    */
    public String getHostLine(String[] reqSplit){
        String hostLine = "";
        for (String reqSplit1 : reqSplit) {
            if (reqSplit1.trim().toUpperCase().startsWith("HOST")) {
                return reqSplit1.trim();
            }
        }
        return hostLine;
    }
    
    /**
    * Get a line from the header specified in headerKey
    *
    * @param header String - The HTTP header
    * @param headerKey String - The thing in the header you want
    * @return String - The line containing the headerKey requested.
    */
    public String getHeaderValue(String header, String headerKey){
        String[] headerValues = header.split(carriageReturn);
        String headerVal = "";
        // Loop through header, get the line with headerKey
        for (String headerValue : headerValues) {
            if (headerValue.toUpperCase().contains(headerKey.toUpperCase())) {
                headerVal = headerValue.split(": ")[1];
            }
        }
        return headerVal;
    }
    
    /**
    * Fix various issues with some websites, such as:
    * http://blog.iosart.com/ (Probably web server issue)
    * http://wiu.edu (BigIP issue)
    *
    * @param header String - The HTTP header
    * @return String - The fixed HTTP header.
    */
    public String cleanLocationValue(String header){
        String newHeader = "";
        // Regexesies
        String wiuBigIPLocIssue = ".*https:\\/\\/.*http:\\/\\/.*";
        String iosartBlogLocIssue = ".*http:\\/\\/.*http\\/.*";
        String locationValue = getHeaderValue(header, "location");
        
        // Look at 'location' field url message with regex, check if matches
        Boolean isoartMatch = locationValue.matches(iosartBlogLocIssue);
        Boolean wiuMatch = locationValue.matches(wiuBigIPLocIssue);
        
        if(isoartMatch || wiuMatch){
            String[] headerLines = header.split(carriageReturn);
            for(String headerLine : headerLines){
                // Find line with location, fix bad redirect
                boolean containsLocation =  headerLine.toUpperCase().contains("location".toUpperCase());
                if(containsLocation){
                    if(wiuMatch)
                        newHeader += "Location: " + locationValue.split("http://")[0] + carriageReturn;
                    else if(isoartMatch)
                        newHeader += "Location: " + "http://" + locationValue.split("http/")[1] + carriageReturn;
                }
                else
                    newHeader += headerLine + carriageReturn;
            }
        } 
        else 
            newHeader = header;
        return newHeader;
    }
    
    /**
    * Read in a header and return back the Content-Length byte integer
    *
    * @param header String - The HTTP header.
    * @return Integer - The content-length value.
    */
    public int getContentLength(String header) {
        String contentLength = getHeaderValue(header, "content-length");
        if(contentLength.isEmpty()){
            return 0;
        } else{
            return Integer.parseInt(contentLength);
        }
    }
    
    /**
    * Return value in the content-type line
    *
    * @param header String - The HTTP header.
    * @return String - The content-type.
    */
    public String getContentType(String header) {
        return getHeaderValue(header, "content-type");
    }
    
    /**
    * Return value in the transfer-encoding line
    *
    * @param header String - The HTTP header.
    * @return String - The transfer-encoding.
    */
    public String getTransferEncoding(String header) {
        return getHeaderValue(header, "transfer-encoding");
    }
    
    /**
    * Return value in the content-encoding line
    *
    * @param header String - The HTTP header.
    * @return String - The content-encoding.
    */
    public String getContentEncoding(String header) {
        return getHeaderValue(header, "content-encoding");
    }
    
    /**
    * Look at header and determine if it's an HTML file
    *
    * @param header String - The HTTP header.
    * @return boolean - T/F if it contains 'text/html'.
    */
    public boolean isHTMLFile(String header){
        // Get content-type from header and see if it's 'text/html'
        String contentType = getContentType(header);
        if(contentType != null){
            Boolean isHTMLFile = contentType.toUpperCase().contains("TEXT/HTML");
            return isHTMLFile;
        } else{
            return false;
        }
    }
    
    /**
    * Split header first line, see if it has 200 in it, indicating OK response
    *
    * @param header String - The HTTP header.
    * @return boolean - T/F if the first line contains '200'.
    */
    public boolean isOKResponse(String header){
        String firstLine = header.split(carriageReturn)[0];
        if(firstLine != null){
            return firstLine.contains("200");
        } else{
            return false;
        }
    }
    
    /**
    * Startup duplexed TCP stream forwarding between the client and external
    * server for HTTPS fun times.
    *
    * @throws java.io.IOException thrown by closeSocketStuff()
    */
    public void setupConnection() throws IOException {
        String statusLine = "HTTP/1.1 200 OK" + carriageReturn; // response line

        Thread clientThread;
        clientThread = new Thread() {
            @Override
            public void run() {
                int offset;
                byte[] clientBuffer = new byte[1024];
                // Read client CONNECT request and send it out to host
                try {
                    while ((offset = clientSocket.getInputStream().read(clientBuffer)) != -1) {
                        externalOut.write(clientBuffer, 0, offset);
                        externalOut.flush();
                    }
                } catch (IOException e) {}
            }
        };
        clientThread.start();
        
        int offset;
        byte[] clientBuffer = new byte[1024];
        try {
            // Return 200 OK to client + response from external host
            clientOut.writeBytes(statusLine + carriageReturn);
            while ((offset = externalSocket.getInputStream().read(clientBuffer)) != -1) {
                clientOut.write(clientBuffer, 0, offset);
                clientOut.flush();
            }
        } catch (IOException e) {} 
        finally {
            closeSocketStuff();
        }
    }
    
    /**
    * Create external socket for requested host address and port.
    *
    * @param hostLineArr [0] = hostAddress; [1] = hostPort or undefined    *
    */
    public void initExternalSocket(String[] hostLineArr) {
        String hostIP;
        int hostPort;
        String hostAddr = hostLineArr[0].trim();

        // Get IP of requested host
        try {
            hostIP = InetAddress.getByName(hostAddr).getHostAddress();
        } catch (UnknownHostException e) {
            System.out.println("Unknown host: " + hostAddr);
            return;
        }

        // Get host port if specified, else 80
        if (hostLineArr.length > 1)
            hostPort = Integer.parseInt(hostLineArr[1]);
        else
            hostPort = 80;
        try{
            externalSocket = new Socket(hostIP, hostPort);
            externalOut = new DataOutputStream(this.externalSocket.getOutputStream());
            externalIn = new DataInputStream(this.externalSocket.getInputStream());
        }
        catch(IOException e){
            //System.out.println("External Socket Error:" + e);
        }
    }
    
    /**
    * Handle a request meant for proxy server
    *
    * @param httpMethod String - GET expected, other methods return 404 page.
    * @param urlRequested String - URL requested by client.
    * @throws java.net.URISyntaxException thrown by returnHTTPResponse()
    * @throws org.xml.sax.SAXException thrown by returnHTTPResponse()
    * @throws java.io.IOException thrown by returnHTTPResponse(), return400()
    * @throws javax.xml.parsers.ParserConfigurationException thrown by returnHTTPResponse()
    */
    public void handleInternalRequest(String httpMethod, String urlRequested) throws URISyntaxException, SAXException, IOException, ParserConfigurationException{
        if (httpMethod.toUpperCase().contains("GET")) 
            clientOut.writeBytes(BusinessLogic.returnHTTPResponse(urlRequested));
        else
            clientOut.writeBytes(BusinessLogic.return400(urlRequested));
    }
    
    /**
    * Handle a HTTP POST request
    * @param request String - HTTP POST request from client to external host.
    * @throws IOException thrown by handleChunkedInput(), write(), readFully()
    */
    public void handlePOST(String request) throws IOException {
        int contentLength = getContentLength(request);
        
        if(getTransferEncoding(request).contains("chunked")){
            // Read chunked response and send it to client
            byte[] unchunkedData = handleChunkedInput(request, clientSocket.getInputStream(), externalOut);
            externalOut.write(unchunkedData);
            externalOut.write(carriageReturn.getBytes());
        }
        else if (contentLength > 0) { 
            // For non-chunked requests, read POST data from client, send to external host.
            byte[] data = new byte[contentLength];
            clientIn.readFully(data);
            externalOut.write(data);        
        }
    }
    /**
    * Handle a request meant for an external host
    *
    * @param request String - HTTP request from client.
    * @param urlRequested String - URL requested by client.
    * @param hostLineArr String[] - [0] = hostAddress; [1] = hostPort or undefined.
    * @param clientReqLine String - First line of HTTP request from client.
    * @param httpMethod String - HTTP method from request (GET/POST/etc).
    * @throws java.io.IOException thrown by writeBytes(), handlePOST(), slurpResponse()
    * @throws java.util.zip.DataFormatException thrown by slurpResponse()
    * @throws org.apache.commons.compress.compressors.CompressorException thrown by slurpResponse()
    * @throws java.net.URISyntaxException thrown by fixAbsoluteURI()
    */
    public void handleExternalRequest(String request, String urlRequested, String[] hostLineArr, String clientReqLine, String httpMethod) throws IOException, DataFormatException, CompressorException, URISyntaxException {
        initExternalSocket(hostLineArr);
        if (externalSocket == null) 
            return;
          
        // Handle CONNECTs by creating connection to both ends
        if (httpMethod.toUpperCase().contains("CONNECT")) {
            setupConnection();
            return;
        }
        request = clientReqLine + carriageReturn + request;
        request = removeKeepAlive(request);
        request = fixAbsoluteURI(request) + carriageReturn;
        
        System.out.println("\n[NEW REQUEST FOR EXTERNAL HOST]:\n" + request);
        externalOut.writeBytes(request + carriageReturn); // Send request out 

        if (httpMethod.toUpperCase().contains("POST"))
            handlePOST(request);

        System.out.println("\n[REQUEST SENT TO EXTERNAL HOST]");
        String response = slurpResponse(urlRequested); // Get response from external host
        System.out.println("\n[RESPONSE FROM EXTERNAL HOST]:\n" + response);
    }
    
    /**
    * Return the correct localIP without returning
    *  the loopback (or some other adapter) IP.
    * InetAddress.getLocalHost().getHostAddress() will return 127.0.0.1 in some cases.
    * 
    * Using "192.168.1.1" below can use the wrong default gateway sometimes,
    *  so we're using Cloudflare's DNS IP.
    * 
    * Credit for this solution: https://stackoverflow.com/a/2381398
    * @return String - IP address of local machine.
    * @throws java.io.IOException thrown by Socket()
    */
    public String getLocalIP() throws IOException{
        String localIP;
        try (Socket s = new Socket("1.1.1.1", 80)) {
            localIP = s.getLocalAddress().getHostAddress();
        }
        return localIP.trim();
    }
    
    /**
    * Check to see if the request is meant to be for -this- server
    *
    * @param hostAddressArr String[] - [0] = hostAddress; [1] = hostPort or undefined
    * @return boolean - T/F if request sent is considered internal.
    * @throws java.io.IOException thrown by getLocalIP()
    *
    */
    public boolean isInternalRequest(String[] hostAddressArr) throws IOException {
        boolean localPortMatch;

        if (hostAddressArr.length > 1) {
            // True if local port and client requested port match
            localPortMatch = this.clientSocket.getLocalPort() == Integer.parseInt(hostAddressArr[hostAddressArr.length - 1].trim());
        } else {
            // See if port is 80
            localPortMatch = this.clientSocket.getLocalPort() == 80;
        }
        String localIPAddress = getLocalIP();
        // (ip same -- localhost or hostaddress) and port same, internal processing
        boolean localIPMatch = localIPAddress.equals(hostAddressArr[0].trim()) || hostAddressArr[0].trim().equals("127.0.0.1");
        // Returns true if above conditions met
        return localPortMatch && localIPMatch;
    }
    
    /**
    * Read in and split the host header line. Removes 'Host' from the array and
    * returns the requested host only. If a port number is specified in the
    * request, this will be the last element of the array.
    *
    * @param hostLine String - The host header line.
    * @return String[] - The host header value, split on ":".
    */
    public String[] splitHostLine(String hostLine) {
        String[] splitHostLine = hostLine.split(":");
        String[] hostAddressArr = Arrays.copyOfRange(splitHostLine, 1, splitHostLine.length); // Remove first element of array
        return hostAddressArr;
    }

    /**
    * Starts up this Proxy server using a defined port number and begins a new
    * thread for each new connection.
    *
    * @param portnum Integer - the port number of this server 
    */
    public static void setup(int portnum) {
        System.out.println("[STARTING PROXY]");
        try {
            //Create server socket
            ServerSocket svrSocket = new ServerSocket(portnum);
            while (true) {
                //Accept client request, this returns a local Socket
                //to communicate with the client
                Socket clientSocket = svrSocket.accept();
                new Thread(() -> {
                    try {
                        Proxy proxyServer = new Proxy(clientSocket);
                    } catch (IOException ex) {
                        Logger.getLogger(Proxy.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }).start();
            }
        } catch (BindException e) {
            System.out.println("[THIS PORT IS ALREADY IN USE]");
        } catch(IOException e){}
    }
}
