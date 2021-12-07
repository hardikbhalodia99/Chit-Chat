package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

public class LoginActivity extends AppCompatActivity
{
    private Button LoginButton,PhoneLoginButton;
    private EditText UserEmail,UserPasword;
    private TextView NeedNewAccountLink,ForgetPasswordLink;
    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;
    private DatabaseReference UserRef;
    private String deviceToken;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth=FirebaseAuth.getInstance();
        UserRef= FirebaseDatabase.getInstance().getReference().child("Users");

        InitializeFields();

        //if user requires new account then send them to register activity
        NeedNewAccountLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendUserToRegisterActivity();
            }
        });

        LoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AllowUserToLogin();
            }
        });
    }

    private void AllowUserToLogin()
    {
        String email=UserEmail.getText().toString();
        String password=UserPasword.getText().toString();

        if(TextUtils.isEmpty(email))
        {
            Toast.makeText(this, "Please enter email...", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(password))
        {
            Toast.makeText(this, "Please enter password...", Toast.LENGTH_SHORT).show();
        }
        else
        {
            loadingBar.setTitle("Sign In");
            loadingBar.setMessage("Please wait...");
            loadingBar.setCanceledOnTouchOutside(true);
            loadingBar.show();
            mAuth.signInWithEmailAndPassword(email,password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                                if(task.isSuccessful())
                                {
                                    String currentUserID = mAuth.getCurrentUser().getUid();

                                    FirebaseInstanceId.getInstance().getInstanceId()
                                            .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                                                @Override
                                                public void onComplete(@NonNull Task<InstanceIdResult> task) {

                                                    if(task.isSuccessful()) {
                                                        deviceToken = task.getResult().getToken();
                                                    }



                                                }
                                            });

                                    UserRef.child(currentUserID).child("device_token").setValue(deviceToken)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {

                                                    if(task.isSuccessful())
                                                    {
                                                        SendUserToMainActivity();
                                                        Toast.makeText(LoginActivity.this, "Logged in Successfully...", Toast.LENGTH_SHORT).show();
                                                        loadingBar.dismiss();
                                                    }
                                                }
                                            });


                                }
                                else
                                {
                                    String message=task.getException().toString();
                                    Toast.makeText(LoginActivity.this, "Error : "+message, Toast.LENGTH_SHORT).show();
                                    loadingBar.dismiss();
                                }
                        }
                    });

        }

    }

    //initalizing the items
    private void InitializeFields()
    {
        LoginButton=(Button)findViewById(R.id.login_button);
        PhoneLoginButton=(Button)findViewById(R.id.phone_login_button);
        UserEmail=(EditText)findViewById(R.id.login_email);
        UserPasword=(EditText)findViewById(R.id.login_password);
        NeedNewAccountLink=(TextView)findViewById(R.id.need_new_account_link);
        ForgetPasswordLink=(TextView)findViewById(R.id.forget_password_link);
        loadingBar=new ProgressDialog(this);
    }


    //sends user to main activity
    private void SendUserToMainActivity()
    {
        Intent mainIntent = new Intent(LoginActivity.this,MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }


    private void SendUserToRegisterActivity()  //sending user to register activity if he want to signup
    {
        Intent registerIntent = new Intent(LoginActivity.this,RegisterActivity.class);
        startActivity(registerIntent);
    }



}
