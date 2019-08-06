package com.andela.travelmantics;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.andela.travelmantics.Adapter.DealAdapter;
import com.andela.travelmantics.Model.FirebaseUtil;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class ListActivity extends AppCompatActivity {

    Snackbar snackbar;
    AlertDialog alertDialog;
    AlertDialog.Builder builder;
    public static ConstraintLayout constraintLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_activity_menu, menu);
        MenuItem insertMenu = menu.findItem(R.id.insert_menu);
        Log.d("IS_ADMIN", String.valueOf(FirebaseUtil.isAdmin));
        if (FirebaseUtil.isAdmin != null) {
            if (FirebaseUtil.isAdmin == true) {
                insertMenu.setVisible(true);
            } else {
                insertMenu.setVisible(false);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.insert_menu) {
            Intent intent = new Intent(this, DealActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.logout_menu) {
            builder = new AlertDialog.Builder(ListActivity.this);
            builder.setMessage("Are you sure?");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // TODO: Show confirmation dialog before doing this
                    AuthUI.getInstance()
                            .signOut(ListActivity.this)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                public void onComplete(@NonNull Task<Void> task) {
                                    FirebaseUtil.attachListener();
                                }
                            });
                    FirebaseUtil.detachListener();
                }
            });

            builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //...
                }
            });

            alertDialog = builder.create();
            alertDialog.show();
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Populate the Snackbar
    public void snackBarAction(String messageInput) {

        snackbar = Snackbar.make(constraintLayout, messageInput, Snackbar.LENGTH_INDEFINITE);
        snackbar.setDuration(500);
        View v = snackbar.getView();
        v.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        snackbar.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        FirebaseUtil.detachListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUtil.openFbReference("traveldeals", this);

        RecyclerView recyclerView = findViewById(R.id.rv_deals);
        constraintLayout = findViewById(R.id.list_constraint_layout);
        final DealAdapter adapter = new DealAdapter();
        recyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        FirebaseUtil.attachListener();
    }

    public void showMenu() {
        invalidateOptionsMenu();
    }
}
