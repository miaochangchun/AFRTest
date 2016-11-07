package com.example.sinovoice.afr;

import android.content.Context;
import android.util.Log;

import com.sinovoice.hcicloudsdk.api.HciCloudUser;
import com.sinovoice.hcicloudsdk.api.afr.HciCloudAfr;
import com.sinovoice.hcicloudsdk.common.HciErrorCode;
import com.sinovoice.hcicloudsdk.common.Session;
import com.sinovoice.hcicloudsdk.common.afr.AfrConfig;
import com.sinovoice.hcicloudsdk.common.afr.AfrDetectFace;
import com.sinovoice.hcicloudsdk.common.afr.AfrDetectFaceAttribute;
import com.sinovoice.hcicloudsdk.common.afr.AfrDetectFacebox;
import com.sinovoice.hcicloudsdk.common.afr.AfrDetectGenderAttribute;
import com.sinovoice.hcicloudsdk.common.afr.AfrDetectLandmark;
import com.sinovoice.hcicloudsdk.common.afr.AfrDetectLeftEyeAttribute;
import com.sinovoice.hcicloudsdk.common.afr.AfrDetectMouthAttribute;
import com.sinovoice.hcicloudsdk.common.afr.AfrDetectPoseAttribute;
import com.sinovoice.hcicloudsdk.common.afr.AfrDetectResult;
import com.sinovoice.hcicloudsdk.common.afr.AfrDetectRightEyeAttribute;
import com.sinovoice.hcicloudsdk.common.afr.AfrDetectSkinAttribute;
import com.sinovoice.hcicloudsdk.common.afr.AfrEnrollResult;
import com.sinovoice.hcicloudsdk.common.afr.AfrInitParam;
import com.sinovoice.hcicloudsdk.common.afr.AfrVerifyResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

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
    public JSONObject detectAfr(String fileName, String afrCapkey){
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
        String detectConfig = "gender=yes";
        //识别结果的保存类
        AfrDetectResult afrDetectResult = new AfrDetectResult();
        //人脸检测的API函数
        errorCode = HciCloudAfr.hciAfrDetect(session, detectConfig, afrDetectResult);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrDetect failed and return " + errorCode);
            return null;
        }

        JSONObject jsonObject = new JSONObject();           //总的json串  json串格式：{"afrdetectface":[{"landmark":[{"y":166,"x":109},{"y":165,"x":140},{"y":163,"x":193},{"y":162,"x":223},{"y":249,"x":137},{"y":248,"x":195},{"y":207,"x":162}],"gender":0,"faceid":"1942405736","facebox":{"bottom":319,"right":258,"left":74,"top":109}}]}
        try {
            JSONArray jsonArray = new JSONArray();
            if (afrDetectResult.getFaceList().size() > 0) {       //检测到人脸
                JSONObject jsonObject1 = new JSONObject();
                Log.d(TAG, "人脸检测结果正确");
                ArrayList<AfrDetectFace> afrDetectFaces = afrDetectResult.getFaceList();
                Iterator<AfrDetectFace> detectFaceIterator = afrDetectFaces.iterator();
                while (detectFaceIterator.hasNext()) {
                    AfrDetectFace afrDetectFace = detectFaceIterator.next();
                    //1.人脸属性列表
                    ArrayList<AfrDetectFaceAttribute> afrDetectFaceAttributes = afrDetectFace.getAttributeList();
                    Iterator<AfrDetectFaceAttribute> detectFaceAttributeIterator = afrDetectFaceAttributes.iterator();
                    //只支持性别检测功能
                    while (detectFaceAttributeIterator.hasNext()) {
                        AfrDetectFaceAttribute afrDetectFaceAttribute = detectFaceAttributeIterator.next();
                        if (afrDetectFaceAttribute instanceof AfrDetectGenderAttribute) {
                            int gender = ((AfrDetectGenderAttribute) afrDetectFaceAttribute).getGender();
                            Log.d(TAG, "性别：" + gender);
                            jsonObject1.put("gender", gender);
                        }
                    }

                    //2.人脸位置信息
                    JSONObject jsonObject2 = new JSONObject();
                    AfrDetectFacebox afrDetectFacebox = afrDetectFace.getFacebox();
                    int bottom = afrDetectFacebox.getBottom();
                    Log.d(TAG, "bottom:" + bottom);
                    jsonObject2.put("bottom", bottom);
                    int top = afrDetectFacebox.getTop();
                    Log.d(TAG, "top:" + top);
                    jsonObject2.put("top", top);
                    int left = afrDetectFacebox.getLeft();
                    Log.d(TAG, "left:" + left);
                    jsonObject2.put("left", left);
                    int right = afrDetectFacebox.getRight();
                    Log.d(TAG, "right:" + right);
                    jsonObject2.put("right", right);
                    jsonObject1.put("facebox", jsonObject2);

                    //3.获取人脸Id
                    String faceId = afrDetectFace.getFaceId();
                    Log.d(TAG, "faceId:" + faceId);
                    jsonObject1.put("faceid", faceId);

                    //4.获取人脸关键点坐标
                    JSONArray jsonArray1 = new JSONArray();

                    ArrayList<AfrDetectLandmark> afrDetectLandmarks = afrDetectFace.getLandmarkList();
                    Iterator<AfrDetectLandmark> detectLandmarkIterator = afrDetectLandmarks.iterator();
                    while (detectLandmarkIterator.hasNext()) {
                        JSONObject jsonObject3 = new JSONObject();
                        AfrDetectLandmark afrDetectLandmark = detectLandmarkIterator.next();
                        int x = afrDetectLandmark.getX();
                        Log.d(TAG, "x:" + x);
                        jsonObject3.put("x", x);
                        int y = afrDetectLandmark.getY();
                        Log.d(TAG, "y:" + y);
                        jsonObject3.put("y", y);
                        jsonArray1.put(jsonObject3);
                    }
                    jsonObject1.put("landmark", jsonArray1);

                    jsonArray.put(jsonObject1);
                }
                jsonObject.put("afrdetectface", jsonArray);
            } else {
                Log.d(TAG, "没检测到人脸！");
                return null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "json串：" + jsonObject.toString());

        //释放人脸检测的结果类
        errorCode = HciCloudAfr.hciAfrFreeDetectResult(afrDetectResult);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrFreeDetectResult failed and return " + errorCode);
            return null;
        }
        //释放Session
        errorCode = HciCloudAfr.hciAfrSessionStop(session);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrSessionStop failed and return " + errorCode);
            return null;
        }
        return jsonObject;
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
        if (afrDetectResult.getFaceList().size() > 0) {         //检测到人脸，才能使用注册功能
            //检测到的人脸Id
            String faceId = afrDetectResult.getFaceList().get(0).getFaceId();
            //使用本地的人脸检测功能，组名只能设置为空
            String groupName = "";
            //把用户添加到组里面，本地组名只能设置为空
            HciCloudUserHelper.getInstance().addUser(groupName, userId);
            String enrollConfig = "faceId = " + faceId + ",userId = " + userId + ",enrollMode = append";
            AfrEnrollResult afrEnrollResult = new AfrEnrollResult();
            errorCode = HciCloudAfr.hciAfrEnroll(session, enrollConfig, afrEnrollResult);
            if (errorCode != HciErrorCode.HCI_ERR_NONE) {
                Log.e(TAG, "HciCloudAfr.hciAfrEnroll failed and return " + errorCode);
                return errorCode;
            }

            Log.d(TAG, "afrEnrollResult.getUserId() = " + afrEnrollResult.getUserId());
        } else {
            Log.e(TAG, "没检测到人脸");
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

    public boolean verifyAfr(String fileName, String afrCapkey, String userId){
        byte[] buffer = getImageBuffer(fileName);
        if (buffer.length == 0) {
            Log.e(TAG, "读取图片错误。");
            return false;
        }
        Session session = new Session();
        String sessionConfig = getSessionParam(afrCapkey);
        int errorCode = HciCloudAfr.hciAfrSessionStart(sessionConfig, session);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrSessionStart failed and return " + errorCode);
            return false;
        }
        errorCode = HciCloudAfr.hciAfrSetImageBuffer(session, buffer);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrSetImageBuffer failed and return " + errorCode);
            return false;
        }
        String detectConfig = "";
        AfrDetectResult afrDetectResult = new AfrDetectResult();
        errorCode = HciCloudAfr.hciAfrDetect(session, detectConfig, afrDetectResult);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrDetect failed and return " + errorCode);
            return false;
        }

        AfrVerifyResult afrVerifyResult = null;
        //检测到人脸
        if (afrDetectResult.getFaceList().size() > 0) {
            String faceId = afrDetectResult.getFaceList().get(0).getFaceId();
            String enrollConfig = "faceId = " + faceId + ",userId = " + userId + ",threshold = 50";
            afrVerifyResult = new AfrVerifyResult();
            errorCode = HciCloudAfr.hciAfrVerify(session, enrollConfig, afrVerifyResult);
            if (errorCode != HciErrorCode.HCI_ERR_NONE) {
                Log.e(TAG, "HciCloudAfr.hciAfrVerify failed and return " + errorCode);
                return false;
            }
            if (afrVerifyResult.getStatus() == 0) {
                Log.d(TAG, "识别结果匹配，得分是：" + afrVerifyResult.getScore());
            }else{
                Log.e(TAG, "识别结果不匹配，得分是：" + afrVerifyResult.getScore());
                return false;
            }
        } else {
            Log.e(TAG, "没检测到人脸！");
        }

        // 关闭会话
        errorCode = HciCloudAfr.hciAfrSessionStop(session);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrSessionStop failed and return " + errorCode);
            return false;
        }

        return true;
    }

    //暂不实现
    public int identifyAfr() {
        return HciErrorCode.HCI_ERR_NONE;
    }
}
