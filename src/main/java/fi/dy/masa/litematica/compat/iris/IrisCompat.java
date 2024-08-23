package fi.dy.masa.litematica.compat.iris;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class IrisCompat {
    private static Object API;
    private static MethodHandle isShaderActive;
    private static MethodHandle isShadowPassActive;
    public static boolean isIrisActive;

    static {
        try {
            Class<?> irisAPI = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            API = irisAPI.getMethod("getInstance").invoke(null);
            isShaderActive = MethodHandles.lookup().findVirtual(irisAPI, "isShaderPackInUse", MethodType.methodType(boolean.class));
            isShadowPassActive = MethodHandles.lookup().findVirtual(irisAPI, "isRenderingShadowPass", MethodType.methodType(boolean.class));
            isIrisActive = true;
        } catch (Exception e) {
            isIrisActive = false;
        }
    }

    public static boolean isShaderActive() {
        if (!isIrisActive) return false;

        try {
            return (boolean) isShaderActive.invoke(API);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isShadowPassActive() {
        if (!isIrisActive) return false;

        try {
            return (boolean) isShadowPassActive.invoke(API);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
