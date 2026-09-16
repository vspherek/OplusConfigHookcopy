package com.astor.oplusconfighook;

import com.astor.oplusconfighook.hooks.OplusAppPowerSaveHook;
import com.astor.oplusconfighook.hooks.OplusBackgroundRestrictHook;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 仅Hook系统框架android 和 GMS
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

        // ========== 保留2个核心Hook，FCM必须 ==========
        try{
            new OplusAppPowerSaveHook(lpparam).hook();
        }catch (Throwable e){
            // 去掉XLog，直接打印堆栈
            e.printStackTrace();
        }

        try{
            new OplusBackgroundRestrictHook(lpparam).hook();
        }catch (Throwable e){
            e.printStackTrace();
        }

        // ========== 其余全部Hook注释，不执行 ==========
        /*
        try{
            new OplusMultiAppHook(lpparam).hook();
        }catch (Throwable e){
            e.printStackTrace();
        }

        try{
            new ProxyBatchHook(lpparam).hook();
        }catch (Throwable e){
            e.printStackTrace();
        }

        try{
            new OplusPrivacyHook(lpparam).hook();
        }catch (Throwable e){
            e.printStackTrace();
        }

        try{
            new NotificationHook(lpparam).hook();
        }catch (Throwable e){
            e.printStackTrace();
        }
        */
    }
}
