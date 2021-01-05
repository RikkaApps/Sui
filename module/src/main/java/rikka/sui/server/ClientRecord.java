package rikka.sui.server;

import android.os.Bundle;

import moe.shizuku.server.IShizukuClient;

import static rikka.sui.server.ServerConstants.LOGGER;
import static rikka.sui.server.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_ALLOWED;

public class ClientRecord {

    public final int uid;
    public final int pid;
    public final IShizukuClient client;
    public final String packageName;
    public boolean allowed;

    public ClientRecord(int uid, int pid, IShizukuClient client, String packageName) {
        this.uid = uid;
        this.pid = pid;
        this.client = client;
        this.packageName = packageName;
        this.allowed = false;
    }

    public void callOnRequestPermissionResult(int requestCode, boolean allowed) {
        Bundle reply = new Bundle();
        reply.putBoolean(REQUEST_PERMISSION_REPLY_ALLOWED, allowed);
        try {
            client.onRequestPermissionResult(requestCode, reply);
        } catch (Throwable e) {
            LOGGER.w(e, "onRequestPermissionResult failed for client (uid=%d, pid=%d, package=%s)", uid, pid, packageName);
        }
    }
}