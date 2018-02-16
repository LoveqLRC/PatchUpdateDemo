package rc.loveq.patchupdatedemo.utils;

/**
 * Author：Rc
 * 0n 2018/2/16 11:40
 */

public class PatchUtils {

    static {
        System.loadLibrary("bspatch");
    }

    /**
     * @param oldApkPath 原来的本地安装的apk路径
     * @param newApkPath 与差分包合并后新的apk路径
     * @param patchPath  差分包路径，从服务器下载下来
     */
    public static native void patch(String oldApkPath, String newApkPath,
                                    String patchPath);
}
