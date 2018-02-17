# 增量更新

1.定义native增量更新方法

	public class PatchUtils {
    /**
     * @param oldApkPath 原来的本地安装的apk路径
     * @param newApkPath 与差分包合并后新的apk路径
     * @param patchPath  差分包路径，从服务器下载下来
     */
    public static native void patch(String oldApkPath, String newApkPath,
                                    String patchPath);
	}

2. rebulid项目，进入项目\app\build\intermediates\classes\debug目录下，执行
 `javah (项目名).PatchUtils(例如rc.loveq.patchupdatedemo.utils.PatchUtils)` 此时在app\build\intermediates\classes\debug生成.h头文件

2.将生成的.h头文件复制到jni目录下，将bsdiff和bzip里面的的C文件和.h文件复制到jni目录下

3.修改CMakeLists，修改生成的so的名字改为bspatch，修改编译的入口改为bspatch.c

4.这时运行是编译不通过的，需要修改bspatch.c，将#include <bzlib.h>改成#include "bzlib.h"，另外要引入
	#include "bzlib.c"
	#include "crctable.c"
	#include "compress.c"
	#include "decompress.c"
	#include "randtable.c"
	#include "blocksort.c"
	#include "huffman.c"
以上文件。

5.经过上面几步，运行项目可以正常运行了，另外可以在\app\build\intermediates\cmake\debug\obj目录下，看到生成了对应的libbspatch.so文件。

6.接下来实现patch的native方法，bspatch.c引入#include "rc_loveq_patchupdatedemo_utils_PatchUtils.h"头文件，将main方法改为patch，编写patch的native方法

	JNIEXPORT void JNICALL Java_rc_loveq_patchupdatedemo_utils_PatchUtils_patch
	  (JNIEnv *env, jclass jclz, jstring old_apk_path, jstring new_apk_path,
	      jstring patch_path){
	    // 1.封装参数
	  	int argc = 4;
	  	char * argv[4];
	  	// 1.1 转换  jstring -> char*
	  	char* old_pak_cstr = (char*)(*env)->GetStringUTFChars(env,old_apk_path,NULL);
	  	char* new_apk_cstr = (char*)(*env)->GetStringUTFChars(env,new_apk_path,NULL);
	  	char* patch_cstr = (char*)(*env)->GetStringUTFChars(env,patch_path,NULL);
	  	// 第0的位置随便给
	  	argv[0] = "combine";
	  	argv[1] = old_pak_cstr;
	  	argv[2] = new_apk_cstr;
	  	argv[3] = patch_cstr;
	
	  	// 2.调用上面的方法  int argc,char * argv[]
	  	patch(argc,argv);
	
	  	// 3.释放资源
	  	(*env)->ReleaseStringUTFChars(env,old_apk_path,old_pak_cstr);
	  	(*env)->ReleaseStringUTFChars(env,new_apk_path,new_apk_cstr);
	  	(*env)->ReleaseStringUTFChars(env,patch_path,patch_cstr);
	
	  }

7.java层调用patch方法

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

8.最后服务器生成差分包

	bsdiff old.apk new.apk patch.patch


服务器合成差分包
	
	bspatch old.apk new.apk patch.patch
	

9.签名校验

	public class SignatureUtils {
    private static final String TAG = "SignatureUtils";
    /**
     * 获取当前apk的签名
     *
     * @param context
     * @return
     */
    public static String getSignature(Context context) {
        // 通过Context获取当前包名
        String currentApkPackageName = context.getApplicationInfo().packageName;

        Log.e(TAG, "TAG -> " + currentApkPackageName);

        // 通过PackageManager获取所有应用的PackageInfo信息
        PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(PackageManager.GET_SIGNATURES);
        for (PackageInfo packageInfo : packageInfos) {
            if (packageInfo.packageName.equals(currentApkPackageName)) {
                // 获取签名
                return packageInfo.signatures[0].toCharsString();
            }
        }
        return null;
    }

    /**
     * 获取该目录下的apk签名
     *
     * @param path 当前apk路径
     * @return
     */
    public static String getSignature(String path) throws Exception {
        // 1.反射实例化PackageParser对象
        Object packageParser = getPackageParser(path);

        // 2.反射获取parsePackage方法
        Object packageObject = getPackageInfo(path, packageParser);

        // 3.调用collectCertificates方法
        Method collectCertificatesMethod = packageParser.getClass().
                getDeclaredMethod("collectCertificates", packageObject.getClass(), int.class);
        collectCertificatesMethod.invoke(packageParser, packageObject, 0);

        // 4.获取mSignatures属性
        Field signaturesField = packageObject.getClass().getDeclaredField("mSignatures");
        signaturesField.setAccessible(true);
        Signature[] mSignatures = (Signature[]) signaturesField.get(packageObject);
        return mSignatures[0].toCharsString();
    }

    /**
     * 创建PackageParser.Package类，兼容5.0
     *
     * @param path
     * @return
     * @throws Exception
     */
    private static Object getPackageInfo(String path, Object packageParser) throws Exception {
        if (Build.VERSION.SDK_INT >= 21) {
            Class<?>[] paramClass = new Class[2];
            paramClass[0] = File.class;
            paramClass[1] = int.class;
            Method parsePackageMethod = packageParser.getClass().getDeclaredMethod("parsePackage", paramClass);

            // 3.反射执行parsePackage方法
            Object[] paramObject = new Object[2];
            paramObject[0] = new File(path);
            paramObject[1] = 0;
            parsePackageMethod.setAccessible(true);
            return parsePackageMethod.invoke(packageParser, paramObject);
        } else {
            Class<?>[] paramClass = new Class[4];
            paramClass[0] = File.class;
            paramClass[1] = String.class;
            paramClass[2] = DisplayMetrics.class;
            paramClass[3] = int.class;
            Method parsePackageMethod = packageParser.getClass().getDeclaredMethod("parsePackage", paramClass);

            // 3.反射执行parsePackage方法
            Object[] paramObject = new Object[4];
            paramObject[0] = new File(path);
            paramObject[1] = path;
            DisplayMetrics metrics = new DisplayMetrics();
            metrics.setToDefaults();
            paramObject[2] = metrics;
            paramObject[3] = 0;
            parsePackageMethod.setAccessible(true);
            return parsePackageMethod.invoke(packageParser, paramObject);
        }
    }

    /**
     * 创建PackageParser类
     *
     * @param path
     * @return
     * @throws Exception
     */
    private static Object getPackageParser(String path) throws Exception {
        Class<?> packageParserClazz = Class.forName("android.content.pm.PackageParser");
        // 版本兼容
        if (Build.VERSION.SDK_INT >= 21) {
            Constructor<?> packageParserConstructor = packageParserClazz.getDeclaredConstructor();
            return packageParserConstructor.newInstance();
        } else {
            Constructor<?> packageParserConstructor = packageParserClazz.getDeclaredConstructor(String.class);
            return packageParserConstructor.newInstance(path);
        }
    }


	}

	
