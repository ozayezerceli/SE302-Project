package com.se302.photonest;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;


import java.util.ArrayList;
import java.util.List;

import DataModels.Photo;
import DataModels.PhotoInformation;
import DataModels.User;
import DataModels.UserInformation;
import Utils.BottomNavigationViewHelper;
import Utils.FirebaseMethods;
import Utils.GridImageAdapter;
import Utils.StringManipulation;

public class ViewProfileActivity extends AppCompatActivity {

    private int NUM_COLUMNS = 3;

    private FirebaseAuth mAuth;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference myRef;
    private TextView username;
    private Context myContext = ViewProfileActivity.this;
    private String userID;
    private User muser;
    private String viewUserID;

    private ImageView image_profile;
    private TextView posts, followers,following, fullname, bio;
    private Button follow_Btn, unfollow_Btn, editprofile_Btn;
    private RelativeLayout mrelativelayout;
    private GridView mGridView;

    private FirebaseMethods firebaseMethods;

    ImageButton my_photos;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);
        username = findViewById(R.id.ViewusernameTxt);
        fullname = findViewById(R.id.View_fullname_profile);
        bio = findViewById(R.id.View_bio_profile);
        posts = findViewById(R.id.View_posts);
        followers = findViewById(R.id.View_followers);
        following = findViewById(R.id.View_following);
        follow_Btn = findViewById(R.id.Follow_button);
        unfollow_Btn = findViewById(R.id.UnFollow_button);
        editprofile_Btn = findViewById(R.id.View_editprofile_button);
        image_profile = findViewById(R.id.View_profile_image);
        mrelativelayout = findViewById(R.id.relativeTop);
        mGridView = findViewById(R.id.grid_view);

        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        myRef = firebaseDatabase.getReference();
        FirebaseUser user =mAuth.getCurrentUser();
        userID = user.getUid();
        firebaseMethods = new FirebaseMethods(ViewProfileActivity.this);

        init();
        getFollowers();
        getNrPosts();
        setupBottomNavBar();
        setUserPhotos();

        follow_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseMethods.addFollowingAndFollowers(viewUserID);
                setFollowing();
            }
        });

        unfollow_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseMethods.removeFollowingAndFollowers(viewUserID);
                setUnfollowing();
            }
        });

        editprofile_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mrelativelayout.setVisibility(View.GONE);
                EditProfileFragment fragment = new EditProfileFragment();
                FragmentTransaction transaction = ViewProfileActivity.this.getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.container, fragment);
                transaction.addToBackStack(getString(R.string.profile_fragment));
                transaction.commit();
            }
        });

        if(viewUserID.equals(userID)){
            follow_Btn.setVisibility(View.GONE);
            unfollow_Btn.setVisibility(View.GONE);
            editprofile_Btn.setVisibility(View.VISIBLE);
        } else{
            checkFollow();
        }
    }

    private void init(){
        Intent intent = getIntent();
        if(intent.hasExtra(getString(R.string.users_id))){
            Bundle args = intent.getExtras();
            viewUserID = args.getString(getString(R.string.users_id));
            DatabaseReference myRef = FirebaseDatabase.getInstance().getReference();
            Query query = myRef.child(getString(R.string.users_node)).child(viewUserID);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    muser = dataSnapshot.getValue(User.class);
                    username.setText(muser.getUsername());
                    fullname.setText(muser.getFullName());
                    bio.setText(muser.getBio());
                    Glide.with(getApplicationContext()).load(muser.getImageUrl()).into(image_profile);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }else{
            Toast.makeText(this, "something went wrong", Toast.LENGTH_SHORT).show();
        }

    }

    private  void checkFollow(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child(getString(R.string.following_node))
                .child(userID)
                .orderByChild(getString(R.string.users_id))
                .equalTo(viewUserID);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot :  dataSnapshot.getChildren()){
                    setFollowing();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setFollowing(){
        //Log.d(TAG, "setFollowing: updating UI for following this user");
        follow_Btn.setVisibility(View.GONE);
        unfollow_Btn.setVisibility(View.VISIBLE);
        editprofile_Btn.setVisibility(View.GONE);
    }

    private void setUnfollowing(){
        //Log.d(TAG, "setFollowing: updating UI for unfollowing this user");
        follow_Btn.setVisibility(View.VISIBLE);
        unfollow_Btn.setVisibility(View.GONE);
        editprofile_Btn.setVisibility(View.GONE);
    }

    private  void getFollowers(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                followers.setText(""+firebaseMethods.getFollowerCount(dataSnapshot, viewUserID));
                following.setText(""+firebaseMethods.getFollowingCount(dataSnapshot, viewUserID));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void getNrPosts(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                posts.setText(""+firebaseMethods.getImageCount(dataSnapshot));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setUserPhotos(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().
                child(getString(R.string.dbname_user_photos)).child(viewUserID);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final ArrayList<PhotoInformation> photoArrayList = new ArrayList<PhotoInformation>();
                for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                    PhotoInformation photoInformation = new PhotoInformation();
                    photoInformation.setCaption(snapshot.child("caption").getValue().toString());
                    photoInformation.setPhoto_id(snapshot.child("photo_id").getValue().toString());
                    photoInformation.setUser_id(snapshot.child("user_id").getValue().toString());
                    List<String> hashTags = StringManipulation.getHashTags(photoInformation.getCaption());
                    photoInformation.setHashTags(hashTags);
                    photoInformation.setDate_created(snapshot.child("date_created").getValue().toString());
                    photoInformation.setImage_path(snapshot.child("image_path").getValue().toString());


                    photoArrayList.add(photoInformation);
                }
                int gridWidth = getResources().getDisplayMetrics().widthPixels;
                int imageWidth = gridWidth/NUM_COLUMNS;
                mGridView.setColumnWidth(imageWidth);

                ArrayList<String> imgUrls = new ArrayList<String>();
                for(int i = 0; i < photoArrayList.size(); i++){
                    imgUrls.add(photoArrayList.get(i).getImage_path());
                }

                GridImageAdapter adapter = new GridImageAdapter(ViewProfileActivity.this , R.layout.grid_imageview, "", imgUrls);
                mGridView.setAdapter(adapter);

                mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        mrelativelayout.setVisibility(View.GONE);
                        PostViewFragment fragment = new PostViewFragment();
                        Bundle args = new Bundle();
                        args.putParcelable("photo", photoArrayList.get(position));
                        args.putInt("activityNumber", 1);
                        fragment.setArguments(args);

                        FragmentTransaction transaction  = getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.container, fragment);
                        transaction.addToBackStack("View Post");
                        transaction.commit();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setupBottomNavBar(){
        BottomNavigationViewEx bottomNavBar = (BottomNavigationViewEx) findViewById(R.id.bottomNavBar);
        BottomNavigationViewHelper.setupBottomNavigationView(bottomNavBar);
        BottomNavigationViewHelper.enableNavigation(myContext, this, bottomNavBar);
        Menu menu = bottomNavBar.getMenu();
        MenuItem mItem = menu.getItem(1);
        mItem.setChecked(true);
    }
}
