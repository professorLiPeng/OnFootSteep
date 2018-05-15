package com.step.pedometer.mystep;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.step.pedometer.mystep.config.Constant;
import com.step.pedometer.mystep.service.StepService;

public class MainActivity extends AppCompatActivity  implements Handler.Callback {
    //循环取当前时刻的步数中间的时间间隔
    private long TIME_INTERVAL = 500;
    //控件
    private TextView text_step,text_long,text_kaluli;    //显示走的步数,公里数，卡路里数
   private ImageView iv; //设置目标按钮

    private Messenger messenger;
    private Messenger mGetReplyMessenger = new Messenger(new Handler(this));
    private Handler delayHandler;
    private SharedPreferences sp;
    private int step;
    //以bind形式开启service，故有ServiceConnection接收回调
    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                messenger = new Messenger(service);
                Message msg = Message.obtain(null, Constant.MSG_FROM_CLIENT);
                msg.replyTo = mGetReplyMessenger;
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {}
    };

    //接收从服务端回调的步数
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case Constant.MSG_FROM_SERVER:
                //更新步数
                step=msg.getData().getInt("step");
                text_step.setText(msg.getData().getInt("step") + "  步");
                //从数据库读取到用户输入的步长
                String str=sp.getString("step","0.65");
                Double strD=Double.parseDouble(str);
                //更新公里数
                String longS=String.valueOf((strD*msg.getData().getInt("step")/1000));
                if(longS.length()>6){
                    longS=longS.substring(0,6);
                }

                text_long.setText(longS+"  公里");
                //从数据库读取到用户输入的体重
                String st=sp.getString("weight","55");
                Double stD=Double.parseDouble(st);
                //更新消耗卡路里
                String kaluliS=String.valueOf((stD/2000)*msg.getData().getInt("step"));
                if(kaluliS.length()>5){
                    kaluliS=kaluliS.substring(0,4);
                }
                text_kaluli.setText(kaluliS+"  卡路里");
                delayHandler.sendEmptyMessageDelayed(Constant.REQUEST_SERVER, TIME_INTERVAL);
                break;
            case Constant.REQUEST_SERVER:
                try {
                    Message msgl = Message.obtain(null, Constant.MSG_FROM_CLIENT);
                    msgl.replyTo = mGetReplyMessenger;
                    messenger.send(msgl);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text_step = (TextView) findViewById(R.id.main_text_step);
        text_long= (TextView) findViewById(R.id.main_text_long);
        text_kaluli= (TextView) findViewById(R.id.main_text_kaluli);
        iv= (ImageView) findViewById(R.id.iv);
        sp=getSharedPreferences("data",MODE_PRIVATE);

        //设置目标
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText editText=new EditText(MainActivity.this);
                new AlertDialog.Builder(MainActivity.this).setTitle("请设置您的目标步数").setView(editText)
                        .setIcon(R.drawable.icon).setPositiveButton("设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(!TextUtils.isEmpty(editText.getText())){
                            sp.edit().putString("goal",editText.getText().toString().trim()).commit();
                            Toast.makeText(MainActivity.this,"设置成功",Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this,"未成功设置目标步数",Toast.LENGTH_SHORT).show();
                        }
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                }).show();
            }
        });
        delayHandler = new Handler(this);
    }
    @Override
    public void onStart() {
        super.onStart();
        setupService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String goal=sp.getString("goal","0");
        String message=null;
        if(goal.equals("0")){
            message="您还没有设置目标，快去设置每日目标吧";
        }else if (100<=(((double)step/(Integer.parseInt(goal))*100))){
            message="恭喜您，完成了目标的100%，真是个运动达人呢！";
        }else if(80<(((double)step/(Integer.parseInt(goal))*100))){
            message="恭喜您，完成了目标的"+(double)step/(Integer.parseInt(goal))*100+"%,离运动达人还有一步之遥喔！";
        }else if(50<(((double)step/(Integer.parseInt(goal)))*100)){
            message="恭喜您，完成了目标的"+(double)step/(Integer.parseInt(goal))*100+"%,离运动达人还有一段距离喔，加油！";
        }else {
            message="亲，你仅仅完成了目标的"+(double)step/(Integer.parseInt(goal))*100+"%,离运动达人的路还很长喔，行动起来吧！";
        }

        new AlertDialog.Builder(MainActivity.this).setTitle("今日走了"+step+"步")
                .setMessage(message).setPositiveButton("进入", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        }).show();
    }

    /**
     * 开启服务
     */
    private void setupService() {
        Intent intent = new Intent(this, StepService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        //取消服务绑定
        unbindService(conn);
        super.onDestroy();
    }
}

