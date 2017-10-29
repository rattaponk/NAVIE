package com.rattapon.navie;

import android.app.DatePickerDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;


public class RegisterActivity extends AppCompatActivity {

    private int mDay , mMonth, mYear;
    private EditText etDOB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etDOB = (EditText)findViewById(R.id.et_dob);
        etDOB.setInputType(InputType.TYPE_NULL);

        etDOB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog datePickerDialog = new DatePickerDialog(RegisterActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                        month = month+1;
                        etDOB.setText(day + "/"+ month + "/" + year);
                        mDay = day;
                        mMonth = month;
                        mYear = year;
                    }
                }, mYear, mMonth, mDay);
                datePickerDialog.show();
            }
        });

    }
}
