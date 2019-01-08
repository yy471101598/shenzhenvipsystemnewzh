package com.shoppay.szvipnewzh;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;
import com.loopj.android.http.RequestParams;
import com.shoppay.szvipnewzh.adapter.RechargeAdapter;
import com.shoppay.szvipnewzh.bean.SystemQuanxian;
import com.shoppay.szvipnewzh.bean.VipInfo;
import com.shoppay.szvipnewzh.bean.VipInfoMsg;
import com.shoppay.szvipnewzh.bean.VipRecharge;
import com.shoppay.szvipnewzh.card.ReadCardOpt;
import com.shoppay.szvipnewzh.card.ReadCardOptHander;
import com.shoppay.szvipnewzh.http.InterfaceBack;
import com.shoppay.szvipnewzh.tools.ActivityStack;
import com.shoppay.szvipnewzh.tools.BluetoothUtil;
import com.shoppay.szvipnewzh.tools.CommonUtils;
import com.shoppay.szvipnewzh.tools.DateUtils;
import com.shoppay.szvipnewzh.tools.DayinUtils;
import com.shoppay.szvipnewzh.tools.DialogUtil;
import com.shoppay.szvipnewzh.tools.LogUtils;
import com.shoppay.szvipnewzh.tools.NoDoubleClickListener;
import com.shoppay.szvipnewzh.tools.NullUtils;
import com.shoppay.szvipnewzh.tools.PreferenceHelper;
import com.shoppay.szvipnewzh.tools.ToastUtils;
import com.shoppay.szvipnewzh.tools.UrlTools;
import com.shoppay.szvipnewzh.view.MyGridViews;
import com.shoppay.szvipnewzh.wxcode.MipcaActivityCapture;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

import cz.msebera.android.httpclient.Header;

/**
 * Created by songxiaotao on 2017/6/30.
 */

public class VipRechargeActivity extends Activity implements View.OnClickListener {
    private RelativeLayout rl_left, rl_rechage;
    private EditText et_vipcard, et_money;
    private TextView tv_title, tv_vipname, tv_vipyue, tv_jifen, tv_dengji, mVipTvKamcard;
    private MyGridViews myGridViews;
    private Activity ac;
    private String state = "现金";
    private String editString;
    private Dialog dialog;
    private Dialog paydialog;
    private RechargeAdapter adapter;
    private VipRecharge recharge;
    private boolean isSuccess = false;
    private boolean isMoney = true, isWx = false, isZhifubao = false, isYinlian = false;
    private RadioButton rb_money, rb_wx, rb_zhifubao, rb_isYinlian;
    private static final int CAMERA_PERMISSIONS_REQUEST_CODE = 0x03;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    VipInfo info = (VipInfo) msg.obj;
                    tv_vipname.setText(info.getMemName());
                    tv_vipyue.setText(info.getMemMoney());
                    tv_jifen.setText(info.getMemPoint());
                    tv_dengji.setText(info.getLevelName());
                    mVipTvKamcard.setText(NullUtils.noNullHandle(info.MemCardNumber).toString());
                    try {
                        if (isVipcar) {
                            new ReadCardOptHander().overReadCard();
                        } else {
                            new ReadCardOpt().overReadCard();
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    PreferenceHelper.write(ac, "shoppay", "memid", info.getMemID());
                    PreferenceHelper.write(ac, "shoppay", "vipcar", et_vipcard.getText().toString());
                    PreferenceHelper.write(ac, "shoppay", "Discount", info.getDiscount());
                    PreferenceHelper.write(ac, "shoppay", "DiscountPoint", info.getDiscountPoint());
                    PreferenceHelper.write(ac, "shoppay", "jifen", info.getMemPoint());
                    isSuccess = true;
                    break;
                case 2:
                    tv_vipname.setText("");
                    tv_vipyue.setText("");
                    tv_jifen.setText("");
                    tv_dengji.setText("");
                    mVipTvKamcard.setText("");
                    isSuccess = false;
                    PreferenceHelper.write(ac, "shoppay", "memid", "123");
                    PreferenceHelper.write(ac, "shoppay", "vipcar", "123");
                    break;
            }
        }
    };
    private RadioGroup mRadiogroup;
    private RelativeLayout rl_right;
    private String orderAccount;
    private SystemQuanxian sysquanxian;
    private MyApplication app;
    private RelativeLayout rl_tvcard, rl_card;
    private TextView tv_tvcard;
    private boolean isVipcar = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viprecharge);
        ac = this;
        app = (MyApplication) getApplication();
        sysquanxian = app.getSysquanxian();
        dialog = DialogUtil.loadingDialog(VipRechargeActivity.this, 1);
        paydialog = DialogUtil.payloadingDialog(VipRechargeActivity.this, 1);
        PreferenceHelper.write(MyApplication.context, "shoppay", "viptoast", "未查询到会员");
        ActivityStack.create().addActivity(VipRechargeActivity.this);
        initView();
//        obtainVipRecharge();
        mRadiogroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i) {
                    case R.id.rb_money:
                        isMoney = true;
                        isWx = false;
                        isYinlian = false;
                        isZhifubao = false;
                        break;
                    case R.id.rb_zhifubao:
                        isZhifubao = true;
                        isMoney = false;
                        isWx = false;
                        isYinlian = false;
                        break;
                    case R.id.rb_yinlian:
                        isYinlian = true;
                        isMoney = false;
                        isWx = false;
                        isZhifubao = false;
                        break;
                    case R.id.rb_wx:
                        isWx = true;
                        isMoney = false;
                        isYinlian = false;
                        isZhifubao = false;
                        break;
                }
            }
        });
        et_vipcard.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (delayRun != null) {
                    //每次editText有变化的时候，则移除上次发出的延迟线程
                    handler.removeCallbacks(delayRun);
                }
                editString = editable.toString();

                //延迟800ms，如果不再输入字符，则执行该线程的run方法

                handler.postDelayed(delayRun, 800);
            }
        });


//        PreferenceHelper.write(getApplicationContext(), "PayOk", "time", "false");
//        //动态注册广播接收器
//        msgReceiver = new MsgReceiver();
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction("com.example.communication.RECEIVER");
//        registerReceiver(msgReceiver, intentFilter);
    }

    /**
     * 延迟线程，看是否还有下一个字符输入
     */
    private Runnable delayRun = new Runnable() {

        @Override
        public void run() {
            //在这里调用服务器的接口，获取数据
            ontainVipInfo();
        }
    };

    private void ontainVipInfo() {
        if (isVipcar) {
            dialog.show();
        }
        AsyncHttpClient client = new AsyncHttpClient();
        final PersistentCookieStore myCookieStore = new PersistentCookieStore(this);
        client.setCookieStore(myCookieStore);
        RequestParams params = new RequestParams();
        params.put("MemCard", editString);
        LogUtils.d("xxparams", params.toString());
        String url = UrlTools.obtainUrl(ac, "?Source=3", "GetMem");
        LogUtils.d("xxurl", url);
        client.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    if (isVipcar) {
                        dialog.dismiss();
                    }
                    LogUtils.d("xxVipinfoS", new String(responseBody, "UTF-8"));
                    JSONObject jso = new JSONObject(new String(responseBody, "UTF-8"));
                    if (jso.getInt("flag") == 1) {
                        Gson gson = new Gson();
                        VipInfoMsg infomsg = gson.fromJson(new String(responseBody, "UTF-8"), VipInfoMsg.class);
                        Message msg = handler.obtainMessage();
                        msg.what = 1;
                        msg.obj = infomsg.getVdata().get(0);
                        handler.sendMessage(msg);
                    } else {
                        Message msg = handler.obtainMessage();
                        msg.what = 2;
                        handler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    if (isVipcar) {
                        dialog.dismiss();
                    }
                    Message msg = handler.obtainMessage();
                    msg.what = 2;
                    handler.sendMessage(msg);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                if (isVipcar) {
                    dialog.dismiss();
                }
                Message msg = handler.obtainMessage();
                msg.what = 2;
                handler.sendMessage(msg);
            }
        });
    }


    private void initView() {
        rl_left = (RelativeLayout) findViewById(R.id.rl_left);
        rl_rechage = (RelativeLayout) findViewById(R.id.viprecharge_rl_recharge);
        et_vipcard = (EditText) findViewById(R.id.viprecharge_et_cardnum);
        et_money = (EditText) findViewById(R.id.et_money);
        tv_title = (TextView) findViewById(R.id.tv_title);
        tv_vipname = (TextView) findViewById(R.id.viprecharge_et_name);
        tv_vipyue = (TextView) findViewById(R.id.viprecharge_et_yue);
        tv_jifen = (TextView) findViewById(R.id.viprecharge_et_jifen);
        tv_dengji = (TextView) findViewById(R.id.viprecharge_et_dengji);
        myGridViews = (MyGridViews) findViewById(R.id.gridview);
        mVipTvKamcard = findViewById(R.id.vip_tv_kamcard);
        mRadiogroup = (RadioGroup) findViewById(R.id.radiogroup);
        tv_title.setText("会员充值");
        rl_right = (RelativeLayout) findViewById(R.id.rl_right);
        rl_right.setOnClickListener(this);
        rl_tvcard = findViewById(R.id.rl_tvcard);
        tv_tvcard = findViewById(R.id.tv_tvcard);
        rl_card = findViewById(R.id.rl_etcard);
        if (Integer.parseInt(NullUtils.noNullHandle(sysquanxian.isvipcard).toString()) == 0) {
            rl_tvcard.setVisibility(View.GONE);
            rl_card.setVisibility(View.VISIBLE);
            isVipcar = false;
        } else {
            rl_tvcard.setVisibility(View.VISIBLE);
            rl_card.setVisibility(View.GONE);
            isVipcar = true;
        }
        rb_isYinlian = (RadioButton) findViewById(R.id.rb_yinlian);
        rb_money = (RadioButton) findViewById(R.id.rb_money);
        rb_zhifubao = (RadioButton) findViewById(R.id.rb_zhifubao);
        rb_wx = (RadioButton) findViewById(R.id.rb_wx);
        if (sysquanxian.isweixin == 0) {
            rb_wx.setVisibility(View.GONE);
        }
        if (sysquanxian.iszhifubao == 0) {
            rb_zhifubao.setVisibility(View.GONE);
        }
        if (sysquanxian.isyinlian == 0) {
            rb_isYinlian.setVisibility(View.GONE);
        }
        if (sysquanxian.isxianjin == 0) {
            rb_money.setVisibility(View.GONE);
        }


        rl_left.setOnClickListener(this);
        rl_rechage.setOnClickListener(new NoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View view) {
                if (et_vipcard.getText().toString().equals("")
                        || et_vipcard.getText().toString() == null) {
                    Toast.makeText(getApplicationContext(), "请输入会员卡号",
                            Toast.LENGTH_SHORT).show();
                } else if (et_money.getText().toString() == null || et_money.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(), "请输入充值金额",
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (isSuccess) {
                        if (CommonUtils.checkNet(getApplicationContext())) {
                            if (isWx) {
                                if (sysquanxian.iswxpay == 0) {
                                    Intent mipca = new Intent(ac, MipcaActivityCapture.class);
                                    mipca.putExtra("type", "pay");
                                    startActivityForResult(mipca, 222);
                                } else {
                                    vipRecharge(DateUtils.getCurrentTime("yyyyMMddHHmmss"));
                                }
                            } else if (isZhifubao) {
                                if (sysquanxian.iszfbpay == 0) {
                                    Intent mipca = new Intent(ac, MipcaActivityCapture.class);
                                    mipca.putExtra("type", "pay");
                                    startActivityForResult(mipca, 222);
                                } else {
                                    vipRecharge(DateUtils.getCurrentTime("yyyyMMddHHmmss"));
                                }
                            } else {
                                vipRecharge(DateUtils.getCurrentTime("yyyyMMddHHmmss"));
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "请检查网络是否可用",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MyApplication.context, PreferenceHelper.readString(MyApplication.context, "shoppay", "viptoast", "未查询到会员"), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 111:
                if (resultCode == RESULT_OK) {
                    if (isVipcar) {
                        tv_tvcard.setText(data.getStringExtra("codedata"));
                        editString = data.getStringExtra("codedata");
                        ontainVipInfo();
                    } else {
                        et_vipcard.setText(data.getStringExtra("codedata"));
                    }
                }
                break;
            case 222:
                if (resultCode == RESULT_OK) {
                    pay(data.getStringExtra("codedata"));
                }
                break;
        }
    }

    private void pay(String codedata) {
        paydialog.show();
        AsyncHttpClient client = new AsyncHttpClient();
        final PersistentCookieStore myCookieStore = new PersistentCookieStore(this);
        client.setCookieStore(myCookieStore);
        RequestParams map = new RequestParams();
        map.put("auth_code", codedata);
        map.put("UserID", PreferenceHelper.readString(ac, "shoppay", "UserID", ""));
//        （1会员充值7商品消费9快速消费11会员充次）
        map.put("ordertype", 1);
        orderAccount = DateUtils.getCurrentTime("yyyyMMddHHmmss");
        map.put("account", orderAccount);
        map.put("money", et_money.getText().toString());
//        0=现金 1=银联 2=微信 3=支付宝
        if (isMoney) {
            map.put("payType", 0);
        } else if (isWx) {
            map.put("payType", 2);
        } else if (isYinlian) {
            map.put("payType", 1);
        } else {
            map.put("payType", 3);
        }
        client.setTimeout(120 * 1000);
        LogUtils.d("xxparams", map.toString());
        String url = UrlTools.obtainUrl(ac, "?Source=3", "PayOnLine");
        LogUtils.d("xxurl", url);
        client.post(url, map, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    paydialog.dismiss();
                    LogUtils.d("xxpayS", new String(responseBody, "UTF-8"));
                    JSONObject jso = new JSONObject(new String(responseBody, "UTF-8"));
                    if (jso.getInt("flag") == 1) {

                        JSONObject jsonObject = (JSONObject) jso.getJSONArray("print").get(0);
                        DayinUtils.dayin(jsonObject.getString("printContent"));
                        if (jsonObject.getInt("printNumber") == 0) {
                        } else {
                            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                            if (bluetoothAdapter.isEnabled()) {
                                BluetoothUtil.connectBlueTooth(MyApplication.context);
                                BluetoothUtil.sendData(DayinUtils.dayin(jsonObject.getString("printContent")), jsonObject.getInt("printNumber"));
                            } else {
                            }
                        }
                        vipRecharge(orderAccount);
                    } else {
                        Toast.makeText(ac, jso.getString("msg"), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    paydialog.dismiss();
                    Toast.makeText(ac, "支付失败，请稍后再试", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                paydialog.dismiss();
                Toast.makeText(ac, "支付失败，请稍后再试", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rl_right:
                if (ContextCompat.checkSelfPermission(ac, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                    if (ActivityCompat.shouldShowRequestPermissionRationale(ac, Manifest.permission.CAMERA)) {
                        ToastUtils.showToast(ac, "您已经拒绝过一次");
                    }
                    ActivityCompat.requestPermissions(ac, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSIONS_REQUEST_CODE);
                } else {//有权限直接调用系统相机拍照
                    Intent mipca = new Intent(ac, MipcaActivityCapture.class);
                    startActivityForResult(mipca, 111);
                }
                break;
            case R.id.rl_left:
                finish();
                break;
        }
    }


    private void vipRecharge(String ordernum) {
        dialog.show();
        AsyncHttpClient client = new AsyncHttpClient();
        final PersistentCookieStore myCookieStore = new PersistentCookieStore(this);
        client.setCookieStore(myCookieStore);
        RequestParams map = new RequestParams();
        map.put("MemID", PreferenceHelper.readString(ac, "shoppay", "memid", ""));
        map.put("rechargeAccount", ordernum);
        map.put("money", et_money.getText().toString());
//        0=现金 1=银联 2=微信 3=支付宝
        if (isMoney) {
            map.put("payType", 0);
        } else if (isWx) {
            map.put("payType", 2);
        } else if (isYinlian) {
            map.put("payType", 1);
        } else {
            map.put("payType", 3);
        }

        LogUtils.d("xxparams", map.toString());
        String url = UrlTools.obtainUrl(ac, "?Source=3", "MemRechargeMoney");
        LogUtils.d("xxurl", url);
        client.post(url, map, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    dialog.dismiss();
                    LogUtils.d("xxviprechargeS", new String(responseBody, "UTF-8"));
                    JSONObject jso = new JSONObject(new String(responseBody, "UTF-8"));
                    if (jso.getInt("flag") == 1) {

                        Toast.makeText(ac, jso.getString("msg"), Toast.LENGTH_LONG).show();
                        JSONObject jsonObject = (JSONObject) jso.getJSONArray("print").get(0);
                        DayinUtils.dayin(jsonObject.getString("printContent"));
                        if (jsonObject.getInt("printNumber") == 0) {
                            finish();
                        } else {
                            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                            if (bluetoothAdapter.isEnabled()) {
                                BluetoothUtil.connectBlueTooth(MyApplication.context);
                                BluetoothUtil.sendData(DayinUtils.dayin(jsonObject.getString("printContent")), jsonObject.getInt("printNumber"));
                                finish();
                            } else {
                                finish();
                            }
                        }
                    } else {
                        Toast.makeText(ac, jso.getString("msg"), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(ac, "会员充值失败，请重新登录", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                dialog.dismiss();
                Toast.makeText(ac, "会员充值失败，请重新登录", Toast.LENGTH_SHORT).show();
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isVipcar) {
            new ReadCardOptHander(new InterfaceBack() {
                @Override
                public void onResponse(Object response) {
                    tv_tvcard.setText(response.toString());
                    editString = tv_tvcard.getText().toString();
                    ontainVipInfo();
                }

                @Override
                public void onErrorResponse(Object msg) {

                }
            });
        } else {
            new ReadCardOpt(et_vipcard);
        }
    }

    @Override
    protected void onStop() {
        try {
            if (isVipcar) {
                new ReadCardOptHander().overReadCard();
            } else {
                new ReadCardOpt().overReadCard();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        super.onStop();
        if (delayRun != null) {
            //每次editText有变化的时候，则移除上次发出的延迟线程
            handler.removeCallbacks(delayRun);
        }
    }


    public static String getStringDate() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(currentTime);
        return dateString;
    }

    /**
     * 广播接收器
     *
     * @author len
     */
    public class MsgReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //拿到进度，更新UI
//            String state = intent.getStringExtra("success");
//            Log.d("MsgReceiver", "MsgReceiver" + state);
//            if (state == null || state.equals("")) {
//
//            } else {
//                if (state.equals("success")) {
//                    weixinDialog.dismiss();
//                     vipRecharge();
//                } else {
//                    String msg = intent.getStringExtra("msg");
//                    Toast.makeText(ac,msg,Toast.LENGTH_SHORT).show();
//
//                }
//            }
        }

    }

    @Override
    protected void onDestroy() {
        // TODO 自动生成的方法存根
        super.onDestroy();
//        if (intent != null) {
//
//            stopService(intent);
//        }
//
//        //关闭闹钟机制启动service
//        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
//        int anHour =2 * 1000; // 这是一小时的毫秒数 60 * 60 * 1000
//        long triggerAtTime = SystemClock.elapsedRealtime() + anHour;
//        Intent i = new Intent(this, AlarmReceiver.class);
//        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
//        manager.cancel(pi);
//        //注销广播
//        unregisterReceiver(msgReceiver);
    }
}
