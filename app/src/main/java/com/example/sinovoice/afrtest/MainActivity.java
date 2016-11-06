package com.example.sinovoice.afrtest;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.io.ByteArrayOutputStream;
import java.io.File;

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
                mHciCloudAfrHelper.detectAfr(imagePath, afrCapkey);
                break;
            case R.id.afr_enroll:           //选择人脸注册按钮
                if (afrUserid.getText().toString().isEmpty()) {
                    Log.e(TAG, "userid" + afrUserid.getText().toString());
                    Toast.makeText(this, "请输入人脸对应的userId", Toast.LENGTH_SHORT).show();
                }else {
                    //人脸注册的功能
                    mHciCloudAfrHelper.enrollAfr(imagePath, afrCapkey, afrUserid.getText().toString());
                }
                break;
            case R.id.afr_query:
                break;
            case R.id.afr_identify:
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String fileSrc = null;
        if (requestCode == REQUEST_PICTURE_CHOOSE) {
            // Uri模型为content
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(data.getData(), proj, null, null, null);
            cursor.moveToFirst();
            int idx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            fileSrc = cursor.getString(idx);
            cursor.close();
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
