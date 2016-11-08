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
import com.sinovoice.hcicloudsdk.common.afr.AfrIdentifyResult;
import com.sinovoice.hcicloudsdk.common.afr.AfrIdentifyResultItem;
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

        JSONObject jsonObject = null;
        if (afrDetectResult.getFaceList().size() > 0) {
            jsonObject = detectResultToJson(afrDetectResult);
        } else {
            Log.e(TAG, "没检测到人脸！");
        }

        //释放人脸检测的结果类，否则会有内存泄露
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
     * 把人脸检测的结果转换为json串，保存到json数据中
     * @param afrDetectResult   人脸检测的结果
     * @return  json数据串
     */
    private JSONObject detectResultToJson(AfrDetectResult afrDetectResult) {
        //总的json串  json串格式：{"afrdetectface":[{"landmark":[{"y":166,"x":109},{"y":165,"x":140},{"y":163,"x":193},{"y":162,"x":223},{"y":249,"x":137},{"y":248,"x":195},{"y":207,"x":162}],"gender":0,"faceid":"1942405736","facebox":{"bottom":319,"right":258,"left":74,"top":109}}]}
        JSONObject jsonObject = new JSONObject();
        try {
            JSONArray jsonArray = new JSONArray();  //第二层，json数组，可以包含多个人脸数据
            Log.d(TAG, "人脸检测结果正确");
            ArrayList<AfrDetectFace> afrDetectFaces = afrDetectResult.getFaceList();
            Iterator<AfrDetectFace> detectFaceIterator = afrDetectFaces.iterator();
            while (detectFaceIterator.hasNext()) {
                JSONObject faceJson = new JSONObject();          //检测到其中一个人脸的json数据，可能会有多个
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
                        faceJson.put("gender", gender);         //把性别添加到json数据里面，0代表男性，1代表女性
                    }
                }

                //2.人脸位置信息
                JSONObject faceboxJson = new JSONObject();      //把人脸的位置信息添加进来
                AfrDetectFacebox afrDetectFacebox = afrDetectFace.getFacebox();
                int bottom = afrDetectFacebox.getBottom();
                Log.d(TAG, "bottom:" + bottom);
                faceboxJson.put("bottom", bottom);
                int top = afrDetectFacebox.getTop();
                Log.d(TAG, "top:" + top);
                faceboxJson.put("top", top);
                int left = afrDetectFacebox.getLeft();
                Log.d(TAG, "left:" + left);
                faceboxJson.put("left", left);
                int right = afrDetectFacebox.getRight();
                Log.d(TAG, "right:" + right);
                faceboxJson.put("right", right);
                faceJson.put("facebox", faceboxJson);           //把人脸位置的json添加到人脸json串中

                //3.获取人脸Id
                String faceId = afrDetectFace.getFaceId();
                Log.d(TAG, "faceId:" + faceId);
                faceJson.put("faceId", faceId);                 //把人脸的faceId添加到Json串中

                //4.获取人脸关键点坐标
                JSONArray landmarkArray = new JSONArray();       //json数组用来添加人脸的关键点数据，共有七个关键点，需要保存到数组里面
                ArrayList<AfrDetectLandmark> afrDetectLandmarks = afrDetectFace.getLandmarkList();
                Iterator<AfrDetectLandmark> detectLandmarkIterator = afrDetectLandmarks.iterator();
                while (detectLandmarkIterator.hasNext()) {
                    JSONObject landmarkJson = new JSONObject();      //人脸的每一个关键点数据，之后需要保存到数组中
                    AfrDetectLandmark afrDetectLandmark = detectLandmarkIterator.next();
                    int x = afrDetectLandmark.getX();
                    Log.d(TAG, "x:" + x);
                    landmarkJson.put("x", x);
                    int y = afrDetectLandmark.getY();
                    Log.d(TAG, "y:" + y);
                    landmarkJson.put("y", y);
                    landmarkArray.put(landmarkJson);
                }
                faceJson.put("landmark", landmarkArray);

                jsonArray.put(faceJson);        //把人脸信息添加到json数组
            }
            jsonObject.put("detectFaceResult", jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        Log.d(TAG, "json串：" + jsonObject.toString());
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
    public String enrollAfr(String fileName, String afrCapkey, String userId) {
        byte[] buffer = getImageBuffer(fileName);
        if (buffer.length == 0) {
            Log.e(TAG, "读取图片错误。");
            return "读取图片错误!";
        }
        Session session = new Session();
        String sessionConfig = getSessionParam(afrCapkey);
        int errorCode = HciCloudAfr.hciAfrSessionStart(sessionConfig, session);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrSessionStart failed and return " + errorCode);
            return null;
        }
        errorCode = HciCloudAfr.hciAfrSetImageBuffer(session, buffer);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrSetImageBuffer failed and return " + errorCode);
            return null;
        }
        String detectConfig = "";
        AfrDetectResult afrDetectResult = new AfrDetectResult();
        errorCode = HciCloudAfr.hciAfrDetect(session, detectConfig, afrDetectResult);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrDetect failed and return " + errorCode);
            return null;
        }
        String result = "";
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
                return "人脸注册失败，错误码=" + errorCode;
            }
            result = "人脸注册成功，userId=" + afrEnrollResult.getUserId();
        } else {
            Log.e(TAG, "没检测到人脸");
            result = "没检测到人脸";
        }

        //释放检测结果。
        errorCode = HciCloudAfr.hciAfrFreeDetectResult(afrDetectResult);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrFreeDetectResult failed and return " + errorCode);
            return null;
        }
        // 关闭会话
        errorCode = HciCloudAfr.hciAfrSessionStop(session);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrSessionStop failed and return " + errorCode);
            return null;
        }
        return result;
    }

    /**
     *
     * @param fileName
     * @param afrCapkey
     * @param userId
     * @return
     */
    public String verifyAfr(String fileName, String afrCapkey, String userId){
        byte[] buffer = getImageBuffer(fileName);
        if (buffer.length == 0) {
            Log.e(TAG, "读取图片错误。");
            return "读取图片错误!";
        }
        Session session = new Session();
        String sessionConfig = getSessionParam(afrCapkey);
        int errorCode = HciCloudAfr.hciAfrSessionStart(sessionConfig, session);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrSessionStart failed and return " + errorCode);
            return null;
        }
        errorCode = HciCloudAfr.hciAfrSetImageBuffer(session, buffer);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrSetImageBuffer failed and return " + errorCode);
            return null;
        }
        String detectConfig = "";
        AfrDetectResult afrDetectResult = new AfrDetectResult();
        errorCode = HciCloudAfr.hciAfrDetect(session, detectConfig, afrDetectResult);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrDetect failed and return " + errorCode);
            return null;
        }

        String result = "";
        //检测到人脸
        if (afrDetectResult.getFaceList().size() > 0) {
            String faceId = afrDetectResult.getFaceList().get(0).getFaceId();
            String enrollConfig = "faceId = " + faceId + ",userId = " + userId + ",threshold = 50";
            AfrVerifyResult afrVerifyResult = new AfrVerifyResult();
            errorCode = HciCloudAfr.hciAfrVerify(session, enrollConfig, afrVerifyResult);
            if (errorCode != HciErrorCode.HCI_ERR_NONE) {
                Log.e(TAG, "HciCloudAfr.hciAfrVerify failed and return " + errorCode);
                return null;
            }
            if (afrVerifyResult.getStatus() == 0) {
                Log.d(TAG, "识别结果匹配，得分是：" + afrVerifyResult.getScore());
                result = "识别结果匹配，得分是：" + afrVerifyResult.getScore();
            }else{
                Log.e(TAG, "识别结果不匹配，得分是：" + afrVerifyResult.getScore());
                result = "识别结果不匹配，得分是：" + afrVerifyResult.getScore();
            }
        } else {
            Log.e(TAG, "没检测到人脸！");
            result = "没检测到人脸！";
        }

        //释放人脸检测的结果
        errorCode = HciCloudAfr.hciAfrFreeDetectResult(afrDetectResult);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrFreeDetectResult failed and return " + errorCode);
            return null;
        }
        // 关闭会话
        errorCode = HciCloudAfr.hciAfrSessionStop(session);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrSessionStop failed and return " + errorCode);
            return null;
        }
        return result;
    }

    /**
     *
     * @param fileName
     * @param afrCapkey
     * @return
     */
    public String identifyAfr(String fileName, String afrCapkey) {
        byte[] buffer = getImageBuffer(fileName);
        if (buffer.length == 0) {
            Log.e(TAG, "读取图片错误。");
            return "读取图片错误!";
        }
        Session session = new Session();
        String sessionConfig = getSessionParam(afrCapkey);
        int errorCode = HciCloudAfr.hciAfrSessionStart(sessionConfig, session);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrSessionStart failed and return " + errorCode);
            return null;
        }
        errorCode = HciCloudAfr.hciAfrSetImageBuffer(session, buffer);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrSetImageBuffer failed and return " + errorCode);
            return null;
        }
        String detectConfig = "";
        AfrDetectResult afrDetectResult = new AfrDetectResult();
        errorCode = HciCloudAfr.hciAfrDetect(session, detectConfig, afrDetectResult);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrDetect failed and return " + errorCode);
            return null;
        }

        String result = "";
        //检测到人脸
        if (afrDetectResult.getFaceList().size() > 0) {
            String faceId = afrDetectResult.getFaceList().get(0).getFaceId();
            String groupName = "";
            String enrollConfig = "faceId = " + faceId + ",threshold = 50,groupId=" + groupName + ",candnum=10";
            AfrIdentifyResult afrIdentifyResult = new AfrIdentifyResult();
            errorCode = HciCloudAfr.hciAfrIdentify(session, enrollConfig, afrIdentifyResult);
            if (errorCode != HciErrorCode.HCI_ERR_NONE) {
                Log.e(TAG, "HciCloudAfr.hciAfrIdentify failed and return " + errorCode);
                return null;
            }
            ArrayList<AfrIdentifyResultItem> afrIdentifyResultItems = afrIdentifyResult.getIdentifyResultItemList();
            if (afrIdentifyResultItems.size() < 0) {
                Log.d(TAG, "没有找到匹配的人脸");
                result = "没有找到匹配的人脸";
            } else {
                result = "你是：" + afrIdentifyResultItems.get(0).getUserId() + "，得分：" + afrIdentifyResultItems.get(0).getScore();
            }
            Iterator<AfrIdentifyResultItem> afrIdentifyResultItemIterator = afrIdentifyResultItems.iterator();
            while (afrIdentifyResultItemIterator.hasNext()) {
                AfrIdentifyResultItem afrIdentifyResultItem = afrIdentifyResultItemIterator.next();
                Log.d(TAG, "你是：" + afrIdentifyResultItem.getUserId() + ",得分：" + afrIdentifyResultItem.getScore());
            }
        } else {
            Log.e(TAG, "没检测到人脸！");
            result = "没检测到人脸！";
        }

        //释放人脸检测的结果
        errorCode = HciCloudAfr.hciAfrFreeDetectResult(afrDetectResult);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrFreeDetectResult failed and return " + errorCode);
            return null;
        }
        // 关闭会话
        errorCode = HciCloudAfr.hciAfrSessionStop(session);
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            Log.e(TAG, "HciCloudAfr.hciAfrSessionStop failed and return " + errorCode);
            return null;
        }
        return result;
    }
}
