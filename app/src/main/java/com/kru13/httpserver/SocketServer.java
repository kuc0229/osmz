package com.kru13.httpserver;

import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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


public class SocketServer extends Thread {

    ServerSocket serverSocket;
    public final int port = 12345;
    boolean bRunning;

    public void close() {
        try {
            serverSocket.close();
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

                OutputStream o = s.getOutputStream();
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o));
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

                ArrayList<String> http_req = new ArrayList<String>();
                String tmp;

                while (!(tmp = in.readLine()).isEmpty()) {
                    Log.d("HTTP_REQ", tmp);
                    http_req.add(tmp);
                }

                String fileName = "";
                if (http_req.get(0).contains("GET")) {
                    fileName = http_req.get(0).split(" ")[1];
                    Log.d("REQ_FILE", fileName);
                }

                if ("/".equals(fileName)) {
                    fileName = "/index.htm";
                }

                File f = new File(Environment.getExternalStorageDirectory().getPath() + fileName);

                Log.d("FILE_PATH", Environment.getExternalStorageDirectory().getPath() + fileName);

                if (f.exists()) {
                    Log.d("HTTP", "200 OK");
                    out.write("HTTP/1.0 200 OK\n");
                    out.write("Content-Type: " + getMimeType(fileName) + "\n");
                    out.write("Content-Length: " + String.valueOf(f.length()) + "\n");
                    out.write("\n");
                    out.flush();

                    FileInputStream fis = new FileInputStream(f);
                    byte buffer[] = new byte[1024];
                    int len;

                    while ((len = fis.read(buffer, 0, 1024)) > 0) {
                        o.write(buffer, 0, len);
                    }

                    o.flush();

                } else {
                    Log.d("HTTP", "404 Not Found");
                    out.write("HTTP/1.0 404 Not Found\n");
                    out.write("Content-Type: text/html\n");
                    out.write("\n");
                    out.write("<html><body>File not found</body></html>");
                    out.flush();
                }

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

}