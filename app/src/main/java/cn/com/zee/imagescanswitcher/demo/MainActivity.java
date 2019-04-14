package cn.com.zee.imagescanswitcher.demo;

import androidx.appcompat.app.AppCompatActivity;
import cn.com.zee.imagescanswitcher.library.ImageScanSwitcher;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageScanSwitcher imageScanSwitcher = findViewById(R.id.switcher);
        imageScanSwitcher.setData(new int[]{R.mipmap.pic0,R.mipmap.pic1,R.mipmap.pic2,R.mipmap.pic3});
        imageScanSwitcher.start();
    }
}
