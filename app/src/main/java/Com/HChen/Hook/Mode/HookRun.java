package Com.HChen.Hook.Mode;

import Com.HChen.Hook.Base.BaseGetKey;
import Com.HChen.Hook.BuildConfig;
import Com.HChen.Hook.Execute.MiuiFramework.PowerKeeper;
import Com.HChen.Hook.Execute.MiuiService.MiuiService;
import Com.HChen.Hook.Execute.SystemService.SystemService;
import Com.HChen.Hook.Execute.Test.TestHook;
import Com.HChen.Hook.Utils.GetKey;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HookRun extends HookLog {
    public final String TAG = "HookRun";
    LoadPackageParam loadPackageParam;

    public final GetKey<String, Object> mPrefsMap = BaseGetKey.mPrefsMap;

    public void HookPackage(LoadPackageParam loadPackageParam) {
        String PackageName = loadPackageParam.packageName;
        this.loadPackageParam = loadPackageParam;
        init(PackageName);
    }

    public void init(String PackageName) {
        switch (PackageName) {
            case "android" -> {
                initHook(new SystemService(), mPrefsMap.getBoolean("system_service"));
                initHook(new MiuiService(), mPrefsMap.getBoolean("miui_service"));
            }
            case "com.miui.powerkeeper" ->
                initHook(new PowerKeeper(), mPrefsMap.getBoolean("powerkeeper"));
            case "Com.HChen.App" -> {
                if (BuildConfig.DEBUG) {
                    initHook(new TestHook(), true);
                }
            }
            case "com.android.settings" -> initHook(new TestHook(), mPrefsMap.getBoolean("text"));
        }
    }

    public void initHook(HookMode hchenHookMode, boolean isTrue) {
        if (isTrue) {
            hchenHookMode.Run(loadPackageParam);
        }
        /*else {
            logI("Hook " + hchenHookMode + " is :" + isTrue);
        }*/
    }
}
