package lee.snakegame;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.SoundPool;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.lang.ref.WeakReference;
import java.util.Random;

public class GameActivity extends AppCompatActivity implements RewardedVideoAdListener {

    Canvas canvas;
    SnakeView snakeView;

    Bitmap headBitmap;
    Bitmap bodyBitmap;
    Bitmap tailBitmap;
    Bitmap appleBitmap;

    private SoundPool soundpool;
    int sample1 = -1;
    int sample2 = -1;
    int sample3 = -1;
    int sample4 = -1;

    int directionOfTravel=0;

    int screenWidth;
    int screenHeight;
    int topGap;

    long lastFrameTIme;
    int fps;
    int score;
    int hi;

    int [] snakeX;
    int [] snakeY;
    int snakeLength;
    int appleX;
    int appleY;

    int blockSize;
    int numBlocksWide;
    int numBlocksHigh;

    private RewardedVideoAd mRewardedVideoAd;

    private void loadRewardedVideoAd() {
        mRewardedVideoAd.loadAd("ca-app-pub-3847525926087017/2846575255",
                new AdRequest.Builder().build());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_game);


// Use an activity context to get the rewarded video instance.
        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this);
        mRewardedVideoAd.setRewardedVideoAdListener(this);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;
        boolean isImmersiveModeEnabled = ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions);
        if (isImmersiveModeEnabled) {
            Log.i("Is on?", "Turning immersive mode mode off. ");
        } else {
            Log.i("Is on?", "Turning immersive mode mode on.");
        }

        newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(newUiOptions);

        loadSound();
        configureDisplay();
        snakeView = new SnakeView(this);
        setContentView(snakeView);

    }

    @Override
    public void onRewardedVideoAdLoaded() {

    }

    @Override
    public void onRewardedVideoAdOpened() {

    }

    @Override
    public void onRewardedVideoStarted() {

    }

    @Override
    public void onRewardedVideoAdClosed() {

    }

    @Override
    public void onRewarded(RewardItem rewardItem) {

    }

    @Override
    public void onRewardedVideoAdLeftApplication() {

    }

    @Override
    public void onRewardedVideoAdFailedToLoad(int i) {

    }

    @Override
    public void onRewardedVideoCompleted() {

    }

    class SnakeView extends SurfaceView implements Runnable{

        Thread ourThread = null;
        SurfaceHolder ourHolder;
        volatile boolean playingSanke;
        Paint paint;

        public SnakeView(Context context) {
            super(context);

            ourHolder = getHolder();
            paint = new Paint();

            snakeX= new int[200];
            snakeY= new int[200];

            getSnake();
            getApple();

        }

        public void getSnake(){
            snakeLength=3;
            snakeX[0] = numBlocksWide /2;
            snakeY[0] = numBlocksHigh/2;

            snakeX[1] = snakeX[0]-1;
            snakeY[1] = snakeY[0];

            snakeX[1] = snakeX[1]-1;
            snakeY[1] = snakeY[0];

        }

        public void getApple(){
            Random random = new Random();
            appleX = random.nextInt(numBlocksWide-1)+1;
            appleY = random.nextInt(numBlocksHigh-1)+1;
        }

        @Override
        public void run() {
            while(playingSanke){
                updateGame();
                drawGame();
                controlFPS();
            }
        }

        public void updateGame(){
            if(snakeX[0]==appleX&&snakeY[0]==appleY){
                snakeLength++;
                getApple();
                score = score + snakeLength;
                soundpool.play(sample1,1,1,0,0,1);
            }

            for(int i=snakeLength;i>0;i--){
                snakeX[i] = snakeX[i-1];
                snakeY[i] = snakeY[i-1];
            }

            switch (directionOfTravel){
                case 0:
                snakeY[0] --;
                break;

                case 1:
                    snakeX[0] ++;
                    break;
                case 2:
                    snakeY[0] ++;
                    break;
                case 3:
                    snakeX[0] --;
                    break;
            }

            boolean dead = false;

            if(snakeX[0] ==-1||snakeX[0]>=numBlocksWide||snakeY[0]==-1||snakeY[0]==numBlocksHigh){
                dead=true;
            }

            for(int i=snakeLength-1;i>0;i--){
                if((i>4)&&(snakeX[0]==snakeX[i])&&(snakeY[0]==snakeY[i])){
                    dead = true;
                }
            }

            if(dead){
                soundpool.play(sample4,1,1,0,0,1);
                hi = score;
                loadRewardedVideoAd();
                score = 0;
                getSnake();
            }

        }

        public void drawGame(){
            if(ourHolder.getSurface().isValid()){
                canvas = ourHolder.lockCanvas();

                canvas.drawColor(Color.BLACK);
                paint.setColor(Color.argb(255,255,255,255));
                paint.setTextSize(topGap/2);
                canvas.drawText("Score:"+score+" Hi:"+hi,10,topGap-6,paint);

                paint.setStrokeWidth(3);
                canvas.drawLine(1,topGap,screenWidth-1,topGap,paint);
                canvas.drawLine(screenWidth-1,topGap,screenWidth-1,topGap+(numBlocksHigh*blockSize),paint);
                canvas.drawLine(screenWidth-1,topGap+(numBlocksHigh*blockSize),1,topGap+(numBlocksHigh*blockSize),paint);
                canvas.drawLine(1,topGap,1,topGap+(numBlocksHigh*blockSize),paint);

                canvas.drawBitmap(headBitmap,snakeX[0]*blockSize,(snakeY[0]*blockSize)+topGap,paint);

                for(int i=1;i<snakeLength-1;i++){
                    canvas.drawBitmap(bodyBitmap,snakeX[i]*blockSize,(snakeY[i]*blockSize)+topGap,paint);
                }

                canvas.drawBitmap(tailBitmap,snakeX[snakeLength-1]*blockSize,(snakeY[0]*blockSize)+topGap,paint);

                canvas.drawBitmap(appleBitmap,appleX*blockSize,(appleY*blockSize)+topGap,paint);

                ourHolder.unlockCanvasAndPost(canvas);
            }
        }

        public void controlFPS(){
            long timeThisFrame = (System.currentTimeMillis()-lastFrameTIme);
            long timeToSleep = 100-timeThisFrame;
            if(timeThisFrame>0){
                fps=(int)(1000/timeThisFrame);
            }
            if(timeToSleep>0){
                try {
                    ourThread.sleep(timeToSleep);

                }catch (Exception e){

                }
            }
            lastFrameTIme = System.currentTimeMillis();
        }

        public void pause(){
            playingSanke = false;
            try {
                ourThread.join();
            }catch (Exception e){

            }
        }

        public void resume(){
            playingSanke = true;
            ourThread = new Thread(this);
            ourThread.start();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        switch (motionEvent.getAction()&MotionEvent.ACTION_MASK){
            case MotionEvent.ACTION_UP:
                if(motionEvent.getX()>=screenWidth/2){
                    directionOfTravel++;
                    if(directionOfTravel==4){
                        directionOfTravel=0;
                    }
                }else{
                    directionOfTravel--;
                    if(directionOfTravel==-1){
                        directionOfTravel=3;
                    }
                }

        }
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();

        while(true){
            snakeView.pause();
            break;
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        snakeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        snakeView.pause();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(keyCode== KeyEvent.KEYCODE_BACK){
            snakeView.pause();

            Intent i = new Intent(this,MainActivity.class);
            startActivity(i);
            finish();
            return true;
        }
        return false;
    }

    public void loadSound(){
        soundpool = new SoundPool(10, AudioManager.STREAM_MUSIC,0);
        try {
            AssetManager assetManager = getAssets();
            AssetFileDescriptor descriptor;

            descriptor = assetManager.openFd("sample1.ogg");
            sample1 = soundpool.load(descriptor,0);

            descriptor = assetManager.openFd("sample2.ogg");
            sample2 = soundpool.load(descriptor,0);
            descriptor = assetManager.openFd("sample3.ogg");
            sample3 = soundpool.load(descriptor,0);
            descriptor = assetManager.openFd("sample4.ogg");
            sample4 = soundpool.load(descriptor,0);

        }catch (Exception e){

        }
    }

    public void configureDisplay(){
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();

        display.getSize(size);

        screenWidth = size.x;
        screenHeight = size.y;
        topGap = screenHeight/14;
        blockSize = screenWidth/40;

        numBlocksWide = 40;
        numBlocksHigh = ((screenHeight - topGap))/blockSize;

        headBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.head);
        bodyBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.body);
        tailBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.tail);
        appleBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.apple);

        headBitmap = Bitmap.createScaledBitmap(headBitmap,blockSize,blockSize,false);
        bodyBitmap = Bitmap.createScaledBitmap(bodyBitmap,blockSize,blockSize,false);
        tailBitmap = Bitmap.createScaledBitmap(tailBitmap,blockSize,blockSize,false);
        appleBitmap = Bitmap.createScaledBitmap(appleBitmap,blockSize,blockSize,false);

    }
}
