package com.astor.oplusconfighook;

/**
 * 应用信息数据模型，描述包名、名称、安装时间等基础属性。
 */
public class AppInfo {
    public final String packageName;
    public final String label;
    public final boolean systemApp;
    public final long firstInstallTime;
    public final long lastUpdateTime;

    public AppInfo(String packageName, String label, boolean systemApp, long firstInstallTime, long lastUpdateTime) {
        this.packageName = packageName;
        this.label = label;
        this.systemApp = systemApp;
        this.firstInstallTime = firstInstallTime;
        this.lastUpdateTime = lastUpdateTime;
    }
}
