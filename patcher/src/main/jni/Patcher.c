#include "bspatch.h"
#include "Patcher.h"
#include <jni.h>
#include <stdlib.h>
#include <android/log.h>

JNIEXPORT jint JNICALL Java_com_baize_patcher_api_utils_AppUtils_patcher(JNIEnv* env,
		jobject othis, jstring argv1, jstring argv2, jstring argv3) {
	char *argv[4];
	int loopVar,result;

	__android_log_print(ANDROID_LOG_INFO, "bspatch.c", "开始打包");

	argv[0] = "bspatch";
	argv[1] = (char*)(*env)->GetStringUTFChars(env, argv1, 0);
	argv[2] = (char*)(*env)->GetStringUTFChars(env, argv2, 0);
	argv[3] = (char*)(*env)->GetStringUTFChars(env, argv3, 0);

	result = bspatch(argv);

	//Important:释放内存，防止内存泄露
	(*env)->ReleaseStringUTFChars(env, argv1, argv[1]);
	(*env)->ReleaseStringUTFChars(env, argv2, argv[2]);
	(*env)->ReleaseStringUTFChars(env, argv3, argv[3]);

	__android_log_print(ANDROID_LOG_INFO, "bspatch.c", "打包完成");

	return result;
}
