package com.example.waqarsahi.servertest;

import android.Manifest;
import android.Manifest.permission;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.database.DatabaseReference;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int RD_EXT_ST_PER_CODE = 1;
    private static final int CAMERA_PER_CODE = 2;
    private static final int REQUEST_CAPTURE_IMAGE = 100;
    private static final int PICK_IMAGE_REQUEST = 101;
    private TextView textView,output_server_text;
    private Button button, button2;
    private ImageView imageView;
    private String imageFilePath;
    boolean connected = false;


    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //AndroidNetworking.initialize(getApplicationContext());
        button = findViewById(R.id.button);
        button2 = findViewById(R.id.button2);
        textView = findViewById(R.id.textView);
        output_server_text = findViewById(R.id.output_text);
        imageView = findViewById(R.id.imageView);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        handlepermissions();


        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                // Show only images, no videos or anything else
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                // Always show the chooser (if there are multiple options available)
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
            }
        });

        button2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent pictureIntent = new Intent(
                        MediaStore.ACTION_IMAGE_CAPTURE);
                if(pictureIntent.resolveActivity(getPackageManager()) != null){
                    //Create a file to store the image
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    if (photoFile != null) {
                        Uri photoURI = FileProvider.getUriForFile(MainActivity.this, "com.example.waqarsahi.servertest.provider",photoFile);
                        pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(pictureIntent,
                                REQUEST_CAPTURE_IMAGE);
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST  && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri uri = data.getData();

            setImagetoView(uri);

            String realPath = ImageFilePath.getPath(MainActivity.this, data.getData());


            textView.setText("Sending...");
            uploadToServer(realPath);

        }

        if (requestCode == REQUEST_CAPTURE_IMAGE && resultCode == RESULT_OK) {
            setImagetoView(Uri.fromFile(new File(imageFilePath)));
            uploadToServer(imageFilePath);

        }
    }



    public interface UploadAPIs {
        @Multipart
        @POST("/uploader")
        Call<ResponseBody> uploadImage(@Part MultipartBody.Part file);

    }


    private void uploadToServer(String filePath) {
        Retrofit retrofit = NetworkClient.getRetrofitClient(this);
        UploadAPIs uploadAPIs = retrofit.create(UploadAPIs.class);
        //Create a file object using file path
        File file = new File(filePath);
        // Create a request body with file and image media type
        RequestBody fileReqBody = RequestBody.create(MediaType.parse("image/*"), file);
        // Create MultipartBody.Part using file request-body,file name and part name
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", file.getName(), fileReqBody);


        //
        Call call = uploadAPIs.uploadImage(part);
        try {
            sleep(1000);
            textView.setText("Image Pass To Server");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        call.enqueue(new Callback() {


            @Override
            public void onResponse(Call call, Response response) {


               // String json = new UploadAPIs().getInfo().execute().body().string();
                //output_server_text.setText(response.body().toString());
                Log.i("onSuccess", response.message());
                textView.setText("Image Pass To Server");


            }
            @Override
            public void onFailure(Call call, Throwable t) {

                t.printStackTrace();
            }
        });
    }

    private void handlepermissions(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, RD_EXT_ST_PER_CODE);
        }
        if(ContextCompat.checkSelfPermission(this, permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PER_CODE);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp =
                new SimpleDateFormat("yyyyMMdd_HHmmss",
                        Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir =
                getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        imageFilePath = image.getAbsolutePath();
        return image;
    }

    private void setImagetoView(Uri uri){
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

            ImageView imageView = findViewById(R.id.imageView);
            imageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public interface ApiInterface {

        String JSONURL = "http://192.168.43.249:5005/";

        @GET("/uploader")
        Call<String> getString();

    }
    private void getResponse(){

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiInterface.JSONURL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        ApiInterface api = retrofit.create(ApiInterface.class);

        Call<String> call = api.getString();

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Log.i("Responsestring", response.body().toString());
                //Toast.makeText()
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        Log.i("onSuccess", response.body().toString());
                        String jsonresponse = response.body().toString();
//                        Log.d("onSuccess", String.valueOf(jsonresponse.length()));
//
//                        if (jsonresponse.equals(""))
//                        {
//                            //Toast.makeText(MainActivity.this,"No Text Found",Toast.LENGTH_LONG).show();
//                            Alert_dial_for_image();
//                            output_server_text.setText("No Image Select");
//                        }
//                        else

                            output_server_text.setText(jsonresponse);


                    } else {
                        Log.i("onEmptyResponse", "Returned empty response");//Toast.makeText(getContext(),"Nothing returned",Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {

            }
        });
    }
    public void get_Responce(View view ){
        getResponse();
    }


    public void upload_firebase(View view)

    {
        Toast.makeText(this,"Record are updated",Toast.LENGTH_LONG).show();
//        network();
//        if (connected==true)
//        {
//            Toast.makeText(MainActivity.this,"Internet Connect",Toast.LENGTH_LONG).show();
//        }
//        else if (connected==false)
//        {
//            Alert_dia();
//        }
    }
    public void Alert_dia()
    {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage("Make Sure Your Internet Connection.");
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "Ok.",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    }
                });

        AlertDialog alert11 = builder1.create();
        alert11.show();
    }
    public void network (){

        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            //we are connected to a network
            connected = true;
        }
        else
            connected = false;
    }

    public void Alert_dial_for_image()
    {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage("Select Any Image..... ");
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "Ok.",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });

        AlertDialog alert11 = builder1.create();
        alert11.show();
    }


}
