package com.kru13.httpserver.util;

import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ImageUtil {
    public static String convertToBase64(File f) throws IOException {

        FileInputStream fis = new FileInputStream(f);
        int available = fis.available();
        byte[] bytes = new byte[available];
        int read = fis.read(bytes, 0, available);

        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }
}
