package com.kru13.httpserver.http;

import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class HttpResponseProcessor {

    // url = file path or whatever suitable URL you want.
    private static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        if (type == null) {
            type = "application/x-binary";
        }

        return type;
    }

    static void processOkResponse(OutputStream os, File f) throws IOException {
        Log.d("HTTP", "200 OK");
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os));

        out.write("HTTP/1.0 200 OK\n");
        out.write("Content-Type: " + getMimeType(f.getName()) + "\n");
        out.write("Content-Length: " + String.valueOf(f.length()) + "\n");
        out.write("\n");
        out.flush();
    }

    static void processOkResponse(OutputStream os, String body) throws IOException {
        Log.d("HTTP", "200 OK");
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os));

        out.write("HTTP/1.0 200 OK\n");
        out.write("Content-Type: text/html\n");
        out.write("Content-Length: " + body.length() + "\n");
        out.write("\n");
        out.write(body);
        out.flush();
    }

    static void processBadResponse(OutputStream o, String msg) throws IOException {

        Log.d("HTTP", "400 Bad request");
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o));

        out.write("HTTP/1.0 400 Bad Request\n");
        out.write("Content-Type: text/html\n");
        out.write("\n");
        out.write("<html><body>");
        out.write("<h3>Bad Request</h3>");
        out.write("<h4>Message:</h4>");
        out.write("<p>" + msg + "</p>");
        out.write("</body></html>");
        out.flush();
    }

    static void writeFileToResponse(OutputStream os, File targetFile) throws IOException {
        FileInputStream fis = new FileInputStream(targetFile);
        byte buffer[] = new byte[1024];
        int len;

        while ((len = fis.read(buffer, 0, 1024)) > 0) {
            os.write(buffer, 0, len);
        }
        os.flush();
    }

    static void processNotFoundResponse(OutputStream os, File targetFile) throws IOException {
        Log.d("HTTP", "400 NotFound");
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os));

        out.write("HTTP/1.0 400 NotFound\n");
        out.write("\n");
        out.write("<html><body>File: " + targetFile.getName() + " Not Found</body></html>");
        out.flush();
    }

}
