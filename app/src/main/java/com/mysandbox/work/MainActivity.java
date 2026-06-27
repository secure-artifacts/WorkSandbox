package com.mysandbox.work;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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

        Intent intent = new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
        intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, adminComponent);
        intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION, true);

        try {
            provisioningLauncher.launch(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.toast_provisioning_unsupported), Toast.LENGTH_LONG).show();
        }
    }

    /** 跳转到系统设置中的账户/工作资料管理页面，方便用户查看或移除工作资料 */
    private void openWorkProfileSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
        try {
            startActivity(intent);
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
