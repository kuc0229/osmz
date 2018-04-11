package com.kru13.httpserver;

import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

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

        OutputStream out = s.getOutputStream();

        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(s.getInputStream()));

            ArrayList<String> http_req = getHeaders(in);

            if (http_req.isEmpty()) {
                processResponse(HttpStatus.BAD_REQUEST, http_req, in, out);
                return;
            }

            String httpMethod = http_req.get(0);

            if (httpMethod.contains("GET")) {
                processResponse(HttpStatus.GET, http_req, in, out);
            }

        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private void processResponse(HttpStatus httpStatus, ArrayList<String> http_req, BufferedReader in, OutputStream out) throws IOException {
        switch (httpStatus) {
            case BAD_REQUEST:
                processBadRequestResponse(out);
                break;
            case GET:
                processGetResponse(http_req, out);
                break;

        }
    }

    private void processGetResponse(ArrayList<String> http_req, OutputStream os) throws IOException {

        String fileName;
        fileName = http_req.get(0).split(" ")[1];

        Log.d("REQ_FILE", fileName);

        if (fileName.isEmpty()) {
            processBadRequestResponse(os);
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

    private void processBadRequestResponse(OutputStream o, String msg) throws IOException {

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

    private void processBadRequestResponse(OutputStream o) throws IOException {
        processBadRequestResponse(o, "");
    }

    private ArrayList<String> getHeaders(BufferedReader in) throws IOException {
        ArrayList<String> http_req = new ArrayList<String>();

        String tmp = in.readLine();
        if (tmp != null) {
            while (!tmp.isEmpty()) {
                Log.d("HTTP_REQ", tmp);
                http_req.add(tmp);
                tmp = in.readLine();
            }
        }

        return http_req;
    }

}