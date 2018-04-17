package com.kru13.httpserver;

import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

// ADB je soucasti SDK, cestu naleznete v local.properties
// presmerovani portu na hostitelsky pocitac
// platform-tools/adb.exe forward tcp:12345 tcp:12345

// ladeni
// PuTTY  (connection type RAW,  IP 127.0.0.1 PORT 12345

// C:\Users\kucabpet\AppData\Local\Android\Sdk\platform-tools\adb.exe forward tcp:12345 tcp:12345


public class SocketServer extends Thread {

    private ServerSocket serverSocket;

    private final String storageRoot = Environment.getExternalStorageDirectory().getPath();
    public final int port = 12345;
    private boolean bRunning;

    public void close() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Log.d("SERVER", "Error, probably interrupted in accept(), see log");
            e.printStackTrace();
        }
        bRunning = false;
    }

    // url = file path or whatever suitable URL you want.
    public static String getMimeType(String url) {
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

    public void run() {
        try {
            Log.d("SERVER", "Creating Socket");
            serverSocket = new ServerSocket(port);
            bRunning = true;

            while (bRunning) {
                Log.d("SERVER", "Socket Waiting for connection");
                Socket s = serverSocket.accept();
                Log.d("SERVER", "Socket Accepted");

                processRequest(s);

                s.close();
                Log.d("SERVER", "Socket Closed");
            }
        } catch (IOException e) {
            if (serverSocket != null && serverSocket.isClosed())
                Log.d("SERVER", "Normal exit");
            else {
                Log.d("SERVER", "Error");
                e.printStackTrace();
            }
        } finally {
            serverSocket = null;
            bRunning = false;
        }
    }

    private void processRequest(Socket s) throws IOException {

        InputStream in = s.getInputStream();
        OutputStream out = s.getOutputStream();

        try {
            Data payload = new Data();
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

    private void printHeaders(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        String tmp = null;
        while ((tmp = reader.readLine()) != null) {
            System.out.println(tmp);
        }

    }

    private void processResponse(HttpStatus httpStatus, ArrayList<String> http_req, Data payload, OutputStream out) throws IOException {
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

    private void processPost(ArrayList<String> http_req, Data payload, OutputStream out) throws IOException {

        String requestURI = http_req.get(0).split(" ")[1];

        if ("/uploadFile".equals(requestURI)) {
            processUploadFile(http_req, payload, out);
        }

        processOkResponse(out, "");
    }

    private void processUploadFile(ArrayList<String> http_req, Data payload, OutputStream out) throws IOException {

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

        FileOutputStream fos = new FileOutputStream(storageRoot + "/file");
        fos.write(d.getBytes("UTF-8"));
        fos.flush();

        // todo filename + to folder storageRoot/Upload
    }

    private void loadRequestPayloadAndData(ArrayList<String> http_req, InputStream in, OutputStream out, String boundary) throws IOException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        String tmp = reader.readLine();

        if (boundary.equals(tmp)) {
            Log.d("REQUEST_PAYLOAD", "Found start boundary token");
        }

        while (!(tmp = reader.readLine()).isEmpty()) {
            Log.d("HTTP_REQ", tmp);
            http_req.add(tmp);
        }


    }

    private int getContentLength(ArrayList<String> http_req) {

        String len = null;

        for (String s : http_req) {
            if (s.contains("Content-Length:")) {
                len = s.split("Content-Length:")[1];
            }
        }

        if (len != null) {
            return Integer.parseInt(len.trim());
        }

        return -1;
    }

    private void processContinue(OutputStream out) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));

        writer.write("100 Continue");
        writer.newLine();

//        writer.newLine();

        writer.flush();
    }

    private void processRedirect(OutputStream os, ArrayList<String> http_req, String path) throws IOException {
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

    private String parseUploadFileNameFromURI(String requestURI) {

        int questingMarkIndex = requestURI.indexOf("?");

        if (questingMarkIndex == -1) {
            return null;
        }

        String[] args = requestURI.substring(questingMarkIndex + 1).split("&");

        String fileName = null;

        for (String s : args) {
            String[] data = s.split("=");
            String key = data[0];

            if ("fileName".equals(key)) {
                fileName = data[1];
                break;
            }
        }

        return fileName;
    }

    private void processGet(ArrayList<String> http_req, OutputStream os) throws IOException {

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
                    processOkResponse(os, attemptIndex);
                    writeFileToResponse(os, attemptIndex);

                } else {
                    Log.d("RESPONSE", "directory listing");
                    File directory = new File(storageRoot + fileName);

                    // check if target directory exist
                    if (!directory.exists()) {
                        processNotFoundResponse(os, directory);
                    }

                    String httpBody = createDirectoryListing(directory, Environment.getExternalStorageDirectory().getPath());
                    processOkResponse(os, httpBody);
                }
            } else {
                processOkResponse(os, targetFile);
                writeFileToResponse(os, targetFile);
            }
        } else {
            processNotFoundResponse(os, targetFile);
        }
    }

    private void writeFileToResponse(OutputStream os, File targetFile) throws IOException {
        FileInputStream fis = new FileInputStream(targetFile);
        byte buffer[] = new byte[1024];
        int len;

        while ((len = fis.read(buffer, 0, 1024)) > 0) {
            os.write(buffer, 0, len);
        }
        os.flush();
    }

    private void processNotFoundResponse(OutputStream os, File targetFile) throws IOException {
        Log.d("HTTP", "400 NotFound");
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os));

        out.write("HTTP/1.0 400 NotFound\n");
        out.write("\n");
        out.write("<html><body>File: " + targetFile.getName() + " Not Found</body></html>");
        out.flush();
    }

    private String createDirectoryListing(File directory, String rootPath) {
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

    private void processOkResponse(OutputStream os, File f) throws IOException {
        Log.d("HTTP", "200 OK");
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os));

        out.write("HTTP/1.0 200 OK\n");
        out.write("Content-Type: " + getMimeType(f.getName()) + "\n");
        out.write("Content-Length: " + String.valueOf(f.length()) + "\n");
        out.write("\n");
        out.flush();
    }

    private void processOkResponse(OutputStream os, String body) throws IOException {
        Log.d("HTTP", "200 OK");
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os));

        out.write("HTTP/1.0 200 OK\n");
        out.write("Content-Type: text/html\n");
        out.write("\n");
        out.write(body);
        out.flush();
    }

    private void processBadResponse(OutputStream o, String msg) throws IOException {

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

    private void processBadResponse(OutputStream o) throws IOException {
        processBadResponse(o, "");
    }

    private void getHeadersAndPayload(InputStream is, ArrayList<String> http_req, Data data) throws IOException {
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

    private String parseBoundary(String token) {
        String[] args = token.split("boundary=");

        if (args.length > 0) {
            return args[1];
        }

        return null;
    }

}