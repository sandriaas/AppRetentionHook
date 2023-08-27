package Com.HChen.Hook.MiuiFramework;

import static Com.HChen.Hook.HookValue.MiuiValue.*;
import static Com.HChen.Hook.HookName.MiuiName.*;

import Com.HChen.Hook.HookMode.HookMode;
import de.robv.android.xposed.XC_MethodHook;

public class PowerKeeper extends HookMode {
    @Override
    public int smOr() {
        return 2;
    }

    @Override
    public void init() {
        hookAllMethods(ProcessManager,
                kill,
                new HookAction() {
                    @Override
                    protected void before(XC_MethodHook.MethodHookParam param) {
                        setLog(2, ProcessManager, kill);
                        param.setResult(null);
                    }
                }
        );

        findAndHookConstructor(PressureStateSettings,
                new HookAction() {
                    @Override
                    protected void after(MethodHookParam param) {
                        setLog(2, PressureStateSettings, "PROCESS_CLEANER_ENABLED");
                        setBoolean(param.thisObject, "PROCESS_CLEANER_ENABLED", false);
                    }
                }
        );
    }
}
