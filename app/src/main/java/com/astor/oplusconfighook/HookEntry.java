package com.astor.oplusconfighook;

import com.astor.oplusconfighook.hooks.OplusAppPowerSaveHook;
import com.astor.oplusconfighook.hooks.OplusBackgroundRestrictHook;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 只针对 android系统框架 和 GMS服务
        String[] targetPkgs = {"android", "com.google.android.gms"};
        boolean match = false;
        for(String pkg : targetPkgs){
            if(lpparam.packageName.equals(pkg)){
                match = true;
                break;
            }
        }
        if(!match){
            return;
        }

        // ========== 保留2个核心Hook，维持GMS/FCM不断连 ==========
        try{
            new OplusAppPowerSaveHook(lpparam).hook();
        }catch (Throwable e){
            XLog.e("OplusAppPowerSaveHook hook fail", e);
        }

        try{
            new OplusBackgroundRestrictHook(lpparam).hook();
        }catch (Throwable e){
            XLog.e("OplusBackgroundRestrictHook hook fail", e);
        }

        // ========== 下面全部其他Hook注释禁用，不再运行 ==========
        /*
        try{
            new OplusMultiAppHook(lpparam).hook();
        }catch (Throwable e){
            XLog.e("OplusMultiAppHook hook fail", e);
        }

        try{
            new ProxyBatchHook(lpparam).hook();
        }catch (Throwable e){
            XLog.e("ProxyBatchHook hook fail", e);
        }

        try{
            new OplusPrivacyHook(lpparam).hook();
        }catch (Throwable e){
            XLog.e("OplusPrivacyHook hook fail", e);
        }

        try{
            new NotificationHook(lpparam).hook();
        }catch (Throwable e){
            XLog.e("NotificationHook hook fail", e);
        }
        */
    }
}
