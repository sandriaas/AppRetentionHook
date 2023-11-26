package Com.HChen.Hook.Mode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Com.HChen.Hook.Base.BaseGetKey;
import Com.HChen.Hook.Utils.AllValue;
import Com.HChen.Hook.Utils.GetKey;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public abstract class HookMode extends HookLog {
    public String tag = getClass().getSimpleName();
    public final GetKey<String, Object> mPrefsMap = BaseGetKey.mPrefsMap;
    public final String[] allValue = AllValue.AllValue;
    List<String> tempString, tempStringT, tempStringS = new ArrayList<>();
    LoadPackageParam loadPackageParam;

    public abstract void init();

    public void Run(LoadPackageParam loadPackageParam) {
        try {
            SetLoadPackageParam(loadPackageParam);
            init();
            logI(tag, "Hook Success!");
        } catch (Throwable e) {
            logE(tag, "Hook Failed! code: " + e);
        }
    }

    public void SetLoadPackageParam(LoadPackageParam loadPackageParam) {
        this.loadPackageParam = loadPackageParam;
    }

    public Class<?> findClass(String className) {
        return findClass(className, loadPackageParam.classLoader);
    }

    public Class<?> findClass(String className, ClassLoader classLoader) {
        return XposedHelpers.findClass(className, classLoader);
    }

    public Class<?> findClassIfExists(String className) {
        try {
            return findClass(className);
        } catch (XposedHelpers.ClassNotFoundError e) {
            logE(tag, "Class not found: " + e);
            return null;
        }
    }

    public Class<?> findClassIfExists(String newClassName, String oldClassName) {
        try {
            return findClass(findClassIfExists(newClassName) != null ? newClassName : oldClassName);
        } catch (XposedHelpers.ClassNotFoundError e) {
            logE(tag, "Find " + newClassName + " and " + oldClassName + " is null, code: " + e);
            return null;
        }
    }

    public static class HookAction extends XC_MethodHook {

        private String methodProcessed = null;

        protected void before(MethodHookParam param) {
        }

        protected void after(MethodHookParam param) {
        }

        private Info paramCheck(MethodHookParam param) {
            String method = null;
            String thisObject = null;
            if (param.method != null) {
                method = param.method.toString();
            }
            if (param.thisObject != null) {
                thisObject = param.thisObject.toString();
            }
            if (param.method == null && param.thisObject == null)
                logE("paramCheck", "param.method is: " + param.method
                    + " param.thisObject is: " + param.thisObject);
            return new Info(method, thisObject, null);
        }

        private Info getInfo(String method, String thisObject) {
//            int lastIndex = all.lastIndexOf(".");
            if (method == null) return
                new Info(null, null, null);
            if (thisObject != null) {
                Pattern pattern = Pattern.compile(".*\\.(.*\\(.*\\))");
                Matcher matcher = pattern.matcher(method);
                if (thisObject.contains("@")) {
                    if (matcher.find()) {
                        methodProcessed = matcher.group(1);
                    } else methodProcessed = null;
                    pattern = Pattern.compile(".*\\.(\\w+)\\..*\\(.*\\)");
                    matcher = pattern.matcher(method);
                    if (matcher.find()) {
                        thisObject = matcher.group(1);
                    } else thisObject = null;
                } else {
                    if (matcher.find()) {
                        thisObject = matcher.group(1);
                    } else methodProcessed = "constructor";
                }
            } else {
                Pattern pattern = Pattern.compile(".*\\.(\\w+)\\.(.*\\(.*\\))");
                Matcher matcher = pattern.matcher(method);
                if (matcher.find()) {
                    thisObject = matcher.group(1);
                    methodProcessed = matcher.group(2);
                } else return new Info(null, method, thisObject);
            }
            return new Info(null, thisObject, methodProcessed);
        }

        private static StringBuilder paramLog(MethodHookParam param) {
            StringBuilder log = null;
            for (int i = 0; i < param.args.length; i++) {
                log = (log == null ? new StringBuilder() : log).append("param(").append(i).append("): ").append(param.args[i]).append(" ");
            }
            return log;
        }

        private static class Info {
            public String method;
            public String thisObject;
            public String methodProcessed;

            public Info(String method, String thisObject, String methodProcessed) {
                this.method = method;
                this.thisObject = thisObject;
                this.methodProcessed = methodProcessed;
            }
        }

        public HookAction() {
            super();
        }

        public HookAction(int priority) {
            super(priority);
        }

        public static HookAction returnConstant(final Object result) {
            return new HookAction(PRIORITY_DEFAULT) {
                @Override
                protected void before(MethodHookParam param) {
                    super.before(param);
                    param.setResult(result);
                }
            };
        }

        public static final HookAction DO_NOTHING = new HookAction(PRIORITY_HIGHEST * 2) {
            @Override
            protected void before(MethodHookParam param) {
                super.before(param);
                param.setResult(null);
            }

        };

        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            try {
                before(param);
                Info info = paramCheck(param);
                info = getInfo(info.method, info.thisObject);
                logSI(info.thisObject, info.methodProcessed + " " + paramLog(param));
            } catch (Throwable e) {
                logE("beforeHookedMethod", "" + e);
            }
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            try {
                after(param);
            } catch (Throwable e) {
                logE("afterHookedMethod", "" + e);
            }
        }

    }

    public abstract static class replaceHookedMethod extends HookAction {

        public replaceHookedMethod() {
            super();
        }

        public replaceHookedMethod(int priority) {
            super(priority);
        }

        protected abstract Object replace(XC_MethodHook.MethodHookParam param);

        @Override
        public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
            try {
                Object result = replace(param);
                param.setResult(result);
            } catch (Throwable t) {
                param.setThrowable(t);
            }
        }
    }

    public void findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        try {
            /*获取class*/
            if (parameterTypesAndCallback.length != 1) {
                Object[] newArray = new Object[parameterTypesAndCallback.length - 1];
                System.arraycopy(parameterTypesAndCallback, 0, newArray, 0, newArray.length);
                Class<?>[] classes = new Class<?>[newArray.length];
                for (int i = 0; i < newArray.length; i++) {
                    Class<?> newclass = (Class<?>) newArray[i];
                    classes[i] = newclass;
                }
                /*clazz.getDeclaredMethod(methodName, classes);*/
                checkDeclaredMethod(clazz, methodName, classes);
            }
            XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
            logI(tag, "Hook: " + clazz + " method is: " + methodName);
        } catch (NoSuchMethodException e) {
            logE(tag, "Not find method: " + methodName + " in: " + clazz);
        }
    }

    public void findAndHookMethod(String className, String methodName, Object... parameterTypesAndCallback) {
        int lastIndex = className.lastIndexOf(".");
        if (lastIndex != -1) {
            String result = className.substring(lastIndex + 1);
            int lastIndexA = result.lastIndexOf("$");
            if (lastIndexA == -1) {
                if (mPrefsMap.getBoolean(methodName)) {
                    findAndHookMethod(findClassIfExists(className), methodName, parameterTypesAndCallback);
                } else {
                    if (mPrefsMap.getBoolean(result + "_" + methodName)) {
                        findAndHookMethod(findClassIfExists(className), methodName, parameterTypesAndCallback);
                    } else {
                        if (mPrefsMap.getBoolean(result)) {
                            findAndHookMethod(findClassIfExists(className), methodName, parameterTypesAndCallback);
                        } else {
                            checkAndRun(tempStringT, methodName,
                                () -> findAndHookMethod(
                                    findClassIfExists(className),
                                    methodName,
                                    parameterTypesAndCallback),
                                "findAndHookMethod the class: " + className + " ; ");
                        }
                    }
                }
            } else {
                String resultA = result.substring(0, lastIndexA);
                if (mPrefsMap.getBoolean(resultA)) {
                    findAndHookMethod(findClassIfExists(className), methodName, parameterTypesAndCallback);
                } else {
                    checkAndRun(tempStringT, methodName,
                        () -> findAndHookMethod(
                            findClassIfExists(className),
                            methodName,
                            parameterTypesAndCallback),
                        "findAndHookMethod the class: " + className + " ; ");
                }
            }
        } else {
            logE(tag, "Cant get key, class: " + className);
        }
    }

    public void findAndHookConstructor(Class<?> clazz, Object... parameterTypesAndCallback) {
        try {
            XposedHelpers.findAndHookConstructor(clazz, parameterTypesAndCallback);
            logI(tag, "Hook: " + clazz + " is success");
        } catch (Throwable f) {
            logE(tag, "findAndHookConstructor error: " + f + " class: " + clazz);
        }
    }

    public void findAndHookConstructor(String className, Object... parameterTypesAndCallback) {
        int lastIndex = className.lastIndexOf(".");
        if (lastIndex != -1) {
            String result = className.substring(lastIndex + 1);
            if (mPrefsMap.getBoolean(result)) {
                findAndHookConstructor(findClassIfExists(className), parameterTypesAndCallback);
            } else {
                checkAndRun(tempStringS, result,
                    () -> findAndHookConstructor(
                        findClassIfExists(className),
                        parameterTypesAndCallback),
                    "findAndHookConstructor the class: " + className + " ; ");
            }
        } else {
            logE(tag, "Cant get key, class: " + className);
        }
    }

    public void hookAllMethods(String className, String methodName, HookAction callback) {
        try {
            Class<?> hookClass = findClassIfExists(className);
            if (hookClass == null) {
                logE(tag, "Hook class: " + className + " method: " + methodName + " is Null");
                return;
            }
            int lastIndex = className.lastIndexOf(".");
            if (mPrefsMap.getBoolean(methodName)) {
                hookAllMethods(hookClass, methodName, callback);
            } else {
                if (lastIndex != -1) {
                    String result = className.substring(lastIndex + 1);
                    if (mPrefsMap.getBoolean(result + "_" + methodName)) {
                        hookAllMethods(hookClass, methodName, callback);
                    } else {
                        if (mPrefsMap.getBoolean(result)) {
                            hookAllMethods(hookClass, methodName, callback);
                        } else {
                            checkAndRun(tempString, methodName,
                                () -> hookAllMethods(
                                    hookClass,
                                    methodName,
                                    callback),
                                "hookAllMethods the class: " + className + " ; ");
                        }
                    }
                }
            }
        } catch (Throwable e) {
            logE(tag, "Hook The: " + e + " Error");
        }
    }

    public void hookAllMethods(Class<?> hookClass, String methodName, HookAction callback) {
        try {
            int Num = XposedBridge.hookAllMethods(hookClass, methodName, callback).size();
            logI(tag, "Hook: " + hookClass + " methodName: " + methodName + " Num is: " + Num);
        } catch (Throwable e) {
            logE(tag, "Hook The: " + e + " Error");
        }
    }

    public void hookAllConstructors(String className, HookAction callback) {
        Class<?> hookClass = findClassIfExists(className);
        if (hookClass != null) {
            hookAllConstructors(hookClass, callback);
        }
    }

    public void hookAllConstructors(Class<?> hookClass, HookAction callback) {
        try {
            XposedBridge.hookAllConstructors(hookClass, callback);
        } catch (Throwable f) {
            logE(tag, "hookAllConstructors error: " + f + " class: " + hookClass);
        }
    }

    public void hookAllConstructors(String className, ClassLoader classLoader, HookAction callback) {
        Class<?> hookClass = XposedHelpers.findClassIfExists(className, classLoader);
        if (hookClass != null) {
            hookAllConstructors(hookClass, callback);
        }
    }

    public void callMethod(Object obj, String methodName, Object... args) {
        XposedHelpers.callMethod(obj, methodName, args);
    }

    public void callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        XposedHelpers.callStaticMethod(clazz, methodName, args);
    }

    public void getDeclaredField(XC_MethodHook.MethodHookParam param, String iNeedString, Object iNeedTo) {
        if (param != null) {
            try {
                Field setString = param.thisObject.getClass().getDeclaredField(iNeedString);
                setString.setAccessible(true);
                try {
                    setString.set(param.thisObject, iNeedTo);
                    Object result = setString.get(param.thisObject);
                    checkLast("getDeclaredField", iNeedString, iNeedTo, result);
                } catch (IllegalAccessException e) {
                    logE(tag, "IllegalAccessException to: " + iNeedString + " Need to: " + iNeedTo + " Code:" + e);
                }
            } catch (NoSuchFieldException e) {
                logE(tag, "No such the: " + iNeedString + " Code: " + e);
            }
        } else {
            logE(tag, "Param is null Code: " + iNeedString + " And: " + iNeedTo);
        }
    }

    public void checkLast(String setObject, Object fieldName, Object value, Object last) {
        if (value != null && last != null) {
            if (value == last || value.equals(last)) {
                logI(tag, setObject + " Success! set " + fieldName + " to " + value);
            } else {
                logE(tag, setObject + " Failed! set " + fieldName + " to " + value + " i hope is: " + value + " but is: " + last);
            }
        } else {
            logE(tag, setObject + " Error value: " + value + " or last: " + last + " is null");
        }
    }

    public void setInt(Object obj, String fieldName, int value) {
        checkAndHookField(obj, fieldName,
            () -> XposedHelpers.setIntField(obj, fieldName, value),
            () -> checkLast("setInt", fieldName, value,
                XposedHelpers.getIntField(obj, fieldName)));
    }

    public void setBoolean(Object obj, String fieldName, boolean value) {
        checkAndHookField(obj, fieldName,
            () -> XposedHelpers.setBooleanField(obj, fieldName, value),
            () -> checkLast("setBoolean", fieldName, value,
                XposedHelpers.getBooleanField(obj, fieldName)));
    }

    public void setObject(Object obj, String fieldName, Object value) {
        checkAndHookField(obj, fieldName,
            () -> XposedHelpers.setObjectField(obj, fieldName, value),
            () -> checkLast("setObject", fieldName, value,
                XposedHelpers.getObjectField(obj, fieldName)));
    }

    public void checkDeclaredMethod(String className, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        Class<?> hookClass = findClassIfExists(className);
        if (hookClass != null) {
            hookClass.getDeclaredMethod(name, parameterTypes);
            return;
        }
        throw new NoSuchMethodException();
    }

    public void checkDeclaredMethod(Class<?> clazz, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        if (clazz != null) {
            clazz.getDeclaredMethod(name, parameterTypes);
            return;
        }
        throw new NoSuchMethodException();
    }

    public void checkAndHookField(Object obj, String fieldName, Runnable setField, Runnable checkLast) {
        try {
            obj.getClass().getDeclaredField(fieldName);
            setField.run();
            checkLast.run();
        } catch (NoSuchFieldException e) {
            logE(tag, "No such field: " + fieldName + " in param: " + obj + " error: " + e);
        }
    }

    public void checkAndRun(List<String> mList, String methodName, Runnable mCode, String log) {
        List<String> endRetrieval = retrievalValue(methodName, allValue);
        StringBuilder combinedKeywords = new StringBuilder();
        if (!endRetrieval.isEmpty()) {
            for (int i = 0; i < endRetrieval.size(); i++) {
                combinedKeywords.append(endRetrieval.get(i));
                if (i < endRetrieval.size() - 1) {
                    combinedKeywords.append("_");
                }
            }
            mList.add(combinedKeywords.toString());
            boolean check = false;
            if (mPrefsMap.getBoolean(combinedKeywords.toString())) {
                if (mCode != null) mCode.run();
            } else {
                String find = null;
                for (int i = 0; i < mList.size(); i++) {
                    try {
                        Pattern pattern = Pattern.compile(".*" + methodName + ".*");
                        Matcher matcher = pattern.matcher(mList.get(i));
                        if (matcher.matches()) {
                            find = mList.get(i);
                            mList.clear();
                            check = true;
                            break;
                        }
                    } catch (Exception e) {
                        logW(tag, "Such key error: " + methodName);
                    }
                }
                if (mPrefsMap.getBoolean(find) && check) {
                    if (mCode != null) mCode.run();
                } else
                    logW(tag, log + combinedKeywords + " is false");
            }
        } else {
            logW(tag, log + methodName + " is false");
        }
    }

    public List<String> retrievalValue(String needString, String[] allValue) {
        List<String> matchedKeywords = new ArrayList<>();
        for (String keyWord : allValue) {
            if (isFuzzyMatch(keyWord, needString)) {
                matchedKeywords.add(keyWord);
            }
        }
        return matchedKeywords;
    }

    private boolean isFuzzyMatch(String keyWord, String needString) {
        try {
            String regex = ".*" + needString + ".*";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(keyWord);
            return matcher.matches();
        } catch (Exception e) {
            logW(tag, "Such key error: " + keyWord + " " + needString);
            return false;
        }
    }
}
