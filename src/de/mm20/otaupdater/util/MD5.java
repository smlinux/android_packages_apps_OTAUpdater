package de.mm20.otaupdater.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 {
    public static String getMd5(String path) {
        try {
            InputStream inputStream = new FileInputStream(path);
            byte[] buffer = new byte[1024];
            MessageDigest digest = MessageDigest.getInstance("MD5");
            int numRead = 0;
            while (numRead != -1) {
                numRead = inputStream.read(buffer);
                if (numRead > 0)
                    digest.update(buffer, 0, numRead);
            }
            byte[] md5Bytes = digest.digest();
            String md5 = "";
            for (byte md5Byte : md5Bytes) {
                md5 += Integer.toString((md5Byte & 0xff) + 0x100, 16).substring(1);
            }
            inputStream.close();
            return md5.toLowerCase();
        } catch (NoSuchAlgorithmException | IOException e) {
            return null;
        }
    }
}