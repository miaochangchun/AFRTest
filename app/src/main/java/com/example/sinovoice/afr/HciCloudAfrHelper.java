package com.example.sinovoice.afr;

import android.content.Context;
import android.util.Log;

import com.sinovoice.hcicloudsdk.api.HciCloudUser;
import com.sinovoice.hcicloudsdk.api.afr.HciCloudAfr;
import com.sinovoice.hcicloudsdk.common.HciErrorCode;
import com.sinovoice.hcicloudsdk.common.Session;
import com.sinovoice.hcicloudsdk.common.afr.AfrConfig;
import com.sinovoice.hcicloudsdk.common.afr.AfrDetectResult;
import com.sinovoice.hcicloudsdk.common.afr.AfrEnrollResult;
import com.sinovoice.hcicloudsdk.common.afr.AfrInitParam;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by miaochangchun on 2016/11/4.
 */
public class HciCloudAfrHelper {
    private static final String TAG = HciCloudAfrHelper.class.getSimpleName();
    private static HciCloudAfrHelper mHciCloudAfrHelper = null;

    private HciCloudAfrHelper(){

    }

    public static HciCloudAfrHelper getInstance() {
        if (mHciCloudAfrHelper == null) {
            return new HciCloudAfrHelper();
        }
        return mHciCloudAfrHelper;
    }

    /**
     * 人脸识别的初始化函数
     * @param context
     * @param initCapkeys
     * @return
     */
    public int initAfr(Context context, String initCapkeys) {
        String strConfig = getInitParam(context, initCapkeys);
        int errorCode = HciCloudAfr.hciAfrInit(strConfig);
        return errorCode;
    }

    /**
     *
     * @param context
     * @param initCapkeys
     * @return
     */
    private String getInitParam(Context context, String initCapkeys) {
        AfrInitParam afrInitParam = new AfrInitParam();
        String dataPath = context.getFilesDir().getAbsolutePath().replace("files", "lib");
        afrInitParam.addParam(AfrInitParam.PARAM_KEY_DATA_PATH, dataPath);
        afrInitParam.addParam(AfrInitParam.PARAM_KEY_FILE_FLAG, "android_so");
        afrInitParam.addParam(AfrInitParam.PARAM_KEY_INIT_CAP_KEYS, initCapkeys);
        return afrInitParam.getStringConfig();
    }

    /**
     * 释放人脸识别的函数
     * @return
     */
    public int releaseAfr() {
        return HciCloudAfr.hciAfrRelease();
    }

    /**
     * 人脸检测函数
     * @param fileName  人脸图片路径
     * @param afrCapkey 人脸识别的capkey，本地为afr.local.recog；云端为afr.cloud.recog
     * @return  人脸检测的结果类
     */
    public AfrDetectResult detectAfr(String fileName, String afrCapkey){
        byte[] buffer = getImageBuffer(fileName);
        if (buffer.length == 0) {
            Log.e(TAG, "图片数据错误");
            return null;
        }
        Session session = new Session();
        String sessionConfig = getSessionParam(afrCapkey);
        //开启session
        int errorCode = HciCloudAfr.hciAfrSessionStart(sessionConfig, session);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrSessionStart failed and return " + errorCode);
            return null;
        }
        //设置要识别的图片数据
        errorCode = HciCloudAfr.hciAfrSetImageBuffer(session, buffer);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrSetImageBuffer failed and return " + errorCode);
            return null;
        }
        //人脸检测的配置参数，如果设置为空，则使用sessionConfig的配置串
        String detectConfig = "";
        //识别结果的保存类
        AfrDetectResult afrDetectResult = new AfrDetectResult();
        //人脸检测的API函数
        errorCode = HciCloudAfr.hciAfrDetect(session, detectConfig, afrDetectResult);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrDetect failed and return " + errorCode);
            return null;
        }
        if(afrDetectResult.getFaceList().size() > 0){
            Log.d(TAG, "人脸检测结果正确");
        }
        //释放人脸检测的结果类
//        errorCode = HciCloudAfr.hciAfrFreeDetectResult(afrDetectResult);
//        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
//            Log.e(TAG, "HciCloudAfr.hciAfrFreeDetectResult failed and return " + errorCode);
//            return errorCode;
//        }
        //释放Session
        errorCode = HciCloudAfr.hciAfrSessionStop(session);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrSessionStop failed and return " + errorCode);
            return null;
        }
        return afrDetectResult;
    }

    /**
     * 人脸识别的配置串参数
     * @param afrCapkey
     * @return
     */
    private String getSessionParam(String afrCapkey) {
        AfrConfig sessionConfig = new AfrConfig();
        sessionConfig.addParam(AfrConfig.SessionConfig.PARAM_KEY_CAP_KEY, afrCapkey);
        return sessionConfig.getStringConfig();
    }

    /**
     * 把图片数据转为byte数组
     * @param fileName
     * @return
     */
    private byte[] getImageBuffer(String fileName) {
        File file = new File(fileName);
        long fileSize = file.length();
        if (fileSize > Integer.MAX_VALUE) {
            Log.e(TAG, "图片太大~");
            return  null;
        }
        FileInputStream inputStream = null;
        byte[] buffer = null;
        try {
            inputStream = new FileInputStream(file);
            buffer = new byte[(int) fileSize];
            int offset = 0;
            int numRead = 0;
            while (offset < buffer.length && (numRead = inputStream.read(buffer, offset, buffer.length - offset)) >= 0) {
                offset += numRead;
            }
            if (offset != buffer.length) {
                throw new IOException("Could not completely read file " + file.getName());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return buffer;
    }

    /**
     * 人脸注册功能
     * @param fileName
     * @param afrCapkey
     * @param userId
     * @return
     */
    public int enrollAfr(String fileName, String afrCapkey, String userId) {
        byte[] buffer = getImageBuffer(fileName);
        if (buffer.length == 0) {
            Log.e(TAG, "读取图片错误。");
            return -1;
        }
        Session session = new Session();
        String sessionConfig = getSessionParam(afrCapkey);
        int errorCode = HciCloudAfr.hciAfrSessionStart(sessionConfig, session);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrSessionStart failed and return " + errorCode);
            return errorCode;
        }
        errorCode = HciCloudAfr.hciAfrSetImageBuffer(session, buffer);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrSetImageBuffer failed and return " + errorCode);
            return errorCode;
        }
        String detectConfig = "";
        AfrDetectResult afrDetectResult = new AfrDetectResult();
        errorCode = HciCloudAfr.hciAfrDetect(session, detectConfig, afrDetectResult);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrDetect failed and return " + errorCode);
            return errorCode;
        }
        //检测到的人脸Id
        String faceId = afrDetectResult.getFaceList().get(0).getFaceId();
        //使用本地的人脸检测功能，组名只能设置为空
        String groupName = "";
        ArrayList<String> lists = new ArrayList<>();
        //把groupName组下的所有用户保存到lists里面
        HciCloudUser.hciGetUserlist(groupName, lists);
        if (lists.contains(userId)) {
            Log.e(TAG, "userId已经存在，请重新输入");
        }else {
            //把用户添加到组名下
            HciCloudUser.hciAddUser(groupName, userId);
        }
        String enrollConfig = "faceId = " + faceId + ",userId = " + userId + ",enrollMode = append";
        AfrEnrollResult afrEnrollResult = new AfrEnrollResult();
        errorCode = HciCloudAfr.hciAfrEnroll(session, enrollConfig, afrEnrollResult);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrEnroll failed and return " + errorCode);
            return errorCode;
        }
        //释放检测结果。
        errorCode = HciCloudAfr.hciAfrFreeDetectResult(afrDetectResult);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrFreeDetectResult failed and return " + errorCode);
            return errorCode;
        }
        // 关闭会话
        errorCode = HciCloudAfr.hciAfrSessionStop(session);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrSessionStop failed and return " + errorCode);
            return errorCode;
        }
        return HciErrorCode.HCI_ERR_NONE;
    }

    //暂不实现
    public int verifyAfr(String fileName, String afrCapkey, String userId){
        return 0;
    }

    //暂不实现
    public int identifyAfr() {
        return HciErrorCode.HCI_ERR_NONE;
    }
}
