package net.mooctest;

import java.util.*;

public class NoteDiffUtil {
    public static String diff(String oldContent, String newContent) {
        if(oldContent == null) oldContent = "";
        if(newContent == null) newContent = "";
        StringBuilder sb = new StringBuilder();
        String[] oldLines = oldContent.split("\\r?\\n");
        String[] newLines = newContent.split("\\r?\\n");
        int max = Math.max(oldLines.length, newLines.length);
        for(int i=0; i<max; i++) {
            String oldLine = i<oldLines.length ? oldLines[i] : "";
            String newLine = i<newLines.length ? newLines[i] : "";
            if(!oldLine.equals(newLine)) {
                sb.append("- ").append(oldLine).append("\n");
                sb.append("+ ").append(newLine).append("\n");
            } else {
                sb.append("  ").append(oldLine).append("\n");
            }
        }
        return sb.toString();
    }
}
