/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.cameraview.demo;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by USER on 2017-11-09.
 */

public class PreviewActivity extends AppCompatActivity {

    private ImageView previewImage;
    private String foldername;//filename->foldername
    private String filename;//imagename->filename
    private ImageView saveImage, deleteImage;
    private boolean flag_save=false;
    private Uri uri;

    private String imagePath;
    private Bitmap image;
    private static boolean camera_front;//전면/후면 카메라 확인 -> 좌우반전 여부 결정
    private static String filter_view;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        previewImage = (ImageView) findViewById(R.id.preview_img);
        saveImage=(ImageView)findViewById(R.id.save_img);
        deleteImage=(ImageView)findViewById(R.id.delete_img);

        Intent intent=getIntent();
        foldername=intent.getExtras().getString("foldername");
        filename = intent.getExtras().getString("filename");//___.jpg
        camera_front=intent.getExtras().getBoolean("camera_front");
        filter_view=intent.getExtras().getString("filter_view");

        if(foldername.equals("")){Toast.makeText(this,"없넹",Toast.LENGTH_LONG).show();}
        uri = Uri.parse("file://"+foldername);

        try
        {
            // 비트맵 이미지로 가져온다
            imagePath = uri.getPath();//uri 전체경로 file://까지
            image = BitmapFactory.decodeFile(imagePath);

            // 이미지를 상황에 맞게 회전시킨다
            ExifInterface exif = new ExifInterface(imagePath);
            int exifOrientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int exifDegree = exifOrientationToDegrees(exifOrientation);
            image = rotate(image, exifDegree);

            //Bitmap위에 resource 뿌려줌.
            Bitmap tmpB = BitmapFactory.decodeResource( getResources(),Integer.parseInt(filter_view) );

            //bitmap 크기조절
            double aspectRatio = (double) tmpB.getHeight() / (double) tmpB.getWidth();
            int targetHeight = (int) (image.getWidth() * aspectRatio);
            Bitmap result = Bitmap.createScaledBitmap(tmpB, image.getWidth(), targetHeight, false);

            Canvas tmpC = new Canvas( image );
            tmpC.drawBitmap( result, 0, 0, null );

            //이미지 저장 again
            String root = Environment.getExternalStorageDirectory().toString();
            File myDir = new File(root + "/followcam_images");
            myDir.mkdirs();

            File file = new File(myDir,filename);
            OutputStream os = null;
            try {
                os = new FileOutputStream(file);
                image.compress(Bitmap.CompressFormat.JPEG, 100, os);
                os.flush();
                os.close();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), file.toString(), Toast.LENGTH_LONG).show();
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
            //image scanning
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.parse("file://"+myDir.getPath()+"/"+filename)));

            // 변환된 이미지 사용
            previewImage.setImageBitmap(image);
        }
        catch(Exception e)
        {
            Toast.makeText(this, "오류발생: " + e.getLocalizedMessage(),
                    Toast.LENGTH_LONG).show();
        }

        saveImage.setOnClickListener(new ImageView.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),filter_view,Toast.LENGTH_SHORT).show();
                flag_save=true;
            }
        });

        deleteImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"delete it!!",Toast.LENGTH_SHORT).show();
                File file =new File(filename);
                file.delete();

                //파일삭제 후 갤러리 썸네일 남는 현상 해결
                ContentResolver resolver = getContentResolver();
                Uri resolver_uri  = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                String selection = MediaStore.Images.Media.DATA + " = ?";
                String[] selectionArgs = {filename}; // 실제 파일의 경로
                resolver.delete(resolver_uri, selection,selectionArgs);

                finish();
            }
        });
    }
    public static int exifOrientationToDegrees(int exifOrientation)
    {
        if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_90){ return 90;}
        else if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_180){return 180;}
        else if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_270){return 270;}
        return 0;
    }

    public static Bitmap rotate(Bitmap bitmap, int degrees)
    {
        if(degrees != 0 && bitmap != null){
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) bitmap.getWidth() / 2,(float) bitmap.getHeight() / 2);
            if(camera_front==true){
                m.postScale(-1,1);
            }
            try {
                Bitmap converted = Bitmap.createBitmap(bitmap, 0, 0,bitmap.getWidth(), bitmap.getHeight(), m, true);
                if(bitmap != converted){
                    bitmap.recycle();
                    bitmap = converted;
                }
            }
            catch(OutOfMemoryError ex){

                // 메모리가 부족하여 회전을 시키지 못할 경우 그냥 원본을 반환합니다.
            }
        }
        return bitmap;
    }
    public void onBackPressed() {

        if(flag_save==true){
            Toast.makeText(getApplicationContext(),"save it!!",Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(getApplicationContext(),"delete it!!",Toast.LENGTH_SHORT).show();
            File file =new File(filename);
            file.delete();

            //파일삭제 후 갤러리 썸네일 남는 현상 해결
            ContentResolver resolver = getContentResolver();
            Uri resolver_uri  = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String selection = MediaStore.Images.Media.DATA + " = ?";
            String[] selectionArgs = {filename}; // 실제 파일의 경로
            resolver.delete(resolver_uri, selection,selectionArgs);
        }
        super.onBackPressed();
    }
    protected void onUserLeaveHint()
    {
        if(flag_save==true){
            Toast.makeText(getApplicationContext(),"save it!!",Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(getApplicationContext(),"delete it!!",Toast.LENGTH_SHORT).show();
            File file =new File(filename);
            file.delete();

            //파일삭제 후 갤러리 썸네일 남는 현상 해결
            ContentResolver resolver = getContentResolver();
            Uri resolver_uri  = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String selection = MediaStore.Images.Media.DATA + " = ?";
            String[] selectionArgs = {filename}; // 실제 파일의 경로
            resolver.delete(resolver_uri, selection,selectionArgs);
        }
        super.onUserLeaveHint();
    }

}