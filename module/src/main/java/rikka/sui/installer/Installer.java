package rikka.sui.installer;

import android.content.pm.ApplicationInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

import rikka.sui.server.api.SystemService;

public class Installer {

    private static void saveApplicationInfoToFile(String packageName, String name) throws IOException {
        ApplicationInfo ai = SystemService.getApplicationInfoNoThrow(packageName, 0, 0);
        if (ai == null) {
            System.out.println("! Can't fetch application info for package " + packageName);
            return;
        }
        int uid = ai.uid;
        String processName = ai.processName != null ? ai.processName : packageName;
        System.out.println("- " + name + ": uid=" + uid + ", processName=" + processName);

        File file = new File("/data/adb/sui/" + packageName);
        if (!file.exists() && !file.createNewFile()) {
            System.out.println("! Can't create " + file);
            return;
        }

        FileWriter writer = new FileWriter(file);
        writer.write(String.format(Locale.ENGLISH, "%d\n%s", uid, processName));
        writer.flush();
        writer.close();
    }

    public static void main(String[] args) throws IOException {
        System.out.println("- AppProcess: main");
        saveApplicationInfoToFile("com.android.systemui", "SystemUI");
        saveApplicationInfoToFile("com.android.settings", "Settings");
        System.out.println("- AppProcess: exit");
    }
}
