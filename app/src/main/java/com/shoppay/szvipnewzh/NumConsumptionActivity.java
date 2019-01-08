package com.shoppay.szvipnewzh;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;
import com.loopj.android.http.RequestParams;
import com.shoppay.szvipnewzh.adapter.NumAdapter;
import com.shoppay.szvipnewzh.bean.NumShop;
import com.shoppay.szvipnewzh.bean.SystemQuanxian;
import com.shoppay.szvipnewzh.bean.VipInfo;
import com.shoppay.szvipnewzh.bean.VipInfoMsg;
import com.shoppay.szvipnewzh.bean.VipServece;
import com.shoppay.szvipnewzh.card.ReadCardOpt;
import com.shoppay.szvipnewzh.card.ReadCardOptHander;
import com.shoppay.szvipnewzh.db.DBAdapter;
import com.shoppay.szvipnewzh.http.InterfaceBack;
import com.shoppay.szvipnewzh.tools.ActivityStack;
import com.shoppay.szvipnewzh.tools.BluetoothUtil;
import com.shoppay.szvipnewzh.tools.CommonUtils;
import com.shoppay.szvipnewzh.tools.DateUtils;
import com.shoppay.szvipnewzh.tools.DayinUtils;
import com.shoppay.szvipnewzh.tools.DialogUtil;
import com.shoppay.szvipnewzh.tools.ESCUtil;
import com.shoppay.szvipnewzh.tools.LogUtils;
import com.shoppay.szvipnewzh.tools.MergeLinearArraysUtil;
import com.shoppay.szvipnewzh.tools.NullUtils;
import com.shoppay.szvipnewzh.tools.PreferenceHelper;
import com.shoppay.szvipnewzh.tools.ToastUtils;
import com.shoppay.szvipnewzh.tools.UrlTools;
import com.shoppay.szvipnewzh.wxcode.MipcaActivityCapture;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cz.msebera.android.httpclient.Header;

/**
 * Created by songxiaotao on 2017/6/30.
 */

public class NumConsumptionActivity extends Activity implements View.OnClickListener {
    private RelativeLayout rl_left, rl_jiesuan, rl_vipname;
    private EditText et_vipcard;
    private TextView tv_title, tv_num, tv_vipname,mVipTvKamcard;
    private Activity ac;
    private ListView listView;
    private String editString;
    private NumAdapter adapter;
    private List<VipServece> list;
    private NumchangeReceiver numchangeReceiver;
    private Dialog dialog;
    private DBAdapter dbAdapter;
    private int datalength = 0;
    private int shopnum = 0;
    private boolean isSuccess = false;
    private List<NumShop> numShopList = new ArrayList<>();
    private TextView vipTvJifen, vipTvVipyue, vipTvVipdengji;
    private String password = "";
    private RelativeLayout rl_right;
    private boolean isClick = true;
    private MyApplication app;
    private RelativeLayout rl_tvcard, rl_card;
    private TextView tv_tvcard;
    private boolean isVipcar = false;
    private SystemQuanxian sysquanxian;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    VipInfo info = (VipInfo) msg.obj;
                    tv_vipname.setText(info.getMemName());
                    vipTvVipyue.setText(info.getMemMoney());
                    vipTvJifen.setText(info.getMemPoint());
                    vipTvVipdengji.setText(info.getLevelName());
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
                    obtainVipServece();
                    break;
                case 2:
                    isSuccess = false;
                    tv_vipname.setText("");
                    vipTvVipyue.setText("");
                    vipTvJifen.setText("");
                    vipTvVipdengji.setText("");
                    mVipTvKamcard.setText("");
                    listView.setVisibility(View.GONE);
                    break;
            }
        }
    };
    private static final int CAMERA_PERMISSIONS_REQUEST_CODE = 0x03;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_numconsumption);
        ac = this;
        app = (MyApplication) getApplication();
        sysquanxian = app.getSysquanxian();
        ActivityStack.create().addActivity(NumConsumptionActivity.this);
        initView();
        dialog = DialogUtil.loadingDialog(NumConsumptionActivity.this, 1);
        dbAdapter = DBAdapter.getInstance(ac);
        dbAdapter.deleteNumShopCar();
        PreferenceHelper.write(ac, "shoppay", "memid", "123");
        PreferenceHelper.write(MyApplication.context, "shoppay", "viptoast", "未查询到会员");
        // 注册广播
        numchangeReceiver = new NumchangeReceiver();
        IntentFilter iiiff = new IntentFilter();
        iiiff.addAction("com.shoppay.wy.servecenumberchange");
        registerReceiver(numchangeReceiver, iiiff);
        et_vipcard.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                rl_vipname.setVisibility(View.VISIBLE);
                tv_vipname.setText("");
                if (delayRun != null) {
                    //每次editText有变化的时候，则移除上次发出的延迟线程
                    handler.removeCallbacks(delayRun);
                }
                editString = editable.toString();

                //延迟800ms，如果不再输入字符，则执行该线程的run方法
                handler.postDelayed(delayRun, 800);

            }
        });
    }

    /**
     * 延迟线程，看是否还有下一个字符输入
     */
    private Runnable delayRun = new Runnable() {

        @Override
        public void run() {
            //在这里调用服务器的接口，获取数据
            obtainVipInfo();
        }
    };

    private void obtainVipInfo() {
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

    private void obtainVipServece() {
        AsyncHttpClient client = new AsyncHttpClient();
        final PersistentCookieStore myCookieStore = new PersistentCookieStore(this);
        client.setCookieStore(myCookieStore);
        RequestParams params = new RequestParams();
        params.put("MemID", PreferenceHelper.readString(ac, "shoppay", "memid", "123"));
        LogUtils.d("xxparams", params.toString());
        String url = UrlTools.obtainUrl(ac, "?Source=3", "CountOrderListGet");
        LogUtils.d("xxurl", url);
        client.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    LogUtils.d("xxVipServeceS", new String(responseBody, "UTF-8"));
                    JSONObject jso = new JSONObject(new String(responseBody, "UTF-8"));
                    if (jso.getInt("flag") == 1) {
                        Gson gson = new Gson();
                        Type listType = new TypeToken<List<VipServece>>() {
                        }.getType();
                        list = gson.fromJson(jso.getString("vdata"), listType);
                        listView.setVisibility(View.VISIBLE);
                        adapter = new NumAdapter(ac, list);
                        listView.setAdapter(adapter);


                    } else {
                        listView.setVisibility(View.GONE);
                        Toast.makeText(ac, "未查询到项目", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    listView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                PreferenceHelper.write(ac, "shoppay", "memid", "123");
                listView.setVisibility(View.GONE);
            }
        });

    }

    private void initView() {
        rl_left = (RelativeLayout) findViewById(R.id.rl_left);
        rl_jiesuan = (RelativeLayout) findViewById(R.id.num_rl_jiesan);
        rl_vipname = (RelativeLayout) findViewById(R.id.num_rl_vipname);
        et_vipcard = (EditText) findViewById(R.id.num_et_card);
        tv_title = (TextView) findViewById(R.id.tv_title);
        tv_num = (TextView) findViewById(R.id.num_tv_num);
        vipTvVipdengji = (TextView) findViewById(R.id.vip_tv_vipdengji);
        vipTvVipyue = (TextView) findViewById(R.id.vip_tv_vipyue);
        vipTvJifen = (TextView) findViewById(R.id.vip_tv_jifen);
        mVipTvKamcard=findViewById(R.id.vip_tv_kamcard);
        tv_vipname = (TextView) findViewById(R.id.num_tv_vipname);
        listView = (ListView) findViewById(R.id.num_listview);


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
        tv_title.setText("计次消费");
        rl_right = (RelativeLayout) findViewById(R.id.rl_right);
        rl_right.setOnClickListener(this);
        rl_jiesuan.setOnClickListener(this);
        rl_left.setOnClickListener(this);
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
                        obtainVipInfo();
                    } else {
                        et_vipcard.setText(data.getStringExtra("codedata"));
                    }
                }
                break;

        }
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
            case R.id.num_rl_jiesan:
                if (tv_num.getText().toString().equals("0")) {
                    Toast.makeText(ac, "请选择消费项目",
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (CommonUtils.checkNet(ac)) {
//                        if (LoginActivity.sysquanxian.ispassword==1) {
//                            DialogUtil.pwdDialog( NumConsumptionActivity.this, 1, new InterfaceBack() {
//                                @Override
//                                public void onResponse(Object response) {
//                                    password=(String) response;
//                                    jiesuan();
//                                }
//
//                                @Override
//                                public void onErrorResponse(Object msg) {
//
//                                }
//                            });
//                        } else {
                        if (isClick) {
                            jiesuan();
                        }
//                        }
                    } else {
                        Toast.makeText(ac, "请检查网络是否可用",
                                Toast.LENGTH_SHORT).show();
                    }
                }
                break;

        }
    }

    private void jiesuan() {
        dialog.show();
        isClick = false;
        AsyncHttpClient client = new AsyncHttpClient();
        final PersistentCookieStore myCookieStore = new PersistentCookieStore(this);
        client.setCookieStore(myCookieStore);
        RequestParams params = new RequestParams();
        List<NumShop> listn = dbAdapter.getListNumShopCar(PreferenceHelper.readString(ac, "shoppay", "account", "123"));
        for (int i = 0; i < listn.size(); i++) {
            if (listn.get(i).count == 0) {
            } else {
                numShopList.add(listn.get(i));
            }
        }
        params.put("MemID", PreferenceHelper.readString(ac, "shoppay", "memid", "123"));
        params.put("OrderAccount", DateUtils.getCurrentTime("yyyyMMddHHmmss"));
        params.put("GlistCount", numShopList.size());//消费总次
//        params.put("count", datalength);
        for (int i = 0; i < numShopList.size(); i++) {
            params.put("Glist[" + i + "][GoodsID]", numShopList.get(i).CountDetailGoodsID);
            params.put("Glist[" + i + "][number]", numShopList.get(i).count);
        }
        LogUtils.d("xxparams", params.toString());
        String url = UrlTools.obtainUrl(ac, "?Source=3", "CountExpense");
        LogUtils.d("xxurl", url);
        client.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                dialog.dismiss();
                try {
                    Log.d("xxjiesuanS", new String(responseBody, "UTF-8"));
                    JSONObject jso = new JSONObject(new String(responseBody, "UTF-8"));
                    if (jso.getInt("flag") == 1) {
                        Toast.makeText(ac, jso.getString("msg"), Toast.LENGTH_LONG).show();
                        JSONObject jsonObject = (JSONObject) jso.getJSONArray("print").get(0);
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
                        isClick = true;
                        Toast.makeText(ac, jso.getString("msg"), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    dialog.dismiss();
                    isClick = true;
                    Toast.makeText(ac, "结算失败，请重新结算", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                dialog.dismiss();
                isClick = true;
                Toast.makeText(ac, "结算失败，请重新结算", Toast.LENGTH_SHORT).show();
            }
        });
    }


    public byte[] printReceipt_BlueTooth() {
        String danhao = "消费单号:" + PreferenceHelper.readString(ac, "shoppay", "OrderAccount", "");
        String huiyuankahao = "会员卡号:" + et_vipcard.getText().toString();
        String huiyuanming = "会员名称:" + tv_vipname.getText().toString();
        String xfnum = "消费次数:" + tv_num.getText().toString();

        String shopdetai = "服务名称    " + "次数    " + "剩余次数";
        try {
            byte[] next2Line = ESCUtil.nextLine(2);
            //            byte[] title = titleset.getBytes("gb2312");
            byte[] title = PreferenceHelper.readString(ac, "shoppay", "PrintTitle", "").getBytes("gb2312");
            byte[] bottom = PreferenceHelper.readString(ac, "shoppay", "PrintFootNote", "").getBytes("gb2312");
            byte[] tickname = "计次消费小票".getBytes("gb2312");
            byte[] ordernum = danhao.getBytes("gb2312");
            byte[] vipcardnum = huiyuankahao.getBytes("gb2312");
            byte[] vipname = huiyuanming.getBytes("gb2312");
            byte[] xiaofeinum = xfnum.getBytes("gb2312");
            byte[] xiahuaxian = "------------------------------".getBytes("gb2312");

            byte[] shoptitle = shopdetai.getBytes("gb2312");
            byte[] boldOn = ESCUtil.boldOn();
            byte[] fontSize2Big = ESCUtil.fontSizeSetBig(3);
            byte[] center = ESCUtil.alignCenter();
            byte[] Focus = "网 507".getBytes("gb2312");
            byte[] boldOff = ESCUtil.boldOff();
            byte[] fontSize2Small = ESCUtil.fontSizeSetSmall(3);
            byte[] left = ESCUtil.alignLeft();
            boldOn = ESCUtil.boldOn();
            byte[] fontSize1Big = ESCUtil.fontSizeSetBig(2);
            boldOff = ESCUtil.boldOff();
            byte[] fontSize1Small = ESCUtil.fontSizeSetSmall(2);
            next2Line = ESCUtil.nextLine(2);
            byte[] nextLine = ESCUtil.nextLine(1);
            nextLine = ESCUtil.nextLine(1);
            byte[] next4Line = ESCUtil.nextLine(4);
            byte[] breakPartial = ESCUtil.feedPaperCutPartial();
            byte[][] mytitle = {nextLine, center, boldOn, title, boldOff, next2Line, left, tickname, nextLine, left, ordernum, nextLine, left,
                    vipcardnum, nextLine,
                    left, vipname, nextLine, left, xiaofeinum, nextLine, xiahuaxian};

            byte[] headerBytes = ESCUtil.byteMerger(mytitle);
            List<byte[]> bytesList = new ArrayList<>();
            bytesList.add(headerBytes);
            //商品头
            byte[] sh = shopdetai.getBytes("gb2312");
            byte[][] mticket1 = {nextLine, left, sh};
            bytesList.add(ESCUtil.byteMerger(mticket1));
            //商品明细
            List<NumShop> list = dbAdapter.getListNumShopCar(PreferenceHelper.readString(ac, "shoppay", "account", "123"));
            for (NumShop numShop : list) {
                if (numShop.count == 0) {
                } else {
                    byte[] a = (numShop.shopname + "             " + numShop.count + "      " + (Integer.parseInt(numShop.allnum) - numShop.count) + "").getBytes("gb2312");
                    byte[][] mticket = {nextLine, left, a};
                    bytesList.add(ESCUtil.byteMerger(mticket));
                }
            }
            byte[][] mtickets = {nextLine, xiahuaxian};
            bytesList.add(ESCUtil.byteMerger(mtickets));
            byte[] ha = ("操作人员:" + PreferenceHelper.readString(ac, "shoppay", "UserName", "")).trim().getBytes("gb2312");
            byte[] time = ("消费时间:" + getStringDate()).trim().getBytes("gb2312");
            byte[] qianming = ("客户签名:").getBytes("gb2312");
            byte[][] footerBytes = {nextLine, left, ha, nextLine, left, time, nextLine, left, qianming, nextLine, left,
                    nextLine, left, nextLine, left, bottom, next2Line, next4Line, breakPartial};

            bytesList.add(ESCUtil.byteMerger(footerBytes));
            return MergeLinearArraysUtil.mergeLinearArrays(bytesList);

            //            bluetoothUtil.send(MergeLinearArraysUtil.mergeLinearArrays(bytesList));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getStringDate() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(currentTime);
        return dateString;
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
                    obtainVipInfo();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(numchangeReceiver);
        dbAdapter.deleteNumShopCar();
    }

    private class NumchangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("xx", "ShopChangeReceiver");
            List<NumShop> list = dbAdapter.getListNumShopCar(PreferenceHelper.readString(context, "shoppay", "account", "123"));
            shopnum = 0;
            datalength = 0;
            Gson gson = new Gson();
            for (NumShop numShop : list) {
                Log.d("xxx", gson.toJson(numShop));
                if (numShop.count == 0) {

                } else {
                    shopnum = shopnum + numShop.count;
                    datalength = datalength + 1;
                }

            }

            tv_num.setText(shopnum + "");

        }
    }


}
