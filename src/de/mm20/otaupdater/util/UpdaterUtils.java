package de.mm20.otaupdater.util;

import android.os.SystemProperties;

import java.text.DecimalFormat;

public class UpdaterUtils {

    private static String sDevice;
    private static int sBuildDate;

    static {
        sDevice = SystemProperties.get("ro.cm.device");
        String currentVersion = SystemProperties.get("ro.cm.version");
        int start = currentVersion.indexOf('-') + 1;
        sBuildDate = Integer.parseInt(currentVersion.substring(start, start + 8));
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
        String patchLevel = SystemProperties.get("ro.cm.patchlevel");
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
