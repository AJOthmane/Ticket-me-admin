package ma.ensias.ticket_me.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ma.ensias.ticket_me.R;
import ma.ensias.ticket_me.api.APIClient;
import ma.ensias.ticket_me.api.APIInterface;
import ma.ensias.ticket_me.response.ResponseCategories;
import ma.ensias.ticket_me.response.ResponseTicket;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TicketCreation extends AppCompatActivity {
    public static final int PERMISSION_WRITE = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_creation);
        EditText cin = (EditText) findViewById(R.id.ccin);
        EditText nom = (EditText) findViewById(R.id.cnom);
        EditText prenom = (EditText) findViewById(R.id.cprenom);
        EditText email = (EditText) findViewById(R.id.cemail);
        Spinner categories = (Spinner) findViewById(R.id.spinner);
        Button creer = (Button) findViewById(R.id.creerTicket);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        List<String> categoriesId = new ArrayList<String>();
        List<String> categoriesSpinner = new ArrayList<String>();
        APIInterface apiInterface = APIClient.createService(APIInterface.class);
        Call<ResponseCategories> call = apiInterface.getCategories2(1);
        checkPermission();
        call.enqueue(new Callback<ResponseCategories>() {
            @Override
            public void onResponse(Call<ResponseCategories> call, Response<ResponseCategories> response) {
                if(response.body().getValid())
                {
                    List<HashMap<String,String>> bresponse = response.body().getCategories();
                    for (HashMap<String,String> cat:bresponse
                         ) {
                        categoriesId.add(cat.get("id_categorie"));
                        categoriesSpinner.add(cat.get("nom_categorie")+" - "+cat.get("prix_categorie")+" DH");
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_spinner_item, categoriesSpinner);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    categories.setAdapter(adapter);
                }
                else
                {
                    // event dont exist or dont have categories
                }
            }

            @Override
            public void onFailure(Call<ResponseCategories> call, Throwable t) {
                Log.e("Fail : Categories check",t.getMessage());
            }
        });
        creer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.setMessage( getString(R.string.ticket_creation_confirmation_content) +" "+ categories.getSelectedItem().toString());
                builder.setTitle(R.string.ticket_creation_confirmation_title);
                builder.setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // to replace later
                        //finish();
                        HashMap<String,String> reqBody = new HashMap<>();
                        reqBody.put("cin",cin.getText().toString());
                        reqBody.put("nom",nom.getText().toString());
                        reqBody.put("prenom",prenom.getText().toString());
                        reqBody.put("email",email.getText().toString());
                        // to update when linking with other activities
                        reqBody.put("event","1");
                        reqBody.put("category",categoriesId.get(categories.getSelectedItemPosition()));
                        // to update when linking with user session
                        reqBody.put("user_creation","1");
                        Call<ResponseBody> tcall = apiInterface.createTicket(reqBody);
                        tcall.enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                Bitmap ticket = BitmapFactory.decodeStream(response.body().byteStream());
                                String fileUri = "";
                                try {
                                    File mydir = new File(Environment.getExternalStorageDirectory() + "/ticketme");
                                    if (!mydir.exists()) {
                                        mydir.mkdirs();
                                    }

                                    fileUri = mydir.getAbsolutePath() + File.separator + System.currentTimeMillis() + ".jpg";
                                    FileOutputStream outputStream = new FileOutputStream(fileUri);

                                    ticket.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                                    outputStream.flush();
                                    outputStream.close();
                                } catch(IOException e) {
                                    e.printStackTrace();
                                }
                                Uri uri= Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), BitmapFactory.decodeFile(fileUri),null,null));
                                // use intent to share image
                                Intent share = new Intent(Intent.ACTION_SEND);
                                share.setType("image/*");
                                share.putExtra(Intent.EXTRA_STREAM, uri);
                                startActivity(Intent.createChooser(share, "Share Image"));
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {

                            }
                        });
                    }
                });
                builder.setNegativeButton("Non", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

    }
    //runtime storage permission
    public boolean checkPermission()
    {
        int READ_EXTERNAL_PERMISSION = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if((READ_EXTERNAL_PERMISSION != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_WRITE);
            return false;
        }
        return true;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==PERMISSION_WRITE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //do somethings
        }
    }
}