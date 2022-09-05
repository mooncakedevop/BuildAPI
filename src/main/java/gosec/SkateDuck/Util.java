package gosec.SkateDuck;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Util {
    public static void output(String path, String cls, String content) {
        try {
            File file = new File(path);
            FileUtils.writeStringToFile(file, "method:" + cls + "\r\n", "utf-8", true);
            FileUtils.writeStringToFile(file, content + "\r\n", "utf-8", true);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> allAPK(String path) {
        List<String> fileList = new ArrayList<>();
        File file = new File(path);
        File[] files = file.listFiles();
        for (File f : files) {
            if (f.isDirectory()) {
                allAPK(f.getAbsolutePath());
            } else if (f.getAbsolutePath().contains(".apk")) {
                fileList.add(f.getAbsolutePath());
            }
        }
        return fileList;
    }


}