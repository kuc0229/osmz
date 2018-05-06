package com.kru13.httpserver.util;

import com.kru13.httpserver.exceptions.ExecuteCommandException;

import java.util.ArrayList;
import java.util.List;

public class CommandUtil {

    public static List<String> parseCommand(String command) {
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


    public static String executeCommand(List<String> commandSplitted) throws ExecuteCommandException {

        try {
            Process start = new ProcessBuilder(commandSplitted).start();
            int retCode = start.waitFor();

            String data = null;
            if (retCode == 0) {
                int available = start.getInputStream().available();
                byte[] buffer = new byte[available];
                int read = start.getInputStream().read(buffer, 0, available);

                if (read > 0) {
                    data = new String(buffer);
                    data += "\n\nExit code: " + retCode;
                } else {
                    data = "No data.\n\nExit code: " + retCode;
                }
            } else {
                int available = start.getErrorStream().available();
                byte[] buffer = new byte[available];
                int read = start.getErrorStream().read(buffer, 0, available);
                if (read > 0) {
                    data = new String(buffer);
                    data += "\n\nExit code: " + retCode;
                }
            }
            return data;
        } catch (Exception e) {
            throw new ExecuteCommandException(e);
        }
    }
}
