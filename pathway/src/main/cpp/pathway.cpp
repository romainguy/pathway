/*
 * Copyright (C) 2021 Romain Guy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "PathIterator.h"

#include <jni.h>

#include <sys/system_properties.h>

#include <mutex>

#define JNI_CLASS_NAME "dev/romainguy/graphics/path/PathIterator"

#if !defined(NDEBUG)
#include <android/log.h>
#define ANDROID_LOG_TAG "PathIterator"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, ANDROID_LOG_TAG, __VA_ARGS__)
#endif

struct {
    jclass jniClass;
    jfieldID nativePath;
} sPath{};

uint32_t sApiLevel = 0;
std::once_flag sApiLevelOnceFlag;

static int api_level() {
    std::call_once(sApiLevelOnceFlag, []() {
        char sdkVersion[PROP_VALUE_MAX];
        __system_property_get("ro.build.version.sdk", sdkVersion);
        sApiLevel = atoi(sdkVersion); // NOLINT(cert-err34-c)
    });
    return sApiLevel;
}

static jlong createPathIterator(JNIEnv* env, jobject,
        jobject path_, jint conicEvaluation_, jfloat tolerance_) {

    auto nativePath = static_cast<intptr_t>(env->GetLongField(path_, sPath.nativePath));
    auto* path = reinterpret_cast<Path*>(nativePath);

    Point* points;
    Verb* verbs;
    float* conicWeights;
    int count;
    PathIterator::VerbDirection direction;

    const int apiLevel = api_level();
    if (apiLevel >= 30) {
        auto* ref = reinterpret_cast<PathRef30*>(path->pathRef);
        points = ref->points;
        verbs = ref->verbs;
        conicWeights = ref->conicWeights;
        count = ref->verbCount;
        direction = PathIterator::VerbDirection::Forward;
    } else if (apiLevel >= 26) {
        auto* ref = reinterpret_cast<PathRef26*>(path->pathRef);
        points = ref->points;
        verbs = ref->verbs;
        conicWeights = ref->conicWeights;
        count = ref->verbCount;
        direction = PathIterator::VerbDirection::Backward;
    } else if (apiLevel >= 24) {
        auto* ref = reinterpret_cast<PathRef24*>(path->pathRef);
        points = ref->points;
        verbs = ref->verbs;
        conicWeights = ref->conicWeights;
        count = ref->verbCount;
        direction = PathIterator::VerbDirection::Backward;
    } else {
        auto* ref = path->pathRef;
        points = ref->points;
        verbs = ref->verbs;
        conicWeights = ref->conicWeights;
        count = ref->verbCount;
        direction = PathIterator::VerbDirection::Backward;
    }

    return jlong(new PathIterator(
            points, verbs, conicWeights, count, direction,
            PathIterator::ConicEvaluation(conicEvaluation_), tolerance_
    ));
}

static void destroyPathIterator(JNIEnv*, jobject, jlong pathIterator_) {
    delete reinterpret_cast<PathIterator*>(pathIterator_);
}

static jboolean pathIteratorHasNext(JNIEnv*, jobject, jlong pathIterator_) {
    return reinterpret_cast<PathIterator*>(pathIterator_)->hasNext();
}

static jint pathIteratorNext(JNIEnv* env, jobject, jlong pathIterator_, jfloatArray points_) {
    auto pathIterator = reinterpret_cast<PathIterator*>(pathIterator_);
    Point pointsData[4];
    Verb verb = pathIterator->next(pointsData);

    if (verb != Verb::Done && verb != Verb::Close) {
        jfloat* points = env->GetFloatArrayElements(points_, nullptr);
        switch (verb) {
            case Verb::Cubic:
            case Verb::Conic: // to copy the weight
                points[6] = pointsData[3].x;
                points[7] = pointsData[3].y;
            case Verb::Quadratic:
                points[4] = pointsData[2].x;
                points[5] = pointsData[2].y;
            case Verb::Move:
            case Verb::Line:
                points[2] = pointsData[1].x;
                points[3] = pointsData[1].y;
                points[0] = pointsData[0].x;
                points[1] = pointsData[0].y;
                break;
#pragma clang diagnostic push
#pragma ide diagnostic ignored "UnreachableCode"
            case Verb::Close:
            case Verb::Done:
                break;
#pragma clang diagnostic pop
        }
        env->ReleaseFloatArrayElements(points_, points, 0);
    }

    return static_cast<jint>(verb);
}

static jint pathIteratorPeek(JNIEnv*, jobject, jlong pathIterator_) {
    return static_cast<jint>(reinterpret_cast<PathIterator *>(pathIterator_)->peek());
}

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void*) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    sPath.jniClass = env->FindClass("android/graphics/Path");
    if (sPath.jniClass == nullptr) return JNI_ERR;

    sPath.nativePath = env->GetFieldID(sPath.jniClass, "mNativePath", "J");
    if (sPath.nativePath == nullptr) return JNI_ERR;

    {
        jclass pathsClass = env->FindClass(JNI_CLASS_NAME);
        if (pathsClass == nullptr) return JNI_ERR;

        static const JNINativeMethod methods[] = {
                {
                    (char*) "createInternalPathIterator",
                    (char*) "(Landroid/graphics/Path;IF)J",
                    reinterpret_cast<void*>(createPathIterator)
                },
                {
                    (char*) "destroyInternalPathIterator",
                    (char*) "(J)V",
                    reinterpret_cast<void*>(destroyPathIterator)
                },
                {
                    (char*) "internalPathIteratorHasNext",
                    (char*) "(J)Z",
                    reinterpret_cast<void*>(pathIteratorHasNext)
                },
                {
                    (char*) "internalPathIteratorNext",
                    (char*) "(J[F)I",
                    reinterpret_cast<void*>(pathIteratorNext)
                },
                {
                    (char*) "internalPathIteratorPeek",
                    (char*) "(J)I",
                    reinterpret_cast<void*>(pathIteratorPeek)
                },
        };

        int result = env->RegisterNatives(
                pathsClass, methods, sizeof(methods) / sizeof(JNINativeMethod)
        );
        if (result != JNI_OK) return result;

        env->DeleteLocalRef(pathsClass);
    }

    return JNI_VERSION_1_6;
}
