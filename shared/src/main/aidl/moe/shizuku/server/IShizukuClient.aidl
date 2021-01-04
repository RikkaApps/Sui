package moe.shizuku.server;

interface IShizukuClient {

    oneway void onClientAttached(in Bundle data) = 1;

    oneway void onRequestPermissionResult(int requestCode, in Bundle data) = 2;
}