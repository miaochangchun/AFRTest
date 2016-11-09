package com.example.sinovoice.afrtest;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.sinovoice.afr.FaceUtil;
import com.example.sinovoice.afr.HciCloudAfrHelper;
import com.example.sinovoice.afr.HciCloudSysHelper;
import com.example.sinovoice.afr.HciCloudUserHelper;
import com.sinovoice.hcicloudsdk.common.HciErrorCode;
import com.sinovoice.hcicloudsdk.common.afr.AfrDetectFace;
import com.sinovoice.hcicloudsdk.common.afr.AfrDetectFacebox;
import com.sinovoice.hcicloudsdk.common.afr.AfrDetectLandmark;
import com.sinovoice.hcicloudsdk.common.afr.AfrDetectResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CAMERA_IMAGE = 1;
    private static final int REQUEST_PICTURE_CHOOSE = 2;
    private ImageView afrImage;
    private Button afrCamera;
    private Button afrPick;
    private Button afrDetect;
    private Button afrEnroll;
    private Button afrQuery;
    private Button afrIdentiry;
    private EditText afrUserid;
    private HciCloudSysHelper mHciCloudSysHelper;
    private HciCloudAfrHelper mHciCloudAfrHelper;
    private String afrCapkey = "afr.local.recog";
    private File mPictureFile;
    private String imagePath = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator + "sinovoice" + File.separator + "ifd.jpg";
    private Bitmap mImage;
    private byte[] mImageData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        afrImage = (ImageView) findViewById(R.id.afr_image);
        afrCamera = (Button) findViewById(R.id.afr_camera);
        afrPick = (Button) findViewById(R.id.afr_pick);
        afrDetect = (Button) findViewById(R.id.afr_detect);
        afrEnroll = (Button) findViewById(R.id.afr_enroll);
        afrQuery = (Button) findViewById(R.id.afr_query);
        afrIdentiry = (Button) findViewById(R.id.afr_identify);
        afrUserid = (EditText) findViewById(R.id.afr_userid);

        afrCamera.setOnClickListener(this);
        afrPick.setOnClickListener(this);
        afrDetect.setOnClickListener(this);
        afrEnroll.setOnClickListener(this);
        afrQuery.setOnClickListener(this);
        afrIdentiry.setOnClickListener(this);

        //灵云系统初始化
        mHciCloudSysHelper = HciCloudSysHelper.getInstance();
        mHciCloudAfrHelper = HciCloudAfrHelper.getInstance();

        int errorCode = mHciCloudSysHelper.init(this);
        Log.d(TAG, "mHciCloudSysHelper.init return " + errorCode);
        mHciCloudAfrHelper.initAfr(this, afrCapkey);
        Log.d(TAG, "mHciCloudAfrHelper.initAfr return " + errorCode);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.afr_camera:           //选择相机
                // 设置相机拍照后照片保存路径
                mPictureFile = new File(Environment.getExternalStorageDirectory(), "picture" + System.currentTimeMillis() / 1000 + ".jpg");
                //启动拍照,并保存到临时文件
                Intent mIntent = new Intent();
                mIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                mIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mPictureFile));
                mIntent.putExtra(MediaStore.Images.Media.ORIENTATION, 0);
                startActivityForResult(mIntent, REQUEST_CAMERA_IMAGE);
                break;
            case R.id.afr_pick:             //选择相册
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_PICK);
                startActivityForResult(intent, REQUEST_PICTURE_CHOOSE);
                break;
            case R.id.afr_detect:           //选择人脸检测按钮
                JSONObject jsonObject = mHciCloudAfrHelper.detectAfr(imagePath, afrCapkey);
                if (jsonObject == null) {
                    Toast.makeText(this, "没有检测到人脸！", Toast.LENGTH_SHORT).show();
                } else {
                    printAfrDetectResutl(jsonObject);
                }
                break;
            case R.id.afr_enroll:           //选择人脸注册按钮
                if (afrUserid.getText().toString().isEmpty()) {
                    Log.e(TAG, "userid不能为空！");
                    Toast.makeText(this, "必须输入userId", Toast.LENGTH_SHORT).show();
                }else if(afrUserid.getText().toString().length() > 64){
                    Log.e(TAG, "userId超过64字符串了");
                    Toast.makeText(this, "userId超过64字符串了", Toast.LENGTH_SHORT).show();
                } else if (HciCloudUserHelper.getInstance().getUserList("").contains(afrUserid.getText().toString())) {
                    Log.e(TAG, "userId已经存在了，请重新输入！");
                    Toast.makeText(this, "userId已经存在了，请重新输入！", Toast.LENGTH_SHORT).show();
                } else {
                    //人脸注册的功能
                    String result = mHciCloudAfrHelper.enrollAfr(imagePath, afrCapkey, afrUserid.getText().toString());
                    Log.d(TAG, "result = " + result);
                    Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.afr_query:            //人脸一对一识别功能
                if (afrUserid.getText().toString().isEmpty()) {
                    Log.e(TAG, "userid不能为空！");
                    Toast.makeText(this, "必须输入userId", Toast.LENGTH_SHORT).show();
                } else if (afrUserid.getText().toString().length() > 64) {
                    Log.e(TAG, "userId超过64字符串了");
                    Toast.makeText(this, "userId超过64字符串了", Toast.LENGTH_SHORT).show();
                } else if (!HciCloudUserHelper.getInstance().getUserList("").contains(afrUserid.getText().toString())) {
                    Log.e(TAG, "userId不存在，请先注册！");
                    Toast.makeText(this, "userId不存在，请先注册！", Toast.LENGTH_SHORT).show();
                } else {
                    //人脸一对一识别功能
                    String verifyResult = mHciCloudAfrHelper.verifyAfr(imagePath, afrCapkey, afrUserid.getText().toString());
                    Toast.makeText(this, verifyResult, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.afr_identify:
                //人脸一对多识别功能
                String identifyResult = mHciCloudAfrHelper.identifyAfr(imagePath, afrCapkey);
                Toast.makeText(this, identifyResult, Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    /**
     * 将检测的人脸信息print一下
     * @param jsonObject
     */
    private void printAfrDetectResutl(JSONObject jsonObject) {
        Log.d(TAG, "jsonObject = " + jsonObject.toString());
        try {
            JSONArray jsonArray = jsonObject.getJSONArray("detectFaceResult");
            for (int i=0; i<jsonArray.length(); i++) {
                JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                JSONArray jsonArray1 = jsonObject1.getJSONArray("landmark");
                for (int j=0; j<jsonArray1.length(); j++) {
                    JSONObject jsonObject2 = jsonArray1.getJSONObject(j);
                    int y = jsonObject2.getInt("y");
                    int x = jsonObject2.getInt("x");

                    //把关键点坐标在人脸上显示出来
                    showLandmarkImage(x, y);
                }
                int gender = jsonObject1.getInt("gender");
                if (gender == 0) {
                    Toast.makeText(this, "性别：男", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "性别：女", Toast.LENGTH_SHORT).show();
                }

                String faceId = jsonObject1.getString("faceId");

                JSONObject jsonObject2 = jsonObject1.getJSONObject("facebox");
                int bottom = jsonObject2.getInt("bottom");
                int right = jsonObject2.getInt("right");
                int left = jsonObject2.getInt("left");
                int top = jsonObject2.getInt("top");

                //在人脸的图片上，把关键点给勾画出来
                showFaceImage(bottom, top, left, right);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将人脸的四个边框勾画出来
     * @param bottom    底边坐标
     * @param top   顶边坐标
     * @param left  左边坐标
     * @param right 右边坐标
     */
    private void showFaceImage(int bottom, int top, int left, int right) {
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(Math.max(mImage.getWidth(), mImage.getHeight()) / 100f);
        Bitmap bitmap = Bitmap.createBitmap(mImage.getWidth(), mImage.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(mImage, new Matrix(), null);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(new Rect(left, top, right, bottom), paint);
        mImage = bitmap;
        afrImage.setImageBitmap(mImage);
    }

    /**
     * 将x，y坐标在Image图片上显示出来
     * @param x
     * @param y
     */
    private void showLandmarkImage(int x, int y) {
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(Math.max(mImage.getWidth(), mImage.getHeight()) / 100f);
        Bitmap bitmap = Bitmap.createBitmap(mImage.getWidth(), mImage.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(mImage, new Matrix(), null);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawPoint((float) x, (float) y, paint);
        mImage = bitmap;
        afrImage.setImageBitmap(mImage);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String fileSrc = null;
        if (requestCode == REQUEST_PICTURE_CHOOSE) {
            if (data == null) {
                return;
            }
            if ("file".equals(data.getData().getScheme())) {
                // 有些低版本机型返回的Uri模式为file
                fileSrc = data.getData().getPath();
            } else {
                // Uri模型为content
                String[] proj = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(data.getData(), proj, null, null, null);
                cursor.moveToFirst();
                int idx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                fileSrc = cursor.getString(idx);
                cursor.close();
            }
            // 跳转到图片裁剪页面
            FaceUtil.cropPicture(this,Uri.fromFile(new File(fileSrc)));
        }else if (requestCode == REQUEST_CAMERA_IMAGE){
            if (null == mPictureFile) {
                Log.e(TAG, "拍照失败，请重试");
                return;
            }
            fileSrc = mPictureFile.getAbsolutePath();
            // 跳转到图片裁剪页面
            FaceUtil.cropPicture(this,Uri.fromFile(new File(fileSrc)));
        } else if (requestCode == FaceUtil.REQUEST_CROP_IMAGE) {
            // 获取返回数据
            if (data == null) {
                return;
            }
            Bitmap bitmap = data.getParcelableExtra("data");
            // 若返回数据不为null，保存至本地，防止裁剪时未能正常保存
            if(null != bitmap){
                FaceUtil.saveBitmapToFile(MainActivity.this, bitmap);
            }
            // 获取图片保存路径
            fileSrc = FaceUtil.getImagePath(MainActivity.this);
            // 获取图片的宽和高
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            mImage = BitmapFactory.decodeFile(fileSrc, options);
            // 压缩图片
            options.inSampleSize = Math.max(1, (int) Math.ceil(Math.max((double) options.outWidth / 1024f, (double) options.outHeight / 1024f)));
            options.inJustDecodeBounds = false;
            mImage = BitmapFactory.decodeFile(fileSrc, options);
            // 若mImageBitmap为空则图片信息不能正常获取
            if(null == mImage) {
                Log.e(TAG, "图片信息无法正常获取！");
                return;
            }
            // 部分手机会对图片做旋转，这里检测旋转角度
            int degree = FaceUtil.readPictureDegree(fileSrc);
            if (degree != 0) {
                // 把图片旋转为正的方向
                mImage = FaceUtil.rotateImage(degree, mImage);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //可根据流量及网络状况对图片进行压缩
            mImage.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            mImageData = baos.toByteArray();
            afrImage.setImageBitmap(mImage);
        }
    }

    @Override
    protected void onDestroy() {
        if (mHciCloudAfrHelper != null) {
            mHciCloudAfrHelper.releaseAfr();
        }
        if (mHciCloudSysHelper != null) {
            mHciCloudSysHelper.release();
        }
        super.onDestroy();
    }
}
