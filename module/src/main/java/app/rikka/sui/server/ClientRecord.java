package app.rikka.sui.server;

import moe.shizuku.server.IShizukuClient;

public class ClientRecord {

    public final int uid;
    public final int pid;
    public final IShizukuClient client;
    public final String packageName;
    public boolean permissionGranted;

    public ClientRecord(int uid, int pid, IShizukuClient client, String packageName) {
        this.uid = uid;
        this.pid = pid;
        this.client = client;
        this.packageName = packageName;
        this.permissionGranted = false;
    }
}