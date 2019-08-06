package com.andela.travelmantics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.andela.travelmantics.Model.FirebaseUtil;
import com.andela.travelmantics.Model.TravelDeal;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class DealActivity extends AppCompatActivity {

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private static final int PICTURE_RESULT = 42;

    public static Boolean isDealAdded;
    public static Boolean isDealUpdated;
    public static Boolean isDealDeleted;

    ConstraintLayout constraintLayout;
    EditText txtTitle;
    EditText txtPrice;
    EditText txtDescription;
    ImageView imageView;
    Snackbar snackbar;
    Button btnImage;

    TravelDeal deal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);
        mFirebaseDatabase = FirebaseUtil.mFirebaseDatabase;
        mDatabaseReference = FirebaseUtil.mDatabaseReference;
        txtTitle = findViewById(R.id.txtTitle);
        txtPrice = findViewById(R.id.txtPrice);
        txtDescription = findViewById(R.id.txtDescription);
        constraintLayout = findViewById(R.id.constraint_layout_id);
        imageView = findViewById(R.id.image);

        isDealAdded = false;
        isDealUpdated = false;
        isDealDeleted = false;

        Intent intent = getIntent();
        TravelDeal deal = (TravelDeal) intent.getSerializableExtra("Deal");
        if (deal == null) {
            deal = new TravelDeal();
        }
        this.deal = deal;
        txtTitle.setText(deal.getTitle());
        txtDescription.setText(deal.getDescription());
        txtPrice.setText(deal.getPrice());

        showImage(deal.getImageUrl());

        btnImage = findViewById(R.id.btnImage);
        btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
//                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(intent.createChooser(intent,
                        "Insert Picture"), PICTURE_RESULT);
            }
        });
    }

    //Listening for clicks on items in menu options
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save_menu:
                String title = txtTitle.getText().toString();
                String description = txtDescription.getText().toString();
                String price = txtPrice.getText().toString();
                View view = this.getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) DealActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                if (title.isEmpty() || description.isEmpty() || price.isEmpty()) {
                    snackBarAction("Kindly fill all fields!");
                } else {
                    saveDeal();
                }
                return true;
            case R.id.delete_menu:
                deleteDeal();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //Adding the menu to our actionBar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.save_menu, menu);
        if (FirebaseUtil.isAdmin){
            menu.findItem(R.id.delete_menu).setVisible(true);
            menu.findItem(R.id.save_menu).setVisible(true);
            enableEditTexts(true);
        } else {
            menu.findItem(R.id.delete_menu).setVisible(false);
            menu.findItem(R.id.save_menu).setVisible(false);
            enableEditTexts(false);
        }
        return true;
    }

    //Populate the Snackbar
    public void snackBarAction(String messageInput) {

        snackbar = Snackbar.make(constraintLayout, messageInput, Snackbar.LENGTH_INDEFINITE);
        snackbar.setDuration(1000);
        View v = snackbar.getView();
        v.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        snackbar.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("REQUEST CODE", String.valueOf(requestCode));
        Log.d("RESULT OK", String.valueOf(resultCode == RESULT_OK));
        if (!(requestCode == PICTURE_RESULT && requestCode == RESULT_OK)){
            Uri imageUri = data.getData();
            Log.d("IMAGE URI", String.valueOf(imageUri));
            final StorageReference ref = FirebaseUtil.mStorageRef.child(imageUri.getLastPathSegment());
            Log.i("REF NAME", ref.getDownloadUrl().toString());

            ref.putFile(imageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {
                    ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String url = uri.toString();
                            Log.d("Image URl", url);
                            String pictureName = taskSnapshot.getStorage().getPath();
                            deal.setImageUrl(url);
                            deal.setImageName(pictureName);
                            showImage(url);
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    snackBarAction("Upload Failed!");
                }
            });
        }
    }

    //Save deals to Firebase
    private void saveDeal() {

        deal.setTitle(txtTitle.getText().toString());
        deal.setDescription(txtDescription.getText().toString());
        deal.setPrice(txtPrice.getText().toString());
        if (deal.getId() == null) {
            mDatabaseReference.push().setValue(deal);
            isDealAdded = true;
            clean();
            backToList();
        } else {
            mDatabaseReference.child(deal.getId()).setValue(deal);
            isDealUpdated = true;
            backToList();
        }
    }

    //Delete a deal
    private void deleteDeal() {
        Log.d("Deal", deal.toString());
        if (deal == null) {
            snackBarAction("Please save the deal before deleting");
            return;
        } else if (deal.getId() == null || deal.getTitle() == null || deal.getDescription() == null || deal.getPrice() == null) {
            snackBarAction("Please save the deal before deleting");
            return;
        } else {
            mDatabaseReference.child(deal.getId()).removeValue();
            if (deal.getImageName() != null && !deal.getImageName().isEmpty()){
                StorageReference reference = FirebaseUtil.mStorage.getReference().child(deal.getImageName());
                reference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        snackBarAction("Deal Deleted Successfully");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        snackBarAction("Failed to delete deal!");
                    }
                });
            }
            backToList();
        }
        isDealDeleted = true;
    }

    //Back to list
    private void backToList() {
        Intent intent = new Intent(DealActivity.this, ListActivity.class);
        startActivity(intent);
    }

    // Clean Inputs in EditText
    private void clean() {
        txtTitle.setText("");
        txtDescription.setText("");
        txtPrice.setText("");
//        txtTitle.requestFocus();
    }

    private void enableEditTexts(Boolean isEnabled) {
        txtTitle.setEnabled(isEnabled);
        txtPrice.setEnabled(isEnabled);
        txtDescription.setEnabled(isEnabled);
        btnImage.setEnabled(isEnabled);
    }

    private void showImage(String url) {
        if (url != null && !url.isEmpty()) {
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            Picasso.with(this)
                    .load(url)
                    .centerCrop()
                    .resize(width, width * 2/3)
                    .into(imageView);
        } else {
            Log.d("IMAGE EMPTY", "Empty Image");
        }
    }
}
