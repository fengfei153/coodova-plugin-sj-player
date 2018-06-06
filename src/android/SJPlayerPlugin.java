package com.fhsjdz.cordova.player;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.alivc.player.AliVcMediaPlayer;
import com.alivc.player.MediaPlayer;

import android.Manifest;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SJPlayerPlugin extends CordovaPlugin {

	private static String TAG = "SJPlayerPlugin";
	
	private JSONArray requestArgs;
    private CallbackContext callbackContext = null;
    private LinearLayout wrapLayout = null;
    private RelativeLayout contentLayout = null;
    private SurfaceView mSurfaceView = null;
    private MainActivity mainActivity;
    private TextView textView = null;
    private ViewGroup webViewContainer = null;
    private long lastClickTime = 0L;//上次点击时间
    private boolean isHorizontal = false;//是否横屏
    
    public AliVcMediaPlayer mPlayer = null;
    public String url = null;
    private String [] permissions = { Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE };

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        //判断是否显示播放器界面
        if (action.equals("playVideo")) {
        	if(!hasPermisssion()) {
    			this.requestArgs = args;
    			PermissionHelper.requestPermissions(this, 0, permissions);
            } else {
            	playVideo(args, callbackContext);
            }
        }else if(action.equals("destroy")){
            //销毁
        	destroy();
        }else{
        	callbackContext.error("no such method: " + action);
    		return false;
        }
        return true;
    }

    private void playVideo(JSONArray args, CallbackContext callbackContext){
    	mainActivity = (MainActivity) cordova.getActivity();
   	 	try {
//   	 		ApplicationInfo appInfo = mainActivity.getPackageManager().getApplicationInfo(mainActivity.getPackageName(), PackageManager.GET_META_DATA);  
//   	 		final String accessKeyId = appInfo.metaData.getString("accessKeyId"); 
//   	 		final String accessKeySecret = appInfo.metaData.getString("accessKeySecret"); 
//   	 		final String businessId = appInfo.metaData.getString("businessId");

            AliVcMediaPlayer.init(mainActivity.getApplicationContext());
            JSONObject jsonObject = args.getJSONObject(0);
            url = jsonObject.getString("url");
            final int videoTop = jsonObject.getInt("top");
            //videoWidth = jsonObject.getInt("width");
            //videoHeight = jsonObject.getInt("height");
            //显示或者隐藏
            try {
        		mainActivity.runOnUiThread(new Runnable(){
                    @Override
                    public void run(){
                    	if(mPlayer == null){
                    		createView(videoTop);
                    	}else{
                    		mPlayer.releaseVideoSurface();
                        	mPlayer.stop();
                            mPlayer.destroy();
                            mPlayer = null;
                            startToPlay();
                    	}
                    }
                });
            }catch (Exception e){
                callbackContext.error(-1);
            }
        } catch (Throwable t){
            callbackContext.error(-1);
        }
    }
    
    private void destroy(){
    	if (mainActivity != null){
            mainActivity.runOnUiThread(new Runnable(){
                @Override
                public void run(){
                    if (mPlayer != null){
                    	mPlayer.releaseVideoSurface();
                    	mPlayer.stop();
                        mPlayer.destroy();
                        mPlayer = null;
                    }
                    if (mSurfaceView != null){
                        mSurfaceView.setVisibility(View.GONE);
                        mSurfaceView = null ;
                    }
                    if (wrapLayout != null){
                    	wrapLayout.setVisibility(View.GONE);
                        webViewContainer.removeView(wrapLayout);
                        wrapLayout = null ;
                    }
                }
            });
        }
    }

    private void createView(final int top){
    	wrapLayout = new LinearLayout(mainActivity);
    	contentLayout = new RelativeLayout(mainActivity);
    	
    	RelativeLayout.LayoutParams playParam = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        //设置放在父控件中间
        //playParam.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        //创建一个videoView==============用surfaceview代替
        mSurfaceView = new SurfaceView(mainActivity);
        mSurfaceView.setZOrderOnTop(false);
        contentLayout.addView(mSurfaceView, playParam);

        //显示正在加载
        textView = new TextView(mainActivity);
        textView.setTextSize(20);
        textView.setTextColor(Color.WHITE);
        RelativeLayout.LayoutParams msgParam = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        //设置放在父控件中间
        msgParam.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        contentLayout.addView(textView, msgParam);
        
        Point deviceSize = new Point();
        mainActivity.getWindowManager().getDefaultDisplay().getSize(deviceSize);
        LinearLayout.LayoutParams contentParam = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, deviceSize.x*3/4);
        contentParam.setMargins(0, top, 0, 0);
    	wrapLayout.addView(contentLayout, contentParam);
    	
    	webViewContainer = (ViewGroup) webView.getView().getParent();
    	webViewContainer.addView(wrapLayout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @SuppressWarnings("deprecation")
            public void surfaceCreated(SurfaceHolder holder) {
            	holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
                holder.setKeepScreenOn(true);
                // 重点:
                if (mPlayer != null) {
                    // 对于从后台切换到前台,需要重设surface;部分手机锁屏也会做前后台切换的处理
                    mPlayer.setVideoSurface(mSurfaceView.getHolder().getSurface());
                }
            }
            public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
                if (mPlayer != null)
                    mPlayer.setSurfaceChanged();
            }
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (mPlayer != null) {
                    mPlayer.releaseVideoSurface();
                }
            }
        });
        //双击视屏全屏
        mSurfaceView.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				mainActivity.runOnUiThread(new Runnable(){
	                @Override
	                public void run(){
	                	if(System.currentTimeMillis() - lastClickTime < 500){
	                		wrapLayout.removeView(contentLayout);
		    		        if(isHorizontal){
		    		        	Point deviceSize = new Point();
		    		            mainActivity.getWindowManager().getDefaultDisplay().getSize(deviceSize);
		    		            LinearLayout.LayoutParams contentParam = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, deviceSize.x*3/4);
		    		            contentParam.setMargins(0, top, 0, 0);
		    		        	wrapLayout.addView(contentLayout, contentParam);
			    				mPlayer.setRenderRotate(MediaPlayer.VideoRotate.ROTATE_0);
		    		        }else{
		    		        	wrapLayout.addView(contentLayout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
			    				mPlayer.setRenderRotate(MediaPlayer.VideoRotate.ROTATE_90);
		    		        }
		    		        isHorizontal = !isHorizontal;
	                	}
	                	lastClickTime = System.currentTimeMillis();
	                }
	            });
			}
        });
        startToPlay();
    }

    private boolean startToPlay(){
    	try {
            if (mPlayer == null){
                // 初始化播放器
                mPlayer = new AliVcMediaPlayer(mainActivity, mSurfaceView);            
                //mPlayer.setVideoScalingMode(VideoScalingMode.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                mPlayer.setPreparedListener(new MyPreparedListener());//播放器就绪事件
                mPlayer.setFrameInfoListener(new MyFrameInfoListener());
                mPlayer.setErrorListener(new MyErrorListener());	 	//异常错误事件
                mPlayer.setInfoListener(new MyInfolistener());		//信息状态监听事件 
                mPlayer.setCompletedListener(new MyPlayerCompletedListener());//播放结束事件
                mPlayer.setSeekCompleteListener(new MySeekCompleteListener());
                mPlayer.setStoppedListener(new MyStoppedListener());//播放停止事件
                mPlayer.setDefaultDecoder(0);		// 如果同时支持软解和硬解是有用
                //mPlayer.enableNativeLog();//打开底层日志。备注：仅在开发阶段调用此方法
            }
            textView.setVisibility(View.VISIBLE);
        	textView.setText(getStringValue("willPlay"));
            //设置播放的url; rtmp://live.hkstv.hk.lxdns.com/live/hks
            mPlayer.prepareAndPlay(url);
        }catch (Exception e){
            callbackContext.error(e.getMessage().toString());
        }
        return true;
    }

    /**
     * 准备完成监听器:调度更新进度
     */
    private class MyPreparedListener implements MediaPlayer.MediaPlayerPreparedListener {
        @Override
        public void onPrepared() {
            textView.setVisibility(View.GONE);
        }
    }
    
    private class MyErrorListener implements MediaPlayer.MediaPlayerErrorListener{
        @Override
        public void onError(int i, String s) {
        	textView.setText("播放失败：" + s);
        }
    }
    
    private class MyStoppedListener implements MediaPlayer.MediaPlayerStoppedListener {
        @Override
        public void onStopped() {
        }
    }

    /**
     * 信息通知监听器:重点是缓存开始/结束
     */
    private class MyInfolistener implements MediaPlayer.MediaPlayerInfoListener {

        public void onInfo(int what, int extra) {
            switch (what) {
                case MediaPlayer.MEDIA_INFO_UNKNOW:
                    break;
                case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                    break;
                case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                	textView.setVisibility(View.GONE);
                    break;
                case MediaPlayer.MEDIA_INFO_NETWORK_ERROR:
                	textView.setText(getStringValue("MEDIA_INFO_NETWORK_ERROR"));
                    break;
            }
        }
    }
    
    private static class MyFrameInfoListener implements com.alivc.player.MediaPlayer.MediaPlayerFrameInfoListener {
        @Override
        public void onFrameInfoListener() {
        }
    }
    
    private static class MyPlayerCompletedListener implements MediaPlayer.MediaPlayerCompletedListener {
        @Override
        public void onCompleted() {
        }
    }
    
    private static class MySeekCompleteListener implements MediaPlayer.MediaPlayerSeekCompleteListener {
        @Override
        public void onSeekCompleted() {
        }
    }
    
    private String getStringValue(String key){
    	//return (String) mainActivity.getResources().getString(R.string.app_name); 
    	//初始化
    	try {
    		int id = cordova.getActivity().getResources().getIdentifier(key, "string", cordova.getActivity().getPackageName());
            return cordova.getActivity().getString(id) ;
		} catch (Exception e) {
			return "" ;
		}
    }
    /**
     * check application's permissions
     */
	@Override
	public boolean hasPermisssion() {
		for (String p : permissions) {
			if (!PermissionHelper.hasPermission(this, p)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * processes the result of permission request
	 *
	 * @param requestCode
	 *            The code to get request action
	 * @param permissions
	 *            The collection of permissions
	 * @param grantResults
	 *            The result of grant
	 */
	@Override
	public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults)
			throws JSONException {
		PluginResult result;
		for (int r : grantResults) {
			if (r == PackageManager.PERMISSION_DENIED) {
				Log.d(TAG, "Permission Denied!");
				this.callbackContext.success(1);
				return;
			}
		}
		switch (requestCode) {
		case 0:
			playVideo(this.requestArgs, this.callbackContext);
			break;
		}
	}
}
