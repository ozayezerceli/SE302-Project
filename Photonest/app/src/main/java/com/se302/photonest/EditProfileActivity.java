package com.se302.photonest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

import DataModels.UserInformation;
import Utils.FilePaths;
import Utils.FirebaseMethods;
import Utils.GlideImageLoader;

public class EditProfileActivity extends AppCompatActivity {
    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private FirebaseMethods mFirebaseMethods;
    private String userID;
    private FirebaseUser user;
    private UserInformation uInfo;
    private static int GaleriPick=1;
    final int PIC_CROP = 1;
    private Uri ImageUri;
    private Button delete_photo;
    private Button change_photo;
    private StorageTask uploadtask;
    private  StorageTask delete_photo_task;
    private StorageReference user_image_ref;
    private Context context= EditProfileActivity.this;
    private String downloadURL;
    private Intent intent;
    private String imgUrl;
    private boolean profilechanged= false;
    private boolean uploaded = false;
    private boolean photoDeleted=false;
    private Bitmap bitmap;

    private EditText EditFullName, EditUsername, EditWebsite, EditBio;
    private ImageView edit_profile_image;
    private  String def_image="https://firebasestorage.googleapis.com/v0/b/photonest-11327.appspot.com/o/defaultphoto%2Fplace_holder_photo.png?alt=media&token=f450daed-b913-4991-8456-ff6920d63b25";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_edit_profile);
        EditFullName = findViewById(R.id.EditFullName);
        EditUsername = findViewById(R.id.EditUsername);
        EditWebsite = findViewById(R.id.EditWebsite);
        EditBio = findViewById(R.id.EditBio);
        edit_profile_image= findViewById(R.id.profile_image_edit);
        delete_photo = findViewById(R.id.delete_image_photo);
        user_image_ref= FirebaseStorage.getInstance().getReference().child("imagephoto");

        edit_profile_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galeriIntent= new Intent();
                galeriIntent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                galeriIntent.setType("image/*");
                startActivityForResult(galeriIntent,GaleriPick);

            }
        });


        mAuth=FirebaseAuth.getInstance();
        userID =mAuth.getCurrentUser().getUid();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference().child("Users").child(userID);

        init();

        ImageView backarrow = findViewById(R.id.backArrow);
        backarrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ImageView checkMark = findViewById(R.id.saveChanges);
        checkMark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateData();
                if(profilechanged){
                    uploadImage();
                    Toast.makeText(context, "Changes are being loaded.", Toast.LENGTH_SHORT).show();
                }
                else {
                    startActivity(new Intent(context, ProfileActivity.class));
                    Toast.makeText(context, "Profile Edited!", Toast.LENGTH_SHORT).show();
                    finish();
                }


            }
        });

        delete_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(context)
                        .setTitle("Delete")
                        .setMessage("Your profile photo will be deleted. \nAre you sure?")
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                //Toast.makeText(getActivity(), "Deleting profile photo...", Toast.LENGTH_SHORT).show();
                                DatabaseReference reference= FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                                GlideImageLoader.loadImageWithOutTransition(context, def_image, edit_profile_image);
                                Toast.makeText(context, "Profile photo is deleted", Toast.LENGTH_SHORT).show();

                                HashMap<String,Object> hashMap = new HashMap<>();
                                hashMap.put("imageurl",""+def_image);
                                reference.child("imageurl").setValue(def_image);

                                reference.updateChildren(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task)
                                    {
                                        if (task.isSuccessful()) {
                                            photoDeleted=true;
                                            edit_profile_image.setImageURI(Uri.parse(def_image));
                                            startActivity(new Intent(context, ProfileActivity.class));
                                            Toast.makeText(context, "Profile Photo is deleted.", Toast.LENGTH_SHORT).show();
                                            finish();
                                        }
                                    }
                                });
                            }})
                        .setNegativeButton("NO", null).show();
            }


        });

    }

    public void init(){
        myRef.addValueEventListener(new ValueEventListener() { //to get information of user
            @SuppressLint("ResourceType")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                uInfo = dataSnapshot.getValue(UserInformation.class);
                EditFullName.setText(uInfo.getFullName());
                EditUsername.setText(uInfo.getUsername());
                EditBio.setText(uInfo.getBio());
                EditWebsite.setText(uInfo.getWebsite_link());
                String Image_Url= uInfo.getImageurl();
                GlideImageLoader.loadImageWithOutTransition(getApplicationContext(), Image_Url, edit_profile_image);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==GaleriPick && resultCode==RESULT_OK && data!=null){
            ImageUri=data.getData();
            edit_profile_image.setImageURI(ImageUri);
            profilechanged=true;
        }  else {
            Toast.makeText(context,"Something gone wrong!", Toast.LENGTH_SHORT).show();

        }
    }



    private void uploadImage(){
        if(profilechanged){ //if a image is selected from gallery
            myRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    final String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    FilePaths filePaths = new FilePaths();

                    final StorageReference myStrRef =  FirebaseStorage.getInstance().getReference();
                    final StorageReference filePath=myStrRef
                            .child(filePaths.PROFILE_PHOTO_STORAGE + "/" + user_id);


                    final UploadTask uploadTask = filePath.putFile(ImageUri);


                    uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception
                        {
                            downloadURL=filePath.getDownloadUrl().toString();
                            return filePath.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful() && !uploaded) {
                                downloadURL=task.getResult().toString();

                                DatabaseReference reference= FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                                HashMap<String,Object> hashMap = new HashMap<>();
                                hashMap.put("imageurl",""+downloadURL);
                                reference.child("imageurl").setValue(downloadURL);

                                edit_profile_image.setImageURI(task.getResult());
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
                                ref.child(context.getString(R.string.dbname_photos)).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for(DataSnapshot ds : dataSnapshot.getChildren()){
                                            if(ds.child("comment").getChildrenCount() != 0){
                                                ds.getRef().child("comment").addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        for(DataSnapshot ds2: dataSnapshot.getChildren()){
                                                            if(ds2.child("userId").getValue().equals(userID) && !ds2.child("profile_image").getValue().equals(downloadURL)){
                                                                ds2.getRef().child("profile_image").setValue(downloadURL);
                                                            }
                                                        }
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

                                reference.updateChildren(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull final Task<Void> task)
                                    {
                                        if (task.isSuccessful())
                                        {
                                            uploaded=true;

                                            startActivity(new Intent(context, ProfileActivity.class));
                                            Toast.makeText(context, "Profile Photo is changed.", Toast.LENGTH_SHORT).show();
                                            finish();
                                        }
                                    }
                                });
                            } else if(!task.isSuccessful()){
                                Toast.makeText(context, "Failed.",Toast.LENGTH_SHORT).show();
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(context, e.getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    });

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });



        } else {
            Toast.makeText(context,"No image selected.", Toast.LENGTH_SHORT).show();
        }
    }




    //The method below takes the input from user and updates the user information on firebase
    private void updateData(){
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String username = EditUsername.getText().toString();
                String fullname = EditFullName.getText().toString();
                String bio = EditBio.getText().toString();
                String websitelink = EditWebsite.getText().toString();

                if(!dataSnapshot.child("username").getValue().toString().equals(username)){
                    checkIfUsernameExists(username);
                }
                if(!dataSnapshot.child("fullName").getValue().toString().equals(fullname)){
                    myRef.child("fullName").setValue(fullname);

                }
                if(!dataSnapshot.child("bio").getValue().toString().equals(bio)){
                    myRef.child("bio").setValue(bio);

                }
                if(!dataSnapshot.child("website_link").getValue().toString().equals(websitelink)){
                    myRef.child("website_link").setValue(websitelink);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    //The method below checks if the new username already exist in the database,
    //if username exists, it does not allow user to change his username
    private void checkIfUsernameExists(final String username) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child("Users").orderByChild("username").equalTo(username);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(!dataSnapshot.exists()){
                    myRef.child("username").setValue(username);

                }
                for(DataSnapshot singleSnapshot: dataSnapshot.getChildren()){
                    if (singleSnapshot.exists()){
                        Toast.makeText(context, "That username already exists.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(context, "GET_ACCOUNTS Denied",
                            Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions,
                        grantResults);
        }
    }

}
