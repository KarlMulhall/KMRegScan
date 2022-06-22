package com.example.kmregscan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    Button captureImageButton, clearTextButton;
    TextView userTextView, regTextView, makeTextView, descriptionTextView, engineTextView;
    Bitmap bitmap;

    private FirebaseUser user;
    private DatabaseReference reference;
    private String userID;

    ProgressBar progressBar;
    private static final int REQUEST_CAMERA_CODE = 100;

    Handler handler;
    ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        captureImageButton = findViewById(R.id.capture_image_button);
        clearTextButton = findViewById(R.id.detect_text_button);
        userTextView = findViewById(R.id.user_text_view);
        regTextView = findViewById(R.id.reg_text_view);
        makeTextView = findViewById(R.id.make_text_view);
        descriptionTextView = findViewById(R.id.description_text_view);
        engineTextView = findViewById(R.id.engine_text_view);
        progressBar = findViewById(R.id.progress_bar);

        // If camera permission has not been granted then ask for permission
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.CAMERA
            }, REQUEST_CAMERA_CODE);
        }

        captureImageButton.setOnClickListener(v ->
                CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(MainActivity.this)
        );

        clearTextButton.setOnClickListener(v -> {
            regTextView.setText("...");
            makeTextView.setText("...");
            descriptionTextView.setText("...");
            engineTextView.setText("...");

        });

        user = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("Users");
        userID = user.getUid();

        reference.child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User userProfile = snapshot.getValue(User.class);

                if(userProfile != null){
                    String fullName = userProfile.fullname;

                    userTextView.setText(fullName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "An Error Occurred...", Toast.LENGTH_LONG).show();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK){
                Uri resultUri = result.getUri();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                    getTextFromImage(bitmap);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void getTextFromImage(Bitmap bitmap){
        TextRecognizer recognizer = new TextRecognizer.Builder(this).build();
        if (!recognizer.isOperational()){
            Toast.makeText(MainActivity.this, "Error Occurred...", Toast.LENGTH_SHORT).show();
        }
        else{
            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<TextBlock> textBlockSparseArray = recognizer.detect(frame);
            StringBuilder stringBuilder = new StringBuilder();
            for(int i=0; i<textBlockSparseArray.size(); i++){
                TextBlock textBlock = textBlockSparseArray.valueAt(i);
                stringBuilder.append(textBlock.getValue());
//                stringBuilder.append("\n");
            }
            String textResult = stringBuilder.toString();
            regTextView.setText(textResult);
            getWebContent(textResult);


        }
    }

    private void getWebContent(String input){
        executor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());
        final String[] make = new String[1];
        final String[] description = new String[1];
        final String[] engCap = new String[1];
        final String regURL = input.toLowerCase();

        executor.execute(() -> {

            //Background work here
            try {
                String url = "https://www.cartell.ie/ssl/servlet/beginStarLookup?basketId=PkkLh1SWuSaq9SwqIwk5JdTwDoSGQn3M&registration="+regURL;
                Document doc = Jsoup.connect(url).get();
                Elements data = doc.select("div.col.col-sm-12.col-md-8.col-lg-4.top");
                String makeURL = data.select("table.mx-0.my-0")
                        .select("tbody")
                        .select("tr")
                        .select("td")
                        .eq(0)
                        .text();
                String descURL = data.select("table.mx-0.my-0")
                        .select("tbody")
                        .select("tr")
                        .select("td")
                        .eq(1)
                        .text();
                String engCapURL = data.select("table.mx-0.my-0")
                        .select("tbody")
                        .select("tr")
                        .select("td")
                        .eq(2)
                        .text();

                make[0] = makeURL;
                description[0] = descURL;
                engCap[0] = engCapURL;


            } catch (IOException e) {
                e.printStackTrace();
            }

            handler.post(() -> {
                //UI Thread work here
                makeTextView.setText(make[0]);
                descriptionTextView.setText(description[0]);
                engineTextView.setText(engCap[0]);
            });
        });
    }

}

