package com.mysandbox.work;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 主界面：只做一件事 —— 触发系统标准的"创建工作资料"流程。
 *
 * 创建过程完全由 Android 系统自带的 ManagedProvisioning 组件接管
 * （用户会看到系统自己的引导界面，不是本 App 绘制的），
 * 本 App 在创建完成后即可视为"功能已完成"，
 * 即使之后被卸载，工作资料本身依然存在（由系统 system_server 维护，
 * 这是 Android Work Profile 的设计本质，不依赖管理者 App 常驻）。
 */
public class MainActivity extends AppCompatActivity {

    private DevicePolicyManager dpm;
    private ComponentName adminComponent;
    private TextView statusText;

    private final ActivityResultLauncher<Intent> provisioningLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Toast.makeText(this, getString(R.string.toast_provisioning_started), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, getString(R.string.toast_provisioning_cancelled), Toast.LENGTH_LONG).show();
                }
                updateStatus();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, AdminReceiver.class);
        statusText = findViewById(R.id.text_status);

        Button createButton = findViewById(R.id.button_create_profile);
        createButton.setOnClickListener(v -> startProfileProvisioning());

        Button manageButton = findViewById(R.id.button_open_settings);
        manageButton.setOnClickListener(v -> openWorkProfileSettings());

        updateStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    /**
     * 触发系统标准 Intent：ACTION_PROVISION_MANAGED_PROFILE
     * 这是 Google 官方公开文档教学的标准用法（非私有/隐藏 API），
     * 系统会接管后续所有 UI 与流程，最终在设备上创建一个新的工作资料（沙盒）。
     */
    private void startProfileProvisioning() {
        if (dpm != null && dpm.isProfileOwnerApp(getPackageName())) {
            Toast.makeText(this, getString(R.string.toast_already_provisioned), Toast.LENGTH_LONG).show();
            return;
        }

        // 提前检测设备是否支持多用户/工作资料特性，避免直接抛出系统级异常，
        // 给用户一个明确的中文提示而不是无响应或崩溃。
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_MANAGED_USERS)) {
            Toast.makeText(this, getString(R.string.toast_provisioning_unsupported), Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
        intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, adminComponent);
        // 注：EXTRA_PROVISIONING_SKIP_ENCRYPTION 自 Android 7.0 起已被系统忽略，
        // 保留该字段没有实际效果，容易造成误解，故移除。

        try {
            provisioningLauncher.launch(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.toast_provisioning_unsupported), Toast.LENGTH_LONG).show();
        } catch (SecurityException e) {
            // 部分定制系统（如企业已托管设备）可能拒绝二次创建工作资料
            Toast.makeText(this, getString(R.string.toast_provisioning_unsupported), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 跳转到系统设置中的账户/用户管理页面，方便用户查看或移除工作资料。
     * 不同厂商 ROM 的入口不完全一致，因此按优先级依次尝试，
     * 找不到对应页面时兜底跳到设置首页，而不是让用户無反应。
     */
    private void openWorkProfileSettings() {
        String[] candidateActions = {
                "android.settings.MANAGE_PROFILE_SETTINGS", // 部分系统的账户与用户管理页
                android.provider.Settings.ACTION_SYNC_SETTINGS
        };

        for (String action : candidateActions) {
            try {
                startActivity(new Intent(action));
                return;
            } catch (android.content.ActivityNotFoundException ignored) {
                // 尝试下一个候选 Action
            }
        }

        // 都不支持时兜底跳转系统设置首页
        try {
            startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.toast_settings_unavailable), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStatus() {
        if (dpm != null && dpm.isProfileOwnerApp(getPackageName())) {
            statusText.setText(getString(R.string.status_profile_exists));
        } else {
            statusText.setText(getString(R.string.status_no_profile));
        }
    }
}
