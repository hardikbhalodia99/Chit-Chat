package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity
{

    private Toolbar mtoolbar;
    private ViewPager myViewPager;
    private TabLayout myTabLayout;
    private TabsAccessorAdapter myTabsAccessorAdaptor;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;
    private String CurrentUserID;



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth=FirebaseAuth.getInstance();
        RootRef= FirebaseDatabase.getInstance().getReference();


        mtoolbar=(Toolbar)findViewById(R.id.main_page_toolbar);
        setSupportActionBar(mtoolbar);
        getSupportActionBar().setTitle("GoChat");

        myViewPager=(ViewPager)findViewById(R.id.main_tabs_pager);
        myTabsAccessorAdaptor=new TabsAccessorAdapter(getSupportFragmentManager());
        myViewPager.setAdapter(myTabsAccessorAdaptor);

        myTabLayout=(TabLayout)findViewById(R.id.main_tabs);
        myTabLayout.setupWithViewPager(myViewPager);


    }

    @Override
    protected void onStart()             // it checks if the current user is null if so then it starts to signup process
    {
        super.onStart();

        FirebaseUser currentUser=mAuth.getCurrentUser();


        if(currentUser == null)
        {
            SendUserToLoginActivity();
        }
        else
        {
            updateUserState("online");
            VerifyUserExistance();
        }


    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseUser currentUser=mAuth.getCurrentUser();

        if(currentUser!=null)
        {
            updateUserState("offline");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FirebaseUser currentUser=mAuth.getCurrentUser();

        if(currentUser!=null)
        {
            updateUserState("offline");
        }
    }

    private void VerifyUserExistance()
    {
        String currentUserID =mAuth.getCurrentUser().getUid();

        RootRef.child("Users").child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if((dataSnapshot.child("name").exists()))
                {
                    Toast.makeText(MainActivity.this, "Welcome", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    SendUserToSettingsActivity();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    //sends user to Login Activity
    private void SendUserToLoginActivity()
    {
        Intent loginIntent = new Intent(MainActivity.this,LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    //sends user to Settings Activity
    private void SendUserToSettingsActivity()
    {
        Intent settingsIntent = new Intent(MainActivity.this,SettingsActivity.class);
        startActivity(settingsIntent);
    }


    private void SendUserToFindFriendsActivity()
    {
        Intent friendsIntent = new Intent(MainActivity.this,FindFriendsActivity.class);
        startActivity(friendsIntent);

    }


    // inflates menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
         super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.options_menu,menu);
        return true;
    }

    //creates new groups
    private void RequestNewGroup()
    {
        AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this,R.style.AlertDialog);
        builder.setTitle("Enter Group Name : ");

        final EditText groupNameField = new EditText(MainActivity.this);
        groupNameField.setHint(" e.g Google Coders");
        builder.setView(groupNameField);

        builder.setPositiveButton("create", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                    String groupName=groupNameField.getText().toString();
                    if(TextUtils.isEmpty(groupName))
                    {
                        Toast.makeText(MainActivity.this, "Please Write Group name", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        CreateNewGroup(groupName);

                    }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void CreateNewGroup(final String groupName)
    {
        RootRef.child("Groups").child(groupName).setValue("")
                .addOnCompleteListener(new OnCompleteListener<Void>()
                {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if(task.isSuccessful())
                        {
                            Toast.makeText(MainActivity.this, groupName+" group is created Successfully", Toast.LENGTH_SHORT).show();
                        }

                    }
                });


    }



    //switch case for menu options
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
         super.onOptionsItemSelected(item);

         switch (item.getItemId())
         {
             case R.id.main_find_friends_option:
                SendUserToFindFriendsActivity();
                 return true;

             case R.id.main_create_group_option:
                 RequestNewGroup();
                 return true;

             case R.id.main_logout_option:
                 updateUserState("offline");
                 mAuth.signOut();
                 SendUserToLoginActivity();
                 return true;

             case R.id.main_settings_option:
                 SendUserToSettingsActivity();
                 return true;

             default: return false;
         }


    }


//online offline status
    private void updateUserState(String state)
    {
        String savedCurrentTime,savedCurrentDate;

        Calendar  calender = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        savedCurrentDate= currentDate.format(calender.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
        savedCurrentTime= currentTime.format(calender.getTime());

        HashMap<String ,Object> onlineStateMap = new HashMap<>();
        onlineStateMap.put("time",savedCurrentTime);
        onlineStateMap.put("date",savedCurrentDate);
        onlineStateMap.put("state",state);

        CurrentUserID=mAuth.getCurrentUser().getUid();

        RootRef.child("Users").child(CurrentUserID).child("userState")
                .updateChildren(onlineStateMap);


    }

}
