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

    /** 工作资料创建成功后，系统会在新的工作资料内调用此方法 */
    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        super.onProfileProvisioningComplete(context, intent);

        // 工作资料已创建完成，激活它（使其在桌面上可见、可用）。
        // 这一步是标准做法：见 Android 官方文档 "BasicManagedProfile" 示例。
        android.app.admin.DevicePolicyManager dpm =
                (android.app.admin.DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        android.content.ComponentName admin = getWho(context);
        if (dpm != null) {
            dpm.setProfileEnabled(admin);
        }

        Toast.makeText(context, context.getString(R.string.toast_profile_ready), Toast.LENGTH_LONG).show();
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
