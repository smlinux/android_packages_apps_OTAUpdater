package de.mm20.otaupdater.util;

import android.os.SystemProperties;

import java.text.DecimalFormat;

public class UpdaterUtils {

    private static String sDevice;
    private static long sBuildDate;

    static {
        sDevice = SystemProperties.get("ro.product.device");
        sBuildDate = Long.parseLong(SystemProperties.get("ro.build.date.utc"));
    }

    public static boolean isUpdateCompatible(int newBuildDate, int patchLevel, String device) {
        return sDevice.equals(device) && ((sBuildDate <= newBuildDate && patchLevel == 0) ||
                (sBuildDate == newBuildDate && patchLevel > getSystemPatchLevel()));
    }

    public static boolean isUpdateNew(int newBuildDate, int patchLevel, String device) {
        return sDevice.equals(device) && ((sBuildDate < newBuildDate && patchLevel == 0) ||
                (sBuildDate == newBuildDate && patchLevel > getSystemPatchLevel()));
    }

    public static boolean isBuildInstalled(int newBuildDate, int patchLevel, String device){
        return isUpdateCompatible(newBuildDate, patchLevel, device) &&
                !isUpdateNew(newBuildDate, patchLevel, device);
    }

    public static int getSystemPatchLevel() {
        String patchLevel = SystemProperties.get("ro.build.patchlevel");
        if (patchLevel.isEmpty()) return 0;
        return Integer.parseInt(patchLevel);
    }

    public static String fileSizeAsString(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " +
                units[digitGroups];
    }
}
