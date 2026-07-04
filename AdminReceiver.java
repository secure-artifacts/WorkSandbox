package com.mysandbox.work;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * 工作资料的管理员（Profile Owner）身份载体。
 *
 * 这是 Android 官方标准组件，系统要求：
 * 创建工作资料（Work Profile）时，必须指定一个 DeviceAdminReceiver
 * 作为该工作资料的"管理员"，否则系统不知道该资料归谁管。
 *
 * 本类不做任何额外的策略限制（不冻结应用、不限制功能），
 * 仅满足系统创建工作资料的最低要求。
 */
public class AdminReceiver extends DeviceAdminReceiver {

    // 三星相册的系统包名（预装应用，非第三方 SDK）。
    // 新建工作资料默认不会启用大部分系统应用，需要 Profile Owner 主动启用，
    // 否则用户在工作资料桌面上会找不到相册图标，看起来像是"缺失"了。
    private static final String SAMSUNG_GALLERY_PACKAGE = "com.sec.android.gallery3d";

    /** 工作资料创建成功后，系统会在新的工作资料内调用此方法 */
    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        super.onProfileProvisioningComplete(context, intent);

        // 工作资料已创建完成，激活它（使其在桌面上可见、可用）。
        // 这一步是标准做法：见 Android 官方文档 "BasicManagedProfile" 示例。
        android.app.admin.DevicePolicyManager dpm =
                (android.app.admin.DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        android.content.ComponentName admin = getWho(context);
        try {
            if (dpm != null) {
                dpm.setProfileEnabled(admin);
            }
        } catch (SecurityException e) {
            // 极少数定制系统可能限制此调用，静默失败即可，
            // 不影响工作资料本身已由系统创建完成这一事实。
        }

        enableSamsungGalleryIfPresent(context, dpm, admin);

        Toast.makeText(context, context.getString(R.string.toast_profile_ready), Toast.LENGTH_LONG).show();
    }

    /**
     * 使用官方公开 API DevicePolicyManager#enableSystemApp(ComponentName, String) 启用
     * 已随系统预装、但默认未在工作资料中启用的三星相册。
     *
     * 仅对"设备上已预装的系统应用"生效——该 API 不能安装任何设备上不存在的 APK，
     * 因此非三星设备、或三星设备上相册被运营商精简掉的情况下，此调用会静默失败，
     * 不影响工作资料本身的可用性。
     */
    private void enableSamsungGalleryIfPresent(Context context,
                                                android.app.admin.DevicePolicyManager dpm,
                                                android.content.ComponentName admin) {
        if (dpm == null) {
            return;
        }
        try {
            dpm.enableSystemApp(admin, SAMSUNG_GALLERY_PACKAGE);
        } catch (IllegalArgumentException e) {
            // 该设备的系统镜像里没有这个包（非三星设备，或被精简），属预期情况，忽略即可。
        } catch (SecurityException e) {
            // 个别定制系统可能拒绝此调用，静默忽略，不影响主流程。
        }
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
    }
}
