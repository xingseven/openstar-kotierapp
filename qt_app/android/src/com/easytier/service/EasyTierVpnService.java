package com.easytier.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

/**
 * Android VpnService 薄层 —— 仅用于获取 TUN 文件描述符并传递给 Rust 原生层。
 * 最小 Java 残留层，所有业务逻辑在 C++/Qt 侧。
 */
public class EasyTierVpnService extends VpnService {

    private static final String TAG = "EasyTierVpn";
    private static final String CHANNEL_ID = "easytier_vpn";
    private static final int NOTIFICATION_ID = 1001;

    private ParcelFileDescriptor pfd = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        String instanceName = intent.getStringExtra("instance_name");
        String ipv4 = intent.getStringExtra("ipv4");
        int prefix = intent.getIntExtra("prefix", 24);
        String[] extraRoutes = intent.getStringArrayExtra("routes");

        if (instanceName == null || ipv4 == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        // 创建通知渠道（Android 8+ 前台服务必需）
        createNotificationChannel();

        // 构建 VPN 接口
        try {
            Builder builder = new Builder();
            builder.setSession("EasyTier (" + instanceName + ")");
            builder.setMtu(1500);
            builder.addAddress(ipv4, prefix);

            // 添加子网路由
            try {
                String networkAddr = computeNetworkAddress(ipv4, prefix);
                builder.addRoute(networkAddr, prefix);
            } catch (Exception ignored) {}

            if (extraRoutes != null) {
                for (String route : extraRoutes) {
                    String[] parts = route.split("/");
                    if (parts.length == 2) {
                        try {
                            builder.addRoute(parts[0], Integer.parseInt(parts[1]));
                        } catch (Exception ignored) {}
                    }
                }
            }

            builder.addDnsServer("223.5.5.5");

            ParcelFileDescriptor newPfd = builder.establish();
            if (newPfd == null) {
                stopSelf(startId);
                return START_NOT_STICKY;
            }

            // 关闭旧的 fd
            if (pfd != null) {
                try { pfd.close(); } catch (Exception ignored) {}
            }
            pfd = newPfd;

            // 将 TUN fd 传递给 Rust 原生层
            int fd = pfd.detachFd();
            nativeSetTunFd(instanceName, fd);

            // 启动前台服务通知
            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("EasyTier VPN")
                    .setContentText("VPN is running: " + instanceName)
                    .setSmallIcon(android.R.drawable.ic_menu_manage)
                    .setOngoing(true)
                    .build();
            startForeground(NOTIFICATION_ID, notification);

        } catch (SecurityException e) {
            stopSelf(startId);
            return START_NOT_STICKY;
        } catch (Exception e) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (pfd != null) {
            try { pfd.close(); } catch (Exception ignored) {}
            pfd = null;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "EasyTier VPN",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("EasyTier VPN Service Notification");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    /** 根据 IP 和前缀长度计算网络地址 */
    private static String computeNetworkAddress(String ipv4, int prefix) {
        String[] parts = ipv4.split("\\.");
        int ipInt = (Integer.parseInt(parts[0]) << 24)
                  | (Integer.parseInt(parts[1]) << 16)
                  | (Integer.parseInt(parts[2]) << 8)
                  | Integer.parseInt(parts[3]);
        int mask = prefix == 0 ? 0 : (-1 << (32 - prefix));
        int networkInt = ipInt & mask;
        return ((networkInt >>> 24) & 0xFF) + "."
             + ((networkInt >>> 16) & 0xFF) + "."
             + ((networkInt >>> 8) & 0xFF) + "."
             + (networkInt & 0xFF);
    }

    /** 从 Qt C++ 侧通过 JNI 调用的原生方法 */
    private static native void nativeSetTunFd(String instanceName, int fd);
}
