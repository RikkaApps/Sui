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
import java.util.concurrent.CountDownLatch;

import rikka.shizuku.Shizuku;
import rikka.sui.BuildConfig;
import rikka.sui.Sui;

/**
 * Replaced by rish which provides the full functionality of interactive shells.
 * This will be removed in the next major version.
 */
public class SuiCmd {

    private static void printHelp() {
        printf("usage: sui [OPTION]... [CMD]...\n" +
                "Run command through Sui.\n\n" +
                "Options:\n" +
                "Options:\n" +
                "-h, --help               print this help\n" +
                "-v, --version            print the version of the sui_wrapper tool\n" +
                "--verbose                print more messages\n" +
                "-m, -p,\n" +
                "--preserve-environment   preserve the entire environment\n" +
                "\n" +
                "This file can be used in adb shell or terminal apps.\n" +
                "For terminal apps, the environment variable SUI_APPLICATION_ID needs to be set to the first.");
    }

    private static boolean verboseMessageAllowed = false;
    private static boolean preserveEnvironment = false;

    private static void printf(String format, Object ... args) {
        System.out.printf(format + "\n", args);
        System.out.flush();
    }

    private static void verbose(String format, Object ... args) {
        if (!verboseMessageAllowed) return;

        printf(format, args);
    }

    private static void abort(String format, Object ... args) {
        System.err.printf(format + "\n", args);
        System.err.flush();
        System.exit(1);
    }

    public static void main(String[] args) throws InterruptedException {
        if (BuildConfig.DEBUG) {
            printf("args: " + Arrays.toString(args));
        }

        if (args.length == 0) {
            printHelp();
            return;
        }

        List<String> cmds = new ArrayList<>();

        for (String arg : args) {
            switch (arg) {
                case "--verbose":
                    verboseMessageAllowed = true;
                    break;
                case "--help":
                case "-h":
                    printHelp();
                    return;
                case "--version":
                case "-v":
                    printf("%s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
                    return;
                case "-m":
                case "-p":
                case "--preserve-environment":
                    preserveEnvironment = true;
                    break;
                default:
                    cmds.add(arg);
                    break;
            }
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

        preExec(cmds.toArray(new String[0]));

        Looper.loop();
    }

    private static void preExec(String[] args) throws InterruptedException {
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            doExec(args);
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            abort("Permission denied, please check the management UI of Sui");
        } else {
            verbose("Requesting permission...");

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
        Process process;
        InputStream in;
        InputStream err;
        OutputStream out;

        try {
            String[] env;
            String cwd = new File("").getAbsolutePath();

            if (preserveEnvironment) {
                List<String> envList = new ArrayList<>();
                for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
                    envList.add(entry.getKey() + "=" + entry.getValue());
                }
                env = envList.toArray(new String[0]);
            } else {
                env = null;
            }

            verbose("cwd: " + cwd);
            verbose("env: " + Arrays.toString(env));
            verbose("Starting command " + args[0] + "...");
            process = Shizuku.newProcess(args, env, cwd);

            in = process.getInputStream();
            err = process.getErrorStream();
            out = process.getOutputStream();
        } catch (Throwable e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
            abort(e.getMessage());
            return;
        }

        CountDownLatch latch = new CountDownLatch(2);

        new TransferThread(in, System.out, latch).start();
        new TransferThread(err, System.out, latch).start();
        new TransferThread(System.in, out, null).start();

        int exitCode = process.waitFor();
        latch.await();

        verbose("Command " + args[0] + " exited with " + exitCode);
        System.exit(exitCode);
    }

    private static class TransferThread extends Thread {

        private final InputStream input;
        private final OutputStream output;
        private final CountDownLatch latch;

        public TransferThread(InputStream input, OutputStream output, CountDownLatch latch) {
            this.input = input;
            this.output = output;
            this.latch = latch;
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
            if (latch != null) {
                latch.countDown();
            }
        }
    }
}
