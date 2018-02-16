package rc.loveq.patchupdatedemo.utils;

/**
 * Author：Rc
 * 0n 2018/2/16 11:40
 */

public class PatchUtils {
    /**
     * @param oldApkPath 原来的本地安装的apk路径
     * @param newApkPath 与查分包合并后新的apk路径
     * @param patchPath  查分包路径，从服务器下载下来
     */
    public static native void patch(String oldApkPath, String newApkPath,
                                    String patchPath);
}
