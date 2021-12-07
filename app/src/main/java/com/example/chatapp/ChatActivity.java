package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.mbms.MbmsErrors;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity
{

    private TextView username,userLastSeen;
    private CircleImageView userImage;
    private String messageRecieverId,messageRecieverName,messageRecieverImage,messageSenderId;
    private ImageButton sendMessageButton,sendImageButton;
    private EditText MessageInputText;
    private DatabaseReference rootRef;
    private Toolbar chatToolbar;
    private FirebaseAuth mAuth;

    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessagesAdapter messagesAdapter;
    private RecyclerView userMessagesList;
    private String savedCurrentTime,savedCurrentDate,myUrl="";
private Uri fileUri;
private StorageTask uploadTask;
private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        rootRef= FirebaseDatabase.getInstance().getReference();
        mAuth=FirebaseAuth.getInstance();
        messageSenderId=mAuth.getCurrentUser().getUid();
        messageRecieverId=getIntent().getExtras().get("visit_user_id").toString();
        messageRecieverName=getIntent().getExtras().get("visit_user_name").toString();
        messageRecieverImage=getIntent().getExtras().get("visit_user_image").toString();

        InitializeControllers();

        username.setText(messageRecieverName);
        Picasso.get().load(messageRecieverImage).placeholder(R.drawable.profile_image).into(userImage);

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendMessage();
            }
        });

        DisplayLastSeen();

        sendImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               Intent intent = new Intent();
               intent.setAction(Intent.ACTION_GET_CONTENT);
               intent.setType("image/*");
               startActivityForResult(intent.createChooser(intent,"Select Image"),696);
            }
        });

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==696 && requestCode==RESULT_OK && data!=null && data.getData()!=null)
        {
            loadingBar.setTitle("Sending Image...");
            loadingBar.setMessage("Please wait while we are sending image...");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            fileUri=data.getData();

            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("ImageFile");

            final String messageSenderRef = "Messages/" +messageSenderId+ "/" + messageRecieverId;
            final String messageRecieverRef ="Messages/" +messageRecieverId+ "/" + messageSenderId;

            DatabaseReference UserMessageKeyRef = rootRef.child("Messages").child(messageSenderId).child(messageRecieverId).push();

            final String messagePushId = UserMessageKeyRef.getKey();
            final StorageReference filePath = storageReference.child(messagePushId+".jpeg");
            uploadTask = filePath.putFile(fileUri);
            uploadTask.continueWithTask(new Continuation() {
                @Override
                public Object then(@NonNull Task task) throws Exception {

                    if(task.isSuccessful())
                    {
                        throw  task.getException();
                    }
                    return filePath.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if(task.isSuccessful())
                    {
                        Uri downloadUrl = task.getResult();
                        myUrl = downloadUrl.toString();

                        Map messagePictureBody = new HashMap();
                        messagePictureBody.put("message",myUrl);
                        messagePictureBody.put("name",fileUri.getLastPathSegment());

                        messagePictureBody.put("type","image");
                        messagePictureBody.put("from",messageSenderId);
                        messagePictureBody.put("to",messageRecieverId);
                        messagePictureBody.put("messageID",messagePushId);
                        messagePictureBody.put("time",savedCurrentTime);
                        messagePictureBody.put("date",savedCurrentDate);

                        Map messageBodyDetails = new HashMap();
                        messageBodyDetails.put(messageSenderRef+"/"+messagePushId,messagePictureBody);
                        messageBodyDetails.put(messageRecieverRef+"/"+messagePushId,messagePictureBody);

                        rootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                            @Override
                            public void onComplete(@NonNull Task task) {

                                if(task.isSuccessful())
                                {
                                    Toast.makeText(ChatActivity.this, "message sent", Toast.LENGTH_SHORT).show();
                                    loadingBar.dismiss();
                                }
                                else
                                {
                                    Toast.makeText(ChatActivity.this, "ERROR", Toast.LENGTH_SHORT).show();
                                    loadingBar.dismiss();
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    private void InitializeControllers()
    {
        chatToolbar = (Toolbar)findViewById(R.id.chat_toolbar);
        setSupportActionBar(chatToolbar);

        ActionBar actionBar=getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater=(LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView  = layoutInflater.inflate(R.layout.custom_chat_bar,null);
        actionBar.setCustomView(actionBarView);

        userImage=(CircleImageView)findViewById(R.id.custom_profile_image);
        username=(TextView)findViewById(R.id.custom_profile_name);
        userLastSeen=(TextView)findViewById(R.id.custom_user_last_seen);

        sendImageButton=(ImageButton)findViewById(R.id.send_image_btn);
        sendMessageButton=(ImageButton)findViewById(R.id.send_message_btn);
        MessageInputText=(EditText)findViewById(R.id.input_message);

        messagesAdapter=new MessagesAdapter(messagesList);
        userMessagesList=(RecyclerView)findViewById(R.id.private_messages_list_of_users);
        linearLayoutManager=new LinearLayoutManager(this);
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(messagesAdapter);

        Calendar calender = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        savedCurrentDate= currentDate.format(calender.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
        savedCurrentTime= currentTime.format(calender.getTime());
    }

    private void DisplayLastSeen()
    {
        rootRef.child("Users").child(messageRecieverId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if(dataSnapshot.child("userState").hasChild("state"))
                        {
                            String state = dataSnapshot.child("userState").child("state").getValue().toString();
                            String date = dataSnapshot.child("userState").child("date").getValue().toString();
                            String time = dataSnapshot.child("userState").child("time").getValue().toString();

                            if(state.equals("online"))
                            {
                                userLastSeen.setText("Online");
                            }
                            else if(state.equals("offline"))
                            {
                                userLastSeen.setText("Last Seen : "+date+ " "+time);

                            }
                        }
                        else
                        {
                            userLastSeen.setText("Update your app");

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        rootRef.child("Messages").child(messageSenderId).child(messageRecieverId)
                .addChildEventListener(new ChildEventListener() {


                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Messages messages=dataSnapshot.getValue(Messages.class);

                        messagesList.add(messages);
                        messagesAdapter.notifyDataSetChanged();
                        userMessagesList.smoothScrollToPosition(userMessagesList.getAdapter().getItemCount());
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


    }

    private void SendMessage()
    {
        String messageText = MessageInputText.getText().toString();
        if(TextUtils.isEmpty(messageText))
        {
            Toast.makeText(this, "write a msg", Toast.LENGTH_SHORT).show();
        }
        else
        {
            String messageSenderRef = "Messages/" +messageSenderId+ "/" + messageRecieverId;
            String messageRecieverRef ="Messages/" +messageRecieverId+ "/" + messageSenderId;

            DatabaseReference UserMessageKeyRef = rootRef.child("Messages").child(messageSenderId).child(messageRecieverId).push();

            String messagePushId = UserMessageKeyRef.getKey();

            Map messageTextBody = new HashMap();
            messageTextBody.put("message",messageText);
            messageTextBody.put("type","text");
            messageTextBody.put("from",messageSenderId);
            messageTextBody.put("to",messageRecieverId);
            messageTextBody.put("messageID",messagePushId);
            messageTextBody.put("time",savedCurrentTime);
            messageTextBody.put("date",savedCurrentDate);

            Map messageBodyDetails = new HashMap();
            messageBodyDetails.put(messageSenderRef+"/"+messagePushId,messageTextBody);
            messageBodyDetails.put(messageRecieverRef+"/"+messagePushId,messageTextBody);

            rootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {

                    if(task.isSuccessful())
                    {
                        Toast.makeText(ChatActivity.this, "message sent", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Toast.makeText(ChatActivity.this, "ERROR", Toast.LENGTH_SHORT).show();
                    }
                    MessageInputText.setText("");
                }
            });



        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        messagesList.clear();
    }
}
