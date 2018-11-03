package net.sourceforge.simcpux;


import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.tencent.mm.opensdk.constants.Build;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.json.JSONException;
import org.json.JSONObject;

public class PayActivity extends Activity {
	
	private IWXAPI api;
	private Handler handler = new Handler();
	Button appayBtn;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pay);
		
		api = WXAPIFactory.createWXAPI(this, "wxb4ba3c02aa476ea1");

		appayBtn = (Button) findViewById(R.id.appay_btn);
		appayBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				appayBtn = (Button) findViewById(R.id.appay_btn);
				appayBtn.setEnabled(false);
				Toast.makeText(PayActivity.this, "获取订单中...", Toast.LENGTH_SHORT).show();
				payThread.start();
			}
		});		
		Button checkPayBtn = (Button) findViewById(R.id.check_pay_btn);
		checkPayBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				boolean isPaySupported = api.getWXAppSupportAPI() >= Build.PAY_SUPPORTED_SDK_INT;
				Toast.makeText(PayActivity.this, String.valueOf(isPaySupported), Toast.LENGTH_SHORT).show();
			}
		});
	}

	Thread payThread = new Thread(new Runnable() {
		@Override
		public void run() {
			String url = "https://wxpay.wxutil.com/pub_v2/app/app_pay.php";
			try{
				byte[] buf = Util.httpGet(url);
				if (buf != null && buf.length > 0) {
					String content = new String(buf);
					Log.e("get server pay params:",content);
					final JSONObject json = new JSONObject(content);
					if(null != json && !json.has("retcode") ){
						PayReq req = new PayReq();
						//req.appId = "wxf8b4f85f3a794e77";  // 测试用appId
						req.appId			= json.getString("appid");
						req.partnerId		= json.getString("partnerid");
						req.prepayId		= json.getString("prepayid");
						req.nonceStr		= json.getString("noncestr");
						req.timeStamp		= json.getString("timestamp");
						req.packageValue	= json.getString("package");
						req.sign			= json.getString("sign");
						req.extData			= "app data"; // optional
						handler.post(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(PayActivity.this, "正常调起支付", Toast.LENGTH_SHORT).show();
							}
						});
						// 在支付之前，如果应用没有注册到微信，应该先调用IWXMsg.registerApp将应用注册到微信
						api.sendReq(req);
					}else{
						Log.d("PAY_GET", "返回错误"+json.getString("retmsg"));
						handler.post(new Runnable() {
							@Override
							public void run() {
								try {
									Toast.makeText(PayActivity.this, "返回错误"+json.getString("retmsg"), Toast.LENGTH_SHORT).show();
								} catch (JSONException e) {
									e.printStackTrace();
								}
							}
						});
					}
				}else{
					Log.d("PAY_GET", "服务器请求错误");
					handler.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(PayActivity.this, "服务器请求错误", Toast.LENGTH_SHORT).show();
						}
					});
				}
			}catch(final Exception e){
				Log.e("PAY_GET", "异常："+e.getMessage());
				handler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(PayActivity.this, "异常："+e.getMessage(), Toast.LENGTH_SHORT).show();
					}
				});
			}
			handler.post(new Runnable() {
				@Override
				public void run() {
					appayBtn.setEnabled(true);
				}
			});
		}
	});
}
