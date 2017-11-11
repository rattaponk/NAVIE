package com.rattapon.navie;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText etEmail;
    private EditText etPassword;
    private Button btLogin;
    private TextView tvCreateNewAcount;
    private TextView tvForgotPassword;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initInstances();
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
//        currentUser = mAuth.getCurrentUser();
//        updateUI(currentUser);
    }

    private void initInstances() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btLogin = findViewById(R.id.bt_login);
        tvCreateNewAcount = findViewById(R.id.tv_create);
        tvForgotPassword = findViewById(R.id.tv_forgot);

        btLogin.setOnClickListener(this);
        tvCreateNewAcount.setOnClickListener(this);
        tvForgotPassword.setOnClickListener(this);
    }

    private void updateUI(FirebaseUser currentUser) {
        if (currentUser != null) {
//            // Name, email address, and profile photo Url
//            String name = currentUser.getDisplayName();
//            String email = currentUser.getEmail();
//            Uri photoUrl = currentUser.getPhotoUrl();
//
//            // Check if user's email is verified
//            boolean emailVerified = currentUser.isEmailVerified();
//
//            // The user's ID, unique to the Firebase project. Do NOT use this value to
//            // authenticate with your backend server, if you have one. Use
//            // FirebaseUser.getToken() instead.
//            String uid = currentUser.getUid();
            startActivity(new Intent(this, MapActivity.class));
        }
    }

    @Override
    public void onClick(View view) {
        if (view == btLogin) {
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();
            if (!email.isEmpty() && !password.isEmpty()) {
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Sign in success, update UI with the signed-in user's information
                                    currentUser = mAuth.getCurrentUser();
                                    updateUI(currentUser);
                                    Toast.makeText(LoginActivity.this, "Login success with " + currentUser.getEmail(), Toast.LENGTH_SHORT).show();
                                } else {
                                    // If sign in fails, display a message to the user.
                                    Toast.makeText(LoginActivity.this, "Login failed.\n" + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    updateUI(null);
                                }
                            }
                        });
            } else
                Toast.makeText(LoginActivity.this, "Please enter email and password.", Toast.LENGTH_SHORT).show();

//            etEmail.getText().clear();
            etPassword.getText().clear();
        } else if (view == tvCreateNewAcount) {
            startActivity(new Intent(this, RegisterActivity.class));
        } else if (view == tvForgotPassword) {

        }
    }

}


