package com.rattapon.navie;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rattapon.navie.JavaClass.User;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

    private int mDay, mMonth, mYear;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConPass;
    private EditText etName;
    private RadioGroup rgGender;
    private EditText etDOB;
    private Button btRegister;

    private FirebaseAuth mAuth;

    private class GenericTextWatcher implements TextWatcher {
        private View view;

        private GenericTextWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void afterTextChanged(Editable editable) {
            String text = editable.toString();
            if (view == etEmail) {
                if (!isEmailValid(text)) {
                    etEmail.setError("Invalid Email Address.");
                }
            } else if (view == etPassword) {
                if (!isPasswordValid(text)) {
                    etPassword.setError("Required more 8 characters.");
                }
            } else if (view == etConPass) {
                if (!isSamePassword()) {
                    etConPass.setError("Password not match.");
                }
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void initInstance() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConPass = findViewById(R.id.et_cpassword);
        etName = findViewById(R.id.et_name);
        etDOB = findViewById(R.id.et_dob);
        rgGender = findViewById(R.id.rg_gender);
        btRegister = findViewById(R.id.bt_register);

        etDOB.setInputType(InputType.TYPE_NULL);

        btRegister.setOnClickListener(this);
        etDOB.setOnClickListener(this);

        etEmail.addTextChangedListener(new GenericTextWatcher(etEmail));
        etPassword.addTextChangedListener(new GenericTextWatcher(etPassword));
        etConPass.addTextChangedListener(new GenericTextWatcher(etConPass));
    }

    @Override
    public void onClick(View view) {
        if (view == btRegister) {
            String btgender = "";
            switch (rgGender.getCheckedRadioButtonId()) {
                case R.id.rb_male:
                    btgender = "Male";
                    break;
                case R.id.rb_female:
                    btgender = "Female";
                    break;
                default:
                    break;
            }

            final String email = etEmail.getText().toString();
            final String password = etPassword.getText().toString();
            final String conpass = etConPass.getText().toString();
            final String name = etName.getText().toString();
            if (mMonth < 10) mMonth = '0' + mMonth;
            final String dob = mYear + "-" + mMonth + "-" + mDay;
            final String gender = btgender;

            if (isEmailValid(email) && isPasswordValid(password) && isSamePassword() && !name.isEmpty() && !dob.isEmpty()) {
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Sign in success, update UI with the signed-in user's information
                                    Toast.makeText(RegisterActivity.this, "Authentication success.", Toast.LENGTH_SHORT).show();
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    pushRegisterData(user, email, name, dob, gender);
                                    startActivity(new Intent(RegisterActivity.this, EventsActivity.class));
                                } else {
                                    // If sign in fails, display a message to the user.
                                    Toast.makeText(RegisterActivity.this, "Authentication failed.\n" + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
//            Toast.makeText(this, email + "\n" + password + "\n" + name + "\n" + gender + "\n" + dob, Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(RegisterActivity.this, "Please enter your information.", Toast.LENGTH_SHORT).show();

        } else if (view == etDOB) {
            DatePickerDialog datePickerDialog = new DatePickerDialog(RegisterActivity.this, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                    month = month + 1;
                    etDOB.setText(day + "/" + month + "/" + year);
                    mDay = day;
                    mMonth = month;
                    mYear = year;
                }
            }, mYear, mMonth, mDay);
            datePickerDialog.show();
        }
    }

    public boolean isEmailValid(String email) {
        String VALID_EMAIL_ADDRESS_REGEX = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$";
        Pattern pattern = Pattern.compile(VALID_EMAIL_ADDRESS_REGEX, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        return matcher.find();
    }

    public boolean isPasswordValid(String password) {
//        ^                 # start-of-string
//        (?=.*[0-9])       # a digit must occur at least once
//        (?=.*[a-z])       # a lower case letter must occur at least once
//        (?=\S+$)          # no whitespace allowed in the entire string
//        .{8,}             # anything, at least eight places though
//        $                 # end-of-string
//        String VALID_PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=\\S+$).{8,}$";
        String VALID_PASSWORD_REGEX = "^(?=\\S+$).{8,}$";
        Pattern pattern = Pattern.compile(VALID_PASSWORD_REGEX, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(password);
        return matcher.find();
    }

    public boolean isSamePassword() {
        String conpass = etConPass.getText().toString();
        String password = etPassword.getText().toString();
        if (conpass.equals(password)) {
            return true;
        } else
            return false;
    }

    public void pushRegisterData(FirebaseUser currentUser,String e,String p, String dob, String g) {
        String key = currentUser.getUid();
        DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference mUsersRef = mRootRef.child("users");
        User user = new User(e,p,dob,g);
        mUsersRef.child(key).setValue(user);
    }

    public void hideSoftKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager)getSystemService(this.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(),0);
    }
}
