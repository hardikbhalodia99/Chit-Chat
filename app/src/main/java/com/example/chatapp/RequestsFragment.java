package com.example.chatapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class RequestsFragment extends Fragment {

    private View RequestFragmentView;
    private RecyclerView myRequestList;
    private DatabaseReference ChatRequestRef,UsersRef,ContactsRef;
    private FirebaseAuth mAuth;
    private String CurrentUserId;



    public RequestsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        RequestFragmentView =  inflater.inflate(R.layout.fragment_requests, container, false);

        myRequestList=(RecyclerView)RequestFragmentView.findViewById(R.id.chat_request_list);
        myRequestList.setLayoutManager(new LinearLayoutManager(getContext()));
        ChatRequestRef= FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        ContactsRef=FirebaseDatabase.getInstance().getReference().child("Contacts");
        mAuth=FirebaseAuth.getInstance();
        CurrentUserId=mAuth.getCurrentUser().getUid();
        UsersRef=FirebaseDatabase.getInstance().getReference().child("Users");
        return RequestFragmentView;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Contacts> options=new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(ChatRequestRef.child(CurrentUserId),Contacts.class)
                .build();

        FirebaseRecyclerAdapter<Contacts,RequestViewHolder> adapter = new FirebaseRecyclerAdapter<Contacts, RequestViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final RequestViewHolder holder, int position, @NonNull Contacts model)
            {
                holder.itemView.findViewById(R.id.request_accept_button).setVisibility(View.VISIBLE);
                holder.itemView.findViewById(R.id.request_cancel_button).setVisibility(View.VISIBLE);

                final String list_user_id = getRef(position).getKey();
                final DatabaseReference getTypeRef = getRef(position).child("request_type").getRef();
                getTypeRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if(dataSnapshot.exists())
                        {
                            String type=dataSnapshot.getValue().toString();

                            if(type.equals("received"))
                            {
                                UsersRef.child(list_user_id).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        if(dataSnapshot.hasChild("image"))
                                        {

                                            final String requestProfileImage=dataSnapshot.child("image").getValue().toString();

                                            Picasso.get().load(requestProfileImage).placeholder(R.drawable.profile_image).into(holder.profileImage);

                                        }

                                            final String requestUSerName=dataSnapshot.child("name").getValue().toString();
                                           // final String requestUserStatus=dataSnapshot.child("status").getValue().toString();

                                            holder.userName.setText(requestUSerName);
                                            holder.userstatus.setText("Wants to connect with you");


                                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                CharSequence options[] = new CharSequence[]
                                                        {
                                                                "Accept",
                                                                "Cancel"
                                                        };
                                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                                builder.setTitle(requestUSerName+" Chat Request");
                                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int i)
                                                    {
                                                        if(i == 0)
                                                        {
                                                            ContactsRef.child(CurrentUserId).child(list_user_id).child("Contact")
                                                                    .setValue("Saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {

                                                                    if(task.isSuccessful())
                                                                    {
                                                                        ContactsRef.child(list_user_id).child(CurrentUserId).child("Contact")
                                                                                .setValue("Saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<Void> task) {

                                                                                if(task.isSuccessful())
                                                                                {
                                                                                    ChatRequestRef.child(CurrentUserId).child(list_user_id)
                                                                                            .removeValue()
                                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                @Override
                                                                                                public void onComplete(@NonNull Task<Void> task) {

                                                                                                    if(task.isSuccessful())
                                                                                                    {
                                                                                                        ChatRequestRef.child(list_user_id).child(CurrentUserId)
                                                                                                                .removeValue()
                                                                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                                    @Override
                                                                                                                    public void onComplete(@NonNull Task<Void> task) {

                                                                                                                        Toast.makeText(getContext(), "New Contact Added!!!", Toast.LENGTH_SHORT).show();

                                                                                                                    }
                                                                                                                });
                                                                                                    }

                                                                                                }
                                                                                            });


                                                                                }
                                                                            }
                                                                        });
                                                                    }
                                                                }
                                                            });
                                                        }
                                                        if(i==1)
                                                        {
                                                            ChatRequestRef.child(CurrentUserId).child(list_user_id)
                                                                    .removeValue()
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {

                                                                            if(task.isSuccessful())
                                                                            {
                                                                                ChatRequestRef.child(list_user_id).child(CurrentUserId)
                                                                                        .removeValue()
                                                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                            @Override
                                                                                            public void onComplete(@NonNull Task<Void> task) {

                                                                                                Toast.makeText(getContext(), "Request Deleted", Toast.LENGTH_SHORT).show();

                                                                                            }
                                                                                        });
                                                                            }

                                                                        }
                                                                    });

                                                        }
                                                    }
                                                });
                                                builder.show();
                                            }

                                        });

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }
                            else if(type.equals("sent"))
                            {
                                Button request_sent_btn=holder.itemView.findViewById(R.id.request_accept_button);
                                request_sent_btn.setText("Req Sent");

                                holder.itemView.findViewById(R.id.request_cancel_button).setVisibility(View.INVISIBLE);

                                UsersRef.child(list_user_id).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        if(dataSnapshot.hasChild("image"))
                                        {

                                            final String requestProfileImage=dataSnapshot.child("image").getValue().toString();

                                            Picasso.get().load(requestProfileImage).placeholder(R.drawable.profile_image).into(holder.profileImage);

                                        }

                                        final String requestUSerName=dataSnapshot.child("name").getValue().toString();
                                        // final String requestUserStatus=dataSnapshot.child("status").getValue().toString();

                                        holder.userName.setText(requestUSerName);
                                        holder.userstatus.setText("you have sent a request to " + requestUSerName);


                                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                CharSequence options[] = new CharSequence[]
                                                        {
                                                                "Cancel Chat Request"
                                                        };
                                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                                builder.setTitle("Already Sent Request");
                                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int i)
                                                    {
                                                        if(i==0)
                                                        {
                                                            ChatRequestRef.child(CurrentUserId).child(list_user_id)
                                                                    .removeValue()
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {

                                                                            if(task.isSuccessful())
                                                                            {
                                                                                ChatRequestRef.child(list_user_id).child(CurrentUserId)
                                                                                        .removeValue()
                                                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                            @Override
                                                                                            public void onComplete(@NonNull Task<Void> task) {

                                                                                                Toast.makeText(getContext(), "You ahve Canceled the chat request", Toast.LENGTH_SHORT).show();

                                                                                            }
                                                                                        });
                                                                            }

                                                                        }
                                                                    });

                                                        }
                                                    }
                                                });
                                                builder.show();
                                            }

                                        });

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                            }

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


            }

            @NonNull
            @Override
            public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.users_display_layout,parent,false);
                RequestViewHolder holder=new RequestViewHolder(view);
                return holder;


            }
        };
        myRequestList.setAdapter(adapter);
        adapter.startListening();

    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder
    {
        TextView userName,userstatus;
        CircleImageView profileImage;
        Button AcceptButton,CancelButton;

        public RequestViewHolder(@NonNull View itemView)
        {
            super(itemView);

            userName=(TextView)itemView.findViewById(R.id.user_profile_name);
            userstatus=(TextView)itemView.findViewById(R.id.user_status);
            profileImage=(CircleImageView)itemView.findViewById(R.id.user_profile_image);
            AcceptButton=(Button)itemView.findViewById(R.id.request_accept_button);
            CancelButton=(Button)itemView.findViewById(R.id.request_cancel_button);

        }
    }
}
