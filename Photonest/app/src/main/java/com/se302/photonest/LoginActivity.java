package com.se302.photonest;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText main_email, main_password;
    private Button main_login_btn;

    private TextView register_link,txtForgotPassword;

    private FirebaseAuth auth;
    private FirebaseUser currentUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        init();

        main_login_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    signInClicked();
            }
        });

        register_link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUpClicked();
            }
        });

        txtForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                forgotpasswordClicked();
            }
        });
    }

    public void init(){
        auth= FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        main_email = findViewById(R.id.main_email_adress);
        main_password= findViewById(R.id.main_password);
        main_login_btn = findViewById(R.id.main_login_button);
        register_link = findViewById(R.id.main_signup_button_text);
        txtForgotPassword = findViewById(R.id.main_text_forgot_password);

        if (currentUser != null){
            goToMain();
            finish();
        }

    }

    public void goToMain(){
        if(currentUser.isEmailVerified()){
            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
            startActivity(intent);
        }

    }

    public void signInClicked(){
        String login_email = main_email.getText().toString();
        String login_password= main_password.getText().toString();

        if(TextUtils.isEmpty(login_email)){
            Toast.makeText(this, "Email field is required!", Toast.LENGTH_LONG).show();
        } else if(TextUtils.isEmpty(login_password)){
            Toast.makeText(this, "Password field is required!", Toast.LENGTH_LONG).show();
        } else{
            main_login_btn.setEnabled(false); // to avoid one more than click
            auth.signInWithEmailAndPassword(login_email,login_password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            if(auth.getCurrentUser().isEmailVerified()){
                                Toast.makeText(LoginActivity.this, "Login succesfull!", Toast.LENGTH_LONG).show();
                                Intent main_login_intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(main_login_intent);
                                finish(); // to stop Login Activity
                            }else {
                                Toast.makeText(LoginActivity.this, "Please verify your email address!", Toast.LENGTH_LONG).show();
                                main_login_btn.setEnabled(true);
                            }
                        } else{
                            Toast.makeText(LoginActivity.this, "Email or password is wrong!", Toast.LENGTH_LONG).show();
                            main_login_btn.setEnabled(true); //if login isnot successfull login button will be enable again.
                        }
                }
            });
        }

    }

    public void signUpClicked(){
        Intent register_page_intent= new Intent(LoginActivity.this, RegistrationActivity.class);
        startActivity(register_page_intent);
        finish();

    }

    public void forgotpasswordClicked(){
        Intent register_page_intent= new Intent(LoginActivity.this, ForgotPasswordActivity.class);
        startActivity(register_page_intent);
        finish();
    }
}
