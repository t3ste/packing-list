package com.t3ste.packinglist;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(FileLinkPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
