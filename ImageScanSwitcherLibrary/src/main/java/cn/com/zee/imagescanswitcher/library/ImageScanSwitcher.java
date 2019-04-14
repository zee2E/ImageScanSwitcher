package cn.com.zee.imagescanswitcher.library;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
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
    private final int MAX_CACHE_PIC_NUM = 7;
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

    private void setData(int[] resIds){
        this.resIds = resIds;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(!isRunning){
            start();
        }else{
            if(isPaused){
                drawView(canvas);
            }
        }
        super.onDraw(canvas);
    }

    private void start() {
        if(mHandler != null){
            if(mUpdateRunnable != null){
                mHandler.removeCallbacks(mUpdateRunnable);
            }else{
                mUpdateRunnable = new UpdateRunnable();
            }
            mHandler.post(mUpdateRunnable);
        }
    }

    public class UpdateRunnable implements Runnable{

        @Override
        public void run() {
            if(mHandler != null){
                mHandler.removeCallbacks(this);
            }
            //TODO
        }
    }

    private void drawView(Canvas canvas){
        drawLeft(canvas);
        drawLine(canvas);
        drawRight(canvas);
    }

    private void drawLeft(Canvas canvas) {
        if(currentX == 0){
            return;
        }
        int leftIndex = firstVisibleIndex + 1;
        if(!bitmapCache.containsKey(leftIndex)){
            loadBitmap(leftIndex);
        }else{
            updateLastUsedTime(leftIndex);
        }
        Rect drawSrcRect = new Rect(screenWidth - currentX,0,screenWidth,screenHeight);
        Rect drawDstRect = new Rect(0,0,currentX,screenHeight);
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
            loadBitmap(rightIndex);
        }else{
            updateLastUsedTime(rightIndex);
        }
        Rect drawSrcRect = new Rect(0,0,screenWidth - currentX,screenHeight);
        Rect drawDstRect = new Rect(currentX,0,screenWidth,screenHeight);
        canvas.drawBitmap(bitmapCache.get(rightIndex).getBitmap(),drawSrcRect, drawDstRect,bitmapPaint);
    }

    //cache start
    private void updateLastUsedTime(int index) {
        bitmapCache.get(index).setLastUseTimestamp(System.currentTimeMillis());
    }

    private void loadBitmap(int index) {
        if(bitmapCache.size() > CACHE_LIMIT){
            dropOldestCache();
        }else{
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resIds[index]);
            bitmapCache.put(index, new BitmapData(System.currentTimeMillis(), bitmap));
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
