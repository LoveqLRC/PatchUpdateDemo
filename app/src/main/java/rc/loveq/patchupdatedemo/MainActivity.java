package rc.loveq.patchupdatedemo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import java.io.File;

import rc.loveq.patchupdatedemo.utils.PatchUtils;
import rc.loveq.patchupdatedemo.utils.SignatureUtils;

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
    public TextView mTvV1Signature, mTvV2Signature, mTvV1V2IsEqual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTvV1Signature = findViewById(R.id.tv_v1_signature);
        mTvV2Signature = findViewById(R.id.tv_v2_signature);
        mTvV1V2IsEqual = findViewById(R.id.tv_signature_is_equal);
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
        String v1Signature = SignatureUtils.getSignature(this);
        mTvV1Signature.setText(v1Signature);
        try {
            String v2signature = SignatureUtils.getSignature(mNewApkPath);
            mTvV2Signature.setText(v2signature);
            boolean isEqual = TextUtils.equals(v1Signature, v2signature);
            mTvV1V2IsEqual.setText("isEqual :" + isEqual);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //5.安装最新版本
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(mNewApkPath)),
                "application/vnd.android.package-archive");
        startActivity(intent);

    }
}
