package in.raveesh.pacemaker.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import in.raveesh.pacemaker.Pacemaker;
import in.raveesh.pacemaker.R;

public class MainActivity extends Activity {

    boolean beginOrStop = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button set = (Button)findViewById(R.id.begin);
        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!beginOrStop) {
                    Pacemaker.scheduleLinear(MainActivity.this, 5);
                    set.setText(R.string.stop);
                }
                else{
                    Pacemaker.cancelLinear(MainActivity.this, 5);
                    set.setText(R.string.begin);
                }
                beginOrStop = !beginOrStop;
            }
        });
    }
}
