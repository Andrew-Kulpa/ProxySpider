/** 
* Decompressor
* @author Andrew Kulpa & Darren Wolbers
* Decompress data based on content-encoding.
*/
package spiderproxy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

public class Decompressor {
    /**
    * Handle a HTTP POST request
    * https://commons.apache.org/proper/commons-compress/apidocs/org/apache/commons/compress/compressors/CompressorStreamFactory.html
    * 
    * @param data byte[] - Data from request to be decoded.
    * @param contentEncoding String - Content encoding from request.
    * @return byte[] - Decoded data.
    * @throws java.util.zip.DataFormatException thrown when contentEncoding from request is not supported.
    * @throws org.apache.commons.compress.compressors.CompressorException thrown by CompressorStreamFactory.
    * @throws IOException thrown by read(), close(), flush()
    */
    public static byte[] decode(byte[] data, String contentEncoding) throws DataFormatException, CompressorException, IOException{
        ByteArrayInputStream is = new ByteArrayInputStream(data);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        CompressorInputStream in = null;

        switch (contentEncoding) {
            case "compress":
                in = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.BROTLI, is);
                break;
            case "gzip":
                in = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.GZIP, is);
                break;
            case "deflate":
                in = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.DEFLATE, is);
                break;
            case "identity": // Not compressed?
                return data;
            case "br":
                in = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.BROTLI, is);
                break;
            case "xz":
                in = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.XZ, is);
                break;
            default:
                throw new DataFormatException("Unsupported content-encoding: " + contentEncoding);
        }
        
        if(in == null)
            return data;
        int offset; 
        int bufferSize = 1; // Works for now
        byte buffer[] = new byte[bufferSize];
        
        while((offset=in.read(buffer, 0, bufferSize))!= -1)
            os.write(buffer, 0, offset);
        byte[] newData = os.toByteArray();
        is.close();
        os.flush();
        os.close();
        return newData;
    }
    
    /**
    * Decode data if there's a contentEncodingString
    * 
    * @param data byte[] - Data from request.
    * @param contentEncodingString String - Indicating encoding from request.
    * @return String - Decoded data converted to a String.
    * @throws java.util.zip.DataFormatException thrown by decode()
    * @throws org.apache.commons.compress.compressors.CompressorException thrown by decode()
    * @throws IOException thrown by decode()
    */
    public static String decompress(byte[] data, String contentEncodingString) throws DataFormatException, CompressorException, IOException{
        System.out.println("Decompressing data encoded as: " + contentEncodingString);
        byte[] newData = data;
        if(contentEncodingString.isEmpty() || contentEncodingString.trim().equals(","))
            return new String(newData);
        for(String contentEncoding : contentEncodingString.split(",")){
            if(contentEncoding.isEmpty()) 
                continue;
            newData = decode(newData, contentEncoding.trim().toLowerCase());
        }
        // Convert byte[] to String
        String decompressedData = new String(newData);
        return decompressedData;
    }
}
