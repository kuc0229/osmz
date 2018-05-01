package com.kru13.httpserver.http;

import android.os.Environment;
import android.util.Log;

import com.kru13.httpserver.model.DataWrapper;
import com.kru13.httpserver.enums.HttpStatus;
import com.kru13.httpserver.http.HttpResponseProcessor;

import org.apache.http.protocol.HttpProcessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ResponseProcessor {

    public final String uploadDir = File.separator + "Upload";
    public final String storageRoot = Environment.getExternalStorageDirectory().getPath();

    private List<String> http_req;

    public ResponseProcessor() {
    }

    public void processRequest(Socket s) throws IOException {

        InputStream in = s.getInputStream();
        OutputStream out = s.getOutputStream();

        try {
            DataWrapper payload = new DataWrapper();
            http_req = new ArrayList<String>();

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

    public void processResponse(HttpStatus httpStatus, List<String> http_req, DataWrapper payload, OutputStream out) throws IOException {
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

    public void processPost(List<String> http_req, DataWrapper payload, OutputStream out) throws IOException {

        String requestURI = http_req.get(0).split(" ")[1];

        if ("/uploadFile".equals(requestURI)) {
            processUploadFile(http_req, payload, out);
        }
    }

    private void processCommand(String command, OutputStream out) throws IOException {

        List<String> splittedCommand = parseCommand(command);
        Log.d("RESPONSE PROCESSOR", "Parsed command " + splittedCommand);

        try {
            Process start = new ProcessBuilder(splittedCommand).start();
            int retCode = start.waitFor();
            int available = start.getInputStream().available();
            byte[] buffer = new byte[available];
            int read = start.getInputStream().read(buffer, 0, available);
            String data;
            if (read > 0) {
                data = new String(buffer);
                data += "\n\nExit code: " + retCode;
                HttpResponseProcessor.processOkResponse(out, HttpResponseProcessor.createHtmlBody(data, true));
            } else {
                data = "No data.\n\nExit code: " + retCode;
                HttpResponseProcessor.processOkResponse(out, HttpResponseProcessor.createHtmlBody(data, true));
            }
        } catch (IOException e) {
            Log.d("RESPONSE PROCESSOR", "error during process command " + e);
            HttpResponseProcessor.processBadResponse(out, HttpResponseProcessor.createHtmlBody("Error " + e, false));
        } catch (InterruptedException e) {
            Log.d("RESPONSE PROCESSOR", "interrupted process command " + e);
            HttpResponseProcessor.processBadResponse(out, HttpResponseProcessor.createHtmlBody("Error " + e, false));
        }
    }

    private static List<String> parseCommand(String command) {
        List<String> commandSplited = new ArrayList<String>(10);

        StringBuilder buffer = new StringBuilder();
        boolean waitForDelimiter = false;

        for (int i = 0; i < command.length(); i++) {
            char current = command.charAt(i);

            switch (current) {
                case ' ':
                    if (waitForDelimiter) {
                        buffer.append(' ');
                        continue;
                    } else {
                        if (buffer.length() > 0) {
                            commandSplited.add(buffer.toString());
                            buffer = new StringBuilder();
                        }
                        break;
                    }
                case '"':
                    if (waitForDelimiter) {
                        commandSplited.add(buffer.toString());
                        buffer = new StringBuilder();
                        waitForDelimiter = false;
                    } else {
                        waitForDelimiter = true;
                        buffer = new StringBuilder();
                    }
                    break;
                default:
                    buffer.append(current);
            }

        }

        if (buffer.length() > 0) {
            commandSplited.add(buffer.toString());
        }
        return commandSplited;
    }

    public void processUploadFile(List<String> http_req, DataWrapper payload, OutputStream out) throws IOException {

        char[] data = payload.getData();

        String startBoundary;
        String endBoundary = null;

        for (String header : http_req) {
            if (header.startsWith("Content-Type: ")) {
                startBoundary = "--" + header.split("boundary=")[1];
                endBoundary = startBoundary + "--";
            }
        }

        if (endBoundary == null) {
            Log.d("RESPONSE PROCESSOR", "could not found boundary");
            HttpResponseProcessor.processBadResponse(out, "Error during processing payload.");
            return;
        }

        String stringData = String.valueOf(data, 0, data.length);

        int contentDispositionStart = stringData.indexOf("Content-Disposition: ");
        int contentDispositionEnd = stringData.indexOf("\r\n", contentDispositionStart);
        String dispositionHeader = stringData.substring(contentDispositionStart, contentDispositionEnd);

        int contentTypeStart = stringData.indexOf("Content-Type: ");
        int contentTypeEnd = stringData.indexOf("\r\n", contentTypeStart);
        String contentTypeHeader = stringData.substring(contentTypeStart, contentTypeEnd);

        int dataStart = stringData.indexOf("\r\n\r\n") + 4;
        int dataEnd = stringData.indexOf(endBoundary, dataStart);
        if (dataEnd > -1) {
            Log.d("RESPONSE PROCESSOR", "data start:" + dataStart + " data end: " + dataEnd);
        } else {
            Log.d("RESPONSE PROCESSOR", "data end boundary not found");
            dataEnd = stringData.length() - 2 - endBoundary.length();
        }
        String d = stringData.substring(dataStart, dataEnd);

        String fileName = getFileName(dispositionHeader);

        FileOutputStream fos = new FileOutputStream(storageRoot + uploadDir + File.separator + fileName);
        fos.write(d.getBytes());
        fos.flush();

        HttpResponseProcessor.processOkResponse(out,
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

    public void processGet(List<String> http_req, OutputStream out) throws IOException {

        String requestURI = http_req.get(0).split(" ")[1];

        if (requestURI.startsWith("/cgi-bin/")) {
            String[] parse = requestURI.split("/cgi-bin/");
            if (!(parse.length > 1)) {
                HttpResponseProcessor.processBadResponse(out, "Bad URL. Usage /cgi-bin/&lt;command&gt;");
                return;
            }
            String command = URLDecoder.decode(parse[1], "UTF-8");
            processCommand(command, out);
            return;
        }

        processGetFile(http_req, out);

    }

    private void processGetFile(List<String> http_req, OutputStream os) throws IOException {
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
                    HttpResponseProcessor.processOkResponse(os, attemptIndex);
                    HttpResponseProcessor.writeFileToResponse(os, attemptIndex);

                } else {
                    Log.d("RESPONSE", "directory listing");
                    File directory = new File(storageRoot + fileName);

                    // check if target directory exist
                    if (!directory.exists()) {
                        HttpResponseProcessor.processNotFoundResponse(os, directory);
                    }

                    String httpBody = createDirectoryListing(directory, Environment.getExternalStorageDirectory().getPath());
                    HttpResponseProcessor.processOkResponse(os, httpBody);
                }
            } else {
                HttpResponseProcessor.processOkResponse(os, targetFile);
                HttpResponseProcessor.writeFileToResponse(os, targetFile);
            }
        } else {
            HttpResponseProcessor.processNotFoundResponse(os, targetFile);
        }
    }

    public void processBadResponse(OutputStream o) throws IOException {
        HttpResponseProcessor.processBadResponse(o, "");
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

    void getHeadersAndPayload(InputStream is, List<String> http_req, DataWrapper data) throws IOException {
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

    public List<String> getHttp_req() {
        return http_req;
    }
}