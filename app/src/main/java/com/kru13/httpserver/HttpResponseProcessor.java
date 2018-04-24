package com.kru13.httpserver;

import android.os.Environment;
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
    static String getMimeType(String url) {
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

    void processOkResponse(OutputStream os, File f) throws IOException {
        Log.d("HTTP", "200 OK");
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os));

        out.write("HTTP/1.0 200 OK\n");
        out.write("Content-Type: " + getMimeType(f.getName()) + "\n");
        out.write("Content-Length: " + String.valueOf(f.length()) + "\n");
        out.write("\n");
        out.flush();
    }

    void processOkResponse(OutputStream os, String body) throws IOException {
        Log.d("HTTP", "200 OK");
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os));

        out.write("HTTP/1.0 200 OK\n");
        out.write("Content-Type: text/html\n");
        out.write("Content-Length: " + body.length() + "\n");
        out.write("\n");
        out.write(body);
        out.flush();
    }

    void processBadResponse(OutputStream o, String msg) throws IOException {

        Log.d("HTTP", "400 Bad request");
//        BufferedWriter out = null;

//        try {
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
//        } finally {
//            if (out != null) {
//                out.close();
//            }
//        }
    }

    void writeFileToResponse(OutputStream os, File targetFile) throws IOException {
        FileInputStream fis = new FileInputStream(targetFile);
        byte buffer[] = new byte[1024];
        int len;

        while ((len = fis.read(buffer, 0, 1024)) > 0) {
            os.write(buffer, 0, len);
        }
        os.flush();
    }

    void processNotFoundResponse(OutputStream os, File targetFile) throws IOException {
        Log.d("HTTP", "400 NotFound");
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os));

        out.write("HTTP/1.0 400 NotFound\n");
        out.write("\n");
        out.write("<html><body>File: " + targetFile.getName() + " Not Found</body></html>");
        out.flush();
    }

    void processContinue(OutputStream out) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));

        writer.write("100 Continue");
        writer.newLine();

//        writer.newLine();

        writer.flush();
    }

    void processRedirect(OutputStream os, ArrayList<String> http_req, String path) throws IOException {
        Log.d("HTTP", "302 OK");

        String location = "";

        for (String h : http_req) {
            if (h.contains("Host")) {
                location = h.split(":")[1];
            }
        }

        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os));

        out.write("HTTP/1.0 301 OK\n");
        out.write("Location: " + location + "/" + path);
        out.write("\n");
        out.flush();
    }

}
