package appolonia.com.capcomnotation;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    Bitmap image;
    ImageView OCRImageView;
    private TessBaseAPI mTessItem;

    String dataPath="";

    Button OCRButton;

    static final int IMAGE_CAP = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OCRImageView = findViewById(R.id.OCRView);
        image = BitmapFactory.decodeResource(getResources(), R.drawable.ocrtest);
        dataPath = getFilesDir()+ "/tesseract/";
        OCRButton = findViewById(R.id.OCRButton);
        OCRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraBitmapIntent();
            }
        });
        checkFile(new File(dataPath + "tessdata/"));
        String lang = "eng";

        mTessItem = new TessBaseAPI();
        mTessItem.init(dataPath, lang);
    }

    private void cameraBitmapIntent(){
        Intent cameraImageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(cameraImageIntent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(cameraImageIntent, IMAGE_CAP);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == IMAGE_CAP && resultCode == RESULT_OK){
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            OCRImageView.setImageBitmap(imageBitmap);
            processImage(imageBitmap);

        }
    }


    public void processImage(Bitmap bMap){
        String OCRresult = null;
        mTessItem.setImage(bMap);
        OCRresult = mTessItem.getUTF8Text();
        TextView OCRTextView = findViewById(R.id.OCRText);
        String finalResult = OCRresult.replace("214","qcb XX ")
                .replace("236","qcf XX ")
                .replace("2", "d.")
                .replace("5", "neutral ");

        OCRTextView.setText(finalResult);
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
}
