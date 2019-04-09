package com.appolonia.pinball;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import static com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions.SPARSE_MODEL;

public class MainActivity extends AppCompatActivity {

    Bitmap image;
    ImageView OCRImageView;
    private TessBaseAPI mTessItem;
    String dataPath="";
    Button OCRButton;
    ImageProcessor imgProcessor;
    String currentPhotoPath;
    Uri photoURI;

    private int mShortAnimationDuration;

    DecimalFormat formatter;

    EditText player1;
    EditText player2;
    EditText player3;
    EditText player4;

    private FirebaseVisionCloudTextRecognizerOptions options;

    private ProgressBar mProgressBar;
    private Bitmap bitmapToProcess;
    private ArrayList<String> resultList;

    private static final int PICTURE_REQUEST_CODE = 1234;
    static final int IMAGE_CAP = 1;
    private static final String TAG = "OPENCV Status";

    //TextView Tags to support Drag and Drop

    private static final String PLAYER_ONE_TAG = "player1";
    private static final String PLAYER_TWO_TAG = "player2";
    private static final String PLAYER_THREE_TAG = "player3";
    private static final String PLAYER_FOUR_TAG = "player4";

    dragEventListener mDragEventListener;

    String mDragTextPlaceholder1;
    String mDragTextPlaceholder2;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        player1 = findViewById(R.id.Player1);
        player2 = findViewById(R.id.Player2);
        player3 = findViewById(R.id.Player3);
        player4 = findViewById(R.id.Player4);

        player1.setTag(PLAYER_ONE_TAG);
        player2.setTag(PLAYER_TWO_TAG);
        player3.setTag(PLAYER_THREE_TAG);
        player4.setTag(PLAYER_FOUR_TAG);

        //Set Drag Listeners

        mDragEventListener = new dragEventListener();

        player1.setOnDragListener(new dragEventListener());
        player2.setOnDragListener(new dragEventListener());
        player3.setOnDragListener(new dragEventListener());
        player4.setOnDragListener(new dragEventListener());

        //OnClickListeners
        player1.setOnLongClickListener( new View.OnLongClickListener(){

            public boolean onLongClick(View v){
                ClipData.Item item = new ClipData.Item(player1.getTag().toString());

                ClipData dragData = new ClipData((String)v.getTag(), new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item);

                View.DragShadowBuilder myShadow = new MyDragShadowBuilder(player1);
                v.startDrag(dragData, myShadow, null, 0);
                return true;
            }
        });

        player2.setOnLongClickListener( new View.OnLongClickListener(){

            public boolean onLongClick(View v){
                ClipData.Item item = new ClipData.Item(player2.getTag().toString());

                ClipData dragData = new ClipData((String)v.getTag(), new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item);

                View.DragShadowBuilder myShadow = new MyDragShadowBuilder(player2);
                v.startDrag(dragData, myShadow, null, 0);
                return true;
            }
        });

        player3.setOnLongClickListener( new View.OnLongClickListener(){

            public boolean onLongClick(View v){
                ClipData.Item item = new ClipData.Item(player3.getTag().toString());

                ClipData dragData = new ClipData((String)v.getTag(), new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item);

                View.DragShadowBuilder myShadow = new MyDragShadowBuilder(player3);
                v.startDrag(dragData, myShadow, null, 0);
                return true;
            }
        });

        player4.setOnLongClickListener( new View.OnLongClickListener(){

            public boolean onLongClick(View v){
                ClipData.Item item = new ClipData.Item(player4.getTag().toString());

                ClipData dragData = new ClipData((String)v.getTag(), new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item);

                View.DragShadowBuilder myShadow = new MyDragShadowBuilder(player4);
                v.startDrag(dragData, myShadow, null, 0);
                return true;
            }
        });



        mProgressBar = findViewById(R.id.determinateBar);
        mProgressBar.setVisibility(View.INVISIBLE);

        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        formatter = new DecimalFormat("###,###,###");
        OCRImageView = findViewById(R.id.OCRView);
        image = BitmapFactory.decodeResource(getResources(), R.drawable.pinballcolorcropped);
        dataPath = getFilesDir()+ "/tesseract/";
        OCRButton = findViewById(R.id.OCRButton);
        OCRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //processImage(image);
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivityForResult(intent, PICTURE_REQUEST_CODE);

            }
        });
        checkFile(new File(dataPath + "tessdata/"));
        String lang = "eng";



    }

    private void cameraBitmapIntent(){
        Intent cameraImageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if(cameraImageIntent.resolveActivity(getPackageManager()) != null){
            File photoFile = null;
                    try{
                    photoFile = createImage();
                    } catch (IOException ex) {
                        Log.d("EXCEPTION" , "Unable to create IMAGE");
                    }

                    if(photoFile != null){
                        photoURI = FileProvider.getUriForFile(this,"com.appolonia.android.fileprovider", photoFile);
                        cameraImageIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(cameraImageIntent, IMAGE_CAP);
                    }


        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == PICTURE_REQUEST_CODE && resultCode == RESULT_OK){

            //Hide standard views and make the progressbar visible
            OCRImageView.setVisibility(View.GONE);
            player1.setVisibility(View.GONE);
            player2.setVisibility(View.GONE);
            player3.setVisibility(View.GONE);
            player4.setVisibility(View.GONE);
            OCRButton.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);


            String resultPath = data.getStringExtra("path");
            Bitmap imageBitmap = BitmapFactory.decodeFile(resultPath);
            int orientation = 0;
            try {

                ExifInterface exif = new ExifInterface(resultPath);
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                Log.d("EXIF", "Exif: " + orientation);
                Matrix matrix = new Matrix();
                if (orientation == 6) {
                    matrix.postRotate(90);
                }
                else if (orientation == 3) {
                    matrix.postRotate(180);
                }
                else if (orientation == 8) {
                    matrix.postRotate(270);
                }
                imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), matrix, true); // rotating bitmap
            }
            catch (Exception e) {

            }





            Log.d("ORIGINAL_IMG_RES", "W: " + imageBitmap.getWidth() + " H: " + imageBitmap.getHeight());
            Bundle bundle = data.getExtras();
            ProcessImageTask processImageTask = new ProcessImageTask();
            processImageTask.execute(imageBitmap);




        }
    }






    private void copyFiles(){

        try{
            String filepath = dataPath + "/tessdata/eng.traineddata";

            AssetManager assetM = getAssets();

            InputStream inStream = assetM.open("tessdata/eng.traineddata");
            OutputStream outStream = new FileOutputStream(filepath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inStream.read(buffer)) != -1){
                outStream.write(buffer, 0, read);
            }
            outStream.flush();
            outStream.close();
            inStream.close();

        } catch (FileNotFoundException e ) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void checkFile(File dir){
        if(!dir.exists() && dir.mkdirs()){
            copyFiles();
        }

        if(dir.exists()) {
            String dataFilePath = dataPath + "/tessdata/eng.traineddata";

            File dataFile = new File(dataFilePath);
            if (!dataFile.exists()) {
                copyFiles();

            }
        }
    }

    private File createImage() throws IOException{
        String imageFileName = "PINBALL_BASE_IMG";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
          imageFileName,".jpg",storageDir
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

  /*  private void processText(FirebaseVisionCloudText texts){


        if(texts != null) {
            int i = 1;
            String detectedText = texts.getText();
            String[] splitText = detectedText.split("\\s+");
            ArrayList<String> convertedStringList = new ArrayList<>(Arrays.asList(splitText));
            ListIterator<String> stringIterator = convertedStringList.listIterator();
            while(stringIterator.hasNext())
            {
                String string = stringIterator.next();
                if(string.matches("[a-zA-Z]+"))
                {
                    stringIterator.remove();
                } else {

                    string.replaceAll("[oO]", "0");
                    Long convertedNumber = Long.parseLong(string.replaceAll(",", ""));
                    string = formatter.format(convertedNumber);
                    stringIterator.set(string);
                    Log.d("DETECTED_TEXT", "Detected item " + i + " :" + string);
                    i++;

                }
            }

            if(0 >= convertedStringList.size())
            {
                return;
            } else{
                player1.setText(convertedStringList.get(0));
            }

            if(2 >= convertedStringList.size())
            {
                return;
            } else{
                player2.setText(convertedStringList.get(2));
            }

            if(1 >= convertedStringList.size())
            {
                return;
            } else{
                player3.setText(convertedStringList.get(1));
            }

            if(3 >= convertedStringList.size())
            {
                return;
            } else{
                player4.setText(convertedStringList.get(3));
            }








        }





    } */

    private class ProcessImageTask extends AsyncTask<Bitmap, Integer, imageResults> {
        @Override
        protected imageResults doInBackground(Bitmap... bitmap) {


            Bitmap bMap2 = bitmap[0].copy(bitmap[0].getConfig(), true);


            Mat imageMat = new Mat();
            Utils.bitmapToMat(bMap2, imageMat);
            Mat preMat = new Mat(imageMat.size(), CvType.CV_8UC1);
            Rect cropRegion = new Rect((int) (bitmap[0].getWidth() * .15f), (int) (bitmap[0].getHeight() * .33f), (int) (bitmap[0].getWidth() * .70f), (int) (bitmap[0].getHeight() * .33f));
            Mat croppedMat = new Mat(imageMat, cropRegion);
            final Bitmap croppedBitmap = Bitmap.createBitmap(croppedMat.cols(), croppedMat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(croppedMat, croppedBitmap);

            Imgproc.cvtColor(imageMat, preMat, Imgproc.COLOR_RGB2GRAY, 4);
            Mat edges = new Mat(preMat, cropRegion);
            Core.bitwise_not(edges, edges);
            //Mat range = new Mat();
            //Scalar thresh1 = new Scalar(10);
            //Scalar thresh2 = new Scalar(240);
            //Core.inRange(edges, thresh2, thresh1, range);
            //Mat blackImage = new Mat(edges.size(), CvType.CV_8UC1, Scalar.all(0));
            //blackImage.copyTo(edges, range);
            //Imgproc.threshold(edges, edges, 200, 255, Imgproc.THRESH_OTSU);
            Size kernel = new Size(20, 20);
            Imgproc.blur(edges, edges, kernel);
            Bitmap resultBitmap = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(edges, resultBitmap);
            Log.d("FINAL_RESOLUTION", "W: " + resultBitmap.getWidth() + " H: " + resultBitmap.getHeight());




            imageResults results = new imageResults(resultBitmap, croppedBitmap);
            mProgressBar.setProgress(50);
            return results;
        }

        @Override
        protected void onPostExecute(imageResults imageResults) {
            super.onPostExecute(imageResults);
            OCRImageView.setImageBitmap(imageResults.imageToDisplay);


            FirebaseVisionImage fireImage = FirebaseVisionImage.fromBitmap(imageResults.imageToProcess);
            options = new FirebaseVisionCloudTextRecognizerOptions.Builder().setModelType(SPARSE_MODEL).build();
            FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance().getCloudTextRecognizer();
            detector.processImage(fireImage).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                @Override
                public void onSuccess(FirebaseVisionText firebaseVisionText) {

                    ArrayList<String> convertedStringList = new ArrayList<>();
                    ListIterator<String> stringIterator;

                    if (firebaseVisionText != null) {
                        int i = 1;
                        String detectedText = firebaseVisionText.getText();
                        Log.d("UNMODIFIED TEXT", detectedText);
                        String[] splitText = detectedText.split("\\s+");
                        convertedStringList = new ArrayList<>(Arrays.asList(splitText));
                        stringIterator = convertedStringList.listIterator();
                        while (stringIterator.hasNext()) {
                            String string = stringIterator.next();
                            if (string.matches("[a-zA-Z]+")) {
                                stringIterator.remove();
                            } else {

                                string = string.replaceAll("[^0-9]", "");
                                Long convertedNumber = Long.parseLong(string);
                                string = formatter.format(convertedNumber);
                                stringIterator.set(string);
                                Log.d("DETECTED_TEXT", "Detected item " + i + " :" + string);
                                i++;

                            }
                        }

                    }
                    resultList = convertedStringList;
                    if(0 >= convertedStringList.size())
                    {
                        Animate();
                        return;
                    } else{
                        player1.setText(convertedStringList.get(0));
                    }

                    if(2 >= convertedStringList.size())
                    {
                        Animate();
                        return;
                    } else{
                        player2.setText(convertedStringList.get(2));
                    }

                    if(1 >= convertedStringList.size())
                    {
                        Animate();
                        return;
                    } else{
                        player3.setText(convertedStringList.get(1));
                    }

                    if(3 >= convertedStringList.size())
                    {
                        Animate();
                        return;
                    } else{
                        player4.setText(convertedStringList.get(3));
                    }
                    Animate();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Animate();
                    Toast.makeText(getApplicationContext(), "Unable to retrieve results", Toast.LENGTH_SHORT).show();


                }
            });
        }
    }

    private static class imageParams{
        FirebaseVisionText paramText;
        Bitmap paramBitmap;

        imageParams(FirebaseVisionText paramText, Bitmap paramBitmap){
            this.paramText = paramText;
            this.paramBitmap = paramBitmap;
        }

    }

    private static class imageResults{
        Bitmap imageToProcess;
        Bitmap imageToDisplay;

        imageResults(Bitmap imageToProcess, Bitmap imageToDisplay)
        {
            this.imageToProcess = imageToProcess;
            this.imageToDisplay = imageToDisplay;
        }
    }
    private void Animate(){
        mProgressBar.setProgress(100);
        mProgressBar.setVisibility(View.GONE);
        OCRImageView.setAlpha(0f);
        OCRImageView.setVisibility(View.VISIBLE);
        player1.setAlpha(0f);
        player1.setVisibility(View.VISIBLE);
        player2.setAlpha(0f);
        player2.setVisibility(View.VISIBLE);
        player3.setAlpha(0f);
        player3.setVisibility(View.VISIBLE);
        player4.setAlpha(0f);
        player4.setVisibility(View.VISIBLE);
        OCRButton.setAlpha(0f);
        OCRButton.setVisibility(View.VISIBLE);

        OCRImageView.animate().alpha(1f).setDuration(mShortAnimationDuration).setListener(null);
        player1.animate().alpha(1f).setDuration(mShortAnimationDuration).setListener(null);
        player2.animate().alpha(1f).setDuration(mShortAnimationDuration).setListener(null);
        player3.animate().alpha(1f).setDuration(mShortAnimationDuration).setListener(null);
        player4.animate().alpha(1f).setDuration(mShortAnimationDuration).setListener(null);
        OCRButton.animate().alpha(1f).setDuration(mShortAnimationDuration).setListener(null);
    }

    private static class MyDragShadowBuilder extends View.DragShadowBuilder {
        private static Drawable shadow;

            public MyDragShadowBuilder(View v) {
                super(v);

                shadow = new ColorDrawable(Color.LTGRAY);
            }

            public void onProvideShadowMetrics(Point size, Point touch) {
                int width;
                int height;

                width = getView().getWidth() / 2;

                height = getView().getHeight() / 2;

                shadow.setBounds(0, 0, width, height);

                size.set(width, height);
                touch.set(width / 2, height / 2);
            }

            public void onDrawShadow(Canvas canvas){
                shadow.draw(canvas);
            }
    }

    protected class dragEventListener implements View.OnDragListener {
        public boolean onDrag(View v, DragEvent event) {
            final int action = event.getAction();



            switch(action){

                case DragEvent.ACTION_DRAG_STARTED :

                    if(event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {

                        v.getBackground().setColorFilter(Color.parseColor("#00DD00"), PorterDuff.Mode.DARKEN);

                        v.invalidate();

                        return true;

                    }

                    return false;

                case DragEvent.ACTION_DRAG_ENTERED:

                    v.getBackground().setColorFilter(Color.parseColor("#0000DD"), PorterDuff.Mode.DARKEN);

                    v.invalidate();

                    return true;

                case DragEvent.ACTION_DRAG_LOCATION:

                    return true;




                case DragEvent.ACTION_DRAG_EXITED:

                    v.getBackground().setColorFilter(Color.parseColor("#00DD00"), PorterDuff.Mode.DARKEN);

                    v.invalidate();

                    return true;


                case DragEvent.ACTION_DROP:

                    if(v instanceof EditText){
                        ClipData.Item item = event.getClipData().getItemAt(0);
                        mDragTextPlaceholder1 = String.valueOf(((EditText) v).getText());
                        mDragTextPlaceholder2 = String.valueOf(item.getText().toString());
                        Log.d("Placeholder", mDragTextPlaceholder2);


                       if(mDragTextPlaceholder2.equals("player1") && !v.getTag().equals("player1")) {



                           ((EditText) v).getText().clear();
                           ((EditText) v).setText(player1.getText());

                           player1.getText().clear();
                           player1.setText(mDragTextPlaceholder1);


                       } else if(mDragTextPlaceholder2.equals("player2")) {

                           ((EditText) v).getText().clear();
                           ((EditText) v).setText(player2.getText());

                           player2.getText().clear();
                           player2.setText(mDragTextPlaceholder1);


                       } else if(mDragTextPlaceholder2.equals("player3")) {

                           ((EditText) v).getText().clear();
                           ((EditText) v).setText(player3.getText());

                           player3.getText().clear();
                           player3.setText(mDragTextPlaceholder1);


                       } else if(mDragTextPlaceholder2.equals("player4")) {

                           ((EditText) v).getText().clear();
                           ((EditText) v).setText(player4.getText());

                           player4.getText().clear();
                           player4.setText(mDragTextPlaceholder1);


                       }

                       mDragTextPlaceholder2 = "";
                       mDragTextPlaceholder1 = "";

                    }
                    v.getBackground().clearColorFilter();
                    v.invalidate();

                    return true;


                case DragEvent.ACTION_DRAG_ENDED:

                    v.getBackground().clearColorFilter();
                    v.invalidate();

                    return true;

                default:
                    Log.e("Drag Example", "Unknown action type received by OnDragListener.");
                    break;
            }

            return false;
        }
    }

    static {
    if (!OpenCVLoader.initDebug()) {
        Log.e("OPENCVINIT", "  OpenCVLoader.initDebug(), not working.");
    } else {
        Log.d("OPENCVINIT  ",   "OpenCVLoader.initDebug(), working.");

    }
}



}
