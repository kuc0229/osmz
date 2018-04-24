package com.kru13.httpserver;

import android.os.Environment;
import android.util.Log;

import com.kru13.httpserver.entities.DataWrapper;
import com.kru13.httpserver.enums.HttpStatus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class ResponseProcessor {

    static int c = 8000;

    public final String uploadDir = File.separator + "Upload";
    public final String storageRoot = Environment.getExternalStorageDirectory().getPath();

    private HTTPResponseProcessor responseProcessor;

    public ResponseProcessor() {
        this.responseProcessor = new HTTPResponseProcessor();
    }

    void processRequest(Socket s) throws IOException {

        InputStream in = s.getInputStream();
        OutputStream out = s.getOutputStream();

        try {
            DataWrapper payload = new DataWrapper();
            ArrayList<String> http_req = new ArrayList<String>();

            getHeadersAndPayload(in, http_req, payload);


            if (http_req.isEmpty()) {
                processResponse(HttpStatus.BAD_REQUEST, http_req, payload, out);
                return;
            }

            String httpMethod = http_req.get(0);

            if (httpMethod.contains("GET")) {
                processResponse(HttpStatus.GET, http_req, payload, out);
            }

            if (httpMethod.contains("POST")) {
                processResponse(HttpStatus.POST, http_req, payload, out);
            }

        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    void processResponse(HttpStatus httpStatus, ArrayList<String> http_req, DataWrapper payload, OutputStream out) throws IOException {
        switch (httpStatus) {
            case BAD_REQUEST:
                processBadResponse(out);
                break;
            case GET:
                processGet(http_req, out);
                break;
            case POST:
                processPost(http_req, payload, out);

        }
    }

    void processPost(ArrayList<String> http_req, DataWrapper payload, OutputStream out) throws IOException {

        String requestURI = http_req.get(0).split(" ")[1];

        if ("/uploadFile".equals(requestURI)) {
            processUploadFile(http_req, payload, out);
        }

        responseProcessor.processOkResponse(out, "");
    }

    void processUploadFile(ArrayList<String> http_req, DataWrapper payload, OutputStream out) throws IOException {

        char[] data = payload.getData();

        String startBoundary = null;
        String endBoundary = null;
        String filename = null;
        int contentLength = 0;

        for (String header : http_req) {

            if (header.startsWith("Content-Length:")) {
                contentLength = Integer.parseInt(header.split(": ")[1]);
            }

            if (header.startsWith("Content-Type: ")) {
                startBoundary = "--" + header.split("boundary=")[1];
                endBoundary = startBoundary + "--";
            }
        }

        String stringData = String.valueOf(data, 0, data.length);

        int contentDispozitionStart = stringData.indexOf("Content-Disposition: ");
        int contentDispozitionEnd = stringData.indexOf("\r\n", contentDispozitionStart);
        String dispozitionHeader = stringData.substring(contentDispozitionStart, contentDispozitionEnd);

        int contentTypeStart = stringData.indexOf("Content-Type: ");
        int contentTypeEnd = stringData.indexOf("\r\n", contentTypeStart);
        String contentTypeHeader = stringData.substring(contentTypeStart, contentTypeEnd);

        int dataStart = stringData.indexOf("\r\n\r\n") + 4;
        int dataEnd = stringData.indexOf(endBoundary) - 1;
        String d = stringData.substring(dataStart, dataEnd);

        String fileName = getFileName(dispozitionHeader);

        FileOutputStream fos = new FileOutputStream(storageRoot + uploadDir + File.separator + fileName);
        fos.write(d.getBytes());
        fos.flush();

        responseProcessor.processOkResponse(out,
                "<html><body><p>Upload successful</p><p><a href='/'>Back to root directory</a></p></body></html>");
    }

    String getFileName(String dispositionHeader) {

        String[] split = dispositionHeader.split("filename=");

        if (split.length > 1) {
            String fileName = split[1];
            // trim quotation marks
            return fileName.substring(1, fileName.length() - 1);
        } else {
            return "file";
        }
    }

    void processGet(ArrayList<String> http_req, OutputStream os) throws IOException {

        String fileName;
        fileName = http_req.get(0).split(" ")[1];

        Log.d("REQ_FILE", fileName);

        if (fileName.isEmpty()) {
            processBadResponse(os);
            return;
        }

        File targetFile = new File(storageRoot + fileName);
        Log.d("FILE_PATH", storageRoot + fileName);

        if (targetFile.exists()) {

            if (targetFile.isDirectory()) {
                File attemptIndex = new File(storageRoot + fileName + File.separatorChar + "index.htm");

                // check if exists index.htm in directory otherwise listing directory
                if (attemptIndex.exists() && !attemptIndex.isDirectory()) {
                    responseProcessor.processOkResponse(os, attemptIndex);
                    responseProcessor.writeFileToResponse(os, attemptIndex);

                } else {
                    Log.d("RESPONSE", "directory listing");
                    File directory = new File(storageRoot + fileName);

                    // check if target directory exist
                    if (!directory.exists()) {
                        responseProcessor.processNotFoundResponse(os, directory);
                    }

                    String httpBody = createDirectoryListing(directory, Environment.getExternalStorageDirectory().getPath());
                    responseProcessor.processOkResponse(os, httpBody);
                }
            } else {
                responseProcessor.processOkResponse(os, targetFile);
                responseProcessor.writeFileToResponse(os, targetFile);
            }
        } else {
            responseProcessor.processNotFoundResponse(os, targetFile);
        }
    }

    void processBadResponse(OutputStream o) throws IOException {
        responseProcessor.processBadResponse(o, "");
    }

    String createDirectoryListing(File directory, String rootPath) {
        File[] files = directory.listFiles();
        StringBuilder body = new StringBuilder(1000);
        body.append("<html><body><ul>");

        for (File f : files) {
            body.append("<li>");
            body.append("<a href='");
            body.append(f.getPath().substring(rootPath.length()));
            body.append("'>");
            body.append(f.getName());
            body.append("</a>");
            body.append("</li>");
        }

        body.append("</ul></body></html>");

        return body.toString();
    }

    void getHeadersAndPayload(InputStream is, ArrayList<String> http_req, DataWrapper data) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        boolean isContainData = false;
        int contentLength = 0;

        String tmp = reader.readLine();
        if (tmp != null) {
            while (!tmp.isEmpty()) {
                Log.d("HTTP_REQ", tmp);
                http_req.add(tmp);
                tmp = reader.readLine();

                if (tmp.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(tmp.split(": ")[1]);
                }
            }
        }

        if (contentLength > 0) {
            char[] buffer = new char[contentLength];
            int read = reader.read(buffer, 0, contentLength);

            if (read > 0) {
                data.setData(buffer);
            } else {
                Log.d("HTTP_REQ", "Could not read request payload");
            }
        }
    }

}