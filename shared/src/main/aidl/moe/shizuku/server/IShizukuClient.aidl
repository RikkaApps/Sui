package moe.shizuku.server;

interface IShizukuClient {

    void onAttachClientProcess(in Bundle data) = 1;

    void onRequestPermissionResult(int requestCode, boolean granted) = 2;
}