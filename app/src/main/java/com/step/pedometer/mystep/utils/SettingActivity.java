package com.step.pedometer.mystep.utils;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.step.pedometer.mystep.MainActivity;
import com.step.pedometer.mystep.R;

public class SettingActivity extends AppCompatActivity {
    private EditText etWight;
    private EditText etHigh;
    private EditText etStep;
    private Button btStart;
    private SharedPreferences sp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp=getSharedPreferences("data",MODE_PRIVATE);
        if(!sp.getBoolean("start",true)){
            startActivity(new Intent(SettingActivity.this, MainActivity.class));
        }
        setContentView(R.layout.activity_setting);
        etWight= (EditText) findViewById(R.id.etZhong);
        etHigh= (EditText) findViewById(R.id.etGao);
        etStep= (EditText) findViewById(R.id.etBu);
        btStart= (Button) findViewById(R.id.btStart);

        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!TextUtils.isEmpty(etHigh.getText())&&!TextUtils.isEmpty(etWight.getText())&&!TextUtils.isEmpty(etStep.getText().toString())){
                  sp.edit().putString("weight",etWight.getText().toString().trim()).putString("step",etStep.getText().toString()).putBoolean("start",false).commit();
                   startActivity(new Intent(SettingActivity.this, MainActivity.class));
                }else {
                    Toast.makeText(SettingActivity.this,"请完善健康资料",Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}
