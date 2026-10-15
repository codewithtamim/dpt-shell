package com.luoyesiqiu.shell;

/**
 * LSPosed / Xposed in-process checks, ported from Duck-Detector
 * {@code feature/lsposed} (Apache-2.0): runtime classes + stack frames.
 * Native maps scan lives in {@code dpt_risk.cpp detectLsposed()}.
 * On hit, crashes via native {@code olh()} like other risk checks.
 */
public final class LsposedCheck {

    private LsposedCheck() {}

    // Split literals to avoid a single plain grep string in dex.
    private static String clsXposedBridge() {
        return "de.robv.android.xposed." + "XposedBridge";
    }

    private static String[] bridgeClasses() {
        return new String[]{
                clsXposedBridge(),
                "de.robv.android.xposed." + "XposedHelpers",
                "org.lsposed.lspd." + "core.Main",
                "org.lsposed.lspd.nativebridge." + "HookBridge",
                "org.lsposed.lspd.utils." + "ApkSignature",
        };
    }

    private static String[] stackTokens() {
        return new String[]{
                "xposedbridge",
                "lspHooker",
                "lsposed",
                "lsplant",
                "edxp",
                "lspd",
        };
    }

    private static void crash() {
        try {
            JniBridge.olh();
        } catch (Throwable ignored) {
            // Native not ready yet: fail closed without a clear log.
            try {
                android.os.Process.killProcess(android.os.Process.myPid());
            } catch (Throwable ignored2) {
            }
            System.exit(0);
        }
        // In case the native crash returns (it should not), halt.
        try {
            android.os.Process.killProcess(android.os.Process.myPid());
        } catch (Throwable ignored) {
        }
        System.exit(0);
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        if (haystack == null || needle == null) {
            return false;
        }
        int hlen = haystack.length();
        int nlen = needle.length();
        if (nlen == 0 || nlen > hlen) {
            return false;
        }
        for (int i = 0; i + nlen <= hlen; i++) {
            if (haystack.regionMatches(true, i, needle, 0, nlen)) {
                return true;
            }
        }
        return false;
    }

    public static void check() {
        // Respect --disable-lsposed-detect once native config is loaded.
        // Before native load, fail closed (still check).
        try {
            if (!JniBridge.lde()) {
                return;
            }
        } catch (Throwable ignored) {
            // Native not ready: fall through and check anyway.
        }
        // Never treat our own shell classes as evidence: this class's own
        // name contains "lsposed", so its stack frame would self-match.
        String ownPackage = "";
        try {
            Package pkg = LsposedCheck.class.getPackage();
            if (pkg != null) {
                ownPackage = pkg.getName();
            }
        } catch (Throwable ignored) {
        }
        // 1. Bridge classes reachable from app / boot loaders.
        String[] classes = bridgeClasses();
        for (String name : classes) {
            try {
                Class.forName(name, false, LsposedCheck.class.getClassLoader());
                crash();
                return;
            } catch (Throwable ignored) {
            }
            try {
                Class.forName(name);
                crash();
                return;
            } catch (Throwable ignored) {
            }
        }

        // 2. Framework frames in current + synthetic stacks.
        // Skips our own shell package (see above).
        String[] tokens = stackTokens();
        try {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (StackTraceElement e : stack) {
                String cn = e.getClassName();
                if (cn != null && !ownPackage.isEmpty() && cn.startsWith(ownPackage)) {
                    continue;
                }
                for (String t : tokens) {
                    if (containsIgnoreCase(cn, t)) {
                        crash();
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Thread.UncaughtExceptionHandler h = Thread.getDefaultUncaughtExceptionHandler();
            if (h != null) {
                String hn = h.getClass().getName();
                if (hn != null && !ownPackage.isEmpty() && hn.startsWith(ownPackage)) {
                    return;
                }
                for (String t : tokens) {
                    if (containsIgnoreCase(hn, t)) {
                        crash();
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
