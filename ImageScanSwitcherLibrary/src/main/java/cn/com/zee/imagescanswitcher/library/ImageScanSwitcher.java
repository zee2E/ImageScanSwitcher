package cn.com.zee.imagescanswitcher.library;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import androidx.annotation.Nullable;

/**
 * Created by Zeekey on 2019/4/13.
 */
public class ImageScanSwitcher extends View {

    private static final int CACHE_LIMIT = 10;
    private static final int STEP = 10;
    private final int MAX_CACHE_PIC_NUM = 7;

    private final Object statusLock = new Object();
    private final Object loadLock = new Object();
    private int currentX = 0;
    private int screenWidth;
    private int screenHeight;
    private Paint linePaint;
    private Paint bitmapPaint;

    private boolean isRunning = false;
    private boolean isPaused = false;
    private int[] resIds;
    private int firstVisibleIndex = 0;
    private ConcurrentHashMap<Integer, BitmapData> bitmapCache = new ConcurrentHashMap<>();
    private Handler mHandler;
    private UpdateRunnable mUpdateRunnable;
    private Handler mAsyncHandler;
    private HandlerThread thread;
    private LoadBitmapRunnable mLoadBitmapRunnable;
    private boolean isLoading = false;


    public ImageScanSwitcher(Context context) {
        super(context);
        init(context);
    }

    public ImageScanSwitcher(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ImageScanSwitcher(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        mHandler = new Handler();
        initPaint();
    }

    private void initPaint() {
        linePaint = new Paint();
        linePaint.setColor(Color.WHITE);
        linePaint.setAntiAlias(true);
        bitmapPaint = new Paint();
        bitmapPaint.setAntiAlias(true);
    }

    public void setData(int[] resIds){
        this.resIds = resIds;
        loadBitmap(0,true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        synchronized (statusLock) {
            if (!isRunning) {
                start();
            } else {
                if (!isPaused) {
                    drawView(canvas);
                }
            }
        }
        super.onDraw(canvas);
    }

    public void start() {
        synchronized (statusLock){
            if(mHandler != null){
                if(mUpdateRunnable != null){
                    mHandler.removeCallbacks(mUpdateRunnable);
                }else{
                    mUpdateRunnable = new UpdateRunnable();
                }
                isRunning = true;
                mHandler.post(mUpdateRunnable);
            }
        }

    }


    public synchronized void resume() {
        synchronized (statusLock) {
            isPaused = false;
        }
    }

    public synchronized void pause() {
        synchronized (statusLock) {
            isPaused = true;
        }
    }

    public class UpdateRunnable implements Runnable{

        @Override
        public void run() {
            if(mHandler != null){
                mHandler.removeCallbacks(this);
            }
            synchronized (statusLock) {
                if (!isPaused) {
                    currentX+=STEP;
                    if(currentX >= screenWidth){
                        currentX = 0;
                        firstVisibleIndex = nextIndex(firstVisibleIndex);
                    }
                    postInvalidate();
                    if (mHandler != null) {
                        if (mUpdateRunnable != null) {
                            mHandler.postDelayed(mUpdateRunnable, 10L);
                        }
                    }
                } else {
                    if (mHandler != null) {
                        if (mUpdateRunnable != null) {
                            mHandler.postDelayed(mUpdateRunnable, 200L);
                        }
                    }
                }
            }
        }
    }

    private void drawView(Canvas canvas){
        if(canvas != null) {
            drawLeft(canvas);
            drawLine(canvas);
            drawRight(canvas);
        }
    }

    private void drawLeft(Canvas canvas) {
        if(currentX == 0){
            return;
        }
        int leftIndex = nextIndex(firstVisibleIndex);

        if(!bitmapCache.containsKey(leftIndex)){
            synchronized (loadLock){
                loadBitmap(leftIndex, true);
            }
        }else{
            updateLastUsedTime(leftIndex);
        }
        if(!bitmapCache.containsKey(nextIndex(leftIndex))){
            loadBitmap(nextIndex(leftIndex), false);
        }
        Rect drawSrcRect = new Rect(screenWidth - currentX,0,screenWidth,screenHeight);
        Rect drawDstRect = new Rect();
        getGlobalVisibleRect(drawDstRect);
        drawDstRect.right = drawDstRect.left + currentX;
        int height = drawDstRect.height();
        drawDstRect.top = 0;
        drawDstRect.bottom = height;
        canvas.drawBitmap(bitmapCache.get(leftIndex).getBitmap(),drawSrcRect, drawDstRect,bitmapPaint);
    }

    private void drawLine(Canvas canvas) {
        canvas.drawLine(currentX,0,currentX,screenHeight,linePaint);
    }

    private void drawRight(Canvas canvas) {
        if(currentX == screenWidth){
            return;
        }
        int rightIndex = firstVisibleIndex;
        if(!bitmapCache.containsKey(rightIndex)){
            synchronized (loadLock){
                loadBitmap(rightIndex, true);
            }
        }else{
            updateLastUsedTime(rightIndex);
        }
        if(!bitmapCache.containsKey(nextIndex(rightIndex))){
            loadBitmap(nextIndex(rightIndex), false);
        }
        Rect drawSrcRect = new Rect(0,0,screenWidth - currentX,screenHeight);
        Rect drawDstRect = new Rect();
        getGlobalVisibleRect(drawDstRect);
        drawDstRect.left = currentX + drawDstRect.left;
        int height = drawDstRect.height();
        drawDstRect.top = 0;
        drawDstRect.bottom = height;
        canvas.drawBitmap(bitmapCache.get(rightIndex).getBitmap(),drawSrcRect, drawDstRect,bitmapPaint);
    }



    private int nextIndex(int index) {
        if(index >= resIds.length - 1){
            index = 0;
        }else{
            index ++;
        }
        return index;
    }

    private int previousIndex(int index) {
        if(index <= 0){
            index = resIds.length - 1;
        }else{
            index --;
        }
        return index;
    }





    //cache start
    private void updateLastUsedTime(int index) {
        bitmapCache.get(index).setLastUseTimestamp(System.currentTimeMillis());
    }

    private void loadBitmap(int index, boolean isSync) {

        if(isSync){
            Log.i("zzz", "zzz load :" + index + " isSync:" + isSync);
            loadBitmapExe(index);
        }else{
            if(!isLoading){
                Log.i("zzz", "zzz load :" + index + " isSync:" + isSync);
                isLoading = true;
                if(mAsyncHandler == null){
                    initAsyncHandler();
                }
                if(mAsyncHandler != null){
                    if(mLoadBitmapRunnable != null){
                        mAsyncHandler.removeCallbacks(mLoadBitmapRunnable);
                    }else{
                        mLoadBitmapRunnable = new LoadBitmapRunnable();
                    }
                    mLoadBitmapRunnable.setIndex(index);
                    mAsyncHandler.post(mLoadBitmapRunnable);
                }
            }

        }
    }

    private class LoadBitmapRunnable implements Runnable{

        private int index;

        @Override
        public void run() {
            if(mAsyncHandler != null){
                mAsyncHandler.removeCallbacks(this);
            }
            synchronized (loadLock) {
                loadBitmapExe(index);
            }
            isLoading = false;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }

    private void loadBitmapExe(int index){
        if(bitmapCache.size() > CACHE_LIMIT){
            dropOldestCache();
        }else{
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resIds[index]);
            if(bitmap != null){
                bitmapCache.put(index, new BitmapData(System.currentTimeMillis(), bitmap));
            }
        }

    }

    private void dropOldestCache() {
        Iterator<Map.Entry<Integer,BitmapData>> it = bitmapCache.entrySet().iterator();
        int oldestKey = -1;
        long tempOldestUsedTime = -1;
        while(it.hasNext()){
            Map.Entry<Integer,BitmapData> entry=it.next();
            int key = entry.getKey();
            BitmapData value = entry.getValue();
            if(tempOldestUsedTime == -1){
                tempOldestUsedTime = value.getLastUseTimestamp();
            }else{
                if(tempOldestUsedTime > value.getLastUseTimestamp()){
                    tempOldestUsedTime = value.getLastUseTimestamp();
                    oldestKey = key;
                }else{
                    continue;
                }
            }
        }
        if(oldestKey != -1) {
            Bitmap oldBitmap = bitmapCache.get(oldestKey).getBitmap();
            if(oldBitmap != null && !oldBitmap.isRecycled()){
                oldBitmap.recycle();
            }
            bitmapCache.remove(oldestKey);
        }
    }

    private void initAsyncHandler() {
        thread = new HandlerThread("loadBitmap");
        thread.start();
        mAsyncHandler = new Handler(thread.getLooper());
    }
    //cache end

    private class BitmapData{
        private Bitmap bitmap;
        private long lastUseTimestamp;

        public BitmapData(long lastUseTimestamp, Bitmap bitmap) {
            this.lastUseTimestamp = lastUseTimestamp;
            this.bitmap = bitmap;
        }

        public Bitmap getBitmap() {
            return bitmap;
        }

        public void setBitmap(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        public long getLastUseTimestamp() {
            return lastUseTimestamp;
        }

        public void setLastUseTimestamp(long lastUseTimestamp) {
            this.lastUseTimestamp = lastUseTimestamp;
        }
    }
}
