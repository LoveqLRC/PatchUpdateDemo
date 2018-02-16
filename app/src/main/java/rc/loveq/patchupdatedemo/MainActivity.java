package rc.loveq.patchupdatedemo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.io.File;

import rc.loveq.patchupdatedemo.utils.PatchUtils;

public class MainActivity extends AppCompatActivity {

    /**
     * 差分补丁路径
     */
    private String mPatchPath = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + File.separator + "patch.patch";

    /**
     * 旧apk和差分补丁合并后的新apk路径
     */
    private String mNewApkPath = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + File.separator + "version2.0.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    public void patch(View view) {
        //1.访问后台，是否需要更新版本
        //2.需要更新版本，提示下载
        //3.下载完成差分之后，调用patch方法合成新的apk
        if (!new File(mPatchPath).exists()) {
            return;
        }
        //这是一个耗时操作应该新开一个线程处理
        PatchUtils.patch(getPackageResourcePath(), mNewApkPath, mPatchPath);

        //4.校验签名
        //5.安装最新版本
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(mNewApkPath)),
                "application/vnd.android.package-archive");
        startActivity(intent);

    }
}
