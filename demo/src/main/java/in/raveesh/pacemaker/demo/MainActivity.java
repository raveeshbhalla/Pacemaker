package in.raveesh.pacemaker.demo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import in.raveesh.pacemaker.Pacemaker;
import in.raveesh.pacemaker.R;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.begin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NonNull View v) {
                Pacemaker.scheduleLinear(MainActivity.this, 5);
            }
        });
        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NonNull View v) {
                Pacemaker.cancelLinear(MainActivity.this);
            }
        });

    }
}
