/*
 * This file is part of Sui.
 *
 * Sui is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sui is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Sui.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 Sui Contributors
 */

package rikka.sui.cmd;

import android.content.pm.PackageManager;
import android.os.Looper;
import android.system.Os;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import rikka.shizuku.Shizuku;
import rikka.sui.BuildConfig;
import rikka.sui.Sui;

public class SuiCmd {

    public static void main(String[] args) throws InterruptedException {
        if (BuildConfig.DEBUG) {
            System.out.println("args: " + Arrays.toString(args));
        }

        if (args.length == 0 || (args[0].equals("--help") || args[0].equals("-h"))) {
            printHelp();
            return;
        }

        if (args[0].equals("--version") || args[0].equals("-v")) {
            System.out.println(BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
            return;
        }

        String packageName;
        if (Os.getuid() == 2000) {
            packageName = "com.android.shell";
        } else {
            packageName = System.getenv("SUI_APPLICATION_ID");
            if (TextUtils.isEmpty(packageName)) {
                abort("SUI_APPLICATION_ID is not set, set this environment variable to the id of current application (package name)");
            }
        }

        Looper.prepareMainLooper();

        Sui.init(packageName);
        if (!Sui.isSui()) {
            abort("Sui is not running or this application is set as hidden");
        }
        // If then package name is wrong, attachApplication will throw an exception, isPreV11 will be true
        if (Shizuku.isPreV11()) {
            abort("Please check if the current SUI_APPLICATION_ID, " + packageName + ", is correct");
        }

        preExec(args);

        Looper.loop();
    }

    private static void preExec(String[] args) throws InterruptedException {
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            doExec(args);
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            abort("Permission denied, please check the management UI of Sui");
        } else {
            System.out.println("[ Requesting permission... ]");

            Shizuku.addRequestPermissionResultListener(new Shizuku.OnRequestPermissionResultListener() {
                @Override
                public void onRequestPermissionResult(int requestCode, int grantResult) {
                    Shizuku.removeRequestPermissionResultListener(this);

                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        try {
                            doExec(args);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        abort("Permission denied, please check the management UI of Sui");
                    }
                }
            });
            Shizuku.requestPermission(0);

        }
    }

    private static void doExec(String[] args) throws InterruptedException {
        List<String> envList = new ArrayList<>();
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            envList.add(entry.getKey() + "=" + entry.getValue());
        }
        String[] env = envList.toArray(new String[0]);
        String cwd = new File("").getAbsolutePath();

        if (BuildConfig.DEBUG) {
            System.out.println("[ " + cwd + " ]");
            System.out.println("[ " + Arrays.toString(env) + " ]");
        }

        Process process = Shizuku.newProcess(args, env, cwd);

        InputStream in = process.getInputStream();
        InputStream err = process.getErrorStream();
        OutputStream out = process.getOutputStream();

        new TransferThread(in, System.out).start();
        new TransferThread(err, System.out).start();
        new TransferThread(System.in, out).start();

        int exitCode = process.waitFor();
        System.out.println("[ " + "Command " + args[0] + " exited with " + exitCode + " ]");
        System.exit(exitCode);
    }

    private static class TransferThread extends Thread {

        private final InputStream input;
        private final OutputStream output;

        public TransferThread(InputStream input, OutputStream output) {
            this.input = input;
            this.output = output;
        }

        @Override
        public void run() {
            byte[] buf = new byte[8192];
            int len;
            try {
                while ((len = input.read(buf)) != -1) {
                    output.write(buf, 0, len);
                    output.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void abort(String message) {
        System.err.println("[ " + message + " ]");
        System.exit(1);
    }

    private static void printHelp() {
        System.out.println("usage: sui [OPTION]... [CMD]...\n" +
                "Run command through Sui.\n\n" +
                "Options:\n" +
                "-h, --help        print this help\n" +
                "-v, --version     print the version of the sui_wrapper tool\n" +
                "\n" +
                "This file can be used in adb shell or terminal apps.\n" +
                "For terminal apps, the environment variable SUI_APPLICATION_ID needs to be set to the first.");
    }
}
